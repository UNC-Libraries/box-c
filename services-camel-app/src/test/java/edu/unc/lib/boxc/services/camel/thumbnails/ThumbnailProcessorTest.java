package edu.unc.lib.boxc.services.camel.thumbnails;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import org.apache.jena.rdf.model.Resource;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingService;
import edu.unc.lib.boxc.operations.jms.thumbnail.ThumbnailRequest;
import edu.unc.lib.boxc.operations.jms.thumbnail.ThumbnailRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.ProcessorTestHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.jdom2.Document;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.framework;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import org.junit.jupiter.api.Assertions;

/**
 * @author snluong
 */
public class ThumbnailProcessorTest {
    private ThumbnailRequestProcessor processor;
    private PID filePid;
    private PID workPid;
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private Exchange exchange;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private IndexingMessageSender indexingMessageSender;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private RepositoryObjectFactory repositoryObjectFactory;

    @Before
    public void init() throws IOException {
        MockitoAnnotations.initMocks(this);
        processor = new ThumbnailRequestProcessor();
        processor.setAclService(accessControlService);
        processor.setIndexingMessageSender(indexingMessageSender);
        processor.setRepositoryObjectLoader(repositoryObjectLoader);
        processor.setRepositoryObjectFactory(repositoryObjectFactory);
        filePid = ProcessorTestHelper.makePid();
        workPid = ProcessorTestHelper.makePid();
        exchange = createRequestExchange();
    }

    @Test
    public void testUpdateThumbnail() throws Exception {
        var file = mock(FileObject.class);
        var resource = mock(Resource.class);
        when(file.getPid()).thenReturn(filePid);
        var parentWork = mock(WorkObject.class);
        when(file.getParent()).thenReturn(parentWork);
        when(file.getResource()).thenReturn(resource);
        when(parentWork.getPid()).thenReturn(workPid);
        when(repositoryObjectLoader.getFileObject(filePid)).thenReturn(file);

        processor.process(exchange);
        verify(repositoryObjectFactory).createExclusiveRelationship(eq(parentWork), eq(Cdr.useAsThumbnail), eq(file.getResource()));
        assertIndexingMessageSent();
    }

    @Test
    public void insufficientPermissionsTest() {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(any(), any(PID.class), any(), eq(Permission.editDescription));

            processor.process(exchange);
        });
    }

    private void assertIndexingMessageSent() {
        verify(indexingMessageSender).sendIndexingOperation(agent.getUsername(), workPid,
                IndexingActionType.UPDATE_DATASTREAMS);
    }

    private Exchange createRequestExchange() throws IOException {
        var request = new ThumbnailRequest();
        request.setAgent(agent);
        request.setFilePidString(filePid.toString());
        return ProcessorTestHelper.mockExchange(ThumbnailRequestSerializationHelper.toJson(request));
    }
}

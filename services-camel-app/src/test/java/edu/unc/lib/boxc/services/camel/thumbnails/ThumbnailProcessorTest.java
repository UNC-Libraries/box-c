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
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.thumbnail.ThumbnailRequest;
import edu.unc.lib.boxc.operations.jms.thumbnail.ThumbnailRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.ProcessorTestHelper;
import org.apache.camel.Exchange;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import org.junit.jupiter.api.Assertions;

/**
 * @author snluong
 */
public class ThumbnailProcessorTest {
    private ThumbnailRequestProcessor processor;
    private PID filePid;
    private PID workPid;
    private Resource resource;
    private WorkObject parentWork;
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private AutoCloseable closeable;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private IndexingMessageSender indexingMessageSender;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private RepositoryObjectFactory repositoryObjectFactory;
    @Captor
    private ArgumentCaptor<Object> fileResourceCaptor;

    @Before
    public void init() throws IOException {
        closeable = openMocks(this);
        processor = new ThumbnailRequestProcessor();
        processor.setAclService(accessControlService);
        processor.setIndexingMessageSender(indexingMessageSender);
        processor.setRepositoryObjectLoader(repositoryObjectLoader);
        processor.setRepositoryObjectFactory(repositoryObjectFactory);
        filePid = ProcessorTestHelper.makePid();
        workPid = ProcessorTestHelper.makePid();
        resource = mock(Resource.class);
        parentWork = mock(WorkObject.class);
        
        var file = mock(FileObject.class);
        when(file.getPid()).thenReturn(filePid);
        when(file.getParent()).thenReturn(parentWork);
        when(file.getResource()).thenReturn(resource);
        when(parentWork.getPid()).thenReturn(workPid);
        when(repositoryObjectLoader.getFileObject(filePid)).thenReturn(file);
    }
    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testUpdateThumbnail() throws Exception {
        var exchange = createRequestExchange(ThumbnailRequest.ASSIGN);

        processor.process(exchange);
        verify(repositoryObjectFactory).createExclusiveRelationship(eq(parentWork), eq(Cdr.useAsThumbnail), fileResourceCaptor.capture());
        // check to see the right file was related to the work
        assertEquals(resource, fileResourceCaptor.getValue());
        assertIndexingMessageSent();
    }

    @Test
    public void insufficientPermissionsTest() throws IOException {
        var exchange = createRequestExchange(ThumbnailRequest.ASSIGN);
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(any(), any(PID.class), any(), eq(Permission.editDescription));

            processor.process(exchange);
        });
    }

    @Test
    public void testDeleteAssignedThumbnail() throws IOException {
        var exchange = createRequestExchange(ThumbnailRequest.DELETE);

        processor.process(exchange);
        verify(repositoryObjectFactory).deleteProperty(eq(parentWork), eq(Cdr.useAsThumbnail));
        assertIndexingMessageSent();
    }

    private void assertIndexingMessageSent() {
        verify(indexingMessageSender).sendIndexingOperation(agent.getUsername(), workPid,
                IndexingActionType.UPDATE_DATASTREAMS);
    }

    private Exchange createRequestExchange(String action) throws IOException {
        var request = new ThumbnailRequest();
        request.setAgent(agent);
        request.setFilePidString(filePid.toString());
        request.setAction(action);
        return ProcessorTestHelper.mockExchange(ThumbnailRequestSerializationHelper.toJson(request));
    }
}

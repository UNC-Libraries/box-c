package edu.unc.lib.boxc.services.camel.thumbnails;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
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

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.framework;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        processor = new ThumbnailRequestProcessor();
        processor.setAclService(accessControlService);
        processor.setIndexingMessageSender(indexingMessageSender);
        processor.setRepositoryObjectLoader(repositoryObjectLoader);
        processor.setRepositoryObjectFactory(repositoryObjectFactory);

        exchange = createRequestExchange();
        filePid = ProcessorTestHelper.makePid();
        workPid = ProcessorTestHelper.makePid();

        when(accessControlService.hasAccess(any(), any(), eq(Permission.editDescription))).thenReturn(true);
    }

    @Test
    public void testUpdateThumbnail() throws Exception {
        var file = mock(FileObject.class);
        when(file.getPid()).thenReturn(filePid);
        var parentWork = mock(WorkObject.class);
        when(file.getParent()).thenReturn(parentWork);
        when(parentWork.getPid()).thenReturn(workPid);
        when(repositoryObjectLoader.getRepositoryObject(filePid)).thenReturn(file);

        processor.process(exchange);
        assertIndexingMessageSent();
    }

    private void assertIndexingMessageSent() {
        verify(indexingMessageSender).sendIndexingOperation(agent.getUsername(), workPid,
                IndexingActionType.UPDATE_DATASTREAMS);
    }

    private void assertIndexingMessageNotSent() {
        verify(indexingMessageSender, never()).sendIndexingOperation(agent.getUsername(), workPid,
                IndexingActionType.UPDATE_DATASTREAMS);
    }

    private Exchange createRequestExchange() throws IOException {
        var request = new ThumbnailRequest();
        request.setAgent(agent);
        request.setFilePid(filePid);
        return ProcessorTestHelper.mockExchange(ThumbnailRequestSerializationHelper.toJson(request));
    }
}

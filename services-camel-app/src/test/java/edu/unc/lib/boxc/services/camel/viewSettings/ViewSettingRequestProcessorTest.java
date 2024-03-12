package edu.unc.lib.boxc.services.camel.viewSettings;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.CdrView;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.viewSettings.ViewSettingRequest;
import edu.unc.lib.boxc.operations.jms.viewSettings.ViewSettingRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.ProcessorTestHelper;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author snluong
 */
public class ViewSettingRequestProcessorTest {
    private ViewSettingRequestProcessor processor;
    private WorkObject workObject;
    private PID workPid;
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private AutoCloseable closeable;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private RepositoryObjectFactory repositoryObjectFactory;
    @Mock
    private IndexingMessageSender indexingMessageSender;

    @BeforeEach
    public void init() throws IOException {
        closeable = openMocks(this);
        processor = new ViewSettingRequestProcessor();
        processor.setAccessControlService(accessControlService);
        processor.setRepositoryObjectLoader(repositoryObjectLoader);
        processor.setRepositoryObjectFactory(repositoryObjectFactory);
        processor.setIndexingMessageSender(indexingMessageSender);
        workPid = ProcessorTestHelper.makePid();
        workObject = mock(WorkObject.class);
        when(workObject.getPid()).thenReturn(workPid);
        when(repositoryObjectLoader.getRepositoryObject(workPid)).thenReturn(workObject);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testUpdateViewBehaviorNoPermission() throws Exception {
        var exchange = createRequestExchange(ViewSettingRequest.ViewBehavior.PAGED);

        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            doThrow(new AccessRestrictionException()).when(accessControlService)
                    .assertHasAccess(any(), any(PID.class), any(), eq(Permission.editViewSettings));
            processor.process(exchange);
        });
    }

    @Test
    public void testUpdateViewBehaviorSuccess() throws Exception {
        var exchange = createRequestExchange(ViewSettingRequest.ViewBehavior.PAGED);
        processor.process(exchange);

        verify(repositoryObjectFactory).createExclusiveRelationship(
                eq(workObject), eq(CdrView.viewBehavior),
                eq(ViewSettingRequest.ViewBehavior.PAGED.getString()));
        verify(indexingMessageSender).sendIndexingOperation(agent.getUsername(), workPid,
                IndexingActionType.UPDATE_VIEW_BEHAVIOR);
    }

    @Test
    public void testDeleteViewBehaviorSuccess() throws Exception {
        var exchange = createRequestExchange(null);
        processor.process(exchange);
        verify(repositoryObjectFactory).deleteProperty(eq(workObject), eq(CdrView.viewBehavior));
        verify(indexingMessageSender).sendIndexingOperation(agent.getUsername(), workPid,
                IndexingActionType.UPDATE_VIEW_BEHAVIOR);
    }

    private Exchange createRequestExchange(ViewSettingRequest.ViewBehavior viewBehavior) throws IOException {
        var request = new ViewSettingRequest();
        request.setAgent(agent);
        request.setObjectPidString(workPid.toString());
        request.setViewBehavior(viewBehavior);
        return ProcessorTestHelper.mockExchange(ViewSettingRequestSerializationHelper.toJson(request));
    }
}

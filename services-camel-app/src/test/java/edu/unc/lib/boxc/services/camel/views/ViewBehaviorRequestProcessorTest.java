package edu.unc.lib.boxc.services.camel.views;

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
import edu.unc.lib.boxc.operations.jms.views.ViewBehaviorRequest;
import edu.unc.lib.boxc.operations.jms.views.ViewBehaviorRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.ProcessorTestHelper;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.junit.Assert.assertEquals;

/**
 * @author snluong
 */
public class ViewBehaviorRequestProcessorTest {
    private ViewBehaviorRequestProcessor processor;
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
    @Captor
    private ArgumentCaptor<ViewBehaviorRequest.ViewBehavior> viewBehaviorCaptor;

    @BeforeEach
    public void init() throws IOException {
        closeable = openMocks(this);
        processor = new ViewBehaviorRequestProcessor();
        processor.setAccessControlService(accessControlService);
        processor.setRepositoryObjectLoader(repositoryObjectLoader);
        processor.setRepositoryObjectFactory(repositoryObjectFactory);
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
        var exchange = createRequestExchange(ViewBehaviorRequest.ViewBehavior.PAGED);

        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            doThrow(new AccessRestrictionException()).when(accessControlService)
                    .assertHasAccess(any(), any(PID.class), any(), eq(Permission.ingest));
                    processor.process(exchange);
        });
    }

    @Test
    public void testUpdateViewBehaviorSuccess() throws Exception {
        var exchange = createRequestExchange(ViewBehaviorRequest.ViewBehavior.PAGED);
        processor.process(exchange);

        verify(repositoryObjectFactory).createExclusiveRelationship(eq(workObject), eq(CdrView.viewBehavior), viewBehaviorCaptor.capture());
        Assertions.assertEquals(ViewBehaviorRequest.ViewBehavior.PAGED, viewBehaviorCaptor.getValue());
    }

    @Test
    public void testDeleteViewBehaviorSuccess() throws Exception {
        var exchange = createRequestExchange(null);
        processor.process(exchange);
        verify(repositoryObjectFactory).deleteProperty(eq(workObject), eq(CdrView.viewBehavior));
    }

    private Exchange createRequestExchange(ViewBehaviorRequest.ViewBehavior viewBehavior) throws IOException {
        var request = new ViewBehaviorRequest();
        request.setAgent(agent);
        request.setObjectPidString(workPid.toString());
        request.setViewBehavior(viewBehavior);
        return ProcessorTestHelper.mockExchange(ViewBehaviorRequestSerializationHelper.toJson(request));
    }
}

package edu.unc.lib.boxc.web.access.controllers;

import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.web.common.services.FedoraContentService;
import edu.unc.lib.boxc.web.common.utils.AnalyticsTrackerUtil;
import org.eclipse.jetty.io.EofException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author bbpennel
 */
public class FedoraContentControllerTest {
    protected MockMvc mvc;
    private AutoCloseable closeable;
    private FedoraContentController controller;
    @Mock
    private FedoraContentService fedoraContentService;
    @Mock
    private AnalyticsTrackerUtil analyticsTracker;
    @Mock
    private RepositoryObjectLoader repoObjLoader;
    @Mock
    private AccessControlService accessControlService;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);
        TestHelper.setContentBase("http://localhost:48085/rest");

        controller = new FedoraContentController();
        controller.setFedoraContentService(fedoraContentService);
        controller.setAnalyticsTracker(analyticsTracker);
        controller.setRepositoryObjectLoader(repoObjLoader);
        controller.setAccessControlService(accessControlService);

        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    public void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void getContentEofExceptionTest() throws Exception {
        PID pid = TestHelper.makePid();
        doThrow(new EofException((String) null))
                .when(fedoraContentService)
                .streamData(any(), any(), anyBoolean(), any());
        mvc.perform(get("/content/" + pid.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void getContentWrappedEofExceptionTest() throws Exception {
        PID pid = TestHelper.makePid();
        doThrow(new IOException(new EofException((String) null)))
                .when(fedoraContentService)
                .streamData(any(), any(), anyBoolean(), any());
        mvc.perform(get("/content/" + pid.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void getContentWrappedTimeoutExceptionTest() throws Exception {
        PID pid = TestHelper.makePid();
        doThrow(new IOException(new TimeoutException()))
                .when(fedoraContentService)
                .streamData(any(), any(), anyBoolean(), any());
        mvc.perform(get("/content/" + pid.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void getContentIOExceptionTest() throws Exception {
        PID pid = TestHelper.makePid();
        doThrow(new IOException())
                .when(fedoraContentService)
                .streamData(any(), any(), anyBoolean(), any());
        mvc.perform(get("/content/" + pid.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();
    }
}

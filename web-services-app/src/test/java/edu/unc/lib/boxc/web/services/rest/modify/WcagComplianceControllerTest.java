package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.impl.wcagCompliance.WcagComplianceService;
import edu.unc.lib.boxc.web.services.rest.MvcTestHelpers;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static edu.unc.lib.boxc.operations.impl.wcagCompliance.WcagComplianceService.LEVEL_A_10;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class WcagComplianceControllerTest {

    @Mock
    private WcagComplianceService service;
    private MockMvc mockMvc;
    private PID pid;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        pid = TestHelper.makePid();

        WcagComplianceController controller = new WcagComplianceController();
        controller.setService(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testUpdateWcagComplianceNoPermission() throws Exception {

        doThrow(new AccessRestrictionException("Access Denied"))
                .when(service).updateWcagCompliance(any());

        mockMvc.perform(post("/edit/wcagCompliance/{pidString}?level={level}",
                        pid.getId(), LEVEL_A_10))
                .andExpect(status().isForbidden());

    }

    @Test
    public void testUpdateWcagComplianceInvalidLevel() throws Exception {
        var level = "best level ever";
        doThrow(new IllegalArgumentException("not the best level"))
                .when(service).updateWcagCompliance(any());

        mockMvc.perform(post("/edit/wcagCompliance/{pidString}?level={level}",
                        pid.getId(), level))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateWcagComplianceInvalidObject() throws Exception {
        doThrow(new InvalidOperationForObjectType("This is a work"))
                .when(service).updateWcagCompliance(any());

        mockMvc.perform(post("/edit/wcagCompliance/{pidString}?level={level}",
                        pid.getId(), LEVEL_A_10))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testUpdateWcagComplianceSuccess() throws Exception {
        var level = LEVEL_A_10;
        var result = mockMvc.perform(post("/edit/wcagCompliance/{pidString}?level={level}",
                        pid.getId(), level))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("updateWcagCompliance", respMap.get("action"));
        assertEquals(pid.getId(), respMap.get("pid"));

        // Verify that the service was called with the correct AltTextUpdateRequest
        verify(service).updateWcagCompliance(argThat(request ->
                request.getPidString().equals(pid.getId()) &&
                        request.getLevel().equals(level)
        ));
    }
}

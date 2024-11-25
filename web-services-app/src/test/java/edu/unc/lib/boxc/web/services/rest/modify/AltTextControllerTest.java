package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.impl.altText.AltTextUpdateService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author bbpennel
 */
public class AltTextControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AltTextUpdateService altTextUpdateService;

    @Mock
    private BinaryObject binaryObject;
    private PID pid;
    private PID altTextPid;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        AltTextController controller = new AltTextController();
        controller.setAltTextUpdateService(altTextUpdateService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();

        pid = TestHelper.makePid();
        altTextPid = DatastreamPids.getAltTextPid(pid);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    void testUpdateAltText() throws Exception {
        var altText = "Sample Alt Text";

        // Mock BinaryObject to return a PID
        when(binaryObject.getPid()).thenReturn(altTextPid);
        when(altTextUpdateService.updateAltText(any())).thenReturn(binaryObject);

        var result = mockMvc.perform(post("/edit/altText/{id}", pid.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("altText", altText))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("updateAltText", respMap.get("action"));
        assertEquals(altTextPid.getComponentId(), respMap.get("pid"));

        // Verify that the service was called with the correct AltTextUpdateRequest
        verify(altTextUpdateService).updateAltText(argThat(request ->
                request.getPidString().equals(pid.getId()) &&
                        request.getAltText().equals(altText)
        ));
    }

    @Test
    void testUpdateAltTextFails() throws Exception {
        var altText = "Sample Alt Text";

        doThrow(new AccessRestrictionException("Access Denied"))
                .when(altTextUpdateService).updateAltText(any());

        mockMvc.perform(post("/edit/altText/{id}", pid.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("altText", altText))
                .andExpect(status().isForbidden());
    }
}

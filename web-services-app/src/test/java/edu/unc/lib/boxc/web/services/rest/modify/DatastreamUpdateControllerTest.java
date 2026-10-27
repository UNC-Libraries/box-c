package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.impl.datastreams.DatastreamUpdateService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author snluong
 */
public class DatastreamUpdateControllerTest {
    private MockMvc mockMvc;
    @Mock
    private DatastreamUpdateService service;
    @Mock
    private BinaryObject binaryObject;
    private PID pid;
    private PID altTextPid;

    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);

        DatastreamUpdateController controller = new DatastreamUpdateController();
        controller.setDatastreamUpdateService(service);
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
    void testUpdateDatastreamText() throws Exception {
        var altText = "Sample Alt Text";

        // Mock BinaryObject to return a PID
        when(binaryObject.getPid()).thenReturn(altTextPid);
        when(service.updateDatastreamText(any())).thenReturn(binaryObject);

        var result = mockMvc.perform(
                post("/edit/datastreamText/{id}/{datastreamType}", pid.getId(), "alt_text")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("datastreamText", altText))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("update_alt_text", respMap.get("action"));
        assertEquals(altTextPid.getComponentId(), respMap.get("pid"));

        // Verify that the service was called with the correct AltTextUpdateRequest
        verify(service).updateDatastreamText(argThat(request ->
                request.getPidString().equals(pid.getId()) &&
                        request.getText().equals(altText) && request.getDatastreamType().equals(DatastreamType.ALT_TEXT)
        ));
    }

    @Test
    void testUpdateDatastreamTextFailsPermissionCheck() throws Exception {
        var altText = "Sample Alt Text";

        doThrow(new AccessRestrictionException("Access Denied"))
                .when(service).updateDatastreamText(any());

        mockMvc.perform(post("/edit/datastreamText/{id}/{datastreamType}", pid.getId(), "altText")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("datastreamText", altText))
                .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateDatastreamTextInvalidDatastream() throws Exception {
        var text = "SampleText";

        doThrow(new IllegalArgumentException("Invalid datastream"))
                .when(service).updateDatastreamText(any());

        mockMvc.perform(post("/edit/datastreamText/{id}/{datastreamType}", pid.getId(), "badDatastream")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("datastreamText", text))
                .andExpect(status().isBadRequest());
    }
}

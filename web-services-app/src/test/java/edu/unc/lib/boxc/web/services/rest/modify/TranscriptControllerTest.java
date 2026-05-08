package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.impl.transcript.TranscriptUpdateService;
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

public class TranscriptControllerTest {
    private MockMvc mockMvc;
    @Mock
    private TranscriptUpdateService service;
    @Mock
    private BinaryObject binaryObject;
    private PID pid, transcriptPid;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        var controller = new TranscriptController();
        controller.setService(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        pid = TestHelper.makePid();
        transcriptPid = DatastreamPids.getTranscriptPid(pid);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    void updateTranscriptTest() throws Exception {
        var transcriptText = "a very accurate transcript";

        // Mock BinaryObject to return a PID
        when(binaryObject.getPid()).thenReturn(transcriptPid);
        when(service.updateTranscript(any())).thenReturn(binaryObject);

        var result = mockMvc.perform(post("/edit/transcript/{id}", pid.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("transcript", transcriptText))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("updateTranscript", respMap.get("action"));
        assertEquals(transcriptPid.getComponentId(), respMap.get("pid"));

        // Verify that the service was called with the correct AltTextUpdateRequest
        verify(service).updateTranscript(argThat(request ->
                request.getPidString().equals(pid.getId()) &&
                        request.getTranscriptText().equals(transcriptText)
        ));
    }

    @Test
    void updateTranscriptFailsTest() throws Exception {
        var transcriptText = "a very accurate transcript";

        doThrow(new AccessRestrictionException("Access Denied"))
                .when(service).updateTranscript(any());

        mockMvc.perform(post("/edit/transcript/{id}", pid.getId())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("transcript", transcriptText))
                .andExpect(status().isForbidden());
    }
}

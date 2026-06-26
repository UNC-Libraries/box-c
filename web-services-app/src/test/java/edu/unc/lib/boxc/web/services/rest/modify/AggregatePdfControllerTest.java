package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.operations.jms.pdf.PdfRequest;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequestSender;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.URI;
import java.util.List;

import static edu.unc.lib.boxc.web.services.rest.MvcTestHelpers.getMapFromResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AggregatePdfControllerTest {
    private static final String PID_1 = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String PID_2 = "0e33ad0b-7a16-4bfa-b833-6126c262d889";

    @Mock
    private PdfRequestSender requestSender;
    @InjectMocks
    private AggregatePdfController controller;

    private AutoCloseable closeable;
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    void aggregatePdfWithEmptyIdsTest() throws Exception {
        var ids = "";

        mockMvc.perform(MockMvcRequestBuilders.multipart(URI.create("/edit/aggregatePdf"))
                    .param("ids", ids))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void aggregatePdfFailsTest() throws Exception {
        doThrow(new IllegalArgumentException("could not generate aggregate pdf")).when(requestSender).sendToQueue(any());

        var ids = PID_1 + "\n" + PID_2 + "\n";

        mockMvc.perform(MockMvcRequestBuilders.multipart(URI.create("/edit/aggregatePdf"))
                        .param("ids", ids))
                .andExpect(status().is5xxServerError())
                .andReturn();
    }

    @Test
    void aggregatePdfWithSingleIdTest() throws Exception {
        var ids = PID_1;

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart(URI.create("/edit/aggregatePdf/" + PID_1))
                        .param("id", ids))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        var respMap = getMapFromResponse(result);
        assertEquals("generate aggregate PDFs", respMap.get("action"));

        ArgumentCaptor<PdfRequest> captor = ArgumentCaptor.forClass(PdfRequest.class);
        verify(requestSender, times(1)).sendToQueue(captor.capture());
        PdfRequest request = captor.getValue();
        assertEquals(PID_1, request.getWorkPid());
    }

    @Test
    void aggregatePdfWithMultipleIdsTest() throws Exception {
        var ids = PID_1 + "\n" + PID_2 + "\n";

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart(URI.create("/edit/aggregatePdf"))
                        .param("ids", ids))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        var respMap = getMapFromResponse(result);
        assertEquals("generate aggregate PDFs", respMap.get("action"));

        ArgumentCaptor<PdfRequest> captor = ArgumentCaptor.forClass(PdfRequest.class);
        verify(requestSender, times(2)).sendToQueue(captor.capture());
        List<PdfRequest> requests = captor.getAllValues();
        assertEquals(PID_1, requests.get(0).getWorkPid());
        assertEquals(PID_2, requests.get(1).getWorkPid());
    }
}

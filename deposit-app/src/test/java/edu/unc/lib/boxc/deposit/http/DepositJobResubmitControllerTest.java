package edu.unc.lib.boxc.deposit.http;

import edu.unc.lib.boxc.deposit.CleanupDepositJob;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author bbpennel
 */
public class DepositJobResubmitControllerTest {
    private static final String CLEANUP_JOB = CleanupDepositJob.class.getName();
    private static final String DEPOSIT_ID_1 = "a1b2c3d4-0000-0000-0000-000000000001";
    private static final String DEPOSIT_ID_2 = "a1b2c3d4-0000-0000-0000-000000000002";

    @InjectMocks
    private DepositJobResubmitController controller;
    @Mock
    private DepositJobMessageService depositJobMessageService;
    private MockMvc mvc;
    private AutoCloseable closeable;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void init() {
        closeable = MockitoAnnotations.openMocks(this);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void resubmitSingleDepositTest() throws Exception {
        MvcResult result = mvc.perform(post("/admin/resubmitDepositJobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobClassName\":\"" + CLEANUP_JOB + "\",\"depositIds\":[\"" + DEPOSIT_ID_1 + "\"]}"))
                .andExpect(status().isOk())
                .andReturn();
        var response = parseResponse(result);
        assertEquals(1, response.getSubmitted());

        var captor = ArgumentCaptor.forClass(DepositJobMessage.class);
        verify(depositJobMessageService).sendDepositJobMessage(captor.capture());
        var msg = captor.getValue();
        assertEquals(DEPOSIT_ID_1, msg.getDepositId());
        assertEquals(CLEANUP_JOB, msg.getJobClassName());
    }

    @Test
    public void resubmitMultipleDepositsTest() throws Exception {
        MvcResult result = mvc.perform(post("/admin/resubmitDepositJobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobClassName\":\"" + CLEANUP_JOB + "\","
                                + "\"depositIds\":[\"" + DEPOSIT_ID_1 + "\",\"" + DEPOSIT_ID_2 + "\"]}"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(2, parseResponse(result).getSubmitted());

        var captor = ArgumentCaptor.forClass(DepositJobMessage.class);
        verify(depositJobMessageService, times(2)).sendDepositJobMessage(captor.capture());
        var msgs = captor.getAllValues();
        assertEquals(List.of(DEPOSIT_ID_1, DEPOSIT_ID_2),
                msgs.stream().map(DepositJobMessage::getDepositId).toList());
        msgs.forEach(m -> assertEquals(CLEANUP_JOB, m.getJobClassName()));
    }

    @Test
    public void resubmitInvalidJobClassTest() throws Exception {
        MvcResult result = mvc.perform(post("/admin/resubmitDepositJobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobClassName\":\"com.example.FakeJob\",\"depositIds\":[\"" + DEPOSIT_ID_1 + "\"]}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertEquals(0, parseResponse(result).getSubmitted());

        verifyNoInteractions(depositJobMessageService);
    }

    @Test
    public void resubmitMissingJobClassTest() throws Exception {
        MvcResult result = mvc.perform(post("/admin/resubmitDepositJobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"depositIds\":[\"" + DEPOSIT_ID_1 + "\"]}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertEquals(0, parseResponse(result).getSubmitted());

        verifyNoInteractions(depositJobMessageService);
    }

    @Test
    public void resubmitEmptyDepositIdsTest() throws Exception {
        MvcResult result = mvc.perform(post("/admin/resubmitDepositJobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobClassName\":\"" + CLEANUP_JOB + "\",\"depositIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertEquals(0, parseResponse(result).getSubmitted());

        verifyNoInteractions(depositJobMessageService);
    }

    @Test
    public void resubmitMissingDepositIdsTest() throws Exception {
        MvcResult result = mvc.perform(post("/admin/resubmitDepositJobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobClassName\":\"" + CLEANUP_JOB + "\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertEquals(0, parseResponse(result).getSubmitted());

        verifyNoInteractions(depositJobMessageService);
    }

    @Test
    public void resubmitSkipsBlankDepositIdTest() throws Exception {
        MvcResult result = mvc.perform(post("/admin/resubmitDepositJobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobClassName\":\"" + CLEANUP_JOB + "\","
                                + "\"depositIds\":[\"" + DEPOSIT_ID_1 + "\",\"\",\"" + DEPOSIT_ID_2 + "\"]}"))
                .andExpect(status().isOk())
                .andReturn();
        assertEquals(2, parseResponse(result).getSubmitted());

        verify(depositJobMessageService, times(2)).sendDepositJobMessage(
                org.mockito.ArgumentMatchers.any(DepositJobMessage.class));
    }

    private DepositJobResubmitController.ResubmitResponse parseResponse(MvcResult result) throws Exception {
        return mapper.readValue(result.getResponse().getContentAsString(),
                DepositJobResubmitController.ResubmitResponse.class);
    }
}

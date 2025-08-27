package edu.unc.lib.boxc.web.services.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessageService;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class DepositControllerTest {

    @Mock
    private DepositStatusFactory depositStatusFactory;

    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;

    @Mock
    private DepositOperationMessageService depositOperationMessageService;

    @InjectMocks
    private DepositController controller;

    private MockMvc mvc;

    private static final String TEST_UUID = "test-uuid-123";
    private static final String TEST_USERNAME = "testuser";
    private Map<String, String> depositStatus;
    private AccessGroupSet principals;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();

        depositStatus = new HashMap<>();
        depositStatus.put(DepositField.depositorName.name(), TEST_USERNAME);
        depositStatus.put(DepositField.state.name(), DepositState.queued.name());

        principals = new AccessGroupSetImpl("authenticated");
        GroupsThreadStore.storeGroups(principals);
        GroupsThreadStore.storeUsername(TEST_USERNAME);

        when(depositStatusFactory.get(TEST_UUID)).thenReturn(depositStatus);
        when(globalPermissionEvaluator.hasGlobalPermission(any(AccessGroupSet.class), eq(Permission.ingest)))
                .thenReturn(false);
        when(globalPermissionEvaluator.hasGlobalPermission(any(AccessGroupSet.class), eq(Permission.createAdminUnit)))
                .thenReturn(false);
    }

    @Test
    void update_PauseAction_Success() throws Exception {
        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "pause"))
                .andExpect(status().isNoContent());

        verify(depositOperationMessageService).sendDepositOperationMessage(any(DepositOperationMessage.class));
    }

    @Test
    void update_PauseAction_FinishedDeposit_ThrowsException() throws Exception {
        depositStatus.put(DepositField.state.name(), DepositState.finished.name());

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "pause"))
                .andExpect(status().isBadRequest());

        verify(depositOperationMessageService, never()).sendDepositOperationMessage(any());
    }

    @Test
    void update_PauseAction_FailedDeposit_ThrowsException() throws Exception {
        depositStatus.put(DepositField.state.name(), DepositState.failed.name());

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "pause"))
                .andExpect(status().isBadRequest());

        verify(depositOperationMessageService, never()).sendDepositOperationMessage(any());
    }

    @Test
    void update_ResumeAction_PausedDeposit_Success() throws Exception {
        depositStatus.put(DepositField.state.name(), DepositState.paused.name());

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "resume"))
                .andExpect(status().isNoContent());

        verify(depositOperationMessageService).sendDepositOperationMessage(any(DepositOperationMessage.class));
    }

    @Test
    void update_ResumeAction_FailedDeposit_Success() throws Exception {
        depositStatus.put(DepositField.state.name(), DepositState.failed.name());

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "resume"))
                .andExpect(status().isNoContent());

        verify(depositOperationMessageService).sendDepositOperationMessage(any(DepositOperationMessage.class));
    }

    @Test
    void update_ResumeAction_InvalidState_ThrowsException() throws Exception {
        depositStatus.put(DepositField.state.name(), DepositState.running.name());

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "resume"))
                .andExpect(status().isBadRequest());

        verify(depositOperationMessageService, never()).sendDepositOperationMessage(any());
    }

    @Test
    void update_CancelAction_Success() throws Exception {
        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "cancel"))
                .andExpect(status().isNoContent());

        verify(depositOperationMessageService).sendDepositOperationMessage(any(DepositOperationMessage.class));
    }

    @Test
    void update_CancelAction_FinishedDeposit_ThrowsException() throws Exception {
        depositStatus.put(DepositField.state.name(), DepositState.finished.name());

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "cancel"))
                .andExpect(status().isBadRequest());

        verify(depositOperationMessageService, never()).sendDepositOperationMessage(any());
    }

    @Test
    void update_DestroyAction_CancelledDeposit_Success() throws Exception {
        depositStatus.put(DepositField.state.name(), DepositState.cancelled.name());

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "destroy"))
                .andExpect(status().isNoContent());

        verify(depositOperationMessageService).sendDepositOperationMessage(any(DepositOperationMessage.class));
    }

    @Test
    void update_DestroyAction_FinishedDeposit_Success() throws Exception {
        depositStatus.put(DepositField.state.name(), DepositState.finished.name());

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "destroy"))
                .andExpect(status().isNoContent());

        verify(depositOperationMessageService).sendDepositOperationMessage(any(DepositOperationMessage.class));
    }

    @Test
    void update_DestroyAction_InvalidState_ThrowsException() throws Exception {
        depositStatus.put(DepositField.state.name(), DepositState.running.name());

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "destroy"))
                .andExpect(status().isBadRequest());

        verify(depositOperationMessageService, never()).sendDepositOperationMessage(any());
    }

    @Test
    void update_InvalidAction_ThrowsException() throws Exception {
        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "invalid"))
                .andExpect(status().isBadRequest());

        verify(depositOperationMessageService, never()).sendDepositOperationMessage(any());
    }

    @Test
    void update_NoPermission_WrongUser_Returns403() throws Exception {
        depositStatus.put(DepositField.depositorName.name(), "otheruser");

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "pause"))
                .andExpect(status().isForbidden());

        verify(depositOperationMessageService, never()).sendDepositOperationMessage(any());
    }

    @Test
    void update_HasIngestPermission_Success() throws Exception {
        when(globalPermissionEvaluator.hasGlobalPermission(any(AccessGroupSet.class), eq(Permission.ingest)))
                .thenReturn(true);
        depositStatus.put(DepositField.depositorName.name(), "otheruser");

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "pause"))
                .andExpect(status().isNoContent());

        verify(depositOperationMessageService).sendDepositOperationMessage(any(DepositOperationMessage.class));
    }

    @Test
    void update_HasAdminPermission_Success() throws Exception {
        when(globalPermissionEvaluator.hasGlobalPermission(any(AccessGroupSet.class), eq(Permission.createAdminUnit)))
                .thenReturn(true);
        depositStatus.put(DepositField.depositorName.name(), "otheruser");

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "pause"))
                .andExpect(status().isNoContent());

        verify(depositOperationMessageService).sendDepositOperationMessage(any(DepositOperationMessage.class));
    }

    @Test
    void update_NoUsername_Returns403() throws Exception {
        GroupsThreadStore.storeUsername(null);
        depositStatus.put(DepositField.depositorName.name(), "otheruser");

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "pause"))
                .andExpect(status().isForbidden());

        verify(depositOperationMessageService, never()).sendDepositOperationMessage(any());
    }
}
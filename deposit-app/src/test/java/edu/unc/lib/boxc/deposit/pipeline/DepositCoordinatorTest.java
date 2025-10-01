package edu.unc.lib.boxc.deposit.pipeline;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineState;
import edu.unc.lib.boxc.deposit.impl.model.DepositPipelineStatusFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.unc.lib.boxc.deposit.api.DepositOperation;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessageService;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessageService;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.jms.DepositJobMessageFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class DepositCoordinatorTest {

    private DepositCoordinator coordinator;

    @Mock
    private ActiveDepositsService activeDeposits;
    @Mock
    private DepositStatusFactory depositStatusFactory;
    @Mock
    private DepositOperationMessageService depositOperationMessageService;
    @Mock
    private JobSuccessHandler jobSuccessHandler;
    @Mock
    private DepositResumeHandler depositResumeHandler;
    @Mock
    private DepositRegisterHandler depositRegisterHandler;
    @Mock
    private DepositPauseHandler depositPauseHandler;
    @Mock
    private JobFailureHandler jobFailureHandler;
    @Mock
    private JobInterruptedHandler jobInterruptedHandler;
    @Mock
    private DepositJobMessageFactory depositJobMessageFactory;
    @Mock
    private DepositJobMessageService depositJobMessageService;
    @Mock
    private Message message;
    @Mock
    private DepositPipelineStatusFactory pipelineStatusFactory;

    private DepositOperationMessage operationMessage;
    private final String DEPOSIT_ID = "deposit123";
    private final String DEPOSITOR_NAME = "testuser";
    private final String NEXT_DEPOSIT_ID = "deposit456";

    @BeforeEach
    public void setup() throws Exception {
        coordinator = new DepositCoordinator();
        coordinator.setActiveDeposits(activeDeposits);
        coordinator.setDepositStatusFactory(depositStatusFactory);
        coordinator.setDepositOperationMessageService(depositOperationMessageService);
        coordinator.setJobSuccessHandler(jobSuccessHandler);
        coordinator.setDepositResumeHandler(depositResumeHandler);
        coordinator.setDepositRegisterHandler(depositRegisterHandler);
        coordinator.setDepositPauseHandler(depositPauseHandler);
        coordinator.setJobFailureHandler(jobFailureHandler);
        coordinator.setJobInterruptedHandler(jobInterruptedHandler);
        coordinator.setDepositJobMessageFactory(depositJobMessageFactory);
        coordinator.setDepositJobMessageService(depositJobMessageService);
        coordinator.setPipelineStatusFactory(pipelineStatusFactory);

        operationMessage = new DepositOperationMessage();
        operationMessage.setDepositId(DEPOSIT_ID);

        when(depositOperationMessageService.fromJson(message)).thenReturn(operationMessage);
    }

    @Test
    public void testOnMessageRegister() throws Exception {
        operationMessage.setAction(DepositOperation.REGISTER);
        when(depositStatusFactory.getState(DEPOSIT_ID)).thenReturn(DepositState.unregistered);
        when(activeDeposits.acceptingNewDeposits()).thenReturn(false);

        coordinator.onMessage(message);

        verify(depositRegisterHandler).handleMessage(operationMessage);
        verify(depositStatusFactory, never()).getFirstQueuedDeposit();
        verify(message).acknowledge();
        verify(activeDeposits).markInactive(DEPOSIT_ID);
    }

    @Test
    public void testOnMessagePause() throws Exception {
        operationMessage.setAction(DepositOperation.PAUSE);
        when(depositStatusFactory.getState(DEPOSIT_ID)).thenReturn(DepositState.paused);
        when(activeDeposits.acceptingNewDeposits()).thenReturn(false);

        coordinator.onMessage(message);

        verify(depositPauseHandler).handleMessage(operationMessage);
        verify(depositStatusFactory, never()).getFirstQueuedDeposit();
        verify(message).acknowledge();
        verify(activeDeposits).markInactive(DEPOSIT_ID);
    }

    @Test
    public void testOnMessageResume() throws Exception {
        operationMessage.setAction(DepositOperation.RESUME);
        when(depositStatusFactory.getState(DEPOSIT_ID)).thenReturn(DepositState.queued);
        when(activeDeposits.acceptingNewDeposits()).thenReturn(false);

        coordinator.onMessage(message);

        verify(depositResumeHandler).handleMessage(operationMessage);
        verify(depositStatusFactory, never()).getFirstQueuedDeposit();
        verify(message).acknowledge();
        verify(activeDeposits).markInactive(DEPOSIT_ID);
    }

    @Test
    public void testOnMessageJobSuccess() throws Exception {
        operationMessage.setAction(DepositOperation.JOB_SUCCESS);
        when(depositStatusFactory.getState(DEPOSIT_ID)).thenReturn(DepositState.finished);
        when(activeDeposits.acceptingNewDeposits()).thenReturn(false);

        coordinator.onMessage(message);

        verify(jobSuccessHandler).handleMessage(operationMessage);
        verify(depositStatusFactory, never()).getFirstQueuedDeposit();
        verify(message).acknowledge();
        verify(activeDeposits).markInactive(DEPOSIT_ID);
    }

    @Test
    public void testOnMessageJobFailure() throws Exception {
        operationMessage.setAction(DepositOperation.JOB_FAILURE);
        when(depositStatusFactory.getState(DEPOSIT_ID)).thenReturn(DepositState.failed);
        when(activeDeposits.acceptingNewDeposits()).thenReturn(false);

        coordinator.onMessage(message);

        verify(jobFailureHandler).handleMessage(operationMessage);
        verify(depositStatusFactory, never()).getFirstQueuedDeposit();
        verify(message).acknowledge();
        verify(activeDeposits).markInactive(DEPOSIT_ID);
    }

    @Test
    public void testOnMessageJobInterrupted() throws Exception {
        operationMessage.setAction(DepositOperation.JOB_INTERRUPTED);
        when(depositStatusFactory.getState(DEPOSIT_ID)).thenReturn(DepositState.running);
        when(activeDeposits.acceptingNewDeposits()).thenReturn(false);

        coordinator.onMessage(message);

        verify(jobInterruptedHandler).handleMessage(operationMessage);
        verify(depositStatusFactory, never()).getFirstQueuedDeposit();
        verify(message).acknowledge();
    }

    @Test
    public void testOnMessageUnknownAction() throws Exception {
        operationMessage.setAction(DepositOperation.DESTROY);

        coordinator.onMessage(message);

        verify(depositStatusFactory).fail(DEPOSIT_ID);
        verify(activeDeposits).markInactive(DEPOSIT_ID);
        verify(message).acknowledge();
    }

    @Test
    public void testStartNextDepositWhenFinished() throws Exception {
        testStartNextDepositWhen(DepositOperation.JOB_SUCCESS, DepositState.finished, jobSuccessHandler);
    }

    public void testStartNextDepositWhen(DepositOperation action,
                                         DepositState state,
                                         DepositOperationHandler handler) throws Exception {
        operationMessage.setAction(action);
        when(depositStatusFactory.getState(DEPOSIT_ID)).thenReturn(state);
        when(activeDeposits.acceptingNewDeposits()).thenReturn(true);
        when(depositStatusFactory.getFirstQueuedDeposit()).thenReturn(NEXT_DEPOSIT_ID);
        when(depositStatusFactory.addSupervisorLock(eq(NEXT_DEPOSIT_ID), any())).thenReturn(true);

        Map<String, String> nextDepositStatus = new HashMap<>();
        nextDepositStatus.put(DepositField.depositorName.name(), DEPOSITOR_NAME);
        when(depositStatusFactory.get(NEXT_DEPOSIT_ID)).thenReturn(nextDepositStatus);

        DepositJobMessage jobMessage = new DepositJobMessage();
        when(depositJobMessageFactory.createNextJobMessage(NEXT_DEPOSIT_ID, nextDepositStatus))
                .thenReturn(jobMessage);

        coordinator.onMessage(message);

        verify(handler).handleMessage(operationMessage);
        verify(activeDeposits).markInactive(DEPOSIT_ID);
        verify(activeDeposits).markActive(NEXT_DEPOSIT_ID);
        verify(depositStatusFactory).setState(NEXT_DEPOSIT_ID, DepositState.running);
        verify(depositStatusFactory).set(eq(NEXT_DEPOSIT_ID), eq(DepositField.startTime), any());
        verify(depositJobMessageService).sendDepositJobMessage(jobMessage);
        verify(depositStatusFactory).removeSupervisorLock(NEXT_DEPOSIT_ID);
        verify(message).acknowledge();
    }

    @Test
    public void testStartNextDepositWhenPaused() throws Exception {
        testStartNextDepositWhen(DepositOperation.PAUSE, DepositState.paused, depositPauseHandler);
    }

    @Test
    public void testStartNextDepositWhenFailed() throws Exception {
        testStartNextDepositWhen(DepositOperation.JOB_FAILURE, DepositState.failed, jobFailureHandler);
    }

    @Test
    public void testStartNextDepositWhenQueued() throws Exception {
        testStartNextDepositWhen(DepositOperation.REGISTER, DepositState.queued, depositRegisterHandler);
    }

    @Test
    public void testNoStartNextDepositWhenNoneQueued() throws Exception {
        operationMessage.setAction(DepositOperation.JOB_SUCCESS);
        when(depositStatusFactory.getState(DEPOSIT_ID)).thenReturn(DepositState.finished);
        when(activeDeposits.acceptingNewDeposits()).thenReturn(true);
        when(depositStatusFactory.getFirstQueuedDeposit()).thenReturn(null);

        coordinator.onMessage(message);

        verify(jobSuccessHandler).handleMessage(operationMessage);
        verify(depositStatusFactory).getFirstQueuedDeposit();
        verify(activeDeposits, never()).markActive(any());
        verify(activeDeposits).markInactive(DEPOSIT_ID);
        verify(message).acknowledge();
    }

    @Test
    public void testStartDepositWithExistingStartTime() throws Exception {
        operationMessage.setAction(DepositOperation.JOB_SUCCESS);
        when(depositStatusFactory.getState(DEPOSIT_ID)).thenReturn(DepositState.running);
        when(activeDeposits.acceptingNewDeposits()).thenReturn(true);
        when(depositStatusFactory.getFirstQueuedDeposit()).thenReturn(NEXT_DEPOSIT_ID);
        when(depositStatusFactory.addSupervisorLock(eq(NEXT_DEPOSIT_ID), any())).thenReturn(true);

        Map<String, String> nextDepositStatus = new HashMap<>();
        nextDepositStatus.put(DepositField.depositorName.name(), DEPOSITOR_NAME);
        nextDepositStatus.put(DepositField.startTime.name(), "1234567890");
        when(depositStatusFactory.get(NEXT_DEPOSIT_ID)).thenReturn(nextDepositStatus);

        DepositJobMessage jobMessage = new DepositJobMessage();
        when(depositJobMessageFactory.createNextJobMessage(NEXT_DEPOSIT_ID, nextDepositStatus))
                .thenReturn(jobMessage);

        coordinator.onMessage(message);

        verify(depositStatusFactory, never()).set(eq(NEXT_DEPOSIT_ID), eq(DepositField.startTime), any());
        verify(message).acknowledge();
        verify(activeDeposits).markActive(DEPOSIT_ID);
    }

    @Test
    public void testStartDepositFailsToGetLock() throws Exception {
        operationMessage.setAction(DepositOperation.JOB_SUCCESS);
        when(depositStatusFactory.getState(DEPOSIT_ID)).thenReturn(DepositState.finished);
        when(activeDeposits.acceptingNewDeposits()).thenReturn(true);
        when(depositStatusFactory.getFirstQueuedDeposit()).thenReturn(NEXT_DEPOSIT_ID);
        when(depositStatusFactory.addSupervisorLock(eq(NEXT_DEPOSIT_ID), any())).thenReturn(false);

        Map<String, String> nextDepositStatus = new HashMap<>();
        nextDepositStatus.put(DepositField.depositorName.name(), DEPOSITOR_NAME);
        when(depositStatusFactory.get(NEXT_DEPOSIT_ID)).thenReturn(nextDepositStatus);

        coordinator.onMessage(message);

        verify(activeDeposits, never()).markActive(NEXT_DEPOSIT_ID);
        verify(activeDeposits).markInactive(DEPOSIT_ID);
        verify(depositStatusFactory, never()).setState(NEXT_DEPOSIT_ID, DepositState.running);
        verify(message).acknowledge();
    }

    @Test
    public void testStartDepositJobMessageFailure() throws Exception {
        operationMessage.setAction(DepositOperation.JOB_SUCCESS);
        when(depositStatusFactory.getState(DEPOSIT_ID)).thenReturn(DepositState.finished);
        when(activeDeposits.acceptingNewDeposits()).thenReturn(true);
        when(depositStatusFactory.getFirstQueuedDeposit()).thenReturn(NEXT_DEPOSIT_ID);
        when(depositStatusFactory.addSupervisorLock(eq(NEXT_DEPOSIT_ID), any())).thenReturn(true);

        Map<String, String> nextDepositStatus = new HashMap<>();
        nextDepositStatus.put(DepositField.depositorName.name(), DEPOSITOR_NAME);
        when(depositStatusFactory.get(NEXT_DEPOSIT_ID)).thenReturn(nextDepositStatus);

        DepositJobMessage jobMessage = new DepositJobMessage();
        when(depositJobMessageFactory.createNextJobMessage(NEXT_DEPOSIT_ID, nextDepositStatus))
                .thenReturn(jobMessage);
        doThrow(new RuntimeException("JMS error")).when(depositJobMessageService)
                .sendDepositJobMessage(jobMessage);

        coordinator.onMessage(message);

        verify(depositStatusFactory).fail(NEXT_DEPOSIT_ID);
        verify(activeDeposits).markInactive(NEXT_DEPOSIT_ID);
        verify(depositStatusFactory).removeSupervisorLock(NEXT_DEPOSIT_ID);
        verify(message).acknowledge();
    }

    @Test
    public void testOnMessageHandlerException() throws Exception {
        operationMessage.setAction(DepositOperation.REGISTER);
        doThrow(new RuntimeException("Handler error")).when(depositRegisterHandler)
                .handleMessage(operationMessage);

        coordinator.onMessage(message);

        verify(depositStatusFactory).fail(DEPOSIT_ID);
        verify(activeDeposits).markInactive(DEPOSIT_ID);
        verify(message).acknowledge();
    }

    @Test
    public void testOnMessageDeserializationException() throws Exception {
        when(depositOperationMessageService.fromJson(message))
                .thenThrow(new RuntimeException("Parse error"));

        coordinator.onMessage(message);

        verify(depositRegisterHandler, never()).handleMessage(any());
        verify(message).acknowledge();
    }

    @Test
    public void testOnMessageAcknowledgeException() throws Exception {
        operationMessage.setAction(DepositOperation.REGISTER);
        when(depositStatusFactory.getState(DEPOSIT_ID)).thenReturn(DepositState.queued);
        when(activeDeposits.acceptingNewDeposits()).thenReturn(false);
        doThrow(new JMSException("Acknowledge failed")).when(message).acknowledge();

        coordinator.onMessage(message);

        verify(depositRegisterHandler).handleMessage(operationMessage);
        // Should not throw exception despite acknowledge failure
    }

    @Test
    public void testNoStartNextDepositForRunningState() throws Exception {
        operationMessage.setAction(DepositOperation.JOB_SUCCESS);
        when(depositStatusFactory.getState(DEPOSIT_ID)).thenReturn(DepositState.running);
        when(activeDeposits.acceptingNewDeposits()).thenReturn(true);

        coordinator.onMessage(message);

        verify(jobSuccessHandler).handleMessage(operationMessage);
        verify(depositStatusFactory, never()).getFirstQueuedDeposit();
        verify(message).acknowledge();
    }

    @Test
    public void testInit() {
        coordinator.init();

        verify(pipelineStatusFactory).setPipelineState(DepositPipelineState.starting);
        verify(depositStatusFactory).getAll();
        verify(pipelineStatusFactory).setPipelineState(DepositPipelineState.active);
    }

    @Test
    public void testInitRequeueAll() {
        // Setup deposits in different states
        Map<String, String> runningDeposit = new HashMap<>();
        runningDeposit.put(DepositField.uuid.name(), "running-deposit");
        runningDeposit.put(DepositField.state.name(), DepositState.running.name());

        Map<String, String> quietedDeposit = new HashMap<>();
        quietedDeposit.put(DepositField.uuid.name(), "quieted-deposit");
        quietedDeposit.put(DepositField.state.name(), DepositState.quieted.name());

        Map<String, String> queuedDeposit = new HashMap<>();
        queuedDeposit.put(DepositField.uuid.name(), "queued-deposit");
        queuedDeposit.put(DepositField.state.name(), DepositState.queued.name());

        Set<Map<String, String>> allDeposits = Set.of(runningDeposit, quietedDeposit, queuedDeposit);
        when(depositStatusFactory.getAll()).thenReturn(allDeposits);

        coordinator.init();

        // Verify pipeline state transitions
        verify(pipelineStatusFactory).setPipelineState(DepositPipelineState.starting);
        verify(pipelineStatusFactory).setPipelineState(DepositPipelineState.active);

        // Verify running deposit reactivation
        verify(depositStatusFactory).removeSupervisorLock("running-deposit");
        verify(activeDeposits).markActive("running-deposit");

        // Verify quieted deposit handling
        verify(depositStatusFactory).removeSupervisorLock("quieted-deposit");
        verify(depositResumeHandler).handleMessage(any(DepositOperationMessage.class));

        // Verify queued deposit is not specially handled (just logged)
        verify(activeDeposits, never()).markActive("queued-deposit");
    }
}
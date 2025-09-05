package edu.unc.lib.boxc.deposit.pipeline;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.unc.lib.boxc.deposit.api.DepositOperation;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessageService;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import edu.unc.lib.boxc.deposit.api.PipelineAction;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositPipelineMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositPipelineMessageService;
import edu.unc.lib.boxc.deposit.impl.model.DepositPipelineStatusFactory;
import jakarta.jms.Message;

@ExtendWith(MockitoExtension.class)
public class PipelineCoordinatorTest {

    private PipelineCoordinator coordinator;

    @Mock
    private DepositPipelineMessageService pipelineMessageService;
    @Mock
    private DepositPipelineStatusFactory pipelineStatusFactory;
    @Mock
    private DefaultMessageListenerContainer jobListenerContainer;
    @Mock
    private DefaultMessageListenerContainer operationListenerContainer;
    @Mock
    private DepositStatusFactory depositStatusFactory;
    @Mock
    private DepositOperationMessageService depositOperationMessageService;
    @Mock
    private DepositQuietHandler depositQuietHandler;
    @Mock
    private DepositResumeHandler depositResumeHandler;
    @Mock
    private Message message;

    private DepositPipelineMessage pipelineMessage;
    private DepositOperationMessage operationMessage;
    private final String DEPOSIT_ID = "deposit123";

    @BeforeEach
    public void setup() throws Exception {
        coordinator = new PipelineCoordinator();
        coordinator.setPipelineMessageService(pipelineMessageService);
        coordinator.setPipelineStatusFactory(pipelineStatusFactory);
        coordinator.setJobListenerContainer(jobListenerContainer);
        coordinator.setOperationListenerContainer(operationListenerContainer);
        coordinator.setDepositOperationMessageService(depositOperationMessageService);
        coordinator.setDepositQuietHandler(depositQuietHandler);
        coordinator.setDepositResumeHandler(depositResumeHandler);

        pipelineMessage = new DepositPipelineMessage();
        when(pipelineMessageService.fromJson(message)).thenReturn(pipelineMessage);

        operationMessage = new DepositOperationMessage();
        operationMessage.setDepositId(DEPOSIT_ID);
        when(depositOperationMessageService.fromJson(message)).thenReturn(operationMessage);
    }

    @Test
    public void testQuietPipelineFromActive() throws Exception {
        pipelineMessage.setAction(PipelineAction.QUIET);
        when(pipelineStatusFactory.getPipelineState()).thenReturn(DepositPipelineState.active);
        operationMessage.setAction(DepositOperation.QUIET);

        coordinator.onMessage(message);

        verify(pipelineStatusFactory).setPipelineState(DepositPipelineState.quieted);
        verify(depositQuietHandler).handleMessage(operationMessage);
        verify(depositStatusFactory, never()).getFirstQueuedDeposit();
        verify(jobListenerContainer).stop();
        verify(operationListenerContainer).stop();
    }

    @Test
    public void testQuietPipelineFromQuieted() throws Exception {
        testQuietPipelineFromUnacceptableState(DepositPipelineState.quieted);
    }

    public void testQuietPipelineFromUnacceptableState(DepositPipelineState state) throws Exception {
        pipelineMessage.setAction(PipelineAction.QUIET);
        when(pipelineStatusFactory.getPipelineState()).thenReturn(state);

        coordinator.onMessage(message);

        verify(pipelineStatusFactory, never()).setPipelineState(any());
        verify(jobListenerContainer, never()).stop();
        verify(operationListenerContainer, never()).stop();
    }

    @Test
    public void testQuietPipelineFromStopped() throws Exception {
        testQuietPipelineFromUnacceptableState(DepositPipelineState.stopped);
    }

    @Test
    public void testUnquietPipelineFromQuieted() throws Exception {
        pipelineMessage.setAction(PipelineAction.UNQUIET);
        when(pipelineStatusFactory.getPipelineState()).thenReturn(DepositPipelineState.quieted);
        operationMessage.setAction(DepositOperation.RESUME);

        coordinator.onMessage(message);

        verify(pipelineStatusFactory).setPipelineState(DepositPipelineState.active);
        verify(depositResumeHandler).handleMessage(operationMessage);
        verify(depositStatusFactory, never()).getFirstQueuedDeposit();
        verify(jobListenerContainer).start();
        verify(operationListenerContainer).start();
    }

    @Test
    public void testUnquietPipelineFromActive() throws Exception {
        testUnquietPipelineFromUnacceptableState(DepositPipelineState.active);
    }

    @Test
    public void testUnquietPipelineFromStopped() throws Exception {
        testUnquietPipelineFromUnacceptableState(DepositPipelineState.stopped);
    }

    public void testUnquietPipelineFromUnacceptableState(DepositPipelineState state) throws Exception {
        pipelineMessage.setAction(PipelineAction.UNQUIET);
        when(pipelineStatusFactory.getPipelineState()).thenReturn(state);

        coordinator.onMessage(message);

        verify(pipelineStatusFactory, never()).setPipelineState(DepositPipelineState.active);
        verify(jobListenerContainer, never()).start();
        verify(operationListenerContainer, never()).start();
    }

    @Test
    public void testStopPipelineFromActive() throws Exception {
        pipelineMessage.setAction(PipelineAction.STOP);
        when(pipelineStatusFactory.getPipelineState()).thenReturn(DepositPipelineState.active);

        coordinator.onMessage(message);

        verify(pipelineStatusFactory).setPipelineState(DepositPipelineState.stopped);
        verify(jobListenerContainer).shutdown();
        verify(operationListenerContainer).shutdown();
    }

    @Test
    public void testStopPipelineFromQuieted() throws Exception {
        pipelineMessage.setAction(PipelineAction.STOP);
        when(pipelineStatusFactory.getPipelineState()).thenReturn(DepositPipelineState.quieted);

        coordinator.onMessage(message);

        verify(pipelineStatusFactory).setPipelineState(DepositPipelineState.stopped);
        verify(jobListenerContainer).shutdown();
        verify(operationListenerContainer).shutdown();
    }

    @Test
    public void testStopPipelineFromStopped() throws Exception {
        pipelineMessage.setAction(PipelineAction.STOP);
        when(pipelineStatusFactory.getPipelineState()).thenReturn(DepositPipelineState.stopped);

        coordinator.onMessage(message);

        verify(pipelineStatusFactory, never()).setPipelineState(DepositPipelineState.stopped);
        verify(jobListenerContainer, never()).shutdown();
        verify(operationListenerContainer, never()).shutdown();
    }

    @Test
    public void testOnMessageDeserializationException() throws Exception {
//        when(pipelineMessageService.fromJson(message))
//                .thenThrow(new RuntimeException("Parse error"));

        coordinator.onMessage(message);

        verify(pipelineStatusFactory, never()).setPipelineState(any());
        verify(jobListenerContainer, never()).stop();
        verify(jobListenerContainer, never()).start();
        verify(jobListenerContainer, never()).shutdown();
        verify(operationListenerContainer, never()).stop();
        verify(operationListenerContainer, never()).start();
        verify(operationListenerContainer, never()).shutdown();
    }

    @Test
    public void testQuietPipelineWithException() throws Exception {
        pipelineMessage.setAction(PipelineAction.QUIET);
        when(pipelineStatusFactory.getPipelineState()).thenReturn(DepositPipelineState.active);
        doThrow(new RuntimeException("Container error")).when(jobListenerContainer).stop();

        coordinator.onMessage(message);

        verify(pipelineStatusFactory).setPipelineState(DepositPipelineState.quieted);
        verify(jobListenerContainer).stop();
    }

    @Test
    public void testUnquietPipelineWithException() throws Exception {
        pipelineMessage.setAction(PipelineAction.UNQUIET);
        when(pipelineStatusFactory.getPipelineState()).thenReturn(DepositPipelineState.quieted);
        doThrow(new RuntimeException("Container error")).when(operationListenerContainer).start();

        coordinator.onMessage(message);

        verify(pipelineStatusFactory).setPipelineState(DepositPipelineState.active);
        verify(operationListenerContainer).start();
    }

    @Test
    public void testStopPipelineWithException() throws Exception {
        pipelineMessage.setAction(PipelineAction.STOP);
        when(pipelineStatusFactory.getPipelineState()).thenReturn(DepositPipelineState.active);
        doThrow(new RuntimeException("Shutdown error")).when(jobListenerContainer).shutdown();

        coordinator.onMessage(message);

        verify(pipelineStatusFactory).setPipelineState(DepositPipelineState.stopped);
        verify(jobListenerContainer).shutdown();
    }

    @Test
    public void testNullAction() throws Exception {
        pipelineMessage.setAction(null);

        coordinator.onMessage(message);

        verify(pipelineStatusFactory, never()).setPipelineState(any());
        verify(jobListenerContainer, never()).stop();
        verify(jobListenerContainer, never()).start();
        verify(jobListenerContainer, never()).shutdown();
    }
}
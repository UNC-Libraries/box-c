package edu.unc.lib.boxc.deposit.pipeline;

import edu.unc.lib.boxc.deposit.api.PipelineAction;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositPipelineMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.JobStatusFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PipelineQuietHandlerTest {
    private PipelineQuietHandler handler;

    @Mock
    private JobStatusFactory jobStatusFactory;
    @Mock
    private DepositStatusFactory depositStatusFactory;

    private DepositPipelineMessage pipelineMessage;

    private final static String DEPOSIT_ID = "deposit123";
    private final static String USERNAME = "testuser";

    @BeforeEach
    public void setup() throws Exception {
        handler = new PipelineQuietHandler();
        handler.setDepositStatusFactory(depositStatusFactory);
        handler.setJobStatusFactory(jobStatusFactory);

        pipelineMessage = new DepositPipelineMessage();
        pipelineMessage.setUsername(USERNAME);
    }

    @Test
    public void testSuccessfulQuiet() {
        pipelineMessage.setAction(PipelineAction.QUIET);
        when(depositStatusFactory.addSupervisorLock(DEPOSIT_ID, USERNAME)).thenReturn(true);

        Map<String, String> deposit = new HashMap<>();
        deposit.put(RedisWorkerConstants.DepositField.uuid.name(), DEPOSIT_ID);
        deposit.put(RedisWorkerConstants.DepositField.state.name(), DepositState.running.name());
        Set<Map<String, String>> allDeposits = Set.of(deposit);
        when(depositStatusFactory.getAll()).thenReturn(allDeposits);

        handler.quietAll(pipelineMessage);

        verify(depositStatusFactory).addSupervisorLock(DEPOSIT_ID, USERNAME);
        verify(depositStatusFactory).setState(DEPOSIT_ID, DepositState.quieted);
        verify(depositStatusFactory).removeSupervisorLock(DEPOSIT_ID);
        verify(depositStatusFactory, never()).getFirstQueuedDeposit();
    }

    @Test
    public void testQuietFailsToAcquireLock() {
        pipelineMessage.setAction(PipelineAction.QUIET);
        when(depositStatusFactory.addSupervisorLock(DEPOSIT_ID, USERNAME)).thenReturn(false);

        Map<String, String> deposit = new HashMap<>();
        deposit.put(RedisWorkerConstants.DepositField.uuid.name(), DEPOSIT_ID);
        deposit.put(RedisWorkerConstants.DepositField.state.name(), DepositState.running.name());
        Set<Map<String, String>> allDeposits = Set.of(deposit);
        when(depositStatusFactory.getAll()).thenReturn(allDeposits);

        handler.quietAll(pipelineMessage);

        verify(depositStatusFactory).addSupervisorLock(DEPOSIT_ID, USERNAME);
        verify(depositStatusFactory, never()).setState(DEPOSIT_ID, DepositState.quieted);
        verify(depositStatusFactory, never()).removeSupervisorLock(DEPOSIT_ID);
    }

    @Test
    public void testSuccessfulUnquietFromQuiet() {
        pipelineMessage.setAction(PipelineAction.UNQUIET);
        when(depositStatusFactory.addSupervisorLock(DEPOSIT_ID, USERNAME)).thenReturn(true);

        Map<String, String> deposit = new HashMap<>();
        deposit.put(RedisWorkerConstants.DepositField.uuid.name(), DEPOSIT_ID);
        deposit.put(RedisWorkerConstants.DepositField.state.name(), DepositState.quieted.name());
        Set<Map<String, String>> allDeposits = Set.of(deposit);
        when(depositStatusFactory.getAll()).thenReturn(allDeposits);

        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(RedisWorkerConstants.DepositField.state.name(), DepositState.quieted.name());
        when(depositStatusFactory.get(DEPOSIT_ID)).thenReturn(depositStatus);

        handler.unquietAll(pipelineMessage);

        verify(depositStatusFactory).addSupervisorLock(DEPOSIT_ID, USERNAME);
        verify(jobStatusFactory).clearStale(DEPOSIT_ID);
        verify(depositStatusFactory).deleteField(DEPOSIT_ID, RedisWorkerConstants.DepositField.errorMessage);
        verify(depositStatusFactory).queueDeposit(DEPOSIT_ID);
        verify(depositStatusFactory).removeSupervisorLock(DEPOSIT_ID);
    }

    @Test
    public void testGetDepositStatusFactory() {
        DepositStatusFactory result = handler.getDepositStatusFactory();

        assertEquals(depositStatusFactory, result);
    }
}

package edu.unc.lib.boxc.deposit.pipeline;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.JobStatusFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Handler for deposit resume operations.
 *
 * @author bbpennel
 */
public class DepositResumeHandler implements DepositOperationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DepositResumeHandler.class);
    private JobStatusFactory jobStatusFactory;
    private DepositStatusFactory depositStatusFactory;
    private final Set<DepositState> VALID_STATES = Set.of(
            DepositState.unregistered,
            DepositState.paused,
            DepositState.quieted,
            DepositState.failed
    );

    @Override
    public void handleMessage(DepositOperationMessage opMessage) {
        String depositId = opMessage.getDepositId();
        LOG.info("Resuming deposit {}", depositId);

        try {
            var depositStatus = depositStatusFactory.get(depositId);
            DepositState state = DepositState.valueOf(depositStatus.get(DepositField.state.name()));

            if (!VALID_STATES.contains(state)) {
                LOG.warn("Cannot resume deposit {} from non-resumable state {}", depositId, state);
                return;
            }

            // Clear out the previous failed job if there was one
            jobStatusFactory.clearStale(depositId);
            depositStatusFactory.deleteField(depositId, DepositField.errorMessage);

            depositStatusFactory.queueDeposit(depositId);
        } finally {
            depositStatusFactory.removeSupervisorLock(depositId);
        }
    }

    @Override
    public DepositStatusFactory getDepositStatusFactory() {
        return depositStatusFactory;
    }

    public void setJobStatusFactory(JobStatusFactory jobStatusFactory) {
        this.jobStatusFactory = jobStatusFactory;
    }

    public void setDepositStatusFactory(DepositStatusFactory depositStatusFactory) {
        this.depositStatusFactory = depositStatusFactory;
    }
}

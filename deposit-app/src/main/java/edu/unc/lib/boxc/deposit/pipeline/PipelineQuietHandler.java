package edu.unc.lib.boxc.deposit.pipeline;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositPipelineMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.JobStatusFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Handler for pipeline quieting operations.
 *
 * @author krwong
 */
public class PipelineQuietHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineQuietHandler.class);

    private DepositStatusFactory depositStatusFactory;
    private JobStatusFactory jobStatusFactory;
    private final Set<DepositState> VALID_STATES = Set.of(
            DepositState.quieted
    );

    /**
     * Quiet all deposits
     * @param pipelineMessage
     */
    public void quietAll(DepositPipelineMessage pipelineMessage) {
        Set<Map<String, String>> depositStatuses = depositStatusFactory.getAll();
        for (Map<String, String> fields : depositStatuses) {
            String depositId = fields.get(DepositField.uuid.name());
            LOG.info("Quieting deposit {}", depositId);
            if (depositStatusFactory.addSupervisorLock(depositId, pipelineMessage.getUsername()) &&
                    DepositState.running.equals(DepositState.valueOf(fields.get(DepositField.state.name())))) {
                try {
                    depositStatusFactory.setState(depositId, DepositState.quieted);
                } finally {
                    depositStatusFactory.removeSupervisorLock(depositId);
                }
            }
        }
    }

    /**
     * Resume all quieted deposits
     * @param pipelineMessage
     */
    public void unquietAll(DepositPipelineMessage pipelineMessage) {
        Set<Map<String, String>> depositStatuses = depositStatusFactory.getAll();
        for (Map<String, String> fields : depositStatuses) {
            String depositId = fields.get(DepositField.uuid.name());
            LOG.info("Resuming deposit {}", depositId);
            if (depositStatusFactory.addSupervisorLock(depositId, pipelineMessage.getUsername())) {
                try {
                    var depositStatus = depositStatusFactory.get(depositId);
                    DepositState state = DepositState.valueOf(depositStatus.get(DepositField.state.name()));

                    if (!VALID_STATES.contains(state)) {
                        LOG.debug("Cannot resume deposit {} from non-resumable state {}", depositId, state);
                        continue;
                    }

                    // Clear out the previous failed job if there was one
                    jobStatusFactory.clearStale(depositId);
                    depositStatusFactory.deleteField(depositId, DepositField.errorMessage);

                    depositStatusFactory.queueDeposit(depositId);
                } finally {
                    depositStatusFactory.removeSupervisorLock(depositId);
                }
            }
        }
    }

    public DepositStatusFactory getDepositStatusFactory() {
        return depositStatusFactory;
    }

    public void setDepositStatusFactory(DepositStatusFactory depositStatusFactory) {
        this.depositStatusFactory = depositStatusFactory;
    }

    public void setJobStatusFactory(JobStatusFactory jobStatusFactory) {
        this.jobStatusFactory = jobStatusFactory;
    }
}

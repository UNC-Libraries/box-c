package edu.unc.lib.boxc.deposit.pipeline;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositPipelineMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Handler for deposit quieted operations.
 *
 * @author krwong
 */
public class DepositQuietHandler implements DepositOperationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DepositQuietHandler.class);
    private DepositStatusFactory depositStatusFactory;

    @Override
    public void handleMessage(DepositOperationMessage opMessage) {
        String depositId = opMessage.getDepositId();
        LOG.info("Quieting deposit {}", depositId);

        if (depositStatusFactory.addSupervisorLock(depositId, opMessage.getUsername())) {
            try {
                depositStatusFactory.setState(depositId, DepositState.quieted);
            } finally {
                depositStatusFactory.removeSupervisorLock(depositId);
            }
        }
    }

    public void handleMessage(DepositPipelineMessage pipelineMessage) {
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

    @Override
    public DepositStatusFactory getDepositStatusFactory() {
        return depositStatusFactory;
    }

    public void setDepositStatusFactory(DepositStatusFactory depositStatusFactory) {
        this.depositStatusFactory = depositStatusFactory;
    }
}

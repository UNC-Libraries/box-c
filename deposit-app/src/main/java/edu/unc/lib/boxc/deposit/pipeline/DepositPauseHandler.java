package edu.unc.lib.boxc.deposit.pipeline;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for deposit pause operations
 *
 * @author bbpennel
 */
public class DepositPauseHandler implements DepositOperationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DepositPauseHandler.class);
    private DepositStatusFactory depositStatusFactory;

    @Override
    public void handleMessage(DepositOperationMessage opMessage) {
        String depositId = opMessage.getDepositId();
        LOG.info("Pausing deposit {}", depositId);

        if (depositStatusFactory.addSupervisorLock(depositId, opMessage.getUsername())) {
            try {
                depositStatusFactory.setState(depositId, DepositState.paused);
            } finally {
                depositStatusFactory.removeSupervisorLock(depositId);
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

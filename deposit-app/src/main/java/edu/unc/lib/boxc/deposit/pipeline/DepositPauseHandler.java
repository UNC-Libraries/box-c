package edu.unc.lib.boxc.deposit.pipeline;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author bbpennel
 */
public class DepositPauseHandler implements DepositOperationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DepositPauseHandler.class);
    private DepositStatusFactory depositStatusFactory;
    private ActiveDepositsService activeDeposits;

    @Override
    public void handleMessage(DepositOperationMessage opMessage) {
        String depositId = opMessage.getDepositId();
        LOG.info("Pausing deposit {}", depositId);

        if (depositStatusFactory.addSupervisorLock(depositId, opMessage.getUsername())) {
            try {
                depositStatusFactory.setState(depositId, RedisWorkerConstants.DepositState.paused);
                activeDeposits.markInactive(depositId);
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

    public void setActiveDeposits(ActiveDepositsService activeDeposits) {
        this.activeDeposits = activeDeposits;
    }
}

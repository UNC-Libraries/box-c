package edu.unc.lib.boxc.deposit.pipeline;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                depositStatusFactory.setState(depositId, RedisWorkerConstants.DepositState.quieted);
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

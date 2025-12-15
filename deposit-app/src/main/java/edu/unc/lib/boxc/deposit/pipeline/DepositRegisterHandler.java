package edu.unc.lib.boxc.deposit.pipeline;

import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Register a new deposit, adding it to the deposit queue.
 *
 * @author bbpennel
 */
public class DepositRegisterHandler implements DepositOperationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DepositRegisterHandler.class);
    private DepositStatusFactory depositStatusFactory;

    @Override
    public void handleMessage(DepositOperationMessage opMessage) {
        String depositId = opMessage.getDepositId();
        LOG.info("Registering deposit {}", depositId);

        depositStatusFactory.save(depositId, opMessage.getAdditionalInfo());
        depositStatusFactory.queueDeposit(depositId);
    }

    @Override
    public DepositStatusFactory getDepositStatusFactory() {
        return depositStatusFactory;
    }

    public void setDepositStatusFactory(DepositStatusFactory depositStatusFactory) {
        this.depositStatusFactory = depositStatusFactory;
    }
}

package edu.unc.lib.boxc.deposit.pipeline;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for deposit job interruptions.
 *
 * @author bbpennel
 */
public class JobInterruptedHandler implements DepositOperationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(JobInterruptedHandler.class);
    private DepositStatusFactory depositStatusFactory;

    @Override
    public void handleMessage(DepositOperationMessage opMessage) {
        String depositId = opMessage.getDepositId();
        String jobId = opMessage.getJobId();
        LOG.info("Handling interruption for job {} in deposit {}: {}",
                jobId, depositId, opMessage.getExceptionClassName());

        if (DepositState.running.equals(depositStatusFactory.getState(depositId))) {
            depositStatusFactory.setState(depositId, DepositState.quieted);
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

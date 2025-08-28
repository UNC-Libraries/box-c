package edu.unc.lib.boxc.deposit.pipeline;

import edu.unc.lib.boxc.deposit.CleanupDepositJob;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessageService;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.jms.DepositCompleteService;
import edu.unc.lib.boxc.deposit.jms.DepositJobMessageFactory;
import edu.unc.lib.boxc.deposit.work.DepositEmailHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Deposit operation handler for successful job completion.
 *
 * @author bbpennel
 */
public class JobSuccessHandler implements DepositOperationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(JobSuccessHandler.class);
    private DepositJobMessageService depositJobMessageService;
    private DepositJobMessageFactory depositJobMessageFactory;
    private DepositStatusFactory depositStatusFactory;
    private DepositEmailHandler depositEmailHandler;
    private DepositCompleteService depositCompleteService;
    private int cleanupDelaySeconds;

    @Override
    public void handleMessage(DepositOperationMessage opMessage) {
        String depositId = opMessage.getDepositId();
        LOG.debug("Handling success of job {} for deposit {}", opMessage.getJobId(), depositId);
        // Mark the job as completed
        DepositState depositState = depositStatusFactory.getState(depositId);
        if (!DepositState.running.equals(depositState)){
            LOG.warn("Not queueing next job for deposit {} because it is in state {}",
                    depositId, depositState);
            return;
        }

        // Queue the next job for the deposit
        var depositStatus = getDepositStatus(depositId);
        var nextMessage = depositJobMessageFactory.createNextJobMessage(depositId, depositStatus);
        if (nextMessage.getJobClassName().equals(CleanupDepositJob.class.getName())) {
            // If the next job is a cleanup job, we need to finalize the deposit
            depositStatusFactory.setState(depositId, DepositState.finished);
            depositDuration(depositId, depositStatus);
            // Send email notification of deposit completion
            depositEmailHandler.sendDepositResults(depositId);
            // Send JMS message indicating the deposit has completed
            depositCompleteService.sendDepositCompleteEvent(depositId);
            // Queue the cleanup job after a delay
            depositJobMessageService.sendDepositJobMessage(nextMessage, cleanupDelaySeconds);
        } else {
            LOG.info("Queuing next job {} for deposit {}", nextMessage.getJobClassName(), depositId);
            depositJobMessageService.sendDepositJobMessage(nextMessage);
        }
    }

    private void depositDuration(String depositUUID, Map<String, String> status) {
        String strDepositStartTime = status.get(RedisWorkerConstants.DepositField.startTime.name());
        if (strDepositStartTime == null) {
            return;
        }
        long depositEndTime = System.currentTimeMillis();

        String strDepositEndTime = Long.toString(depositEndTime);
        depositStatusFactory.set(depositUUID, RedisWorkerConstants.DepositField.endTime, strDepositEndTime);
    }

    @Override
    public DepositStatusFactory getDepositStatusFactory() {
        return depositStatusFactory;
    }

    public void setDepositJobMessageService(DepositJobMessageService depositJobMessageService) {
        this.depositJobMessageService = depositJobMessageService;
    }

    public void setDepositJobMessageFactory(DepositJobMessageFactory depositJobMessageFactory) {
        this.depositJobMessageFactory = depositJobMessageFactory;
    }

    public void setDepositStatusFactory(DepositStatusFactory depositStatusFactory) {
        this.depositStatusFactory = depositStatusFactory;
    }

    public void setDepositEmailHandler(DepositEmailHandler depositEmailHandler) {
        this.depositEmailHandler = depositEmailHandler;
    }

    public void setDepositCompleteService(DepositCompleteService depositCompleteService) {
        this.depositCompleteService = depositCompleteService;
    }

    public void setCleanupDelaySeconds(int cleanupDelaySeconds) {
        this.cleanupDelaySeconds = cleanupDelaySeconds;
    }
}

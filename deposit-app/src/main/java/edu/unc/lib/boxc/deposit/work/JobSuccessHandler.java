package edu.unc.lib.boxc.deposit.work;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.unc.lib.boxc.deposit.CleanupDepositJob;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessageService;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.JobStatusFactory;
import edu.unc.lib.boxc.deposit.jms.DepositCompleteService;
import edu.unc.lib.boxc.deposit.jms.DepositJobMessageFactory;
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
    private JobStatusFactory jobStatusFactory;
    private int cleanupDelaySeconds;

    @Override
    public void handleMessage(DepositOperationMessage opMessage) {
        String depositId = opMessage.getDepositId();
        LOG.debug("Handling success of job {} for deposit {}", opMessage.getJobId(), depositId);
        // Mark the job as completed
        jobStatusFactory.completed(opMessage.getJobId());

        // Queue the next job for the deposit
        var depositStatus = getDepositStatus(depositId);
        var nextMessage = depositJobMessageFactory.createNextJobMessage(depositId, depositStatus);
        if (nextMessage.getJobClassName().equals(CleanupDepositJob.class.getName())) {
            // If the next job is a cleanup job, we need to finalize the deposit
            depositStatusFactory.setState(depositId, RedisWorkerConstants.DepositState.finished);
            depositDuration(depositId, depositStatus);
            // Send email notification of deposit completion
            depositEmailHandler.sendDepositResults(depositId);
            // Send JMS message indicating the deposit has completed
            depositCompleteService.sendDepositCompleteEvent(depositId);
            // Queue the cleanup job after a delay
            try {
                depositJobMessageService.sendDepositJobMessage(nextMessage, cleanupDelaySeconds);
            } catch (JsonProcessingException e) {
                throw new DepositFailedException("Failed to submit first job for deposit " + depositId, e);
            }
        } else {
            LOG.info("Queuing next job {} for deposit {}", nextMessage.getJobClassName(), depositId);
            try {
                depositJobMessageService.sendDepositJobMessage(nextMessage);
            } catch (JsonProcessingException e) {
                throw new DepositFailedException("Failed to submit first job for deposit " + depositId, e);
            }
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

    public void setJobStatusFactory(JobStatusFactory jobStatusFactory) {
        this.jobStatusFactory = jobStatusFactory;
    }

    public void setCleanupDelaySeconds(int cleanupDelaySeconds) {
        this.cleanupDelaySeconds = cleanupDelaySeconds;
    }
}

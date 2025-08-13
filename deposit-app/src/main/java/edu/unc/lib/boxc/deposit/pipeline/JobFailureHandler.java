package edu.unc.lib.boxc.deposit.pipeline;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.JobStatusFactory;
import edu.unc.lib.boxc.deposit.work.DepositEmailHandler;
import edu.unc.lib.boxc.deposit.work.JobFailedException;
import edu.unc.lib.boxc.deposit.work.JobInterruptedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Handler for deposit job failures
 *
 * @author bbpennel
 */
public class JobFailureHandler implements DepositOperationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(JobFailureHandler.class);
    private DepositStatusFactory depositStatusFactory;
    private JobStatusFactory jobStatusFactory;
    private ActiveDepositsService activeDeposits;
    private DepositEmailHandler depositEmailHandler;

    @Override
    public void handleMessage(DepositOperationMessage opMessage) {
        String depositId = opMessage.getDepositId();
        String jobId = opMessage.getJobId();
        LOG.info("Handling failure for job {}", depositId);

        if (depositStatusFactory.addSupervisorLock(depositId, opMessage.getUsername())) {
            try {
                if (JobInterruptedException.class.getName().equals(opMessage.getExceptionClassName())) {
                    LOG.info("Job {} in deposit {} was interrupted: {}",
                            jobId, depositId, opMessage.getExceptionMessage());
                    jobStatusFactory.interrupted(jobId);
                    return;
                }
                LOG.error("Job {} in deposit {} failed with exception: {}\n{}",
                        jobId, depositId, opMessage.getExceptionMessage(), opMessage.getExceptionStackTrace());
                jobStatusFactory.failed(jobId);
                if (JobFailedException.class.getName().equals(opMessage.getExceptionClassName())) {
                    depositStatusFactory.fail(depositId, opMessage.getExceptionMessage());
                } else {
                    String serviceName = StringUtils.substringAfterLast(opMessage.getExceptionClassName(), ".");
                    depositStatusFactory.fail(depositId, "Failed while performing service " + serviceName);
                }
                depositDuration(depositId, getDepositStatus(depositId));
                depositEmailHandler.sendDepositResults(depositId);
            } finally {
                activeDeposits.markInactive(depositId);
                depositStatusFactory.removeSupervisorLock(depositId);
            }
        }
    }

    private void depositDuration(String depositUUID, Map<String, String> status) {
        String strDepositStartTime = status.get(DepositField.startTime.name());
        if (strDepositStartTime == null) {
            return;
        }
        long depositEndTime = System.currentTimeMillis();

        String strDepositEndTime = Long.toString(depositEndTime);
        depositStatusFactory.set(depositUUID, DepositField.endTime, strDepositEndTime);
    }

    @Override
    public DepositStatusFactory getDepositStatusFactory() {
        return depositStatusFactory;
    }

    public void setDepositStatusFactory(DepositStatusFactory depositStatusFactory) {
        this.depositStatusFactory = depositStatusFactory;
    }

    public void setJobStatusFactory(JobStatusFactory jobStatusFactory) {
        this.jobStatusFactory = jobStatusFactory;
    }

    public void setActiveDeposits(ActiveDepositsService activeDeposits) {
        this.activeDeposits = activeDeposits;
    }

    public void setDepositEmailHandler(DepositEmailHandler depositEmailHandler) {
        this.depositEmailHandler = depositEmailHandler;
    }
}

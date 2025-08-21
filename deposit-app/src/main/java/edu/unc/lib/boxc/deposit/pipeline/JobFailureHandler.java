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
    private ActiveDepositsService activeDeposits;
    private DepositEmailHandler depositEmailHandler;

    @Override
    public void handleMessage(DepositOperationMessage opMessage) {
        String depositId = opMessage.getDepositId();
        String jobId = opMessage.getJobId();

        if (depositStatusFactory.addSupervisorLock(depositId, opMessage.getUsername())) {
            try {
                LOG.debug("Job {} in deposit {} failed with exception: {}\n{}",
                        jobId, depositId, opMessage.getExceptionMessage(), opMessage.getExceptionStackTrace());
                if (isExceptionOfType(opMessage, JobFailedException.class)) {
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

    private boolean isExceptionOfType(DepositOperationMessage opMessage, Class<?> exceptionType) {
        try {
            Class<?> exceptionClass = Class.forName(opMessage.getExceptionClassName());
            return exceptionType.isAssignableFrom(exceptionClass);
        } catch (ClassNotFoundException e) {
            LOG.warn("Could not load exception class: {}", opMessage.getExceptionClassName());
            return false;
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

    public void setActiveDeposits(ActiveDepositsService activeDeposits) {
        this.activeDeposits = activeDeposits;
    }

    public void setDepositEmailHandler(DepositEmailHandler depositEmailHandler) {
        this.depositEmailHandler = depositEmailHandler;
    }
}

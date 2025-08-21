package edu.unc.lib.boxc.deposit.pipeline;

import edu.unc.lib.boxc.deposit.api.DepositOperation;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessageService;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessageService;
import edu.unc.lib.boxc.deposit.impl.model.DepositPipelineStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.JobStatusFactory;
import edu.unc.lib.boxc.deposit.work.JobInterruptedException;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.IOException;
import java.util.Arrays;

/**
 * Coordinates the execution of deposit jobs by listening for job messages
 *
 * @author bbpennel
 */
public class JobCoordinator implements MessageListener, ApplicationContextAware {
    private static final Logger LOG = LoggerFactory.getLogger(JobCoordinator.class);
    private DepositJobMessageService depositJobMessageService;
    private DepositOperationMessageService depositOperationMessageService;
    private DepositPipelineStatusFactory depositPipelineStatusFactory;
    private ApplicationContext applicationContext;
    private ActiveDepositsService activeDeposits;
    private JobStatusFactory jobStatusFactory;

    @Override
    public void onMessage(Message message) {
        // Check if queues are active, if not then do not acknowledge the message and stop processing
        if (!isAcceptingMessages()) {
            LOG.warn("Ignoring message due to pipeline state");
            return;
        }

        var jobMessage = loadJobMessage(message);
        String depositId = jobMessage.getDepositId();
        String jobId = jobMessage.getJobId();
        if (!activeDeposits.isDepositActive(depositId)) {
            LOG.warn("Skipping job message {} for deposit {} because the deposit is not active", jobId, depositId);
            acknowledgeMessage(message);
            return;
        }

        var jobRunnable = getJobRunnable(jobMessage);
        LOG.debug("Got job runnable {}", jobRunnable.getClass().getName());
        try {
            jobStatusFactory.started(jobId, depositId, jobRunnable.getClass());
            jobRunnable.run();
            var successMessage = buildSuccessMessage(jobMessage);
            sendDepositOperationMessage(successMessage);
        } catch (JobInterruptedException e) {
            LOG.info("Job {} in deposit {} was interrupted: {}", jobId, depositId, e.getMessage());
            jobStatusFactory.interrupted(jobId);
            sendDepositOperationMessage(buildInterruptedMessage(jobMessage, e));
        } catch (Exception e) {
            LOG.error("Error processing job message {} for deposit {}", jobId, depositId, e);
            jobStatusFactory.failed(jobId);
            sendDepositOperationMessage(buildFailureMessage(jobMessage, e));
        } finally {
            acknowledgeMessage(message);
        }
    }

    private boolean isAcceptingMessages() {
        var pipelineState = depositPipelineStatusFactory.getPipelineState();
        return DepositPipelineState.active.equals(pipelineState) || DepositPipelineState.starting.equals(pipelineState);
    }

    private void acknowledgeMessage(Message message) {
        try {
            message.acknowledge();
            LOG.debug("Acknowledged message {}", message.getJMSMessageID());
        } catch (JMSException e) {
            LOG.error("Error acknowledging message", e);
        }
    }

    private DepositOperationMessage buildSuccessMessage(DepositJobMessage jobMessage) {
        var successMessage = new DepositOperationMessage();
        successMessage.setJobId(jobMessage.getJobId());
        successMessage.setDepositId(jobMessage.getDepositId());
        successMessage.setAction(DepositOperation.JOB_SUCCESS);
        successMessage.setUsername(jobMessage.getUsername());
        return successMessage;
    }

    private DepositOperationMessage buildFailureMessage(DepositJobMessage jobMessage, Exception e) {
        var failureMessage = new DepositOperationMessage();
        failureMessage.setJobId(jobMessage.getJobId());
        failureMessage.setDepositId(jobMessage.getDepositId());
        failureMessage.setAction(DepositOperation.JOB_FAILURE);
        failureMessage.setUsername(jobMessage.getUsername());
        failureMessage.setExceptionClassName(e.getClass().getName());
        failureMessage.setExceptionMessage(e.getMessage());
        failureMessage.setExceptionStackTrace(e.getStackTrace() != null ?
                Arrays.toString(e.getStackTrace()) : null);
        return failureMessage;
    }

    private DepositOperationMessage buildInterruptedMessage(DepositJobMessage jobMessage, Exception e) {
        var message = new DepositOperationMessage();
        message.setJobId(jobMessage.getJobId());
        message.setDepositId(jobMessage.getDepositId());
        message.setAction(DepositOperation.JOB_INTERRUPTED);
        message.setUsername(jobMessage.getUsername());
        message.setExceptionClassName(e.getClass().getName());
        message.setExceptionMessage(e.getMessage());
        return message;
    }

    private void sendDepositOperationMessage(DepositOperationMessage message) {
        depositOperationMessageService.sendDepositOperationMessage(message);
    }

    private DepositJobMessage loadJobMessage(Message message) {
        try {
            return depositJobMessageService.fromJson(message);
        } catch (IOException | JMSException e) {
            acknowledgeMessage(message);
            throw new RuntimeException("Unable to load deposit job message", e);
        }
    }

    private Runnable getJobRunnable(DepositJobMessage jobMessage) {
        try {
            return (Runnable) applicationContext.getBean(Class.forName(jobMessage.getJobClassName()),
                    new Object[] { jobMessage.getJobId(), jobMessage.getDepositId() });
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load job class: " + jobMessage.getJobClassName(), e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setDepositJobMessageService(DepositJobMessageService depositJobMessageService) {
        this.depositJobMessageService = depositJobMessageService;
    }

    public void setDepositOperationMessageService(DepositOperationMessageService depositOperationMessageService) {
        this.depositOperationMessageService = depositOperationMessageService;
    }

    public void setDepositPipelineStatusFactory(DepositPipelineStatusFactory depositPipelineStatusFactory) {
        this.depositPipelineStatusFactory = depositPipelineStatusFactory;
    }

    public void setJobStatusFactory(JobStatusFactory jobStatusFactory) {
        this.jobStatusFactory = jobStatusFactory;
    }

    public void setActiveDeposits(ActiveDepositsService activeDeposits) {
        this.activeDeposits = activeDeposits;
    }
}

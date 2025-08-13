package edu.unc.lib.boxc.deposit.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.unc.lib.boxc.deposit.api.DepositOperation;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessageService;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessageService;
import edu.unc.lib.boxc.deposit.impl.model.DepositPipelineStatusFactory;
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

    @Override
    public void onMessage(Message message) {
        // Check if queues are active, if not then do not acknowledge the message and stop processing
        if (!DepositPipelineState.active.equals(depositPipelineStatusFactory.getPipelineState())) {
            return;
        }

        var jobMessage = loadJobMessage(message);
        if (!activeDeposits.isDepositActive(jobMessage.getDepositId())) {
            LOG.warn("Skipping job message {} for deposit {} because the deposit is not active",
                    jobMessage.getJobId(), jobMessage.getDepositId());
            acknowledgeMessage(message);
            return;
        }

        var jobRunnable = getJobRunnable(jobMessage);
        try {
            jobRunnable.run();
            var successMessage = buildSuccessMessage(jobMessage);
            sendDepositOperationMessage(successMessage);
            acknowledgeMessage(message);
        } catch (Exception e) {
            LOG.error("Error processing job message {} for deposit {}",
                    jobMessage.getJobId(), jobMessage.getDepositId(), e);
            var failureMessage = buildFailureMessage(jobMessage, e);
            sendDepositOperationMessage(failureMessage);
            // Not acknowledging the message if the operation message doesn't send, so it can be retried
            acknowledgeMessage(message);
        }
    }

    private void acknowledgeMessage(Message message) {
        try {
            message.acknowledge();
        } catch (JMSException e) {
            LOG.error("Error acknowledging message", e);
        }
    }

    private DepositOperationMessage buildSuccessMessage(DepositJobMessage jobMessage) {
        var successMessage = new DepositOperationMessage();
        successMessage.setJobId(jobMessage.getJobId());
        successMessage.setDepositId(jobMessage.getDepositId());
        successMessage.setAction(DepositOperation.JOB_SUCCESS);
        return successMessage;
    }

    private DepositOperationMessage buildFailureMessage(DepositJobMessage jobMessage, Exception e) {
        var failureMessage = new DepositOperationMessage();
        failureMessage.setJobId(jobMessage.getJobId());
        failureMessage.setDepositId(jobMessage.getDepositId());
        failureMessage.setAction(DepositOperation.JOB_FAILURE);
        failureMessage.setExceptionClassName(e.getClass().getName());
        failureMessage.setExceptionMessage(e.getMessage());
        failureMessage.setExceptionStackTrace(e.getStackTrace() != null ?
                Arrays.toString(e.getStackTrace()) : null);
        return failureMessage;
    }

    private void sendDepositOperationMessage(DepositOperationMessage message) {
        try {
            depositOperationMessageService.sendDepositOperationMessage(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to send deposit operation message", e);
        }
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
            return (Runnable) applicationContext.getBean(Class.forName(jobMessage.getJobClassName()));
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

    public void setActiveDeposits(ActiveDepositsService activeDeposits) {
        this.activeDeposits = activeDeposits;
    }
}

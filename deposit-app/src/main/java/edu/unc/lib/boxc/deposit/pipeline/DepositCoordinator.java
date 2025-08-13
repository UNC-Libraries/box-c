package edu.unc.lib.boxc.deposit.pipeline;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessageService;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessageService;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.jms.DepositJobMessageFactory;
import jakarta.annotation.PostConstruct;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Service for coordinating deposit operations and job execution.
 *
 * @author bbpennel
 */
public class DepositCoordinator implements MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(DepositCoordinator.class);

    private ActiveDepositsService activeDeposits;
    private DepositStatusFactory depositStatusFactory;

    private DepositOperationMessageService depositOperationMessageService;
    private JobSuccessHandler jobSuccessHandler;
    private DepositResumeHandler depositResumeHandler;
    private DepositRegisterHandler depositRegisterHandler;
    private DepositPauseHandler depositPauseHandler;
    private JobFailureHandler jobFailureHandler;
    protected DepositJobMessageFactory depositJobMessageFactory;
    protected DepositJobMessageService depositJobMessageService;

    @PostConstruct
    public void init() {
        // TODO Poll for in progress deposits and repopulate tracking information
        // Make sure all the deposits in queued state are in the queue
    }

    @Override
    public void onMessage(Message message) {
        // Inspect the message to determine what event occurred
        DepositOperationMessage opMessage = null;
        try {
            opMessage = depositOperationMessageService.fromJson(message);
            switch(opMessage.getAction()) {
            case REGISTER -> depositRegisterHandler.handleMessage(opMessage);
            case PAUSE -> depositPauseHandler.handleMessage(opMessage);
            case RESUME -> depositResumeHandler.handleMessage(opMessage);
            case JOB_SUCCESS -> jobSuccessHandler.handleMessage(opMessage);
            case JOB_FAILURE -> jobFailureHandler.handleMessage(opMessage);
            default -> throw new IllegalArgumentException("Unknown deposit action: " + opMessage.getAction());
            }
            startNextDepositIfNeeded(opMessage);
        } catch (Exception e) {
            LOG.error("Error processing deposit operation message", e);
            if (opMessage != null) {
                depositStatusFactory.fail(opMessage.getDepositId());
                activeDeposits.markInactive(opMessage.getDepositId());
            }
        } finally {
            try {
                message.acknowledge();
            } catch (JMSException e) {
                LOG.error("Error acknowledging deposit operation message", e);
            }
        }
    }

    private synchronized void startNextDepositIfNeeded(DepositOperationMessage opMessage) {
        String depositId = opMessage.getDepositId();
        // Check if there are workers available to handle the deposit
        if (!activeDeposits.acceptingNewDeposits()) {
            LOG.debug("No available workers to start next deposit");
            return;
        }
        // Check if the deposit is in a state that would free up a worker or populate the queue
        var depositState = depositStatusFactory.getState(depositId);
        if (DepositState.paused.equals(depositState) || DepositState.finished.equals(depositState)
                || DepositState.failed.equals(depositState) || DepositState.queued.equals(depositState)) {
            // Start next deposit if there is one waiting
            String nextDepositId = depositStatusFactory.getFirstQueuedDeposit();
            if (nextDepositId == null) {
                LOG.debug("No next deposit to start");
            } else {
                LOG.info("Starting next deposit: {}", nextDepositId);
                startDeposit(nextDepositId);
            }
        }
    }

    private void startDeposit(String depositId) {
        var depositStatus = depositStatusFactory.get(depositId);
        var depositUser = depositStatus.get(DepositField.depositorName.name());
        if (depositStatusFactory.addSupervisorLock(depositId, depositUser)) {
            try {
                activeDeposits.markActive(depositId);
                depositStatusFactory.setState(depositId, DepositState.running);
                assignStartTime(depositId, depositStatus);

                var jobMessage = depositJobMessageFactory.createNextJobMessage(depositId, depositStatus);
                depositJobMessageService.sendDepositJobMessage(jobMessage);
            } catch (Exception e) {
                LOG.error("Error sending deposit job message for {}", depositId, e);
                depositStatusFactory.fail(depositId);
                activeDeposits.markInactive(depositId);
            } finally {
                depositStatusFactory.removeSupervisorLock(depositId);
            }
        }
    }

    private void assignStartTime(String depositId, Map<String, String> depositStatus) {
        if (depositStatus.containsKey(DepositField.startTime.name())) {
            return;
        }
        long depositStartTime = System.currentTimeMillis();
        String strDepositStartTime = Long.toString(depositStartTime);
        depositStatusFactory.set(depositId, DepositField.startTime, strDepositStartTime);
    }

    public void setActiveDeposits(ActiveDepositsService activeDeposits) {
        this.activeDeposits = activeDeposits;
    }

    public void setDepositStatusFactory(DepositStatusFactory depositStatusFactory) {
        this.depositStatusFactory = depositStatusFactory;
    }

    public void setDepositOperationMessageService(DepositOperationMessageService depositOperationMessageService) {
        this.depositOperationMessageService = depositOperationMessageService;
    }

    public void setJobSuccessHandler(JobSuccessHandler jobSuccessHandler) {
        this.jobSuccessHandler = jobSuccessHandler;
    }

    public void setDepositResumeHandler(DepositResumeHandler depositResumeHandler) {
        this.depositResumeHandler = depositResumeHandler;
    }

    public void setDepositRegisterHandler(DepositRegisterHandler depositRegisterHandler) {
        this.depositRegisterHandler = depositRegisterHandler;
    }

    public void setDepositPauseHandler(DepositPauseHandler depositPauseHandler) {
        this.depositPauseHandler = depositPauseHandler;
    }

    public void setJobFailureHandler(JobFailureHandler jobFailureHandler) {
        this.jobFailureHandler = jobFailureHandler;
    }

    public void setDepositJobMessageFactory(DepositJobMessageFactory depositJobMessageFactory) {
        this.depositJobMessageFactory = depositJobMessageFactory;
    }

    public void setDepositJobMessageService(DepositJobMessageService depositJobMessageService) {
        this.depositJobMessageService = depositJobMessageService;
    }
}

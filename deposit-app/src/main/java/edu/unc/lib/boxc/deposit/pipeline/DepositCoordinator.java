package edu.unc.lib.boxc.deposit.pipeline;

import edu.unc.lib.boxc.deposit.api.DepositOperation;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineState;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessageService;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessageService;
import edu.unc.lib.boxc.deposit.impl.model.DepositPipelineStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.jms.DepositJobMessageFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service for coordinating deposit operations and job execution.
 *
 * @author bbpennel
 */
public class DepositCoordinator implements MessageListener {
    private static final Logger LOG = LoggerFactory.getLogger(DepositCoordinator.class);

    private ActiveDepositsService activeDeposits;
    private DepositStatusFactory depositStatusFactory;

    private JobSuccessHandler jobSuccessHandler;
    private DepositResumeHandler depositResumeHandler;
    private DepositRegisterHandler depositRegisterHandler;
    private DepositPauseHandler depositPauseHandler;
    private JobFailureHandler jobFailureHandler;
    private JobInterruptedHandler jobInterruptedHandler;
    private DepositPipelineStatusFactory pipelineStatusFactory;
    protected DepositJobMessageFactory depositJobMessageFactory;
    protected DepositJobMessageService depositJobMessageService;

    public void init() {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.starting);
        // Restore queued deposits to the Redis queue before scheduling available work.
        requeueAll();
        pipelineStatusFactory.setPipelineState(DepositPipelineState.active);
    }

    @Override
    public void onMessage(Message message) {
        // Inspect the message to determine what event occurred
        DepositOperationMessage opMessage = null;
        String lockToken = null;
        boolean lockAcquired = false;
        boolean acknowledgeMessage = true;
        try {
            opMessage = DepositOperationMessageService.fromJson(message);
            LOG.debug("Got deposit operation message {} for {}", opMessage.getAction(), opMessage.getDepositId());
            lockToken = UUID.randomUUID().toString();
            lockAcquired = depositStatusFactory.addSupervisorLock(opMessage.getDepositId(), lockToken);
            if (!lockAcquired) {
                acknowledgeMessage = false;
                throw new DepositLockUnavailableException(opMessage.getDepositId());
            }
            switch (opMessage.getAction()) {
            case REGISTER -> depositRegisterHandler.handleMessage(opMessage);
            case PAUSE -> depositPauseHandler.handleMessage(opMessage);
            case RESUME -> depositResumeHandler.handleMessage(opMessage);
            case JOB_SUCCESS -> jobSuccessHandler.handleMessage(opMessage);
            case JOB_FAILURE -> jobFailureHandler.handleMessage(opMessage);
            case JOB_INTERRUPTED -> jobInterruptedHandler.handleMessage(opMessage);
            default -> throw new IllegalArgumentException("Unknown deposit action: " + opMessage.getAction());
            }
            var depositState = depositStatusFactory.getState(opMessage.getDepositId());
            updateActiveDeposits(opMessage.getDepositId(), depositState);
            startNextDepositIfNeeded(opMessage.getDepositId(), depositState);
        } catch (DepositLockUnavailableException e) {
            LOG.info(e.getMessage());
            throw e;
        } catch (Exception e) {
            LOG.error("Error processing deposit operation message", e);
            if (opMessage != null) {
                depositStatusFactory.fail(opMessage.getDepositId());
                activeDeposits.markInactive(opMessage.getDepositId());
            }
        } finally {
            if (lockAcquired) {
                depositStatusFactory.removeSupervisorLock(opMessage.getDepositId(), lockToken);
            }
            if (acknowledgeMessage) {
                try {
                    message.acknowledge();
                } catch (JMSException e) {
                    LOG.error("Error acknowledging deposit operation message", e);
                }
            }
        }
    }

    private static class DepositLockUnavailableException extends RuntimeException {
        DepositLockUnavailableException(String depositId) {
            super("Supervisor lock unavailable for deposit " + depositId + "; retrying operation message");
        }
    }

    private void updateActiveDeposits(String depositId, DepositState depositState) {
        if (DepositState.running.equals(depositState)) {
            LOG.debug("Marking deposit {} as active", depositId);
            activeDeposits.markActive(depositId);
        } else {
            LOG.debug("Marking deposit {} as inactive due to its state {}", depositId, depositState);
            activeDeposits.markInactive(depositId);
        }
    }

    private synchronized void startNextDepositIfNeeded(String depositId, DepositState depositState) {
        // Check if there are workers available to handle the deposit
        if (!activeDeposits.acceptingNewDeposits()) {
            LOG.debug("No available workers to start next deposit");
            return;
        }
        // Check if the deposit is in a state that would free up a worker or populate the queue
        if (DepositState.paused.equals(depositState) || DepositState.finished.equals(depositState)
                || DepositState.failed.equals(depositState) || DepositState.queued.equals(depositState)) {
            // Start next deposit if there is one waiting
            String nextDepositId = depositStatusFactory.getFirstQueuedDeposit();
            if (nextDepositId == null) {
                LOG.debug("No next deposit to start");
            } else {
                LOG.info("Starting next deposit: {}", nextDepositId);
                // Start the next deposit, locking it if it is different from the current deposit.
                // The current deposit is already locked. The queue entry is removed only after that lock is held.
                startDeposit(nextDepositId, !depositId.equals(nextDepositId));
            }
        }
    }

    /**
     * Claims and starts a queued deposit.
     * @param depositId id of the deposit
     * @param lockDeposit {@code true} if the worker is starting a different deposit and needs to lock,
     *                    {@code false} if the worker was already processing the deposit.
     * @return Returns {@code true} when the deposit was started or terminally handled, so queue
     *      processing may continue. Returns {@code false} when it remains queued because its lock
     *      could not be acquired or it could not be marked as active.
     */
    private boolean startDeposit(String depositId, boolean lockDeposit) {
        String lockToken = UUID.randomUUID().toString();
        // Try to acquire the next deposit's lock. If unavailable, another worker still holds it.
        if (lockDeposit && !depositStatusFactory.addSupervisorLock(depositId, lockToken)) {
            LOG.debug("Deposit {} remains queued because its supervisor lock is unavailable", depositId);
            return false;
        }
        try {
            // Worker got the lock, but it was already running so remove it from the pending queue.
            if (activeDeposits.isDepositActive(depositId)
                    || DepositState.running.equals(depositStatusFactory.getState(depositId))) {
                LOG.warn("Skipping deposit {}, deposit is already running", depositId);
                depositStatusFactory.removeQueuedDeposit(depositId);
                return true;
            }

            var depositStatus = depositStatusFactory.get(depositId);
            // Attempt to make deposit active, and if we can't then don't remove it from the queue
            if (!activeDeposits.markActive(depositId)) {
                LOG.debug("Deposit {} remains queued because it was not admitted to the active set", depositId);
                return false;
            }
            // This callback holds the deposit's lock, so it can safely claim the queue entry.
            depositStatusFactory.removeQueuedDeposit(depositId);
            depositStatusFactory.setState(depositId, DepositState.running);
            assignStartTime(depositId, depositStatus);

            var jobMessage = depositJobMessageFactory.createNextJobMessage(depositId, depositStatus);
            depositJobMessageService.sendDepositJobMessage(jobMessage);
            return true;
        } catch (Exception e) {
            LOG.error("Error sending deposit job message for {}", depositId, e);
            depositStatusFactory.fail(depositId);
            activeDeposits.markInactive(depositId);
            return true;
        } finally {
            if (lockDeposit) {
                depositStatusFactory.removeSupervisorLock(depositId, lockToken);
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

    private void requeueAll() {
        Set<Map<String, String>> depositStatuses = depositStatusFactory.getAll();
        for (Map<String, String> fields : depositStatuses) {
            LOG.debug("Scanning deposits during startup: {}", fields.get(DepositField.uuid.name()));
            DepositState depositState = DepositState.valueOf(fields.get(DepositField.state.name()));
            String depositId = fields.get(DepositField.uuid.name());
            if (DepositState.running.equals(depositState)) {
                LOG.info("Reactivating deposit {} during startup", depositId);
                depositStatusFactory.forceRemoveSupervisorLock(depositId);
                activeDeposits.markActive(depositId);
            }
            if (DepositState.quieted.equals(depositState)) {
                // Job may have been locked to a particular supervisor depend on when it was interrupted
                LOG.info("Unquieting deposit {} during startup", depositId);
                depositStatusFactory.forceRemoveSupervisorLock(depositId);
                var opMessage = new DepositOperationMessage(DepositOperation.RESUME, depositId,
                        fields.get(DepositField.depositorName.name()));
                depositResumeHandler.handleMessage(opMessage);
            }
            if (DepositState.queued.equals(depositState)) {
                LOG.info("Restoring queued deposit {} during startup", depositId);
                depositStatusFactory.forceRemoveSupervisorLock(depositId);
                depositStatusFactory.queueDeposit(depositId);
            }
        }
        startQueuedDeposits();
    }

    /**
     * Starts queued deposits until the next queued deposit cannot be started. A deposit that is
     * already running or fails to start is terminally handled and permits processing the next
     * queue entry. Stops when the queue head cannot be claimed or admitted to the active set.
     */
    private void startQueuedDeposits() {
        while (activeDeposits.acceptingNewDeposits()) {
            String depositId = depositStatusFactory.getFirstQueuedDeposit();
            if (depositId == null) {
                return;
            }
            if (!startDeposit(depositId, true)) {
                return;
            }
        }
    }

    public void setActiveDeposits(ActiveDepositsService activeDeposits) {
        this.activeDeposits = activeDeposits;
    }

    public void setDepositStatusFactory(DepositStatusFactory depositStatusFactory) {
        this.depositStatusFactory = depositStatusFactory;
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

    public void setJobInterruptedHandler(JobInterruptedHandler jobInterruptedHandler) {
        this.jobInterruptedHandler = jobInterruptedHandler;
    }

    public void setDepositJobMessageFactory(DepositJobMessageFactory depositJobMessageFactory) {
        this.depositJobMessageFactory = depositJobMessageFactory;
    }

    public void setDepositJobMessageService(DepositJobMessageService depositJobMessageService) {
        this.depositJobMessageService = depositJobMessageService;
    }

    public void setPipelineStatusFactory(DepositPipelineStatusFactory pipelineStatusFactory) {
        this.pipelineStatusFactory = pipelineStatusFactory;
    }
}

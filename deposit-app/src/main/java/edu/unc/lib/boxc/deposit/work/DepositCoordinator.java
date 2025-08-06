package edu.unc.lib.boxc.deposit.work;

import edu.unc.lib.boxc.deposit.impl.jms.DepositJobMessageService;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessageService;
import jakarta.annotation.PostConstruct;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import java.io.IOException;
import java.util.Set;

/**
 * @author bbpennel
 */
public class DepositCoordinator implements MessageListener {
    private Set<String> activeDepositIds;
    private DepositJobMessageService depositJobMessageService;
    private DepositOperationMessageService depositOperationMessageService;

    @PostConstruct
    public void init() {
        // Poll for in progress deposits and repopulate tracking information
    }

    @Override
    public void onMessage(Message message) {
        // Inspect the message to determine what event occurred
        try {
            var opMessage = depositOperationMessageService.fromJson(message);
            switch(opMessage.getAction()) {
            case REGISTER -> handleRegister(opMessage);
            case PAUSE -> handlePause(opMessage);
            case RESUME -> handleResume(opMessage);
            case CANCEL -> handleCancel(opMessage);
            case DESTROY -> handleDestroy(opMessage);
            case JOB_SUCCESS -> handleJobSuccess(opMessage);
            case JOB_FAILURE -> handleJobFailure(opMessage);
            default -> throw new IllegalArgumentException("Unknown deposit action: " + opMessage.getAction());
            }
        } catch (IOException | JMSException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleRegister(DepositOperationMessage opMessage) {
        // Register the depositId in the active deposits set
        activeDepositIds.add(opMessage.getDepositId());
    }

    private void handlePause(DepositOperationMessage opMessage) {

    }

    private void handleResume(DepositOperationMessage opMessage) {

    }

    private void handleCancel(DepositOperationMessage opMessage) {

    }

    private void handleDestroy(DepositOperationMessage opMessage) {

    }

    private void handleJobSuccess(DepositOperationMessage opMessage) {

    }

    private void handleJobFailure(DepositOperationMessage opMessage) {

    }
}

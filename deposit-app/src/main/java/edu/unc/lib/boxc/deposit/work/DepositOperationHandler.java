package edu.unc.lib.boxc.deposit.work;

import edu.unc.lib.boxc.deposit.impl.jms.DepositOperationMessage;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;

import java.util.Map;

/**
 * Interface for handling deposit operations
 *
 * @author bbpennel
 */
public interface DepositOperationHandler {

    /**
     * Perform the operation specified in the deposit operation message.
     *
     * @param opMessage The deposit operation message
     */
    void handleMessage(DepositOperationMessage opMessage);

    /**
     * Get the status of a deposit
     *
     * @param depositId The ID of the deposit
     * @return Map containing the deposit status
     */
    default Map<String, String> getDepositStatus(String depositId) {
        return getDepositStatusFactory().get(depositId);
    }

    /**
     * @return The deposit status factory
     */
    DepositStatusFactory getDepositStatusFactory();
}

package edu.unc.lib.boxc.deposit.api.submit;

import edu.unc.lib.boxc.deposit.api.exceptions.DepositException;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Interface for a deposit handler used to submit deposits to the deposit pipeline
 *
 * @author bbpennel
 *
 */
public interface DepositHandler {
    /**
     * Perform this deposit handler, submitting a deposit request to the pipeline
     *
     * @param destination PID of the object to deposit into
     * @param deposit details about the deposit
     * @return PID of the deposit
     * @throws DepositException
     */
    public PID doDeposit(PID destination, DepositData deposit) throws DepositException;
}

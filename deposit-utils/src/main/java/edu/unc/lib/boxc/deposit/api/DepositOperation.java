package edu.unc.lib.boxc.deposit.api;

/**
 * Names of operations or events that happen to deposits.
 *
 * @author bbpennel
 */
public enum DepositOperation {
    REGISTER,
    PAUSE,
    RESUME,
    CANCEL,
    DESTROY,
    JOB_SUCCESS,
    JOB_FAILURE,
    JOB_INTERRUPTED,
}

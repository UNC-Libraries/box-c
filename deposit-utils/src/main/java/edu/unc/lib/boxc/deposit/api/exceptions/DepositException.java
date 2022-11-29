package edu.unc.lib.boxc.deposit.api.exceptions;

/**
 * @author bbpennel
 * @date Mar 24, 2014
 */
public class DepositException extends Exception {

    private static final long serialVersionUID = -4065348103957132332L;

    public DepositException(String msg) {
        super(msg);
    }

    public DepositException(String msg, Throwable e) {
        super(msg, e);
    }
}
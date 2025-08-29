package edu.unc.lib.boxc.deposit.api.exceptions;

import java.io.Serial;

/**
 * Exception thrown when a deposit message is malformed or unable to send.
 *
 * @author bbpennel
 */
public class DepositMessageException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -4065348103957132332L;

    public DepositMessageException(String msg) {
        super(msg);
    }

    public DepositMessageException(String msg, Throwable e) {
        super(msg, e);
    }
}

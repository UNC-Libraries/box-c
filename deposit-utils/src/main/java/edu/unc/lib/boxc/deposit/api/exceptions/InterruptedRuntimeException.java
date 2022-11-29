package edu.unc.lib.boxc.deposit.api.exceptions;

/**
 * Unchecked InterruptedException
 *
 * @author bbpennel
 */
public class InterruptedRuntimeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InterruptedRuntimeException() {
        super();
    }

    public InterruptedRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public InterruptedRuntimeException(String message) {
        super(message);
    }

    public InterruptedRuntimeException(Throwable cause) {
        super(cause);
    }
}

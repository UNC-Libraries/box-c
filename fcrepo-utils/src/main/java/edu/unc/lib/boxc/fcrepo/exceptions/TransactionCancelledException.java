package edu.unc.lib.boxc.fcrepo.exceptions;

/**
 *
 * @author harring
 *
 */
public class TransactionCancelledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TransactionCancelledException() {
        super();
    }

    public TransactionCancelledException(String message) {
        super(message);
    }

    public TransactionCancelledException(String message, Throwable t) {
        super(message, t);
    }
}

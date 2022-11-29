package edu.unc.lib.boxc.model.api.exceptions;

/**
 * Exception thrown when an operation is attempted on an object type which it is
 * not applicable to.
 *
 * @author bbpennel
 *
 */
public class InvalidOperationForObjectType extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidOperationForObjectType() {
    }

    public InvalidOperationForObjectType(String message) {
        super(message);
    }

    public InvalidOperationForObjectType(Throwable cause) {
        super(cause);
    }

    public InvalidOperationForObjectType(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidOperationForObjectType(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}

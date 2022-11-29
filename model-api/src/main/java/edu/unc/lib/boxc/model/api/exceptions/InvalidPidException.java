package edu.unc.lib.boxc.model.api.exceptions;

/**
 * Exception indicating that the provided value was not a valid PID
 *
 * @author bbpennel
 */
public class InvalidPidException extends RepositoryException {

    private static final long serialVersionUID = 1L;

    public InvalidPidException(String message) {
        super(message);
    }

    public InvalidPidException(Throwable ex) {
        super(ex);
    }

    public InvalidPidException(String message, Throwable ex) {
        super(message, ex);
    }

}

package edu.unc.lib.boxc.model.api.exceptions;

/**
 * Exception indicating that a lock or acquisition of one was interrupted
 *
 * @author bbpennel
 */
public class InterruptedLockException extends RepositoryException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public InterruptedLockException(String message) {
        super(message);
    }

}

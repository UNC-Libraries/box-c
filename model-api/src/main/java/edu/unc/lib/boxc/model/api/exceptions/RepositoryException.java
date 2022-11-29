package edu.unc.lib.boxc.model.api.exceptions;

/**
 * General exception related to the state of the box-c repository
 *
 * @author bbpennel
 *
 */
public class RepositoryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RepositoryException(String message) {
        super(message);
    }

    public RepositoryException(Throwable ex) {
        super(ex);
    }

    public RepositoryException(String message, Throwable ex) {
        super(message, ex);
    }
}

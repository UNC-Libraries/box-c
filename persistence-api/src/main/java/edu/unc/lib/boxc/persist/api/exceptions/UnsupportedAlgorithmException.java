package edu.unc.lib.boxc.persist.api.exceptions;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;

/**
 * Thrown when an unsupported digest algorithm is specified
 *
 * @author bbpennel
 */
public class UnsupportedAlgorithmException extends RepositoryException {
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     * @param ex
     */
    public UnsupportedAlgorithmException(String message, Throwable ex) {
        super(message, ex);
    }

    /**
     * @param message
     */
    public UnsupportedAlgorithmException(String message) {
        super(message);
    }

    /**
     * @param ex
     */
    public UnsupportedAlgorithmException(Throwable ex) {
        super(ex);
    }

}

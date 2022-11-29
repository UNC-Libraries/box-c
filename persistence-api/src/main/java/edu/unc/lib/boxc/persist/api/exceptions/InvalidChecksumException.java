package edu.unc.lib.boxc.persist.api.exceptions;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;

/**
 * Exception indicating that a checksum did not match the expected value
 *
 * @author bbpennel
 */
public class InvalidChecksumException extends RepositoryException {
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public InvalidChecksumException(String message) {
        super(message);
    }

    /**
     * @param ex
     */
    public InvalidChecksumException(Throwable ex) {
        super(ex);
    }

    /**
     * @param message
     * @param ex
     */
    public InvalidChecksumException(String message, Throwable ex) {
        super(message, ex);
    }

}

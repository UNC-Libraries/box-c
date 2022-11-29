package edu.unc.lib.boxc.persist.api.transfer;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;

/**
 * @author bbpennel
 *
 */
public class BinaryTransferException extends RepositoryException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public BinaryTransferException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param ex
     */
    public BinaryTransferException(String message, Throwable ex) {
        super(message, ex);
    }

}

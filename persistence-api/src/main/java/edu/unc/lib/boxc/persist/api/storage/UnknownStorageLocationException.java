package edu.unc.lib.boxc.persist.api.storage;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;

/**
 * @author bbpennel
 *
 */
public class UnknownStorageLocationException extends RepositoryException {
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public UnknownStorageLocationException(String message) {
        super(message);
    }

}

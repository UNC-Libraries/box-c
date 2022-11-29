package edu.unc.lib.boxc.persist.api.exceptions;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;

/**
 * @author bbpennel
 *
 */
public class UnknownIngestSourceException extends RepositoryException {
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public UnknownIngestSourceException(String message) {
        super(message);
    }
}

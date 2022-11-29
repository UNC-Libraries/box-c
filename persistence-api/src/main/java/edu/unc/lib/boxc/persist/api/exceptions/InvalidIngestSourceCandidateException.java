package edu.unc.lib.boxc.persist.api.exceptions;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;

/**
 * @author bbpennel
 *
 */
public class InvalidIngestSourceCandidateException extends RepositoryException {
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public InvalidIngestSourceCandidateException(String message) {
        super(message);
    }

    public InvalidIngestSourceCandidateException(String message, Throwable ex) {
        super(message, ex);
    }
}

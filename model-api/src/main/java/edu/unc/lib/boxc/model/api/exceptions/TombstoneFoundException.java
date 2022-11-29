package edu.unc.lib.boxc.model.api.exceptions;

/**
 * Exception indicating that the requested resource has been destroyed and replaced by a tombstone object
 *
 * @author bbpennel
 */
public class TombstoneFoundException extends RepositoryException {
    private static final long serialVersionUID = 1L;

    public TombstoneFoundException(String message) {
        super(message);
    }
}

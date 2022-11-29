package edu.unc.lib.boxc.model.api.exceptions;

/**
 * Exception thrown when an object is not part of the membership hierarchy of the repository.
 *
 * @author bbpennel
 *
 */
public class OrphanedObjectException extends RepositoryException {
    private static final long serialVersionUID = 1L;

    public OrphanedObjectException(String message) {
        super(message);
    }
}

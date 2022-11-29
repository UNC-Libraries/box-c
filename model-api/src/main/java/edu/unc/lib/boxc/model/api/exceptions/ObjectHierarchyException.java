package edu.unc.lib.boxc.model.api.exceptions;

/**
 * Exception indicating that an invalid membership hierarchy was encountered
 *
 * @author bbpennel
 */
public class ObjectHierarchyException extends RepositoryException {
    private static final long serialVersionUID = 1L;

    public ObjectHierarchyException(String message) {
        super(message);
    }

    public ObjectHierarchyException(Throwable ex) {
        super(ex);
    }

    public ObjectHierarchyException(String message, Throwable ex) {
        super(message, ex);
    }
}

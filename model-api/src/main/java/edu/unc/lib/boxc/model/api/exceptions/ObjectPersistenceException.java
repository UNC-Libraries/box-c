package edu.unc.lib.boxc.model.api.exceptions;

/**
 * Exception thrown when persisting an object or its content fails, either in
 * the repository or locally.
 *
 * @author bbpennel
 *
 */
public class ObjectPersistenceException extends RuntimeException {

    private static final long serialVersionUID = 3956045265306679210L;

    public ObjectPersistenceException(String message, Throwable e) {
        super(message, e);
    }
}

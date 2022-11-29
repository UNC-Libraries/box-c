package edu.unc.lib.boxc.persist.api.exceptions;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;

/**
 * Exception indicating that an unsupport packaging type was submitted for deposit
 *
 * @author bbpennel
 *
 */
public class UnsupportedPackagingTypeException extends RepositoryException {
    private static final long serialVersionUID = 1L;

    public UnsupportedPackagingTypeException(String msg) {
        super(msg);
    }

    public UnsupportedPackagingTypeException(String msg, Throwable e) {
        super(msg, e);
    }
}

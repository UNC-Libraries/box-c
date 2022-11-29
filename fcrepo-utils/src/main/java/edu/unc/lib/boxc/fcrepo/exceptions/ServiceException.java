package edu.unc.lib.boxc.fcrepo.exceptions;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;

/**
 * @author Gregory Jansen
 *
 */
public class ServiceException extends RepositoryException {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(Throwable cause) {
        super(cause);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public Throwable getRootCause() {
        Throwable cause = this.getCause();
        if (cause == null) {
            return null;
        }
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause;
    }

    /**
     *
     */
    private static final long serialVersionUID = -8344216562020118505L;

}

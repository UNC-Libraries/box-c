package edu.unc.lib.boxc.fcrepo.exceptions;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;

/**
 * Error indicates that a HTTP range request could not be satisfied
 *
 * @author bbpennel
 */
public class RangeNotSatisfiableException extends FedoraException {
    private static final long serialVersionUID = 1L;

    public RangeNotSatisfiableException(String message) {
        super(message);
    }

    public RangeNotSatisfiableException(Throwable cause) {
        super(cause);
    }

    public RangeNotSatisfiableException(String message, Throwable cause) {
        super(message, cause);
    }
}

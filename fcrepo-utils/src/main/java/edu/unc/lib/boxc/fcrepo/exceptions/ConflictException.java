package edu.unc.lib.boxc.fcrepo.exceptions;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;

/**
 * Request failed due to a conflict
 *
 * @author bbpennel
 */
public class ConflictException extends FedoraException {

    /**
     * @param e
     */
    public ConflictException(Exception e) {
        super(e);
    }

    /**
     * @param message
     * @param e
     */
    public ConflictException(String message, Exception e) {
        super(message, e);
    }

    /**
     * @param message
     */
    public ConflictException(String message) {
        super(message);
    }

}

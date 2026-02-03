package edu.unc.lib.boxc.fcrepo.exceptions;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;

/**
 * Request failed because the resource is 410 gone
 *
 * @author bbpennel
 */
public class GoneException extends FedoraException {
    public GoneException(String message) {
        super(message);
    }

    public GoneException(Throwable e) {
        super(e);
    }

    public GoneException(String message, Throwable e) {
        super(message, e);
    }
}

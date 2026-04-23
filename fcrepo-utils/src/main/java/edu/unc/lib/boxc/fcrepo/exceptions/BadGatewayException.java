package edu.unc.lib.boxc.fcrepo.exceptions;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;

/**
 * Request failed because the server is acting as a bad gateway
 *
 * @author bbpennel
 */
public class BadGatewayException extends FedoraException {
    public BadGatewayException(String message) {
        super(message);
    }

    public BadGatewayException(Throwable e) {
        super(e);
    }

    public BadGatewayException(String message, Throwable e) {
        super(message, e);
    }
}

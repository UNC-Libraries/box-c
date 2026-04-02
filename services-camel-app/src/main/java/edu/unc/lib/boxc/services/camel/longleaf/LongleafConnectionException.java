package edu.unc.lib.boxc.services.camel.longleaf;

import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;

/**
 * Exception indicating that the longleaf HTTP API could not be reached.
 *
 * @author bbpennel
 */
public class LongleafConnectionException extends ServiceException {

    public LongleafConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}

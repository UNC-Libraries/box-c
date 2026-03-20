package edu.unc.lib.boxc.services.camel.longleaf;

import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;

/**
 * Exception indicating that the longleaf HTTP API rejected the request with a 4xx status.
 * This represents a misconfiguration or client/API mismatch and should not be retried.
 *
 * @author bbpennel
 */
public class LongleafBadRequestException extends ServiceException {

    public LongleafBadRequestException(String message) {
        super(message);
    }
}

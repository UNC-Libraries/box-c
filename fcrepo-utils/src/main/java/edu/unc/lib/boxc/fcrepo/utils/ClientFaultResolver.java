package edu.unc.lib.boxc.fcrepo.utils;

import edu.unc.lib.boxc.fcrepo.exceptions.RangeNotSatisfiableException;
import org.apache.http.HttpStatus;
import org.fcrepo.client.FcrepoOperationFailedException;

import edu.unc.lib.boxc.fcrepo.exceptions.AuthorizationException;
import edu.unc.lib.boxc.fcrepo.exceptions.ConflictException;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;

/**
 * Resolves exceptions which clients encounter while interacting with Fedora into more specific
 * exceptions within the repositories domain.
 *
 * @author bbpennel
 *
 */
public abstract class ClientFaultResolver {

    /**
     * Resolves a FcrepoOperationFailedException into a more specific FedoraException if possible
     *
     * @param ex
     * @return
     */
    public static FedoraException resolve(Exception ex) {
        if (ex instanceof FcrepoOperationFailedException) {
            FcrepoOperationFailedException e = (FcrepoOperationFailedException) ex;

            switch(e.getStatusCode()) {
            case HttpStatus.SC_FORBIDDEN:
                return new AuthorizationException(ex);
            case HttpStatus.SC_NOT_FOUND:
                return new NotFoundException(ex);
            case HttpStatus.SC_CONFLICT:
                return new ConflictException(ex);
            case HttpStatus.SC_REQUESTED_RANGE_NOT_SATISFIABLE:
                return new RangeNotSatisfiableException(ex);
            }
        }
        return new FedoraException(ex);
    }
}

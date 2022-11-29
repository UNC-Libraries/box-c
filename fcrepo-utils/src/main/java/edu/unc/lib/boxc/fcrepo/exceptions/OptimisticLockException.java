package edu.unc.lib.boxc.fcrepo.exceptions;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;

/**
 * @author bbpennel
 * @date Feb 4, 2015
 */
public class OptimisticLockException extends FedoraException {

    private static final long serialVersionUID = 1L;

    public OptimisticLockException(String message) {
        super(message);
    }

}

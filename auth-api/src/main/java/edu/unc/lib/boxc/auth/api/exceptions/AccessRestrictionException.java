package edu.unc.lib.boxc.auth.api.exceptions;

/**
 * Access restriction exceptions.
 * 
 * @author bbpennel
 */
public class AccessRestrictionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AccessRestrictionException() {
    }

    public AccessRestrictionException(String msg) {
        super(msg);
    }
}
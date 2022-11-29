package edu.unc.lib.boxc.auth.api.exceptions;

/**
 * Exception thrown when an invalid access restriction is assigned to an object.
 *
 * @author bbpennel
 *
 */
public class InvalidAssignmentException extends AccessRestrictionException {
    private static final long serialVersionUID = 1L;

    public InvalidAssignmentException() {
    }

    public InvalidAssignmentException(String msg) {
        super(msg);
    }

}

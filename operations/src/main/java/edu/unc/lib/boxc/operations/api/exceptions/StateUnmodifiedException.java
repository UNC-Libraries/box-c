package edu.unc.lib.boxc.operations.api.exceptions;

/**
 * Exception thrown when an operation is skipped due to the incoming state being the same as the existing state.
 *
 * @author bbpennel
 */
public class StateUnmodifiedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public StateUnmodifiedException(String message) {
        super(message);
    }
}

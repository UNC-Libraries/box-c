package edu.unc.lib.boxc.deposit.work;

/**
 * @author bbpennel
 */
public class JobFailedException extends RuntimeException {

    private static final long serialVersionUID = 2094423835072435563L;

    private String details;

    public String getDetails() {
        return details;
    }

    public JobFailedException(String message) {
        super(message);
    }

    public JobFailedException(String message, String details) {
        super(message);
        this.details = details;
    }

    public JobFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

package edu.unc.lib.boxc.operations.api.exceptions;

/**
 * Exception indicating that metadata was determined to be invalid
 *
 * @author bbpennel
 *
 */
public class MetadataValidationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String details;

    public MetadataValidationException() {
    }

    public MetadataValidationException(String message) {
        super(message);
    }

    public MetadataValidationException(String message, String details) {
        super(message);
        this.details = details;
    }

    public MetadataValidationException(Throwable cause) {
        super(cause);
    }

    public MetadataValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    public MetadataValidationException(String message, String details, Throwable cause) {
        super(message, cause);
        this.details = details;
    }

    /**
     * @return the details
     */
    public String getDetails() {
        return details;
    }

    /**
     * Returns the message and details of this exception if available.
     *
     * @return
     */
    public String getDetailedMessage() {
        if (details == null) {
            return getMessage();
        } else {
            return getMessage() + "\n" + details;
        }
    }
}

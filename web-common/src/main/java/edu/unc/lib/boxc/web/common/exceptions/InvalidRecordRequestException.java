package edu.unc.lib.boxc.web.common.exceptions;

/**
 * 
 * @author count0
 */
public class InvalidRecordRequestException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidRecordRequestException() {
    }

    public InvalidRecordRequestException(String message) {
        super(message);
    }

    public InvalidRecordRequestException(Throwable cause) {
        super(cause);
    }
}

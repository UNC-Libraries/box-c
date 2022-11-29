package edu.unc.lib.boxc.persist.api.transfer;

/**
 * @author bbpennel
 *
 */
public class BinaryAlreadyExistsException extends BinaryTransferException {
    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public BinaryAlreadyExistsException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param ex
     */
    public BinaryAlreadyExistsException(String message, Throwable ex) {
        super(message, ex);
    }
}

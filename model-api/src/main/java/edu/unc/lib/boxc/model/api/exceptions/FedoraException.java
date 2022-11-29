package edu.unc.lib.boxc.model.api.exceptions;

/**
 * @author Gregory Jansen
 *
 */
public class FedoraException extends RuntimeException {

    private static final long serialVersionUID = 7276162681909269101L;

    public FedoraException(Exception e) {
        super(e);
    }

    public FedoraException(String message, Exception e) {
        super(message, e);
    }

    public FedoraException(String message) {
        super(message);
    }
}

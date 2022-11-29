package edu.unc.lib.boxc.model.api.exceptions;

/**
 * @author Gregory Jansen
 *
 */
public class ObjectExistsException extends FedoraException {
    /**
     *
     */
    private static final long serialVersionUID = 2177327948413175683L;

    public ObjectExistsException(String message) {
        super(message);
    }
}

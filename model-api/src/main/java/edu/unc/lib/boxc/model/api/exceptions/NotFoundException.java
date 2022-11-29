package edu.unc.lib.boxc.model.api.exceptions;

/**
 * 
 * @author count0
 *
 */
public class NotFoundException extends FedoraException {

    /**
     *
     */
    private static final long serialVersionUID = 4086598984144235534L;

    public NotFoundException(Exception e) {
    super(e);
    }

    public NotFoundException(String message) {
    super(message);
    }

}

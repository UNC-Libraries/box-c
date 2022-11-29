package edu.unc.lib.boxc.fcrepo.exceptions;

/**
 * @author Gregory Jansen
 *
 */
public class FedoraTimeoutException extends ServiceException {

    /**
     *
     */
    private static final long serialVersionUID = -5509017474926163463L;

    public FedoraTimeoutException(String desc) {
        super(desc);
    }

    /**
     * @param cause
     */
    public FedoraTimeoutException(Throwable cause) {
        super(cause);
    }

}

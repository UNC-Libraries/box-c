package edu.unc.lib.boxc.fcrepo.exceptions;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;

/**
 * Checksum did not match the provided checksum value.
 *
 * @author bbpennel
 *
 */
public class ChecksumMismatchException extends FedoraException {

    private static final long serialVersionUID = -7456278395813803155L;

    public ChecksumMismatchException(Exception e) {
        super(e);
    }

    public ChecksumMismatchException(String message, Exception e) {
        super(message, e);
    }

    public ChecksumMismatchException(String message) {
        super(message);
    }

}

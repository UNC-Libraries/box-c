package edu.unc.lib.boxc.model.api.exceptions;

/**
 * Exception which indicates that a repository object did not match the expected RDF types
 *
 * @author bbpennel
 *
 */
public class ObjectTypeMismatchException extends FedoraException {

    private static final long serialVersionUID = -1660852243627715050L;

    public ObjectTypeMismatchException(Exception e) {
        super(e);
    }

    public ObjectTypeMismatchException(String value) {
        super(value);
    }
}

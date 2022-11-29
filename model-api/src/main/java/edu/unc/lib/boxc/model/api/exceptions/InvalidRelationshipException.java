package edu.unc.lib.boxc.model.api.exceptions;

/**
 * Exception thrown when the definition of a relationship does not match usage
 * or schema requirements, such as if the object or subject were of an invalid
 * type
 *
 * @author bbpennel
 *
 */
public class InvalidRelationshipException extends RuntimeException {

    private static final long serialVersionUID = 5027174492232921223L;

    public InvalidRelationshipException(String message) {
        super(message);
    }
}

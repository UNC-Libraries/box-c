package edu.unc.lib.boxc.fcrepo.exceptions;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;

/**
 *
 * @author bbpennel
 *
 */
public class AuthorizationException extends FedoraException {
    private static final long serialVersionUID = 2177327948413175683L;

    private AuthorizationErrorType type = AuthorizationErrorType.DENIED;

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(Exception e) {
        super(e);
    }

    public AuthorizationErrorType getType() {
        return type;
    }

    public void setType(AuthorizationErrorType type) {
        this.type = type;
    }

    public static enum AuthorizationErrorType {
        NOT_APPLICABLE, DENIED, INDETERMINATE
    }
}

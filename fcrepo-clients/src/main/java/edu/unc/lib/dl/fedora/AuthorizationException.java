/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.fedora;

import org.springframework.ws.soap.client.SoapFaultClientException;

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

    public AuthorizationException(SoapFaultClientException e) {
        super(e);

        String reason = e.getFaultStringOrReason();
        if (reason.contains("NotApplicable")) {
            type = AuthorizationErrorType.NOT_APPLICABLE;
        } else if (reason.contains("Indeterminate")) {
            type = AuthorizationErrorType.INDETERMINATE;
        }
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

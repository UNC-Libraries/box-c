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
package edu.unc.lib.boxc.fcrepo.utils;

import org.apache.http.HttpStatus;
import org.fcrepo.client.FcrepoOperationFailedException;

import edu.unc.lib.boxc.fcrepo.exceptions.AuthorizationException;
import edu.unc.lib.boxc.fcrepo.exceptions.ConflictException;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;

/**
 * Resolves exceptions which clients encounter while interacting with Fedora into more specific
 * exceptions within the repositories domain.
 *
 * @author bbpennel
 *
 */
public abstract class ClientFaultResolver {

    /**
     * Resolves a FcrepoOperationFailedException into a more specific FedoraException if possible
     *
     * @param ex
     * @return
     */
    public static FedoraException resolve(Exception ex) {
        if (ex instanceof FcrepoOperationFailedException) {
            FcrepoOperationFailedException e = (FcrepoOperationFailedException) ex;

            switch(e.getStatusCode()) {
            case HttpStatus.SC_FORBIDDEN:
                return new AuthorizationException(ex);
            case HttpStatus.SC_NOT_FOUND:
                    return new NotFoundException(ex);
            case HttpStatus.SC_CONFLICT:
                    return new ConflictException(ex);
            }
        }
        return new FedoraException(ex);
    }
}

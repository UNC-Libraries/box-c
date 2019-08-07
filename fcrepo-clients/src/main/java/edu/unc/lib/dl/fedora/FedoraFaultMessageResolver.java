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
 * @author Gregory Jansen
 *
 */
public abstract class FedoraFaultMessageResolver {

    static void resolveFault(SoapFaultClientException e) throws FedoraException {
        if (e.getFaultStringOrReason() != null) {
            String r = e.getFaultStringOrReason();
            if (r.contains("ObjectNotFoundException") || r.contains("ObjectNotInLowlevelStorageException")
                    || r.contains("DatastreamNotFoundException") || r.contains("no path in db registry")
                    || r.contains("No datastream could be returned")
                    || (r.contains("could not obtain IRODS File System")
                            && r.contains("java.io.FileNotFoundException"))) {
                throw new NotFoundException(e);
            } else if (r.contains("ObjectExistsException")
                    || r.contains("already exists in the registry; the object can't be re-created")
                        || r.contains("A datastream already exists with ID")) {
                throw new ObjectExistsException(e);
            } else if (r.contains("LowlevelStorageException")) {
                throw new FileSystemException(e);
            } else if (r.contains("org.fcrepo.server.security.xacml.pep.AuthzDeniedException")) {
                throw new AuthorizationException(e);
            } else if (r.contains("FOXML IO stream was bad")) {
                throw new ObjectIntegrityException(e);
            } else {
                throw new FedoraException(e);
            }
        } else {
            throw new FedoraException(e);
        }
    }
}

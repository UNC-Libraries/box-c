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
package edu.unc.lib.dl.persist.services.destroy;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.fcrepo.objects.AdminUnitImpl;
import edu.unc.lib.boxc.model.fcrepo.objects.ContentRootObjectImpl;
import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;

/**
 * Helper for destroy operations
 *
 * @author bbpennel
 *
 */
public class DestroyObjectsHelper {

    private static final ObjectWriter MAPPER = new ObjectMapper().writerFor(DestroyObjectsRequest.class);

    private DestroyObjectsHelper() {
    }

    /**
     * Asserts that the provided agent has permission to destroy the indicated object.
     *
     * @param agent agent
     * @param repoObj object to destroy
     * @param aclService acl service
     */
    public static void assertCanDestroy(AgentPrincipals agent, RepositoryObject repoObj,
            AccessControlService aclService) {
        if (repoObj instanceof AdminUnitImpl) {
            aclService.assertHasAccess("User does not have permission to destroy admin unit", repoObj.getPid(),
                    agent.getPrincipals(), Permission.destroyUnit);
        } else if (repoObj instanceof ContentRootObjectImpl) {
            throw new AccessRestrictionException("Cannot destroy content root object");
        } else {
            aclService.assertHasAccess("User does not have permission to destroy this object", repoObj.getPid(),
                    agent.getPrincipals(), Permission.destroy);
        }
    }

    public static String serializeDestroyRequest(DestroyObjectsRequest request) {
        try {
            return MAPPER.writeValueAsString(request);
        } catch (IOException e) {
            throw new RepositoryException("Failed to create destroy request", e);
        }
    }
}

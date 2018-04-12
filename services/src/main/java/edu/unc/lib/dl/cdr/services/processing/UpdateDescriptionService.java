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
package edu.unc.lib.dl.cdr.services.processing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.validation.MODSValidator;
import edu.unc.lib.dl.validation.MetadataValidationException;

/**
 * Service that manages description, e.g., MODS, updates
 *
 * @author harring
 *
 */
public class UpdateDescriptionService {

    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private OperationsMessageSender operationsMessageSender;
    private MODSValidator modsValidator;

    public UpdateDescriptionService() {
    }

    /**
     * Updates the MODS description of a single object
     *
     * @param agent
     * @param pid
     * @param modsStream
     * @throws MetadataValidationException
     * @throws IOException
     */
    public void updateDescription(AgentPrincipals agent, PID pid, InputStream modsStream)
            throws MetadataValidationException, IOException {
        aclService.assertHasAccess("User does not have permissions to update description",
                pid, agent.getPrincipals(), Permission.editDescription);

        String username = agent.getUsername();
        if (!modsStream.markSupported()) {
            modsStream = new ByteArrayInputStream(IOUtils.toByteArray(modsStream));
        }
        modsValidator.validate(modsStream);

        ContentObject obj = (ContentObject) repoObjLoader.getRepositoryObject(pid);
        obj.setDescription(modsStream);

        operationsMessageSender.sendUpdateDescriptionOperation(username, Arrays.asList(pid));
    }

    /**
     * @param aclService the aclService to set
     */
    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    /**
     * @param repoObjLoader the object loader to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    /**
     *
     * @param operationsMessageSender
     */
    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }

    /**
     *
     * @param modsValidator
     */
    public void setModsValidator(MODSValidator modsValidator) {
        this.modsValidator = modsValidator;
    }

}

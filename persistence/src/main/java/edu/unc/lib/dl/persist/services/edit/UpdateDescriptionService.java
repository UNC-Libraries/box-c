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
package edu.unc.lib.dl.persist.services.edit;

import static edu.unc.lib.dl.model.DatastreamPids.getMdDescriptivePid;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.toByteArray;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.validation.MODSValidator;
import edu.unc.lib.dl.validation.MetadataValidationException;
import io.dropwizard.metrics5.Timer;

/**
 * Service that manages description, e.g., MODS, updates
 *
 * @author harring
 *
 */
public class UpdateDescriptionService {
    private static final Logger log = LoggerFactory.getLogger(UpdateDescriptionService.class);

    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private OperationsMessageSender operationsMessageSender;
    private MODSValidator modsValidator;
    private BinaryTransferService transferService;
    private StorageLocationManager locationManager;

    private boolean validate;

    private static final Timer timer = TimerFactory.createTimerForClass(UpdateDescriptionService.class);

    public UpdateDescriptionService() {
        validate = true;
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

        ContentObject obj = (ContentObject) repoObjLoader.getRepositoryObject(pid);
        StorageLocation destLocation = locationManager.getStorageLocation(obj);

        try (BinaryTransferSession transferSession = transferService.getSession(destLocation)) {
            updateDescription(transferSession, agent, obj, modsStream);
        }
    }

    /**
     * Updates the MODS description of an object as part of the provided ongoing session.
     *
     * @param transferSession ongoing transfer session
     * @param agent
     * @param pid
     * @param modsStream
     * @throws MetadataValidationException
     * @throws IOException
     */
    public void updateDescription(BinaryTransferSession transferSession, AgentPrincipals agent,
            PID pid, InputStream modsStream) throws MetadataValidationException, IOException {

        ContentObject obj = (ContentObject) repoObjLoader.getRepositoryObject(pid);

        updateDescription(transferSession, agent, obj, modsStream);
    }

    private void updateDescription(BinaryTransferSession transferSession, AgentPrincipals agent,
            ContentObject obj, InputStream modsStream) throws IOException {

        log.debug("Updating description for {}", obj.getPid().getId());
        try (Timer.Context context = timer.time()) {
            aclService.assertHasAccess("User does not have permissions to update description",
                    obj.getPid(), agent.getPrincipals(), Permission.editDescription);

            String username = agent.getUsername();
            if (validate) {
                if (!modsStream.markSupported()) {
                    modsStream = new ByteArrayInputStream(toByteArray(modsStream));
                }
                modsValidator.validate(modsStream);
            }

            // Transfer the description to its storage location
            PID modsDsPid = getMdDescriptivePid(obj.getPid());
            URI modsUri = transferSession.transferReplaceExisting(modsDsPid, modsStream);

            obj.setDescription(modsUri);
            log.debug("Successfully set desc to {}", modsUri);

            operationsMessageSender.sendUpdateDescriptionOperation(username, asList(obj.getPid()));
        }
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

    /**
     * @param transferService the transferService to set
     */
    public void setTransferService(BinaryTransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * @param locationManager the locationManager to set
     */
    public void setLocationManager(StorageLocationManager locationManager) {
        this.locationManager = locationManager;
    }

    /**
     * @param validate if set to true, then will perform validation of the description.
     */
    public void setValidate(boolean validate) {
        this.validate = validate;
    }
}

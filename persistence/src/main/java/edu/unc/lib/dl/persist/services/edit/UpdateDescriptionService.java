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
import static edu.unc.lib.dl.model.DatastreamType.MD_DESCRIPTIVE;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.metrics.TimerFactory;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.dl.persist.services.versioning.VersionedDatastreamService;
import edu.unc.lib.dl.persist.services.versioning.VersionedDatastreamService.DatastreamVersion;
import edu.unc.lib.dl.rdf.Cdr;
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
    private RepositoryObjectFactory repoObjFactory;
    private OperationsMessageSender operationsMessageSender;
    private MODSValidator modsValidator;
    private VersionedDatastreamService versioningService;

    private boolean validate;
    private boolean sendsMessages;
    private boolean checksAccess;

    private static final Timer timer = TimerFactory.createTimerForClass(UpdateDescriptionService.class);

    public UpdateDescriptionService() {
        validate = true;
        sendsMessages = true;
        checksAccess = true;
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
    public BinaryObject updateDescription(AgentPrincipals agent, PID pid, InputStream modsStream)
            throws MetadataValidationException, IOException {

        ContentObject obj = (ContentObject) repoObjLoader.getRepositoryObject(pid);

        return updateDescription(null, agent, obj, modsStream);
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
    public BinaryObject updateDescription(BinaryTransferSession transferSession, AgentPrincipals agent,
            PID pid, InputStream modsStream) throws MetadataValidationException, IOException {

        ContentObject obj = (ContentObject) repoObjLoader.getRepositoryObject(pid);

        return updateDescription(transferSession, agent, obj, modsStream);
    }

    public BinaryObject updateDescription(BinaryTransferSession transferSession, AgentPrincipals agent,
            ContentObject obj, InputStream modsStream) throws IOException {

        log.debug("Updating description for {}", obj.getPid().getId());
        try (Timer.Context context = timer.time()) {
            if (checksAccess) {
                aclService.assertHasAccess("User does not have permissions to update description",
                    obj.getPid(), agent.getPrincipals(), Permission.editDescription);
            }

            String username = agent.getUsername();
            if (validate) {
                if (!modsStream.markSupported()) {
                    modsStream = new ByteArrayInputStream(toByteArray(modsStream));
                }
                modsValidator.validate(modsStream);
            }

            // Transfer the description to its storage location
            PID modsDsPid = getMdDescriptivePid(obj.getPid());

            DatastreamVersion newVersion = new DatastreamVersion(modsDsPid);
            newVersion.setContentStream(modsStream);
            newVersion.setContentType(MD_DESCRIPTIVE.getMimetype());
            newVersion.setFilename(MD_DESCRIPTIVE.getDefaultFilename());
            newVersion.setTransferSession(transferSession);

            BinaryObject descBinary;
            if (repoObjFactory.objectExists(modsDsPid.getRepositoryUri())) {
                descBinary = versioningService.addVersion(newVersion);
                log.debug("Successfully updated description for {}", obj.getPid());
            } else {
                // setup description for object for the first time
                Model descModel = createDefaultModel();
                descModel.getResource("").addProperty(RDF.type, Cdr.DescriptiveMetadata);
                newVersion.setProperties(descModel);

                descBinary = versioningService.addVersion(newVersion);

                repoObjFactory.createRelationship(obj, Cdr.hasMods, createResource(modsDsPid.getRepositoryPath()));
                log.debug("Successfully set new description for {}", obj.getPid());
            }

            if (sendsMessages) {
                operationsMessageSender.sendUpdateDescriptionOperation(username, asList(obj.getPid()));
            }

            return descBinary;
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

    public void setRepositoryObjectFactory(RepositoryObjectFactory repoObjFactory) {
        this.repoObjFactory = repoObjFactory;
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

    public void setVersionedDatastreamService(VersionedDatastreamService versioningService) {
        this.versioningService = versioningService;
    }

    /**
     * @param validate if set to true, then will perform validation of the description.
     */
    public void setValidate(boolean validate) {
        this.validate = validate;
    }

    public void setSendsMessages(boolean sendsMessages) {
        this.sendsMessages = sendsMessages;
    }

    public void setChecksAccess(boolean checksAccess) {
        this.checksAccess = checksAccess;
    }
}

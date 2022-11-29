package edu.unc.lib.boxc.operations.impl.edit;

import static edu.unc.lib.boxc.model.api.DatastreamType.MD_DESCRIPTIVE;
import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getMdDescriptivePid;
import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.common.metrics.TimerFactory;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.operations.api.exceptions.MetadataValidationException;
import edu.unc.lib.boxc.operations.impl.validation.MODSValidator;
import edu.unc.lib.boxc.operations.impl.versioning.VersionedDatastreamService;
import edu.unc.lib.boxc.operations.impl.versioning.VersionedDatastreamService.DatastreamVersion;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingPriority;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;
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
        return updateDescription(new UpdateDescriptionRequest(agent, pid, modsStream));
    }

    /**
     * Updates the MODS description of an object using the details of the provided request
     *
     * @param request update request
     * @return binary object of the updated description
     * @throws IOException
     */
    public BinaryObject updateDescription(UpdateDescriptionRequest request) throws IOException {
        PID pid = request.getPid();
        ContentObject obj = request.getContentObject();
        if (obj == null) {
            obj = (ContentObject) repoObjLoader.getRepositoryObject(pid);
        }
        log.debug("Updating description for {}", obj.getPid().getId());
        try (Timer.Context context = timer.time()) {
            if (checksAccess) {
                aclService.assertHasAccess("User does not have permissions to update description",
                    obj.getPid(), request.getAgent().getPrincipals(), Permission.editDescription);
            }

            String username = request.getAgent().getUsername();
            InputStream modsStream = request.getModsStream();
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
            newVersion.setTransferSession(request.getTransferSession());
            newVersion.setUnmodifiedSince(request.getUnmodifiedSince());

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
                operationsMessageSender.sendUpdateDescriptionOperation(
                        username, asList(obj.getPid()), request.getPriority());
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

    public static class UpdateDescriptionRequest {
        private PID pid;
        private ContentObject contentObject;
        private BinaryTransferSession transferSession;
        private AgentPrincipals agent;
        private InputStream modsStream;
        private IndexingPriority priority;
        private Instant unmodifiedSince;

        public UpdateDescriptionRequest(AgentPrincipals agent, PID pid, InputStream modsStream) {
            this.agent = agent;
            this.pid = pid;
            this.modsStream = modsStream;
        }
        public UpdateDescriptionRequest(AgentPrincipals agent, ContentObject obj, InputStream modsStream) {
            this.agent = agent;
            this.contentObject = obj;
            this.pid = obj.getPid();
            this.modsStream = modsStream;
        }

        public PID getPid() {
            return pid;
        }

        public void setPid(PID pid) {
            this.pid = pid;
        }

        public ContentObject getContentObject() {
            return contentObject;
        }

        public UpdateDescriptionRequest withContentObject(ContentObject contentObject) {
            this.contentObject = contentObject;
            return this;
        }

        public BinaryTransferSession getTransferSession() {
            return transferSession;
        }

        public UpdateDescriptionRequest withTransferSession(BinaryTransferSession transferSession) {
            this.transferSession = transferSession;
            return this;
        }

        public AgentPrincipals getAgent() {
            return agent;
        }

        public void setAgent(AgentPrincipals agent) {
            this.agent = agent;
        }

        public InputStream getModsStream() {
            return modsStream;
        }

        public void setModsStream(InputStream modsStream) {
            this.modsStream = modsStream;
        }

        public IndexingPriority getPriority() {
            return priority;
        }

        public UpdateDescriptionRequest withPriority(IndexingPriority priority) {
            this.priority = priority;
            return this;
        }

        public Instant getUnmodifiedSince() {
            return unmodifiedSince;
        }

        public UpdateDescriptionRequest withUnmodifiedSince(Instant unmodifiedSince) {
            this.unmodifiedSince = unmodifiedSince;
            return this;
        }
    }
}

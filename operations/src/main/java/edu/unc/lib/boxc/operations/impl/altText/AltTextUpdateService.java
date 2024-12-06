package edu.unc.lib.boxc.operations.impl.altText;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.versioning.VersionedDatastreamService;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import edu.unc.lib.boxc.operations.jms.altText.AltTextUpdateRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static edu.unc.lib.boxc.model.api.DatastreamType.ALT_TEXT;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

/**
 * Service for updating the alt text of a file object
 *
 * @author bbpennel
 */
public class AltTextUpdateService {
    private static final Logger log = LoggerFactory.getLogger(AltTextUpdateService.class);

    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private VersionedDatastreamService versionedDatastreamService;
    private OperationsMessageSender operationsMessageSender;
    private boolean sendsMessages;

    public BinaryObject updateAltText(AltTextUpdateRequest request) {
        var pid = PIDs.get(request.getPidString());
        aclService.assertHasAccess("User does not have permission to update alt text",
                pid, request.getAgent().getPrincipals(), Permission.editDescription);

        var fileObj = repositoryObjectLoader.getFileObject(pid);
        var altTextPid = DatastreamPids.getAltTextPid(pid);

        BinaryObject altTextBinary;
        var newVersion = new VersionedDatastreamService.DatastreamVersion(altTextPid);
        newVersion.setContentStream(new ByteArrayInputStream(request.getAltText().getBytes(StandardCharsets.UTF_8)));
        newVersion.setContentType(ALT_TEXT.getMimetype());
        newVersion.setFilename(ALT_TEXT.getDefaultFilename());
        newVersion.setTransferSession(request.getTransferSession());

        altTextBinary = versionedDatastreamService.addVersion(newVersion);
        if (repositoryObjectFactory.objectExists(altTextPid.getRepositoryUri())) {
            log.debug("Successfully updated alt text for {}", fileObj.getPid());
        } else {
            repositoryObjectFactory.createRelationship(fileObj, Cdr.hasAltText, createResource(altTextPid.getRepositoryPath()));
            log.debug("Successfully add new alt text for {}", fileObj.getPid());
        }

        if (sendsMessages) {
            operationsMessageSender.sendUpdateDescriptionOperation(
                    request.getAgent().getUsername(), Collections.singletonList(fileObj.getPid()), IndexingPriority.normal);
        }

        return altTextBinary;
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setRepositoryObjectFactory(RepositoryObjectFactory repositoryObjectFactory) {
        this.repositoryObjectFactory = repositoryObjectFactory;
    }

    public void setVersionedDatastreamService(VersionedDatastreamService versionedDatastreamService) {
        this.versionedDatastreamService = versionedDatastreamService;
    }

    public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
        this.operationsMessageSender = operationsMessageSender;
    }

    public void setSendsMessages(boolean sendsMessages) {
        this.sendsMessages = sendsMessages;
    }
}

package edu.unc.lib.boxc.operations.impl.fullDescription;

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
import edu.unc.lib.boxc.operations.jms.fullDescription.FullDescriptionUpdateRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingPriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static edu.unc.lib.boxc.model.api.DatastreamType.FULL_DESCRIPTION;
import static edu.unc.lib.boxc.operations.impl.versioning.VersionedDatastreamService.getNewDatastreamVersion;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

/**
 * Service for updating the user-provided full description of a file object
 *
 * @author snluong
 */
public class FullDescriptionUpdateService {
    private static final Logger log = LoggerFactory.getLogger(FullDescriptionUpdateService.class);
    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private VersionedDatastreamService versionedDatastreamService;
    private OperationsMessageSender operationsMessageSender;
    private boolean sendsMessages;

    public BinaryObject updateFullDescription(FullDescriptionUpdateRequest request) {
        var pid = PIDs.get(request.getPidString());
        aclService.assertHasAccess("User does not have permission to update full description",
                pid, request.getAgent().getPrincipals(), Permission.editDescription);

        var fileObj = repositoryObjectLoader.getFileObject(pid);
        var fullDescPid = DatastreamPids.getFullDescriptionPid(pid);

        BinaryObject fullDescriptionBinary;
        var newVersion = getNewDatastreamVersion(fullDescPid, FULL_DESCRIPTION,
                request.getFullDescriptionText(), request.getTransferSession());
        fullDescriptionBinary = versionedDatastreamService.addVersion(newVersion);

        var resource = fileObj.getResource();
        if (resource != null && resource.hasProperty(Cdr.hasFullDescription)) {
            log.debug("Successfully updated full description for {}", fileObj.getPid());
        } else {
            repositoryObjectFactory.createRelationship(fileObj, Cdr.hasFullDescription, createResource(fullDescPid.getRepositoryPath()));
            log.debug("Successfully add new full description for {}", fileObj.getPid());
        }

        if (sendsMessages) {
            operationsMessageSender.sendUpdateDescriptionOperation(
                    request.getAgent().getUsername(), Collections.singletonList(fileObj.getPid()), IndexingPriority.normal);
        }

        return fullDescriptionBinary;
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

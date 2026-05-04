package edu.unc.lib.boxc.operations.impl.transcript;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.versioning.VersionedDatastreamService;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingPriority;
import edu.unc.lib.boxc.operations.jms.transcript.TranscriptUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static edu.unc.lib.boxc.operations.impl.versioning.VersionedDatastreamService.getNewDatastreamVersion;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

/**
 * Service for updating the user-provided or reviewed transcript of a file object
 *
 * @author snluong
 */
public class TranscriptUpdateService {
    private static final Logger log = LoggerFactory.getLogger(TranscriptUpdateService.class);
    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private VersionedDatastreamService versionedDatastreamService;
    private OperationsMessageSender operationsMessageSender;
    private boolean sendsMessages;

    public BinaryObject updateTranscript(TranscriptUpdateRequest request) {
        var pid = PIDs.get(request.getPidString());
        aclService.assertHasAccess("User does not have permission to update the transcript",
                pid, request.getAgent().getPrincipals(), Permission.editDescription);

        var fileObj = repositoryObjectLoader.getFileObject(pid);
        var transcriptPid = DatastreamPids.getTranscriptPid(pid);

        BinaryObject transcriptBinary;
        var newVersion = getNewDatastreamVersion(transcriptPid, DatastreamType.TRANSCRIPT,
                request.getTranscriptText(), request.getTransferSession());
        transcriptBinary = versionedDatastreamService.addVersion(newVersion);

        var resource = fileObj.getResource();
        if (resource != null && resource.hasProperty(Cdr.hasTranscript)) {
            log.debug("Successfully updated transcript for {}", fileObj.getPid());
        } else {
            repositoryObjectFactory.createRelationship(fileObj, Cdr.hasTranscript, createResource(transcriptPid.getRepositoryPath()));
            log.debug("Successfully add new transcript for {}", fileObj.getPid());
        }

        if (sendsMessages) {
            operationsMessageSender.sendUpdateDescriptionOperation(
                    request.getAgent().getUsername(), Collections.singletonList(fileObj.getPid()), IndexingPriority.normal);
        }

        return transcriptBinary;
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

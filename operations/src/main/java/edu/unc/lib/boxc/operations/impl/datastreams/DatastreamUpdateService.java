package edu.unc.lib.boxc.operations.impl.datastreams;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.versioning.VersionedDatastreamService;
import edu.unc.lib.boxc.operations.jms.OperationsMessageSender;
import edu.unc.lib.boxc.operations.jms.datastreams.DatastreamUpdateRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingPriority;
import org.apache.jena.rdf.model.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static edu.unc.lib.boxc.model.api.DatastreamType.ALT_TEXT;
import static edu.unc.lib.boxc.model.api.DatastreamType.FULL_DESCRIPTION;
import static edu.unc.lib.boxc.model.api.DatastreamType.TRANSCRIPT;
import static edu.unc.lib.boxc.operations.impl.versioning.VersionedDatastreamService.getNewDatastreamVersion;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

public class DatastreamUpdateService {
    private static final Logger log = LoggerFactory.getLogger(DatastreamUpdateService.class);
    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private VersionedDatastreamService versionedDatastreamService;
    private OperationsMessageSender operationsMessageSender;
    private boolean sendsMessages;

    public BinaryObject updateDatastreamText(DatastreamUpdateRequest request) {
        var pid = PIDs.get(request.getPidString());
        var datastreamType = request.getDatastreamType();
        validateDatastreamType(datastreamType);
        var property = getProperty(datastreamType);

        aclService.assertHasAccess("User does not have permission to update datastream text",
                pid, request.getAgent().getPrincipals(), Permission.editDescription);

        var fileObj = repositoryObjectLoader.getFileObject(pid);
        var datastreamPid = getDatastreamPid(datastreamType, pid);

        BinaryObject datastreamBinary;
        var newVersion = getNewDatastreamVersion(datastreamPid, datastreamType,
                request.getText(), request.getTransferSession());

        datastreamBinary = versionedDatastreamService.addVersion(newVersion);
        var resource = fileObj.getResource();
        if (resource != null && resource.hasProperty(property)) {
            log.debug("Successfully updated {} for {}", datastreamType.getId(), fileObj.getPid());
        } else {
            repositoryObjectFactory.createRelationship(fileObj, property, createResource(datastreamPid.getRepositoryPath()));
            log.debug("Successfully add new {} for {}", datastreamType.getId(), fileObj.getPid());
        }

        if (sendsMessages) {
            operationsMessageSender.sendUpdateDescriptionOperation(
                    request.getAgent().getUsername(), Collections.singletonList(fileObj.getPid()), IndexingPriority.normal);
        }

        return datastreamBinary;
    }

    private void validateDatastreamType(DatastreamType type) {
        if (type != ALT_TEXT && type != FULL_DESCRIPTION && type != TRANSCRIPT) {
            throw new IllegalArgumentException("DatastreamUpdateRequest must have a valid DatastreamType");
        }
    }

    private PID getDatastreamPid(DatastreamType type, PID pid) {
        if (type == ALT_TEXT) {
            return DatastreamPids.getAltTextPid(pid);
        }
        if (type == FULL_DESCRIPTION) {
            return DatastreamPids.getFullDescriptionPid(pid);
        }
        return DatastreamPids.getTranscriptPid(pid);
    }

    private Property getProperty(DatastreamType type) {
        if (type == ALT_TEXT) {
            return Cdr.hasAltText;
        }
        if (type == FULL_DESCRIPTION) {
            return Cdr.hasFullDescription;
        }
        return Cdr.hasTranscript;
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

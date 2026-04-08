package edu.unc.lib.boxc.operations.impl.fullDescription;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.versioning.VersionedDatastreamService;
import edu.unc.lib.boxc.operations.jms.fullDescription.FullDescriptionUpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static edu.unc.lib.boxc.model.api.DatastreamType.FULL_DESCRIPTION;

/**
 * Service for updating the user-provided full description of a file object
 *
 * @author bbpennel
 */
public class FullDescriptionUpdateService {
    private static final Logger log = LoggerFactory.getLogger(FullDescriptionUpdateService.class);
    private AccessControlService aclService;
    private RepositoryObjectLoader repositoryObjectLoader;
    private VersionedDatastreamService versionedDatastreamService;

    public BinaryObject updateFullDescription(FullDescriptionUpdateRequest request) {
        var pid = PIDs.get(request.getPidString());
        aclService.assertHasAccess("User does not have permission to update full description",
                pid, request.getAgent().getPrincipals(), Permission.editDescription);

        var fileObj = repositoryObjectLoader.getFileObject(pid);
        var fullDescPid = DatastreamPids.getFullDescriptionPid(pid);
        BinaryObject fullDescriptionBinary;
        var newVersion = new VersionedDatastreamService.DatastreamVersion(fullDescPid);
        newVersion.setContentStream(new ByteArrayInputStream(request.getFullDescriptionText().getBytes(StandardCharsets.UTF_8)));
        newVersion.setContentType(FULL_DESCRIPTION.getMimetype());
        newVersion.setFilename(FULL_DESCRIPTION.getDefaultFilename());
        newVersion.setTransferSession(request.getTransferSession());

        fullDescriptionBinary = versionedDatastreamService.addVersion(newVersion);

        return fullDescriptionBinary;
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setRepositoryObjectLoader(RepositoryObjectLoader repositoryObjectLoader) {
        this.repositoryObjectLoader = repositoryObjectLoader;
    }

    public void setVersionedDatastreamService(VersionedDatastreamService versionedDatastreamService) {
        this.versionedDatastreamService = versionedDatastreamService;
    }
}

package edu.unc.lib.boxc.operations.impl.download;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

import java.util.ArrayList;
import java.util.List;

/**
 * Service that manages downloading all of a Work's FileObjects as a zip file
 */
public class DownloadBulkService {
    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private String basePath;

    public void downloadBulk(DownloadBulkRequest request) {
        var workPid = PIDs.get(request.getWorkPidString());
        var agentPrincipals = request.getAgent().getPrincipals();
        aclService.assertHasAccess(
                "User does not have permissions to view the Work for download",
                workPid, agentPrincipals, Permission.viewOriginal);

        RepositoryObject obj = repoObjLoader.getRepositoryObject(workPid);
        if (!(obj instanceof WorkObject)) {
            throw new IllegalArgumentException("Failed to bulk download for " + obj.getPid());
        }

        var filePids = getFileObjectPids(workPid,agentPrincipals);

    }

    private List<PID> getFileObjectPids(PID workPid, AccessGroupSet agentPrincipals) {
        var filePids = new ArrayList<PID>();
        var workObject = repoObjLoader.getWorkObject(workPid);
        var fileObjects = workObject.getMembers();
        for (ContentObject fileObject : fileObjects ) {
            var filePid = fileObject.getPid();
            if (aclService.hasAccess(filePid, agentPrincipals, Permission.viewOriginal)) {
                filePids.add(filePid);
            }
        }
        return filePids;
    }

    private String getZipFilename(String workPidString) {
        return "ZIP-WORK-" + workPidString;
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setRepoObjLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }
}

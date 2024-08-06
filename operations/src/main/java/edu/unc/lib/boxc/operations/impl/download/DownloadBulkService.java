package edu.unc.lib.boxc.operations.impl.download;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import org.apache.commons.io.IOUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service that manages downloading all of a Work's FileObjects as a zip file
 */
public class DownloadBulkService {
    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private String basePath;

    public String downloadBulk(DownloadBulkRequest request) throws IOException {
        var workPid = PIDs.get(request.getWorkPidString());
        var agentPrincipals = request.getAgent().getPrincipals();
        aclService.assertHasAccess(
                "User does not have permissions to view the Work for download",
                workPid, agentPrincipals, Permission.viewOriginal);

        RepositoryObject obj = repoObjLoader.getRepositoryObject(workPid);
        if (!(obj instanceof WorkObject)) {
            throw new IllegalArgumentException("Failed to bulk download for " + obj.getPid());
        }

        var zipFilePath = basePath + getZipFilename(workPid.getId());

        zipFiles(workPid,agentPrincipals, zipFilePath);
        return zipFilePath;
    }

    private void zipFiles(PID workPid, AccessGroupSet agentPrincipals, String zipFilePath) throws IOException {
        var workObject = repoObjLoader.getWorkObject(workPid);
        var memberObjects = workObject.getMembers();
        final FileOutputStream fos = new FileOutputStream(zipFilePath);
        ZipOutputStream zipOut = new ZipOutputStream(fos);

        for (ContentObject memberObject : memberObjects ) {
            var filePid = memberObject.getPid();
            var fileObject = (FileObject) memberObject;
            if (aclService.hasAccess(filePid, agentPrincipals, Permission.viewOriginal)) {
                var binObj = fileObject.getOriginalFile();
                var binaryStream = binObj.getBinaryStream();
                var filename = binObj.getFilename();

                ZipEntry zipEntry = new ZipEntry(filename);
                zipOut.putNextEntry(zipEntry);

                IOUtils.copy(binaryStream, zipOut);
                binaryStream.close();
            }
        }

        zipOut.close();
        fos.close();
    }

    private Datastream getDatastream(ContentObjectRecord contentObjectRecord) {
        var id = DatastreamType.ORIGINAL_FILE.getId();
        return contentObjectRecord.getDatastreamObject(id);
    }
    private String getZipFilename(String workPidString) {
        return "ZIP-WORK-" + workPidString + ".zip";
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

package edu.unc.lib.boxc.operations.impl.download;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service that manages downloading all of a Work's FileObjects as a zip file
 */
public class DownloadBulkService {
    private AccessControlService aclService;
    private RepositoryObjectLoader repoObjLoader;
    private String basePath;

    public String downloadBulk(DownloadBulkRequest request) {
        var pidString = request.getWorkPidString();
        var workPid = PIDs.get(pidString);
        var agentPrincipals = request.getAgent().getPrincipals();
        aclService.assertHasAccess(
                "User does not have permissions to view the Work for download",
                workPid, agentPrincipals, Permission.viewOriginal);

        var zipFilePath = basePath + getZipFilename(workPid.getId());
        try {
            var workObject = repoObjLoader.getWorkObject(workPid);
            zipFiles(workObject, agentPrincipals, zipFilePath);
        } catch (Exception e){
            throw new NotFoundException("Failed to perform bulk download for WorkObject " + pidString+ " : " + e);
        }

        return zipFilePath;
    }

    private void zipFiles(WorkObject workObject, AccessGroupSet agentPrincipals, String zipFilePath) throws IOException {
        var memberObjects = workObject.getMembers();
        try (
                FileOutputStream fos = new FileOutputStream(zipFilePath);
                ZipOutputStream zipOut = new ZipOutputStream(fos)
        ) {
            if (memberObjects.isEmpty()) {
                return;
            }

            Map<String, Integer> duplicates = new HashMap<>();
            for (ContentObject memberObject : memberObjects ) {
                var fileObject = (FileObject) memberObject;
                if (aclService.hasAccess(memberObject.getPid(), agentPrincipals, Permission.viewOriginal)) {
                    var binObj = fileObject.getOriginalFile();
                    if (binObj == null ) {
                        continue;
                    }
                    try (var binaryStream = binObj.getBinaryStream()) {
                        var filename = binObj.getFilename();

                        // start keeping track of filenames
                        duplicates.putIfAbsent(filename, 0);
                        var copyNumber = duplicates.get(filename);
                        var zipFilename = formatFilename(filename, copyNumber);
                        duplicates.put(filename, copyNumber + 1);

                        ZipEntry zipEntry = new ZipEntry(zipFilename);
                        zipOut.putNextEntry(zipEntry);

                        IOUtils.copy(binaryStream, zipOut);
                    }
                }
            }
        }
    }

    public static String getZipFilename(String workPidString) {
        return "bulk-download-" + workPidString + ".zip";
    }

    private String formatFilename(String filename, int copyNumber) {
        if (copyNumber == 0) {
            return filename;
        }
        var extension = FilenameUtils.getExtension(filename);
        var base = FilenameUtils.removeExtension(filename);
        return base + "(" + copyNumber + ")." + extension;
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

package edu.unc.lib.boxc.operations.impl.download;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.fcrepo.exceptions.ServiceException;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private Path basePath;
    private int fileLimit;

    public Path downloadBulk(DownloadBulkRequest request) {
        var pidString = request.getWorkPidString();
        var workPid = PIDs.get(pidString);
        var agentPrincipals = request.getPrincipals();
        aclService.assertHasAccess(
                "User does not have permissions to view the Work for download",
                workPid, agentPrincipals, Permission.viewOriginal);

        var zipFilePath = Paths.get(basePath.toString(), getZipFilename(pidString));
        var workObject = repoObjLoader.getWorkObject(workPid);
        try {
            zipFiles(workObject, agentPrincipals, zipFilePath);
        } catch (IOException e){
            throw new ServiceException("Failed to perform bulk download for WorkObject " + pidString, e);
        }

        return zipFilePath;
    }

    private void zipFiles(WorkObject workObject, AccessGroupSet agentPrincipals, Path zipFilePath) throws IOException {
        var memberObjects = workObject.getMembers();
        try (
                var fos = Files.newOutputStream(zipFilePath);
                ZipOutputStream zipOut = new ZipOutputStream(fos)
        ) {
            if (memberObjects.isEmpty()) {
                // return an empty zip file
                return;
            }

            Map<String, Integer> duplicates = new HashMap<>();
            int count = 0;
            for (ContentObject memberObject : memberObjects ) {
                if (count == fileLimit) {
                    break;
                }
                if (!(memberObject instanceof FileObject)) {
                    continue;
                }
                var fileObject = (FileObject) memberObject;
                if (aclService.hasAccess(memberObject.getPid(), agentPrincipals, Permission.viewOriginal)) {
                    var binObj = fileObject.getOriginalFile();
                    if (binObj == null) {
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
                    count++;
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

    public void setBasePath(Path basePath) {
        this.basePath = basePath;
    }

    public void setFileLimit(int fileLimit) {
        this.fileLimit = fileLimit;
    }
}

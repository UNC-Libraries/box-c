package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.download.DownloadBulkRequest;
import edu.unc.lib.boxc.operations.impl.download.DownloadBulkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static org.apache.jena.ext.com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;

/**
 * Controller for downloading FileObject original files from a WorkObject into a zip file
 */
@Controller
public class DownloadBulkController {
    @Autowired
    private AccessControlService aclService;
    @Autowired
    private DownloadBulkService downloadBulkService;

    @RequestMapping("/bulkDownload/{id}")
    @ResponseBody
    public ResponseEntity<FileSystemResource> getZip(@PathVariable("id") String pidString) {
        PID pid = PIDs.get(pidString);

        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        aclService.assertHasAccess("Insufficient permissions to bulk download for " + pidString,
                pid, principals, Permission.viewOriginal);
        var request = new DownloadBulkRequest(pidString, principals);
        var path = downloadBulkService.downloadBulk(request);
        var filename = DownloadBulkService.getZipFilename(pidString);

        return ResponseEntity.ok()
                .header(CONTENT_DISPOSITION,"attachment;filename=\"" + filename + "\"")
                .contentType(MediaType.valueOf("application/zip"))
                .body(new FileSystemResource(path));
    }

    public void setDownloadBulkService(DownloadBulkService downloadBulkService) {
        this.downloadBulkService = downloadBulkService;
    }
}

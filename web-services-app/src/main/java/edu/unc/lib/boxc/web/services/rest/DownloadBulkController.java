package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.download.DownloadBulkRequest;
import edu.unc.lib.boxc.operations.impl.download.DownloadBulkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;

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
    public ResponseEntity<InputStreamResource> getZip(@PathVariable("id") String pidString) {
        PID pid = PIDs.get(pidString);

        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        aclService.assertHasAccess("Insufficient permissions to bulk download for " + pidString,
                pid, principals, Permission.viewOriginal);
        var request = new DownloadBulkRequest(pidString, principals);
        var path = downloadBulkService.downloadBulk(request);


        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    public void setDownloadBulkService(DownloadBulkService downloadBulkService) {
        this.downloadBulkService = downloadBulkService;
    }
}

package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.download.DownloadBulkRequest;
import edu.unc.lib.boxc.operations.impl.download.DownloadBulkService;
import edu.unc.lib.boxc.web.common.auth.PatronActionPermissionsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

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

        var agentPrincipal = getAgentPrincipals();
        assertHasPermission(pid, agentPrincipal);
        var request = new DownloadBulkRequest(pidString, agentPrincipal.getPrincipals());
        var path = downloadBulkService.downloadBulk(request);
        var filename = DownloadBulkService.getZipFilename(pidString);

        return ResponseEntity.ok()
                .header(CONTENT_DISPOSITION,"attachment;filename=\"" + filename + "\"")
                .contentType(MediaType.valueOf("application/zip"))
                .body(new FileSystemResource(path));
    }

    public void assertHasPermission(PID pid, AgentPrincipals agent) {
        if (!PatronActionPermissionsUtil.hasBulkDownloadPermission(aclService, pid, agent)) {
            throw new AccessRestrictionException("User has insufficient permissions to download bulk export");
        }
    }

    public void setDownloadBulkService(DownloadBulkService downloadBulkService) {
        this.downloadBulkService = downloadBulkService;
    }
}

package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.exceptions.InvalidPidException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.aspace.RefIdService;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequest;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequestSender;
import edu.unc.lib.boxc.operations.jms.aspace.RefIdRequest;
import edu.unc.lib.boxc.web.services.processing.BulkRefIdCsvExporter;
import edu.unc.lib.boxc.web.services.utils.CsvUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * API controller for editing the ArchivesSpace Ref ID associated with a WorkObject.
 */
@Controller
public class AspaceRefIdController {
    private static final Logger log = LoggerFactory.getLogger(AspaceRefIdController.class);
    public static final String[] IMPORT_CSV_HEADERS = new String[] {"workId", "refId"};

    @Autowired
    RefIdService service;
    @Autowired
    BulkRefIdRequestSender sender;
    @Autowired
    BulkRefIdCsvExporter exporter;

    @PostMapping(value = "/edit/aspace/updateRefId/{pid}")
    @ResponseBody
    public ResponseEntity<Object> updateAspaceRefId(@PathVariable("pid") String pid, @RequestParam("aspaceRefId") String refId) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "editAspaceRefID");
        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        try {
            var request = buildRequest(refId, pid, agent);
            service.updateRefId(request);

            result.put("status", "Updated object with PID" + pid + " with Aspace Ref ID: " + refId);
            result.put("timestamp", System.currentTimeMillis());
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (InvalidPidException | AccessRestrictionException | InvalidOperationForObjectType e) {
            throw e;
        } catch (RepositoryException e) {
            log.error("Error editing Aspace Ref ID for {}", agent.getUsername(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/edit/aspace/updateRefIds/")
    public ResponseEntity<Object> importRefIds(@RequestParam("file") MultipartFile csvFile) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "editAspaceRefIDs");
        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        Path csvPath = null;
        try {
            csvPath = CsvUtil.storeCsvToTemp(csvFile, "refId");
            var request = buildBulkImportRequest(agent, csvPath);
            sender.sendToQueue(request);
            result.put("status", "Bulk update of Aspace ref IDs submitted");
            result.put("timestamp", System.currentTimeMillis());
            return new ResponseEntity<>(result,HttpStatus.OK);
        } catch (IOException e) {
            log.error("Error importing ref ID CSV for {}", agent.getUsername(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            CsvUtil.cleanupCsv(csvPath);
        }
    }

    @GetMapping(value = "/edit/aspace/exportRefIds/{pid}")
    public ResponseEntity<Resource> export(@PathVariable("pid") String pidString) {
        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        var pid = PIDs.get(pidString);

        try {
            var csvPath = exporter.export(pid, agent);
            UrlResource urlResource = new UrlResource(csvPath.toUri());
            // get proper format for pid string
            var filename = "export_ref_ids_" + pid.getId() + ".csv";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.valueOf("text/csv"))
                    .body(urlResource);
        } catch (AccessRestrictionException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("Failed to begin export of Aspace Ref IDs",  e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private RefIdRequest buildRequest(String refId, String pidString, AgentPrincipals agent) {
        var request = new RefIdRequest();
        request.setRefId(refId);
        request.setPidString(pidString);
        request.setAgent(agent);
        return request;
    }

    private BulkRefIdRequest buildBulkImportRequest(AgentPrincipals agent, Path csvPath) throws IOException {
        var map = CsvUtil.convertCsvToMap(IMPORT_CSV_HEADERS, csvPath);
        var request = new BulkRefIdRequest();
        request.setAgent(agent);
        request.setRefIdMap(map);
        request.setEmail(GroupsThreadStore.getEmail());
        return request;
    }

    public void setService(RefIdService service) {
        this.service = service;
    }
}

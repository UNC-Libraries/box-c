package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.exceptions.InvalidPidException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequest;
import edu.unc.lib.boxc.operations.jms.aspace.BulkRefIdRequestSender;
import edu.unc.lib.boxc.operations.jms.aspace.RefIdRequest;
import edu.unc.lib.boxc.operations.impl.aspace.RefIdService;
import edu.unc.lib.boxc.web.services.processing.BulkRefIdCsvExporter;
import edu.unc.lib.boxc.web.services.utils.CsvUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * API controller for editing the ArchivesSpace Ref ID associated with a WorkObject.
 */
@Controller
public class AspaceRefIdController {
    private static final Logger log = LoggerFactory.getLogger(AspaceRefIdController.class);
    public static final String[] CSV_HEADERS = new String[] {"workId", "refId"};

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
            var request = buildBulkRequest(agent, csvPath);
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
    public ResponseEntity<Object> export(@PathVariable("pid") String pid, HttpServletResponse response) {
        Map<String, Object> result = new HashMap<>();
        result.put("action", "export aspace ref IDs");
        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();

        try {
            var csvPath = exporter.export(PIDs.get(pid), agent);
            String filename = "exportRefIds.csv";
            response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.addHeader("Content-Type", "text/csv");
            Files.copy(csvPath, response.getOutputStream());
        } catch (AccessRestrictionException e) {
            result.put("error", "User does not have access");
            return new ResponseEntity<>(result, HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            result.put("error", "Failed to begin export of Aspace Ref IDs: " + e.getMessage());
            log.error("Failed to begin export of Aspace Ref IDs",  e);
            return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private RefIdRequest buildRequest(String refId, String pidString, AgentPrincipals agent) {
        var request = new RefIdRequest();
        request.setRefId(refId);
        request.setPidString(pidString);
        request.setAgent(agent);
        return request;
    }

    private BulkRefIdRequest buildBulkRequest(AgentPrincipals agent, Path csvPath) throws IOException {
        var map = CsvUtil.convertCsvToMap(CSV_HEADERS, csvPath);
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

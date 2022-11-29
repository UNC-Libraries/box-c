package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.exceptions.InvalidPidException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.order.MemberOrderRequestSender;
import edu.unc.lib.boxc.web.services.processing.MemberOrderCsvExporter;
import edu.unc.lib.boxc.web.services.processing.MemberOrderCsvTransformer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.io.File.createTempFile;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

/**
 * Controller for interacting with member order
 *
 * @author bbpennel
 */
@Controller
@RequestMapping(value = "/edit/memberOrder")
public class MemberOrderController {
    private static final Logger log = LoggerFactory.getLogger(MemberOrderController.class);
    private static final String EXPORT_DATE_FORMAT = "yyyy_MM_dd_HH_mm_ss";
    @Autowired
    private MemberOrderCsvExporter memberOrderCsvExporter;
    @Autowired
    private MemberOrderRequestSender memberOrderRequestSender;
    @Autowired
    private MemberOrderCsvTransformer memberOrderCsvTransformer;

    @RequestMapping(value = "export/csv", method = RequestMethod.GET)
    public ResponseEntity<Object> exportCsv(@RequestParam("ids") String ids, HttpServletResponse response) {
        if (StringUtils.isBlank(ids)) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        Path csvPath = null;
        AgentPrincipals agent = AgentPrincipalsImpl.createFromThread();
        try {
            var pids = Arrays.stream(ids.split(",")).map(String::trim).map(PIDs::get).collect(Collectors.toList());
            csvPath = memberOrderCsvExporter.export(pids, agent);
            String filename = getExportFilename();
            response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.addHeader("Content-Type", "text/csv");
            Files.copy(csvPath, response.getOutputStream());
            response.setStatus(HttpStatus.OK.value());
            return null;
        } catch (InvalidPidException | AccessRestrictionException | InvalidOperationForObjectType e) {
            throw e;
        } catch (RepositoryException | IOException e) {
            log.error("Error exporting CSV for {}", agent.getUsername(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            cleanupCsv(csvPath);
        }
    }

    private void cleanupCsv(Path csvPath) {
        if (csvPath != null) {
            try {
                Files.deleteIfExists(csvPath);
            } catch (IOException e) {
                log.warn("Failed to cleanup CSV file: " + e.getMessage());
            }
        }
    }

    private String getExportFilename() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(EXPORT_DATE_FORMAT)
                .withZone(ZoneId.systemDefault());
        return "member_order" + formatter.format(Instant.now()) + ".csv";
    }

    @RequestMapping(value = "import/csv", method = RequestMethod.POST)
    public ResponseEntity<Object> exportCsv(@RequestParam("file") MultipartFile csvFile) {
        var agent = AgentPrincipalsImpl.createFromThread();
        Path csvPath = null;
        try {
            csvPath = storeCsvToTemp(csvFile);
            var orderRequest = memberOrderCsvTransformer.toRequest(csvPath);
            orderRequest.setAgent(agent);
            orderRequest.setEmail(GroupsThreadStore.getEmail());
            memberOrderRequestSender.sendToQueue(orderRequest);

            Map<String, Object> result = new HashMap<>();
            result.put("action", "import member order");
            result.put("timestamp", System.currentTimeMillis());
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Error exporting CSV for {}", agent.getUsername(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            cleanupCsv(csvPath);
        }
    }

    private Path storeCsvToTemp(MultipartFile csvFile) throws IOException {
        File importFile = createTempFile("import_order", ".xml");
        copyInputStreamToFile(csvFile.getInputStream(), importFile);
        return importFile.toPath();
    }
}

package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.web.common.controllers.AbstractSolrSearchController;
import edu.unc.lib.boxc.web.services.processing.ExportCsvService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;

/**
 * Controller which generates a CSV listing of a repository object
 * and all of its children, recursively depth first.
 *
 * @author bbpennel
 */
@Controller
public class ExportCsvController extends AbstractSolrSearchController {
    private static final Logger log = LoggerFactory.getLogger(ExportCsvController.class);

    @Autowired
    private ExportCsvService exportCsvService;


    @GetMapping("exportTree/csv")
    public ResponseEntity<Object> export(@RequestParam("ids") String ids, HttpServletRequest request,
                                         HttpServletResponse response) {
        var pidList = Arrays.stream(ids.split(",")).map(String::trim).collect(Collectors.toList());
        var pids = getPids(pidList);

        try {
            String filename = "export.csv";
            response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            response.addHeader("Content-Type", "text/csv");

            exportCsvService.streamCsv(pids, getAgentPrincipals(), response.getOutputStream());

            response.setStatus(HttpStatus.OK.value());
        } catch (NotFoundException e) {
            log.warn("Object not found: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (RepositoryException | IOException e) {
            log.error("Error exporting CSV: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return null;
    }

    private List<PID> getPids(List<String> pidStrings){
        List<PID> pids = new ArrayList<>();
        for (String pidString : pidStrings) {
            pids.add(PIDs.get(pidString));
        }
        return pids;
    }


}
package edu.unc.lib.boxc.web.services.rest.modify;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;

import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.impl.metadata.ExportDominoMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.PathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;

/**
 * @author bbpennel
 */
@Controller
public class ExportDominoController {
    private static final Logger log = LoggerFactory.getLogger(ExportDominoController.class);

    @Autowired
    private ExportDominoMetadataService exportDominoMetadataService;

    @GetMapping("exportDomino/{pidString}")
    public ResponseEntity<Object> exportDomino(@PathVariable("pidString") String pidString,
                                               @RequestParam(value = "startDate",
                                                       required = false,
                                                       defaultValue = "2020-01-01T00:00:00.0Z") String startDate) {
        var pid = PIDs.get(pidString);

        try {
            String filename = "domino_export.csv";
            var csvPath = exportDominoMetadataService.exportCsv(
                    List.of(pid),
                    getAgentPrincipals(),
                    startDate,
                    "*"
            );
            PathResource pathResource = new PathResource(csvPath);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .header("Content-Type", "text/csv")
                    .body(pathResource);
        } catch (NotFoundException e) {
            log.warn("Object not found for domino: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (IOException | RepositoryException e) {
            log.error("Error exporting DOMino CSV: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

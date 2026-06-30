package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequest;
import edu.unc.lib.boxc.operations.jms.pdf.PdfRequestSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Controller for creating aggregate PDFs
 * @author krwong
 */
@Controller
public class AggregatePdfController {
    private static final Logger log = LoggerFactory.getLogger(AggregatePdfController.class);

    @Autowired
    private PdfRequestSender pdfRequestSender;

    @PostMapping(value = "/edit/aggregatePdf/{id}")
    @ResponseBody
    public ResponseEntity<Object> aggregatePdfObject(@PathVariable("id") String id) {
        return aggregatePdfs(id);
    }

    @PostMapping(value = "/edit/aggregatePdf")
    @ResponseBody
    public ResponseEntity<Object> aggregatePdfBatch(@RequestParam("ids") String ids) {
        if (isEmpty(ids)) {
            throw new IllegalArgumentException("Must provide one or more ids");
        }

        return aggregatePdfs(ids.split("\n"));
    }

    private ResponseEntity<Object> aggregatePdfs(String... ids) {
        Map<String, Object> result = new HashMap<>();
        var agent = AgentPrincipalsImpl.createFromThread();

        result.put("action", "generate aggregate PDFs");

        for (String id : ids) {
            PdfRequest pdfRequest = new PdfRequest();
            pdfRequest.setAgent(agent);
            pdfRequest.setWorkPid(id);

            try {
                pdfRequestSender.sendToQueue(pdfRequest);
                log.info("Operation to aggregate PDF for work {}", pdfRequest.getWorkPid());
            } catch (Exception e) {
                result.put("error", "Failed to aggregate PDF operation");
                log.error("Failed to aggregate PDF for work {}", pdfRequest.getWorkPid(), e);
                return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}

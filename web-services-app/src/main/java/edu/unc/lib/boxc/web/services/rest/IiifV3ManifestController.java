package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.web.services.processing.IiifV3ManifestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Controller which handles iiif v3 requests
 *
 * @author bbpennel
 */
@Controller
public class IiifV3ManifestController {
    private static final Logger log = LoggerFactory.getLogger(IiifV3ManifestController.class);

    @Autowired
    private IiifV3ManifestService manifestService;

    /**
     * Handles requests for IIIF v3 manifests
     * @param id
     * @return Response containing the manifest
     */
    @CrossOrigin
    @GetMapping(value = "/iiif/v3/{id}/manifest", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> getManifest(@PathVariable("id") String id) {
        PID pid = PIDs.get(id);
        log.debug("Getting manifest for {}", pid.getId());
        var manifest = manifestService.buildManifest(pid, AgentPrincipalsImpl.createFromThread());
        return new ResponseEntity<>(manifest, HttpStatus.OK);
    }

    /**
     * Handles requests for IIIF v3 manifests
     * @param id
     * @return Response containing the manifest
     */
    @CrossOrigin
    @GetMapping(value = "/iiif/v3/{id}/canvas", produces = APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> getCanvas(@PathVariable("id") String id) {
        PID pid = PIDs.get(id);
        log.debug("Getting canvas for {}", pid.getId());
        var manifest = manifestService.buildCanvas(pid, AgentPrincipalsImpl.createFromThread());
        return new ResponseEntity<>(manifest, HttpStatus.OK);
    }

    public void setManifestService(IiifV3ManifestService manifestService) {
        this.manifestService = manifestService;
    }
}

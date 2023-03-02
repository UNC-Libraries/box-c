
package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.web.common.exceptions.ResourceNotFoundException;
import edu.unc.lib.boxc.web.services.processing.DownloadImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Objects;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;

/**
 * Controller for handling requests to download access copy image
 * @author snluong
 */
@Controller
public class DownloadImageController {
    private static final Logger log = LoggerFactory.getLogger(EditThumbnailController.class);
    @Autowired
    private AccessControlService aclService;
    @Autowired
    private DownloadImageService downloadImageService;

    @RequestMapping("/downloadImage/{pid}/{size}")
    public ResponseEntity<InputStreamResource> getImage(@PathVariable("pid") String pidString,
                                                        @PathVariable("size") String size,
                                                        HttpServletRequest request,
                                                        HttpServletResponse response) {
        PID pid = PIDs.get(pidString);

        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        aclService.assertHasAccess("Insufficient permissions to download access copy for " + pidString,
                pid, principals, Permission.viewAccessCopies);

        String validatedSize = downloadImageService.getSize(pidString, size);

        if (Objects.equals(validatedSize, "full")) {
            aclService.assertHasAccess("Insufficient permissions to download full size copy for " + pidString,
                    pid, principals, Permission.viewOriginal);
        }

        try {
            return downloadImageService.streamImage(pidString, validatedSize, response);
        } catch (FileNotFoundException e) {
            log.error("Error streaming access copy image for {} at size {}", pidString, validatedSize, e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessRestrictionException.class)
    public void handleInvalidRecordRequest() {
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler({ResourceNotFoundException.class, NotFoundException.class, FileNotFoundException.class})
    public void handleResourceNotFound() {
    }
}

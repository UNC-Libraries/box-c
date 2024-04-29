package edu.unc.lib.boxc.web.services.rest.modify;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.InvalidOperationForObjectType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequest;
import edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequestSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequest.SET;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

/**
 * Controller for handling access surrogate requests. This may include things like
 * setting, replacing, or deleting an access surrogate
 */
@Controller
public class AccessSurrogateController {
    private static final Logger log = LoggerFactory.getLogger(AccessSurrogateController.class);
    public static final String ACTION = "action";
    private Path storagePath = Paths.get("/opt/data/");
    @Autowired
    private AccessControlService aclService;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    private AccessSurrogateRequestSender accessSurrogateRequestSender;

    @PostMapping(value = "/edit/accessSurrogate/{id}/{action}")
    @ResponseBody
    public ResponseEntity<Object> updateAccessSurrogate(@PathVariable("id") String pidString,
                                                        @PathVariable("action") String action,
                                                        @RequestParam("file") MultipartFile surrogateFile) {
        Map<String, Object> result = new HashMap<>();
        PID pid = PIDs.get(pidString);
        var agent = getAgentPrincipals();
        AccessGroupSet principals = agent.getPrincipals();
        aclService.assertHasAccess("Insufficient permissions to update access surrogates for " + pidString,
                pid, principals, Permission.editDescription);

        // check if object is a file
        var repositoryObject = repositoryObjectLoader.getRepositoryObject(pid);
        if (!(repositoryObject instanceof FileObject)) {
            throw new InvalidOperationForObjectType("Cannot update access surrogate of object " +
                    pidString + ", as only FileObjects have access surrogates");
        }

        var request = new AccessSurrogateRequest();

        if (Objects.equals(SET, action)) {
            // uploaded file must be an image
            var mimeType = surrogateFile.getContentType();
            if (!containsIgnoreCase(mimeType, "image")) {
                log.error("Uploaded file for collection {} is not an image file", pidString);
                throw new IllegalArgumentException("Uploaded file is not an image");
            }
            request.setMimetype(mimeType);

            try (InputStream inputStream = surrogateFile.getInputStream()) {
                var path = copyFileToPath(pidString, inputStream);
                request.setFilePath(path);;
            } catch (IOException e) {
                log.error("Failed to get submitted file", e);
                result.put("error", e.getMessage());
                return new ResponseEntity<>(result, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        request.setAction( action);
        request.setAgent(agent);
        request.setPidString(pidString);

        try {
            accessSurrogateRequestSender.sendToQueue(request);
        } catch (IOException e) {
            log.error("Error updating access surrogates for {}", request.getPidString(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        result.put(ACTION, action);
        result.put("timestamp", System.currentTimeMillis());
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private Path copyFileToPath(String pidString, InputStream inputStream) throws IOException {
        String basePath = idToPath(pidString, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        File finalLocation = storagePath.resolve(basePath).resolve(pidString).toFile();
        copyInputStreamToFile(inputStream, finalLocation);
        return finalLocation.toPath();
    }
}

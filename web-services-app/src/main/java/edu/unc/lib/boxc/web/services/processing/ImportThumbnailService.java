package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.thumbnails.ImportThumbnailRequest;
import edu.unc.lib.boxc.operations.jms.thumbnails.ThumbnailRequestSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

/**
 * Service to process requests to add/update display thumbnail objects
 *
 * @author lfarrell
 */
public class ImportThumbnailService {
    private static final Logger log = LoggerFactory.getLogger(ImportThumbnailService.class);

    private String sourceImagesDir;
    private Path storagePath;
    private AccessControlService aclService;
    private ThumbnailRequestSender messageSender;

    public void init() {
        storagePath = Paths.get(sourceImagesDir);
    }

    public void run(InputStream importStream, AgentPrincipals agent, String uuid, String mimeType) throws IOException {
        PID pid = PIDs.get(uuid);

        aclService.assertHasAccess("User does not have permission to add/update collection thumbnails",
                pid, agent.getPrincipals(), Permission.editDescription);


        if (!containsIgnoreCase(mimeType, "image")) {
            log.error("Uploaded file for collection {} is not an image file", uuid);
            throw new IllegalArgumentException("Uploaded file is not an image");
        }

        String thumbnailBasePath = idToPath(uuid, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        File finalLocation = storagePath.resolve(thumbnailBasePath).resolve(uuid).toFile();
        copyInputStreamToFile(importStream, finalLocation);

        var request = new ImportThumbnailRequest();
        request.setAgent(agent);
        request.setMimetype(mimeType);
        request.setPidString(uuid);
        request.setStoragePath(finalLocation.toPath());
        
        messageSender.sendToImportQueue(request);

        log.info("Job to to add thumbnail to object {} has been queued by {}",
                uuid, agent.getUsername());
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setMessageSender(ThumbnailRequestSender messageSender) {
        this.messageSender = messageSender;
    }

    public void setSourceImagesDir(String sourceImagesDir) {
        this.sourceImagesDir = sourceImagesDir;
    }

    public String getSourceImagesDir() {
        return sourceImagesDir;
    }
}

/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.services.processing;

import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static edu.unc.lib.dl.services.RunEnhancementsMessageHelpers.makeEnhancementOperationBody;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.services.MessageSender;

/**
 * Service to process requests to add/update display thumbnail objects
 *
 * @author lfarrell
 */
public class ImportThumbnailService extends MessageSender {
    private static final Logger log = LoggerFactory.getLogger(ImportThumbnailService.class);

    private String sourceImagesDir;
    private Path storagePath;
    private AccessControlService aclService;
    private MessageSender messageSender;

    public void init() {
        storagePath = Paths.get(sourceImagesDir);
    }

    public void run(InputStream importStream, AgentPrincipals agent, String uuid, String mimeType) throws Exception {
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

        Document msg = makeEnhancementOperationBody(agent.getUsername(), pid, true);
        messageSender.sendMessage(msg);

        log.info("Job to to add thumbnail to object {} has been queued by {}",
                uuid, agent.getUsername());
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setMessageSender(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    public void setSourceImagesDir(String sourceImagesDir) {
        this.sourceImagesDir = sourceImagesDir;
    }

    public String getSourceImagesDir() {
        return sourceImagesDir;
    }
}

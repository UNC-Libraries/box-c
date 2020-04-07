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

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.idToPath;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.ATOM_NS;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.tika.Tika;
import org.jdom2.Document;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AgentPrincipals;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.MessageSender;

/**
 * Service to process requests to add/update display thumbnail for collection pages
 *
 * @author lfarrell
 */
public class ImportThumbnailService extends MessageSender {
    private static final Logger log = LoggerFactory.getLogger(ImportThumbnailService.class);

    private String dataDir;
    private Path storagePath;
    private AccessControlService aclService;
    private MessageSender messageSender;

    public void init() throws IOException {
        storagePath = Paths.get(dataDir);

        // Create the directory if it doesn't already exist
        Files.createDirectories(storagePath);
    }

    public void run(InputStream importStream, AgentPrincipals agent, String uuid) {
        PID pid = PIDs.get(uuid);

        aclService.assertHasAccess("User does not have permission to add/update collection thumbnails",
                pid, agent.getPrincipals(), Permission.createCollection);

        try {
            Tika tika = new Tika();
            String mimeType = tika.detect(importStream);

            if (!containsIgnoreCase(mimeType, "image")) {
                throw new Exception("Uploaded file is not an image");
            }

            String thumbnailBasePath = idToPath(uuid, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
            String filePath = Paths.get(storagePath.toString(), thumbnailBasePath, uuid).toString();
            File finalLocation = new File(filePath);
            copyInputStreamToFile(importStream, finalLocation);

            Document msg = createEnhancementMsg(agent.getUsername(), finalLocation.getAbsolutePath(), mimeType);
            messageSender.sendMessage(msg);

            log.info("Job to to add thumbnail to collection {} has been queued by {}",
                    uuid, agent.getUsername());
        } catch (Exception e) {
            log.error("Uploaded file for collection {} is not an image file", uuid);
        }
    }

    private Document createEnhancementMsg(String userid, String filePath, String mimeType) {
        Document msg = new Document();
        Element entry = new Element("entry", ATOM_NS);
        entry.addContent(new Element("author", ATOM_NS)
                .addContent(new Element("name", ATOM_NS).setText(userid)));
        entry.addContent(new Element("pid", ATOM_NS).setText(filePath));
        entry.addContent(new Element("mimeType", ATOM_NS).setText(mimeType));
        entry.addContent(new Element("collectionThumbnail", ATOM_NS).setText("true"));

        msg.addContent(entry);

        return msg;
    }

    public void setAclService(AccessControlService aclService) {
        this.aclService = aclService;
    }

    public void setMessageSender(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public String getDataDir() {
        return dataDir;
    }
}

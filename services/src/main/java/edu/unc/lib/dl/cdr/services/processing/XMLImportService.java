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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import edu.unc.lib.dl.acl.util.AgentPrincipals;
import net.greghaines.jesque.Job;

/**
 *
 * @author harring
 *
 */
public class XMLImportService {
    private static final Logger log = LoggerFactory.getLogger(XMLImportService.class);

    @Autowired
    private net.greghaines.jesque.client.Client jesqueClient;
    @Autowired
    private String bulkMetadataQueueName;

    public void pushJobToQueue(Map<String, Object> result, File importFile, String username, String userEmail)
            throws IllegalArgumentException {

        Job job = new Job(XMLImportJob.class.getName(), username, userEmail, AgentPrincipals.createFromThread(),
                importFile.getAbsolutePath());

        jesqueClient.enqueue(bulkMetadataQueueName, job);
        log.info("Job to import " + importFile.getName() + "has been queued for " + username);
    }

    public File createTempFile(Path storagePath, MultipartFile xmlFile) throws IOException {
        File importFile = File.createTempFile("import", ".xml", storagePath.toFile());
        FileUtils.writeByteArrayToFile(importFile, xmlFile.getBytes());
        return importFile;
    }

    public void setClient(net.greghaines.jesque.client.Client jesqueClient) {
        this.jesqueClient = jesqueClient;
    }

    public net.greghaines.jesque.client.Client getClient() {
        return this.jesqueClient;
    }

    public void setQueueName(String queueName) {
        this.bulkMetadataQueueName = queueName;
    }

    public String getQueueName() {
        return this.bulkMetadataQueueName;
    }

}

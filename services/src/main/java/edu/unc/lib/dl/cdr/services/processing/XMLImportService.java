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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.acl.util.AgentPrincipals;
import net.greghaines.jesque.Job;
import net.greghaines.jesque.client.Client;

/**
 * A service for performing import of bulk metadata updates
 *
 * @author harring
 *
 */
public class XMLImportService {
    private static final Logger log = LoggerFactory.getLogger(XMLImportService.class);

    @Autowired
    private Client jesqueClient;
    @Autowired
    private String bulkMetadataQueueName;
    @Autowired
    private String dataDir;

    private Path storagePath;

    public void init() throws IOException {
        storagePath = Paths.get(dataDir, "metadataImport");
        // Create the directory if it doesn't already exist
        Files.createDirectories(storagePath);
    }

    public void pushJobToQueue(Map<String, Object> result, InputStream importStream, AgentPrincipals agent,
            String userEmail) throws IllegalArgumentException, IOException {

        File importFile = File.createTempFile("import", ".xml", storagePath.toFile());
        FileUtils.copyInputStreamToFile(importStream, importFile);

        Job job = new Job(XMLImportJob.class.getName(), agent, userEmail, importFile.getAbsolutePath());

        jesqueClient.enqueue(bulkMetadataQueueName, job);
        log.info("Job to import " + importFile.getName() + "has been queued for " + agent.getUsername());
    }

    public void setClient(Client jesqueClient) {
        this.jesqueClient = jesqueClient;
    }

    public Client getClient() {
        return jesqueClient;
    }

    public void setQueueName(String queueName) {
        this.bulkMetadataQueueName = queueName;
    }

    public String getQueueName() {
        return bulkMetadataQueueName;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public String getDataDir() {
        return dataDir;
    }

}

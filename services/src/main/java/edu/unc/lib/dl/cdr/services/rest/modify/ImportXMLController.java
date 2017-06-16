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
package edu.unc.lib.dl.cdr.services.rest.modify;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import net.greghaines.jesque.Job;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.update.BulkMetadataUpdateJob;

/**
 * Controller which accepts CDR bulk metadata update packages and begins update operations
 * 
 * @author bbpennel
 * @date Jul 30, 2015
 */
@Controller
public class ImportXMLController {
    private static final Logger log = LoggerFactory.getLogger(ImportXMLController.class);

    @Autowired
    private net.greghaines.jesque.client.Client jesqueClient;
    @Autowired
    private String bulkMetadataQueueName;
    @Autowired
    private String dataDir;
    private Path storagePath;

    @PostConstruct
    public void init() throws IOException {
        storagePath = Paths.get(dataDir + "/metadataImport/");
        // Create the directory if it doesn't already exist
        Files.createDirectories(storagePath);
    }

    @RequestMapping(value = "importXML", method = RequestMethod.POST)
    public @ResponseBody Object importXML(@RequestParam("file") MultipartFile xmlFile,
            HttpServletRequest request) throws Exception {
        log.info("User {} has submitted a bulk metadata update package", GroupsThreadStore.getUsername());
        Map<String, String> result = new HashMap<>();

        File importFile = File.createTempFile("import", ".xml", storagePath.toFile());
        FileUtils.writeByteArrayToFile(importFile, xmlFile.getBytes());

        String emailAddress = GroupsThreadStore.getEmail();

        Job job = new Job(BulkMetadataUpdateJob.class.getName(), null, emailAddress, GroupsThreadStore.getUsername(),
                GroupsThreadStore.getGroups(), importFile.getAbsolutePath(), xmlFile.getOriginalFilename());
        jesqueClient.enqueue(bulkMetadataQueueName, job);

        result.put("message", "Import of metadata has begun, " + emailAddress
                + " will be emailed when the update completes");

        return result;
    }
}

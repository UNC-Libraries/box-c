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
package edu.unc.lib.dl.services.camel.images;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.idToPath;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrEditThumbnail;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.exec.ExecResult;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fcrepo4.PIDs;

/**
 * Adds a derivative file to an existing file object
 *
 * @author bbpennel
 * @author harring
 * @author lfarrell
 *
 */
public class AddDerivativeProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(AddDerivativeProcessor.class);

    private final String fileExtension;
    private final String derivativeBasePath;


    public AddDerivativeProcessor(String fileExtension, String derivativeBasePath) {
        this.fileExtension = fileExtension;
        this.derivativeBasePath = derivativeBasePath;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String binaryId;
        Message in = exchange.getIn();
        String binaryUri = (String) in.getHeader(FCREPO_URI);
        boolean editThumbnail = Boolean.parseBoolean((String) in.getHeader(CdrEditThumbnail));

        if (editThumbnail) {
            String[] collThumbPath = binaryUri.split("/");
            binaryId = collThumbPath[collThumbPath.length - 1];
        } else {
            binaryId = PIDs.get(binaryUri).getId();
        }

        String derivativePath = idToPath(binaryId, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        final ExecResult result = (ExecResult) in.getBody();

        try {
            // Prevent further processing if the execution failed
            if (result.getExitValue() != 0) {
                String stdout = result.getStdout() == null ? "" : IOUtils.toString(result.getStdout(), UTF_8).trim();
                String stderr = result.getStderr() == null ? "" : IOUtils.toString(result.getStderr(), UTF_8).trim();
                log.error("Failed to generate derivative for {}: {} {}", binaryId, stdout, stderr);
                return;
            }

            // Read command result as path to derived file, and trim off trailing whitespace
            String derivativeTmpPath = IOUtils.toString(result.getStdout(), UTF_8).trim();
            derivativeTmpPath += "." + fileExtension;

            Path derivativeFinalPath = Paths.get(derivativeBasePath,  derivativePath, binaryId + "." + fileExtension);

            moveFile(derivativeTmpPath, derivativeFinalPath);
            log.info("Added derivative for {} from {}", binaryUri, derivativeFinalPath);
        } catch (IOException e) {
            String stderr = "";
            if (result != null && result.getStderr() != null) {
                stderr = IOUtils.toString(result.getStderr(), UTF_8).trim();
            }
            log.error("Failed to generated derivative to {} for {}: {}", derivativeBasePath, binaryId, stderr);
            throw e;
        }
    }

    private void moveFile(String derivativeTmpPath, Path derivativeFinalPath)
            throws IOException {
        File derivative = derivativeFinalPath.toFile();
        File parentDir = derivative.getParentFile();

        if (parentDir != null) {
            parentDir.mkdirs();
        }

        Files.move(Paths.get(derivativeTmpPath),
                derivativeFinalPath, REPLACE_EXISTING);
    }
}

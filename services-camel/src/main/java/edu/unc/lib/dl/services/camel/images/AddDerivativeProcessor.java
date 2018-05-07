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
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.exec.ExecResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;

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
        Message in = exchange.getIn();
        String binaryUri = (String) in.getHeader(FCREPO_URI);
        String binaryId = PIDs.get(binaryUri).getId();
        String derivativePath = RepositoryPaths
                .idToPath(binaryId, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);

        final ExecResult result = (ExecResult) in.getBody();

        String derivativeTmpPath = new BufferedReader(new InputStreamReader(result.getStdout()))
                .lines().collect(Collectors.joining("\n"));
        derivativeTmpPath += "." + fileExtension;

        String derivativeFinalRelative = derivativePath + binaryId + "." + fileExtension;
        Path derivativeFinalPath = Paths.get(derivativeBasePath,  derivativeFinalRelative);

        moveFile(derivativeTmpPath, derivativeFinalPath);
        log.info("Adding derivative for {} from {}", binaryUri, derivativeFinalPath);
    }

    private void moveFile(String derivativeTmpPath, Path derivativeFinalPath)
            throws IOException {
        File derivative = derivativeFinalPath.toFile();
        File parentDir = derivative.getParentFile();

        if (parentDir != null && !parentDir.mkdirs()) {
            throw new IOException("Failed to create parent directories for " + derivative);
        }

        Files.move(Paths.get(derivativeTmpPath),
                derivativeFinalPath, REPLACE_EXISTING);
    }
}

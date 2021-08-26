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

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.exec.ExecResult;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders;

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

    private final static Pattern ERROR_PATTERN = Pattern.compile("^(.+ @ error/.+)$", Pattern.MULTILINE);
    private final static String IGNORE_ERROR = "; tag ignored.";

    public AddDerivativeProcessor(String fileExtension, String derivativeBasePath) {
        this.fileExtension = fileExtension;
        this.derivativeBasePath = derivativeBasePath;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();

        String binaryUri = (String) in.getHeader(FCREPO_URI);
        String binaryId = PIDs.get(binaryUri).getId();
        Path derivativeFinalPath = setDerivativeFinalPath(binaryId);

        final ExecResult result = (ExecResult) in.getBody();

        try {
            String stdout = result.getStdout() == null ? "" : IOUtils.toString(result.getStdout(), UTF_8).trim();
            // Prevent further processing if the execution failed
            if (result.getExitValue() != 0) {
                String stderr = result.getStderr() == null ? "" : IOUtils.toString(result.getStderr(), UTF_8).trim();
                Matcher errorMatcher = ERROR_PATTERN.matcher(stderr);
                while (errorMatcher.find()) {
                    String errorString = errorMatcher.group(1);
                    if (errorString.contains(IGNORE_ERROR)) {
                        log.debug("Ignoring error message for {}: {}", binaryId, errorString);
                    } else {
                        log.error("Failed to generate derivative for {}: {} {}", binaryId, stdout, stderr);
                        return;
                    }
                }
                log.debug("Result returned error code {} for {} but no errors were present in the output,"
                        + " derivative will be added: {}", result.getExitValue(), binaryId, stderr);
            }

            // Read command result as path to derived file, and trim off trailing whitespace
            String derivativeTmpPath = stdout.trim();
            derivativeTmpPath += "." + fileExtension;

            moveFile(derivativeTmpPath, derivativeFinalPath);
            log.info("Added derivative for {} from {}", binaryUri, derivativeFinalPath);
        } catch (FileAlreadyExistsException e) {
            log.warn("A derivative already exists for {} at {}. Attempting regeneration without the force flag.",
                    binaryUri, derivativeFinalPath);
            throw e;
        } catch (IOException e) {
            String stderr = "";
            if (result != null && result.getStderr() != null) {
                stderr = IOUtils.toString(result.getStderr(), UTF_8).trim();
            }
            log.error("Failed to generated derivative to {} for {}: {}", derivativeBasePath, binaryId, stderr);
            throw e;
        }
    }

    /**
     * Used to filter whether enhancements should be run
     * @param exchange Camel message exchange
     * @return
     */
    public boolean needsRun(Exchange exchange) {
        Message in = exchange.getIn();

        String binaryUri = (String) in.getHeader(FCREPO_URI);
        String binaryId = PIDs.get(binaryUri).getId();
        Path derivativeFinalPath = setDerivativeFinalPath(binaryId);
        if (Files.notExists(derivativeFinalPath)) {
            log.debug("Derivative run needed, no existing derivative for {} in {}", binaryId, derivativeBasePath);
            return true;
        }

        String force = (String) in.getHeader("force");
        if (Boolean.parseBoolean(force)) {
            log.debug("Force flag was provided, forcing run of already existing derivative for {} in {}",
                    binaryId, derivativeBasePath);
            return true;
        } else {
            log.debug("Derivative already exists for {} in {}, run not needed", binaryId, derivativeBasePath);
            return false;
        }
    }

    /**
     * Deletes a temp file listed in the CdrTempPath header if it is present
     *
     * @param exchange
     * @throws Exception
     */
    public void cleanupTempFile(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();
        String tempValue = (String) in.getHeader(CdrFcrepoHeaders.CdrTempPath);
        tempValue += "." + fileExtension;
        Path tempPath = Paths.get(tempValue);

        boolean deleted = Files.deleteIfExists(tempPath);
        if (deleted) {
            log.debug("Cleaned up leftover temp file {}", tempPath);
        }
    }

    private Path setDerivativeFinalPath(String binaryId) {
        String derivativePath = idToPath(binaryId, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        return Paths.get(derivativeBasePath,  derivativePath, binaryId + "." + fileExtension);
    }

    private void moveFile(String derivativeTmpPath, Path derivativeFinalPath)
            throws IOException {
        File derivative = derivativeFinalPath.toFile();
        File parentDir = derivative.getParentFile();

        if (parentDir != null) {
            parentDir.mkdirs();
        }

        if (log.isDebugEnabled()) {
            log.debug("Moving derivative file from source {} to destination {}, which exists? {}",
                    derivativeTmpPath, derivativeFinalPath, derivative.exists());
        }

        Files.move(Paths.get(derivativeTmpPath),
                derivativeFinalPath, REPLACE_EXISTING);
    }
}

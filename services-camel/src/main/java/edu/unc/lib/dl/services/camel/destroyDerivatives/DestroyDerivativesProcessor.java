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
package edu.unc.lib.dl.services.camel.destroyDerivatives;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.idToPath;
import static edu.unc.lib.dl.model.DatastreamType.FULLTEXT_EXTRACTION;
import static edu.unc.lib.dl.model.DatastreamType.JP2_ACCESS_COPY;
import static edu.unc.lib.dl.model.DatastreamType.THUMBNAIL_LARGE;
import static edu.unc.lib.dl.model.DatastreamType.THUMBNAIL_SMALL;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPidId;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Route to execute requests to destroy object derivative files
 *
 * @author lfarrell
 *
 */
public class DestroyDerivativesProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(DestroyDerivativesProcessor.class);
    private final String fileExtension;
    private final String derivativeBasePath;

    public DestroyDerivativesProcessor(String fileExtension, String derivativeBasePath) {
        this.fileExtension = fileExtension;
        this.derivativeBasePath = derivativeBasePath;
    }

    @Override
    public void process(Exchange exchange) {
        Message in = exchange.getIn();
        String binaryId = (String) in.getHeader(CdrBinaryPidId);
        String binarySubPath = idToPath(binaryId, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        Path derivativePath = Paths.get(derivativeBasePath, binarySubPath, binaryId + fileExtension);

        deleteDerivative(derivativePath, binaryId);
    }

    private void deleteDerivative(Path derivativePath, String binaryId) {
        try {
            if (shouldRemoveFile(derivativePath)) {
                boolean deleted = Files.deleteIfExists(derivativePath);
                if (deleted) {
                    deleteDerivative(derivativePath.getParent(), binaryId);
                }
                log.info("Derivative and parent directories destroyed for {}", binaryId);
            }
        } catch (IOException e) {
            log.warn("Unable to destroy derivative and parent directories for {}: {}", binaryId, e.getMessage());
        }
    }

    private boolean shouldRemoveFile(Path path) throws IOException {
        List<String> rootDirs = Arrays.asList(
                THUMBNAIL_SMALL.getId(),
                THUMBNAIL_LARGE.getId(),
                JP2_ACCESS_COPY.getId(),
                FULLTEXT_EXTRACTION.getId()
        );
        String dirName = path.getFileName().toString();

        if (rootDirs.contains(dirName)) {
            return false;
        }

        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> directory = Files.newDirectoryStream(path)) {
                return !directory.iterator().hasNext();
            }
        }
        return true;
    }
}

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
package edu.unc.lib.dl.services.camel;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.idToPath;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryMimeType;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fcrepo4.PIDs;

/**
 * Processor to add headers to create display thumbnails for non-file objects
 *
 * @author lfarrell
 */
public class NonBinaryEnhancementProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(NonBinaryEnhancementProcessor.class);

    private String sourceImagesDir;

    @Override
    public void process(final Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        String uri = (String) in.getHeader(FCREPO_URI);
        String uuid = PIDs.get(uri).getUUID();
        String objBasePath = idToPath(uuid, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);

        Path imgFile = Paths.get(sourceImagesDir, objBasePath, uuid);

        if (Files.isRegularFile(imgFile)) {
            in.setHeader(CdrBinaryPath, imgFile.toAbsolutePath().toString());
            in.setHeader(CdrBinaryMimeType, "image/*");
        }
    }

    public void setSourceImagesDir(String sourceImagesDir) {
        this.sourceImagesDir = sourceImagesDir;
    }
}

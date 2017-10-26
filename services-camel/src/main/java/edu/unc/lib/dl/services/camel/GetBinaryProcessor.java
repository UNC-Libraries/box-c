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

import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryUri;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;

/**
 * Pulls in the content for a binary object if it is not already accessible in the file system.
 *
 * @author bbpennel
 *
 */
public class GetBinaryProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(GetBinaryProcessor.class);

    private RepositoryObjectLoader repoObjLoader;

    private File tempDirectory;

    public GetBinaryProcessor() {
    }

    public void setTempDirectory(String tempDirectoryString) {
        tempDirectory = new File(tempDirectoryString);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        String binaryUri = (String) in.getHeader(CdrBinaryUri);
        String binaryPath = (String) in.getHeader(CdrBinaryPath);

        // Check to see if the current binary path is valid
        if (binaryPath != null && Files.exists(Paths.get(binaryPath))) {
            return;
        }

        // Path wasn't valid, so download the binary and set path
        PID pid = PIDs.get(binaryUri);
        File file = downloadBinary(pid);

        in.setHeader(CdrBinaryPath, file.getAbsolutePath());

        exchange.getOut().setHeaders(in.getHeaders());
    }

    private File downloadBinary(PID pid) throws IOException {
        File binaryFile = new File(tempDirectory, pid.getId() + "." + System.currentTimeMillis());
        binaryFile.createNewFile();
        binaryFile.deleteOnExit();

        log.debug("Binary for {} not found locally, downloading to {}.", pid.getURI(), binaryFile);

        BinaryObject binary = repoObjLoader.getBinaryObject(pid);
        try (InputStream response = binary.getBinaryStream()) {
            Files.copy(response, Paths.get(binaryFile.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return binaryFile;
    }

    /**
     * @param repository the repository to set
     */
    public void setRepositoryObjectLoader(RepositoryObjectLoader repoObjLoader) {
        this.repoObjLoader = repoObjLoader;
    }
}

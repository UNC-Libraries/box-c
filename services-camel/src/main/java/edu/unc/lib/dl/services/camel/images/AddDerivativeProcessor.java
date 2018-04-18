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

import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinarySubPath;
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

/**
 * Adds a derivative file to an existing file object
 *
 * @author bbpennel
 * @author harring
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
        String binarySubPath = (String) in.getHeader(CdrBinarySubPath);

        final ExecResult result = (ExecResult) in.getBody();

        String derivativePath = new BufferedReader(new InputStreamReader(result.getStdout()))
                .lines().collect(Collectors.joining("\n"));

        moveFile(binaryUri, binarySubPath, derivativePath);
    }

    private void moveFile(String binaryUri, String binarySubPath, String derivativeTmpPath)
            throws IOException {
        Path derivative_path = Paths.get(derivativeBasePath,  binarySubPath + "." + fileExtension);
        File derivative = derivative_path.toFile();
        File parentDir = derivative.getParentFile();

        if (parentDir != null) {
            parentDir.mkdirs();
        }
        derivative.createNewFile();

        Files.move(Paths.get(derivativeTmpPath + "." + fileExtension),
                derivative_path, REPLACE_EXISTING);
        log.info("Adding derivative for {} from {}", binaryUri, derivative.getAbsolutePath());
    }
}

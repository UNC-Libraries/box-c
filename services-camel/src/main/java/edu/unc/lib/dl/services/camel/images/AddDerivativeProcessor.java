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
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

        ingestFile(binaryUri, binarySubPath, derivativePath);
    }

    private String derivativeDirType(String derivativePath) {
        String[] fileType = derivativePath.split("-");
        String fileBase = fileType[fileType.length - 1];
        String dirType;

        if (fileBase.equalsIgnoreCase("large.png")) {
            dirType = "large_thumbnail";
        } else if (fileBase.equalsIgnoreCase("small.png")) {
            dirType = "small_thumbnail";
        } else {
            dirType = "jp2";
        }

        return dirType;
    }

    private void ingestFile(String binaryUri, String binarySubPath, String derivativeTmpPath)
            throws IOException {
        String dirType = derivativeDirType(derivativeTmpPath);
        InputStream binaryStream = new FileInputStream(derivativeTmpPath + "." + fileExtension);

        byte[] buffer = new byte[binaryStream.available()];
        binaryStream.read(buffer);

        File derivative = new File(derivativeBasePath + "/" + dirType + "/" +  binarySubPath + "." + fileExtension);
        File parentDir = derivative.getParentFile();

        if (parentDir != null) {
            parentDir.mkdirs();
        }

        derivative.createNewFile();
        OutputStream outStream = new FileOutputStream(derivative);
        outStream.write(buffer);
        outStream.close();

        binaryStream.close();

        log.info("Adding derivative for {} from {}", binaryUri, derivativeTmpPath);
    }
}

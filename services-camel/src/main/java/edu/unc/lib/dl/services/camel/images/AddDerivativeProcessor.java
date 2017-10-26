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

import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.exec.ExecResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.rdf.PcdmUse;

/**
 * Adds a derivative file to an existing file object
 *
 * @author bbpennel
 * @author harring
 *
 */
public class AddDerivativeProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(AddDerivativeProcessor.class);

    private final RepositoryObjectLoader repoObjLoader;
    private final String slug;
    private final String fileExtension;
    private final String mimetype;

    private final int maxRetries;
    private final long retryDelay;

    public AddDerivativeProcessor(RepositoryObjectLoader repoObjLoader, String slug, String fileExtension,
            String mimetype, int maxRetries, long retryDelay) {
        this.repoObjLoader = repoObjLoader;
        this.slug = slug;
        this.fileExtension = fileExtension;
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
        this.mimetype = mimetype;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        String binaryUri = (String) in.getHeader(FCREPO_URI);
        int retryAttempt = 0;

        final ExecResult result = (ExecResult) in.getBody();

        String derivativePath = new BufferedReader(new InputStreamReader(result.getStdout()))
                .lines().collect(Collectors.joining("\n"));

        while (true) {
            try {
                ingestFile(binaryUri, mimetype, derivativePath);
                break;
            } catch (Exception e) {
                if (retryAttempt == maxRetries) {
                    throw e;
                }

                retryAttempt++;
                log.info("Unable to add derivative for {} from {}. Retrying, attempt {}",
                        binaryUri, derivativePath, retryAttempt);
                TimeUnit.MILLISECONDS.sleep(retryDelay);
            }
        }
    }

    private void ingestFile(String binaryUri, String binaryMimeType, String derivativePath)
            throws FileNotFoundException {
        String filename = derivativePath.substring(derivativePath.lastIndexOf('/') + 1);
        InputStream binaryStream = new FileInputStream(derivativePath + "." + fileExtension);

        BinaryObject binary = repoObjLoader.getBinaryObject(PIDs.get(binaryUri));
        FileObject parent = (FileObject) binary.getParent();
        parent.addDerivative(slug, binaryStream, filename, binaryMimeType, PcdmUse.ThumbnailImage);

        log.info("Adding derivative for {} from {}", binaryUri, derivativePath);
    }
}

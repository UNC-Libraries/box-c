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
package edu.unc.lib.dl.services.camel.fulltext;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;

/**
 * Extracts fulltext from documents and adds it as a derivative file on existing file object
 *
 * @author lfarrell
 *
 */
public class FulltextProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(FulltextProcessor.class);

    private final String derivativeBasePath;

    public FulltextProcessor(String derivativeBasePath) {
        this.derivativeBasePath = derivativeBasePath;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        String binaryUri = (String) in.getHeader(FCREPO_URI);
        String binaryPath = (String) in.getHeader(CdrBinaryPath);
        String binaryId = PIDs.get(binaryUri).getId();
        String binarySubPath = RepositoryPaths
                .idToPath(binaryId, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        String text;

        try {
            text = extractText(binaryPath);
        } catch (TikaException e) {
            // Parsing issues aren't going to succeed on retry, so fail gently
            log.error("Failed to extract text for {} due to parsing error", binaryUri, e);
            return;
        }

        Path derivativePath = Paths.get(derivativeBasePath, binarySubPath + "/" + binaryId + ".txt");
        File derivative = derivativePath.toFile();
        File parentDir = derivative.getParentFile();

        // Create missing parent directories if necessary
        if (parentDir != null && !parentDir.mkdirs()) {
            throw new IOException("Failed to create parent directories for " + derivativePath);
        }

        try (PrintWriter fulltext = new PrintWriter(derivativePath.toString())) {
            fulltext.println(text);
        }
    }

    private String extractText(String filepath) throws IOException, SAXException, TikaException {
        BodyContentHandler handler = new BodyContentHandler();

        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();

        try (InputStream stream = new FileInputStream(new File(filepath))) {
            parser.parse(stream, handler, metadata, new ParseContext());
            return handler.toString();
        }
    }
}
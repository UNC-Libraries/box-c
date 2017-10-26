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

import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

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

import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.rdf.PcdmUse;

/**
 * Extracts fulltext from documents and adds it as a derivative file on existing file object
 *
 * @author lfarrell
 *
 */
public class FulltextProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(FulltextProcessor.class);

    private final RepositoryObjectLoader repoObjLoader;
    private final String slug;
    private final String fileName;

    private final int maxRetries;
    private final long retryDelay;

    private final static String MIMETYPE = "text/plain";

    public FulltextProcessor(RepositoryObjectLoader repoObjLoader, String slug, String fileName,
           int maxRetries, long retryDelay) {
        this.repoObjLoader = repoObjLoader;
        this.slug = slug;
        this.fileName = fileName;
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        String binaryUri = (String) in.getHeader(FCREPO_URI);
        String binaryPath = (String) in.getHeader(CdrBinaryPath);
        String text;
        try {
            text = extractText(binaryPath);
        } catch (TikaException e) {
            // Parsing issues aren't going to succeed on retry, so fail gently
            log.error("Failed to extract text for {} due to parsing error", binaryUri, e);
            return;
        }

        int retryAttempt = 0;

        InputStream binaryStream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));

        while (true) {
            try {
                BinaryObject binary = repoObjLoader.getBinaryObject(PIDs.get(binaryUri));
                FileObject parent = (FileObject) binary.getParent();

                parent.addDerivative(slug, binaryStream, fileName, MIMETYPE, PcdmUse.ExtractedText);

                log.info("Adding derivative for {} from {}", binaryUri, fileName);
                break;
            } catch (Exception e) {
                if (retryAttempt == maxRetries) {
                    throw e;
                }

                retryAttempt++;
                log.info("Unable to add derivative for {} from {}. Retrying, attempt {}",
                        binaryUri, binaryPath, retryAttempt);
                TimeUnit.MILLISECONDS.sleep(retryDelay);

            }
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
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
import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.idToPath;
import static edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.commons.io.FileUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.services.camel.util.CdrFcrepoHeaders;

/**
 * Extracts fulltext from documents and adds it as a derivative file on existing file object
 *
 * @author lfarrell
 *
 */
public class FulltextProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(FulltextProcessor.class);

    private static final int CHAR_LIMIT = 1000000;

    private int characterLimit = CHAR_LIMIT;

    private final String derivativeBasePath;

    private static final Pattern MIMETYPE_PATTERN = Pattern.compile( "^(text/|application/pdf|application/msword"
            + "|application/vnd\\.|application/rtf|application/powerpoint"
            + "|application/postscript).*$");

    public FulltextProcessor(String derivativeBasePath) {
        this.derivativeBasePath = derivativeBasePath;
    }

    /**
     * Returns true if the subject of the exchange is a binary which
     * is eligible for having image derivatives generated from it.
     *
     * @param exchange
     * @return
     */
    public static boolean allowedTextType(Exchange exchange) {
        Message in = exchange.getIn();
        String mimetype = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType);
        String binPath = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryPath);

        if (!MIMETYPE_PATTERN.matcher(mimetype).matches()) {
            log.debug("File type {} on object {} is not applicable for text derivatives", mimetype, binPath);
            return false;
        }

        log.debug("Object {} with type {} is permitted for text derivatives", binPath, mimetype);
        return true;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        final Message in = exchange.getIn();

        String fedoraUri = (String) in.getHeader(FCREPO_URI);
        String binaryPath = (String) in.getHeader(CdrBinaryPath);
        String binaryId = PIDs.get(fedoraUri).getId();
        String binarySubPath = idToPath(binaryId, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        String text;

        try {
            text = extractText(binaryPath);
        } catch (TikaException e) {
            // Parsing issues aren't going to succeed on retry, so fail gently
            log.error("Failed to extract text for {} due to parsing error", fedoraUri, e);
            return;
        }

        Path derivativePath = Paths.get(derivativeBasePath, binarySubPath, binaryId + ".txt");
        File derivative = derivativePath.toFile();
        File parentDir = derivative.getParentFile();

        // Create missing parent directories if necessary
        if (parentDir != null) {
            try {
                Files.createDirectories(parentDir.toPath());
            } catch (IOException e) {
                throw new IOException("Failed to create parent directories for " + derivativePath + ".", e);
            }

            FileUtils.write(derivative, text, UTF_8);
        }
    }

    private String extractText(String binaryPath) throws IOException, SAXException, TikaException {
        BodyContentHandler handler = new BodyContentHandler(characterLimit);

        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        File fileToExtract = new File(binaryPath);

        if (fileToExtract.length() > 0) {
            try (InputStream stream = new FileInputStream(fileToExtract)) {
                parser.parse(stream, handler, metadata, new ParseContext());
            } catch (SAXException e) {
                // Check for character limit exceeded message, since the exception is private
                if (e.getMessage().contains("document contained more than")) {
                    log.warn("File {} contained more than {} characters, extracted text limited to this length",
                            binaryPath, characterLimit);
                } else {
                    throw e;
                }
            }
            return handler.toString();
        } else {
            log.warn("File, {}, does not have any text to extract", binaryPath);
            return "";
        }
    }

    public void setCharacterLimit(int characterLimit) {
        this.characterLimit = characterLimit;
    }
}
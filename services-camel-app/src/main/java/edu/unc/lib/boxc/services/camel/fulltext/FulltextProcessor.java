package edu.unc.lib.boxc.services.camel.fulltext;

import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders.CdrBinaryPath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;

/**
 * Extracts fulltext from documents and adds it as a derivative file on existing file object
 *
 * @author lfarrell
 *
 */
public class FulltextProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(FulltextProcessor.class);

    private static final int CHAR_LIMIT = 100000;

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

    private String extractText(String binaryPath) throws IOException, TikaException {
        Path fileToExtract = Paths.get(binaryPath);

        if (Files.size(fileToExtract) > 0) {
            var tika = new Tika();
            tika.setMaxStringLength(characterLimit);
            return tika.parseToString(fileToExtract);
        } else {
            log.warn("File, {}, does not have any text to extract", binaryPath);
            return "";
        }
    }

    public void setCharacterLimit(int characterLimit) {
        this.characterLimit = characterLimit;
    }
}
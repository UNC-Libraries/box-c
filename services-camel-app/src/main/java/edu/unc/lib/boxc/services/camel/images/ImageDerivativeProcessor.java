package edu.unc.lib.boxc.services.camel.images;

import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

/**
 * Processor which validates and prepares image objects for producing derivatives
 *
 * @author bbpennel
 */
public class ImageDerivativeProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(ImageDerivativeProcessor.class);

    private static final Pattern MIMETYPE_PATTERN = Pattern.compile("^(image.*$|application.*?(photoshop|psd)$)");

    private static final Pattern DISALLOWED_PATTERN =
            Pattern.compile(".*(vnd\\.fpx|x-icon|x-raw-panasonic|vnd\\.microsoft\\.icon).*");

    private String tempBasePath;

    /**
     * Returns true if the subject of the exchange is a binary which
     * is eligible for having image derivatives generated from it.
     *
     * @param exchange
     * @return
     */
    public static boolean allowedImageType(Exchange exchange) {
        Message in = exchange.getIn();
        String mimetype = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType);
        String binPath = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryPath);

        if (!MIMETYPE_PATTERN.matcher(mimetype).matches()) {
            log.debug("File type {} on object {} is not applicable for image derivatives", mimetype, binPath);
            return false;
        }

        if (DISALLOWED_PATTERN.matcher(mimetype).matches()) {
            log.debug("File type {} on object {} is disallowed for image derivatives", mimetype, binPath);
            return false;
        }

        log.debug("Object {} with type {} is permitted for image derivatives", binPath, mimetype);
        return true;
    }

    /**
     * Cleans up the temporary image file created for the binary
     * @param exchange
     * @throws IOException
     */
    public void cleanupTempImage(Exchange exchange) throws IOException {
        final Message in = exchange.getIn();
        Boolean tempCleanup = (Boolean) in.getHeader(CdrFcrepoHeaders.CdrImagePathCleanup);
        log.debug("Temp cleanup is set to {}", tempCleanup);
        if (tempCleanup != null && tempCleanup) {
            String binPath = (String) in.getHeader(CdrFcrepoHeaders.CdrImagePath);
            Path binPathObj = Paths.get(binPath);
            if (!Files.exists(binPathObj)) {
                log.info("Image path {} does not exist, skipping cleanup", binPath);
                return;
            }
            if (binPathObj.startsWith(Paths.get(tempBasePath))) {
                log.debug("Cleaning up temp binary file {}", binPath);
                Files.deleteIfExists(binPathObj);
            } else {
                log.error("Image path {} is not a temp file, skipping cleanup", binPath);
            }
        }
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        String mimetype = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType);

        String binPath = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryPath);
        log.debug("Keeping existing image path as {} for type {}", binPath, mimetype);
        in.setHeader(CdrFcrepoHeaders.CdrImagePath, binPath);
    }

    public void setTempBasePath(String tempBasePath) {
        this.tempBasePath = tempBasePath;
    }
}

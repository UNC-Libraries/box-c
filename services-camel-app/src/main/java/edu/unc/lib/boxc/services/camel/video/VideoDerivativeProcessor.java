package edu.unc.lib.boxc.services.camel.video;

import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Processor which validates and prepares video objects for producing derivatives
 * @author krwong
 */
public class VideoDerivativeProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(VideoDerivativeProcessor.class);

    private static final Pattern MIMETYPE_PATTERN = Pattern.compile(
        "^(video.(mp4|quicktime|m2ts|mpeg|mpg|x-ms-wmv|x-msvideo|x-matroska|x-flv|x-m4v|webm|x-ms-asf|3gpp))" +
                "|(application.x-shockwave-flash)$");

    /**
     * Returns true if the subject of the exchange is a binary which
     * is eligible for having video derivatives generated from it.
     * @param exchange
     * @return
     */
    public static boolean allowedVideoType(Exchange exchange) {
        Message in = exchange.getIn();
        String mimetype = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType);
        String binPath = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryPath);

        if (!MIMETYPE_PATTERN.matcher(mimetype).matches()) {
            log.debug("File type {} on object {} is not applicable for video derivatives", mimetype, binPath);
            return false;
        }

        log.debug("Object {} with type {} is permitted for video derivatives", binPath, mimetype);
        return true;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        String mimetype = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType);

        String binPath = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryPath);
        log.debug("Keeping existing video path as {} for type {}", binPath, mimetype);
        in.setHeader(CdrFcrepoHeaders.CdrVideoPath, binPath);
    }
}

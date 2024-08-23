package edu.unc.lib.boxc.services.camel.audio;

import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Processor which validates and prepares audio objects for producing derivatives
 * @author krwong
 */
public class AudioDerivativeProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(AudioDerivativeProcessor.class);

    private static final Pattern MIMETYPE_PATTERN = Pattern.compile("^(audio.wav)$");

    /**
     * Returns true if the subject of the exchange is a binary which
     * is eligible for having audio derivatives generated from it.
     * @param exchange
     * @return
     */
    public static boolean allowedAudioType(Exchange exchange) {
        Message in = exchange.getIn();
        String mimetype = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType);
        String binPath = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryPath);

        if (!MIMETYPE_PATTERN.matcher(mimetype).matches()) {
            log.debug("File type {} on object {} is not applicable for audio derivatives", mimetype, binPath);
            return false;
        }

        log.debug("Object {} with type {} is permitted for audio derivatives", binPath, mimetype);
        return true;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        String mimetype = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType);

        String binPath = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryPath);
        log.debug("Keeping existing audio path as {} for type {}", binPath, mimetype);
        in.setHeader(CdrFcrepoHeaders.CdrAudioPath, binPath);
    }
}

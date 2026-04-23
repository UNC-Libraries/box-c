package edu.unc.lib.boxc.services.camel.pdf;

import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Processor that runs pdf4u to generate PDF derivatives
 * @author krwong
 */
public class PdfDerivativeProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(PdfDerivativeProcessor.class);

    private static final Pattern MIMETYPE_PATTERN = Pattern.compile("^(image.*$|application.*?(photoshop|psd|pdf)$)");

    private static final Pattern DISALLOWED_PATTERN =
            Pattern.compile(".*(vnd\\.fpx|x-icon|x-raw-panasonic|vnd\\.microsoft\\.icon).*");

    /**
     * Returns true if the subject of the exchange is a binary which
     * is eligible for having pdf derivatives generated from it.
     * @param exchange
     * @return
     */
    public static boolean allowedPdfType(Exchange exchange) {
        Message in = exchange.getIn();
        String mimetype = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType);
        String binPath = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryPath);

        if (!MIMETYPE_PATTERN.matcher(mimetype).matches()) {
            log.debug("File type {} on object {} is not applicable for pdf derivatives", mimetype, binPath);
            return false;
        }

        if (DISALLOWED_PATTERN.matcher(mimetype).matches()) {
            log.debug("File type {} on object {} is disallowed for pdf derivatives", mimetype, binPath);
            return false;
        }

        log.debug("Object {} with type {} is permitted for pdf derivatives", binPath, mimetype);
        return true;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        String mimetype = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType);

        String binPath = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryPath);
        log.debug("Keeping existing pdf path as {} for type {}", binPath, mimetype);
        in.setHeader(CdrFcrepoHeaders.CdrPdfPath, binPath);
    }
}

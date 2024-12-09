package edu.unc.lib.boxc.services.camel.images;

import JP2ImageConverter.CLIMain;
import JP2ImageConverter.JP2ImageConverterCommand;
import JP2ImageConverter.options.JP2ImageConverterOptions;
import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Processor that runs jp24u to generate jp2 derivatives
 * @author krwong
 */
public class Jp2Processor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(Jp2Processor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        String imagePath = (String) in.getHeader(CdrFcrepoHeaders.CdrImagePath);
        String tempPath = (String) in.getHeader(CdrFcrepoHeaders.CdrTempPath);
        String mimetype = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType);

        String[] command = new String[]{"jp24u", "kdu_compress", "-f", imagePath,
                "-o", tempPath, "-sf", mimetype};
        log.debug("Run jp24u command {} for type {}", command, mimetype);
        CLIMain.runCommand(command);
    }
}

package edu.unc.lib.boxc.services.camel.images;

import JP2ImageConverter.CLIMain;
import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.component.exec.ExecCommand;
import org.apache.camel.component.exec.ExecResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

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
        int exitCode = CLIMain.runCommand(command);

        Result result = new Result(new ByteArrayInputStream(tempPath.getBytes()), null, exitCode);
        in.setBody(result);
    }

    public static class Result extends ExecResult {
        private static final ExecCommand command = new ExecCommand("jp24u",
                Arrays.asList("kdu_compress"), null, Long.valueOf(60), null,
                null, null, false, LoggingLevel.OFF);

        public Result(InputStream stdout, InputStream stderr, int exitCode) {
            super(command, stdout, stderr, exitCode);
        }
    }
}

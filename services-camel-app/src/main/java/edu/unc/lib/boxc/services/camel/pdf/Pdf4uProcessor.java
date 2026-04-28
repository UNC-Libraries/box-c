package edu.unc.lib.boxc.services.camel.pdf;

import edu.unc.lib.boxc.services.camel.util.CdrFcrepoHeaders;
import pdf4u.CLIMain;
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
 * Processor which validates and prepares PDF objects for producing derivatives
 * @author krwong
 */
public class Pdf4uProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(Pdf4uProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        String pdfPath = (String) in.getHeader(CdrFcrepoHeaders.CdrPdfPath);
        String tempPath = (String) in.getHeader(CdrFcrepoHeaders.CdrTempPath);
        String mimetype = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType);
        //TODO: get text type from the alt text review
        String textType = "HANDWRITTEN-PRINT";

        String[] command = new String[]{"pdf4u", "add_ocr", "-i", pdfPath,
                "-o", tempPath, "-tt", textType};
        log.debug("Run pdf4u command {} for type {}", command, mimetype);
        int exitCode = CLIMain.runCommand(command);

        Result result = new Result(command, new ByteArrayInputStream(tempPath.getBytes()), null, exitCode);
        in.setBody(result);
    }

    public static class Result extends ExecResult {
        public Result(String[] command, InputStream stdout, InputStream stderr, int exitCode) {
            super(new ExecCommand(command[0],
                            Arrays.asList(command), null, 0L, null,
                            null, null, false, LoggingLevel.OFF),
                    stdout, stderr, exitCode);
        }
    }
}

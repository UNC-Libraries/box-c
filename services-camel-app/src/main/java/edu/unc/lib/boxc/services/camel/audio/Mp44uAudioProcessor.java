package edu.unc.lib.boxc.services.camel.audio;

import mp44u.CLIMain;
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
 * Processor that runs mp44u to generate m4a derivatives
 * @author krwong
 */
public class Mp44uAudioProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(Mp44uAudioProcessor.class);
    private String mp44uThreads;

    public Mp44uAudioProcessor(String mp44uThreads) {
        this.mp44uThreads = mp44uThreads;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        String audioPath = (String) in.getHeader(CdrFcrepoHeaders.CdrAudioPath);
        String tempPath = (String) in.getHeader(CdrFcrepoHeaders.CdrTempPath);
        String mimetype = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType);

        String[] command = new String[]{"mp44u", "audio", "-i", audioPath,
                "-o", tempPath, "-t", mp44uThreads};
        log.debug("Run mp44u command {} for type {}", command, mimetype);
        int exitCode = CLIMain.runCommand(command);

        Result result = new Result(new ByteArrayInputStream(tempPath.getBytes()), null, exitCode);
        in.setBody(result);
    }

    public static class Result extends ExecResult {
        private static final ExecCommand command = new ExecCommand("mp44u",
                Arrays.asList("audio"), null, Long.valueOf(60), null,
                null, null, false, LoggingLevel.OFF);

        public Result(InputStream stdout, InputStream stderr, int exitCode) {
            super(command, stdout, stderr, exitCode);
        }
    }
}

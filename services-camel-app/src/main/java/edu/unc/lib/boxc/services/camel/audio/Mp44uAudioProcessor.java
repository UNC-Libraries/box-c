package edu.unc.lib.boxc.services.camel.audio;

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

public class Mp44uAudioProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(Mp44uAudioProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        String videoPath = (String) in.getHeader(CdrFcrepoHeaders.CdrImagePath);
        String tempPath = (String) in.getHeader(CdrFcrepoHeaders.CdrTempPath);
        String mimetype = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType);

        String[] command = new String[]{"mp44u", "audio", "-i", videoPath,
                "-o", tempPath};
        log.debug("Run mp44u command {} for type {}", command, mimetype);
        int exitCode = CLIMain.runCommand(command);

        Mp44uAudioProcessor.Result result = new Mp44uAudioProcessor.Result(
                new ByteArrayInputStream(tempPath.getBytes()), null, exitCode);
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

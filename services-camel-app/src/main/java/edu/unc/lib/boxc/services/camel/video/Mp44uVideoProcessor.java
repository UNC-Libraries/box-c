package edu.unc.lib.boxc.services.camel.video;

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
 * Processor that runs mp44u to generate mp4 derivatives
 * @author krwong
 */
public class Mp44uVideoProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(Mp44uVideoProcessor.class);
    private String mp44uThreads;
    private int mp44uTimeout;

    public Mp44uVideoProcessor(String mp44uThreads, int mp44uTimeout) {
        this.mp44uThreads = mp44uThreads;
        this.mp44uTimeout = mp44uTimeout;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Message in = exchange.getIn();
        String videoPath = (String) in.getHeader(CdrFcrepoHeaders.CdrVideoPath);
        String tempPath = (String) in.getHeader(CdrFcrepoHeaders.CdrTempPath);
        String mimetype = (String) in.getHeader(CdrFcrepoHeaders.CdrBinaryMimeType);

        String[] command = new String[]{"mp44u", "video", "-i", videoPath,
                "-o", tempPath, "-t", mp44uThreads, "-T", Integer.toString(mp44uTimeout)};
        log.debug("Run mp44u command {} for type {}", command, mimetype);
        int exitCode = CLIMain.runCommand(command);

        Result result = new Result(command, new ByteArrayInputStream(tempPath.getBytes()), null, exitCode);
        in.setBody(result);

        if (exitCode != 0) {
            throw new Mp44uExecutionException("mp44u video command " + Arrays.toString(command)
                    + " failed to execute for " + videoPath);
        }
    }

    public static class Result extends ExecResult {
        public Result(String[] command, InputStream stdout, InputStream stderr, int exitCode) {
            super(new ExecCommand(command[0],
                            Arrays.asList(command), null, 0L, null,
                            null, null, false, LoggingLevel.OFF),
                    stdout, stderr, exitCode);
        }
    }

    public static class Mp44uExecutionException extends RuntimeException {
        public Mp44uExecutionException(String message) {
            super(message);
        }
    }
}

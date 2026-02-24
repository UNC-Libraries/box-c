package edu.unc.lib.boxc.common.util;

import edu.unc.lib.boxc.common.errors.CommandException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Utilities for executing commands
 * @author bbpennel
 */
public class CLIUtil {
    private static final Logger log = LoggerFactory.getLogger(CLIUtil.class);

    private CLIUtil() {
    }

    /**
     * Run a given command, with a timeout
     * @param command the command to be executed
     * @param timeout max time for the command to run, in seconds
     * @return List containing the standard output and standard error output
     */
    public static List<String> executeCommand(List<String> command, long timeout) {
        log.debug("Executing command: {}", String.join(" ", command));
        CommandLine cmdLine = CommandLine.parse(command.getFirst());
        cmdLine.addArguments(command.subList(1, command.size()).toArray(new String[0]));

        DefaultExecutor executor = DefaultExecutor.builder().get();
        var watchdog = ExecuteWatchdog.builder()
                .setTimeout(Duration.of(timeout, ChronoUnit.SECONDS))
                .get();
        executor.setWatchdog(watchdog);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        executor.setStreamHandler(new PumpStreamHandler(outputStream, errorStream));

        try {
            executor.execute(cmdLine);
            return List.of(outputStream.toString(), errorStream.toString());
        } catch (IOException e) {
            throw new CommandException("Command failed to execute", command, outputStream.toString(), e);
        }
    }
}

package edu.unc.lib.boxc.common.util;

import edu.unc.lib.boxc.common.errors.CommandException;
import edu.unc.lib.boxc.common.errors.CommandTimeoutException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
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
        log.debug("Executing command with timeout {}s: {}", timeout, String.join(" ", command));
        CommandLine cmdLine = CommandLine.parse(command.getFirst());
        cmdLine.addArguments(command.subList(1, command.size()).toArray(new String[0]));

        DefaultExecutor executor = DefaultExecutor.builder().get();
        ExecuteWatchdog watchdog = null;
        if (timeout > 0) {
            watchdog = EscalatingExecuteWatchdog.create(Duration.ofSeconds(timeout));
            executor.setWatchdog(watchdog);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        executor.setStreamHandler(new PumpStreamHandler(outputStream, errorStream));

        try {
            executor.execute(cmdLine);
            return List.of(outputStream.toString(), errorStream.toString());
        } catch (ExecuteException e) {
            String output = outputStream.toString();
            int exitValue = e.getExitValue();

            if (watchdog != null && watchdog.killedProcess()) {
                throw new CommandTimeoutException("Command timed out after " + timeout + " seconds",
                        command, output);
            }
            throw new CommandException("Command failed to execute", command, output, exitValue, e);
        } catch (IOException e) {
            String output = outputStream + "\n" + errorStream;
            throw new CommandException("Command failed to execute", command, output, e);        }
    }
}

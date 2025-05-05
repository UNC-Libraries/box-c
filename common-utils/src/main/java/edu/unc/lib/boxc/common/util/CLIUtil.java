package edu.unc.lib.boxc.common.util;

import edu.unc.lib.boxc.common.errors.CommandException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            Process process = builder.start();

            // Use a separate thread to read the output concurrently, to avoid deadlock in case command times out
            var outputConsumer = new StreamConsumer(process.getInputStream());
            var errorConsumer = new StreamConsumer(process.getErrorStream());
            outputConsumer.start();
            errorConsumer.start();

            waitForProcess(process, command, timeout);
            // If any errors occurred while reading the output, they will be thrown here
            outputConsumer.join();
            errorConsumer.join();

            if (process.exitValue() != 0) {
                throw new CommandException("Command exited with errors", command,
                        combinedOutput(outputConsumer, errorConsumer), process.exitValue());
            }

            return List.of(outputConsumer.getContent(), errorConsumer.getContent());
        } catch (InterruptedException | IOException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new CommandException("Command failed to execute", command, "", e);
        }
    }

    private static String combinedOutput(StreamConsumer outputConsumer, StreamConsumer errorConsumer) {
        return outputConsumer.getContent() + errorConsumer.getContent();
    }

    private static void waitForProcess(Process process, List<String> command, long timeout)
            throws InterruptedException {
        log.debug("Waiting for process for {} seconds: {}", timeout, command);
        if (!process.waitFor(timeout, TimeUnit.SECONDS)) {
            log.warn("Command timed out, attempting to end process: {}", command);
            process.destroy();
            throw new CommandException("Command timed out after " + timeout + " seconds",
                    command, "", -1);
        }
    }

    private static class StreamConsumer extends Thread {
        private final InputStream inputStream;
        private final StringBuilder content = new StringBuilder();

        public StreamConsumer(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try (var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            } catch (IOException e) {
                log.error("Error reading process stream", e);
            }
        }

        public String getContent() {
            return content.toString();
        }
    }
}

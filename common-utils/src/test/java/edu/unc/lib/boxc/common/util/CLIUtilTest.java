package edu.unc.lib.boxc.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.unc.lib.boxc.common.errors.CommandException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author bbpennel
 */
public class CLIUtilTest {

    @TempDir
    Path tempDir;

    @Test
    public void testExecuteCommandSuccess() throws IOException {
        // Create platform-specific command to echo text
        List<String> command = getPlatformEchoCommand("test output");

        // Execute the command
        List<String> results = CLIUtil.executeCommand(command, 5);

        // Verify results
        assertEquals(2, results.size());
        assertTrue(results.get(0).contains("test output"));
        assertEquals("", results.get(1));
    }

    @Test
    public void testExecuteCommandWithStdErr() throws IOException {
        // Create script that outputs to stderr
        Path scriptPath = createExecutableScript(
                "#!/bin/sh\n" +
                        "echo 'standard output'\n" +
                        "echo 'error output' >&2\n");

        // Execute the command
        List<String> results = CLIUtil.executeCommand(List.of(scriptPath.toString()), 5);

        // Verify results
        assertEquals(2, results.size());
        assertEquals("standard output\n", results.get(0));
        assertEquals("error output\n", results.get(1));
    }

    @Test
    public void testExecuteCommandNonZeroExit() throws IOException {
        // Create script that exits with non-zero
        Path scriptPath = createExecutableScript(
                "#!/bin/sh\n" +
                        "echo 'some output'\n" +
                        "exit 1\n");

        // Execute the command and verify it throws CommandException
        CommandException exception = assertThrows(CommandException.class, () ->
                CLIUtil.executeCommand(List.of(scriptPath.toString()), 5));

        assertEquals(1, exception.getExitCode());
        assertTrue(exception.getMessage().contains("Command exited with errors"));
        assertTrue(exception.getOutput().contains("some output"));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    public void testExecuteCommandTimeout() throws IOException {
        // Create script that sleeps
        Path scriptPath = createExecutableScript(
                "#!/bin/sh\n" +
                        "sleep 10\n");

        // Execute the command with short timeout and verify it throws CommandException
        CommandException exception = assertThrows(CommandException.class, () ->
                CLIUtil.executeCommand(List.of(scriptPath.toString()), 1));

        assertEquals(-1, exception.getExitCode());
        assertTrue(exception.getMessage().contains("Command timed out after 1 seconds"));
    }

    @Test
    public void testExecuteInvalidCommand() {
        // Try to execute non-existent command
        CommandException exception = assertThrows(CommandException.class, () ->
                CLIUtil.executeCommand(List.of("thisCommandDoesNotExist_" + System.currentTimeMillis()), 5));

        assertTrue(exception.getMessage().contains("Command failed to execute"));
    }

    /**
     * Creates platform-specific command to echo text
     */
    private List<String> getPlatformEchoCommand(String text) {
        return Arrays.asList("echo", text);
    }

    /**
     * Creates an executable script file with the given content
     */
    private Path createExecutableScript(String scriptContent) throws IOException {
        Path scriptPath = tempDir.resolve("test_script.sh");
        Files.writeString(scriptPath, scriptContent);
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        Files.setPosixFilePermissions(scriptPath, perms);

        return scriptPath;
    }
}

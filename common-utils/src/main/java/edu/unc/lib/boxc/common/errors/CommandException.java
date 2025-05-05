package edu.unc.lib.boxc.common.errors;

import java.util.List;

/**
 * @author bbpennel
 */
public class CommandException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int exitCode;
    private final String output;
    private final List<String> command;

    public CommandException(String message, List<String> command, String output, Throwable cause) {
        this(message, command, output, -1, cause);
    }

    public CommandException(String message, List<String> command, String output, int exitCode) {
        this(message, command, output, exitCode, null);
    }

    public CommandException(String message, List<String> command, String output, int exitCode, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
        this.output = output;
        this.command = command;
    }

    @Override
    public String getMessage() {
        var message = super.getMessage()
                + System.lineSeparator() + "Command: " + String.join(" ", getCommand());
        if (getExitCode() != -1) {
            message += System.lineSeparator() + " with exit code: " + getExitCode();
        }
        if (getOutput() != null) {
            message += System.lineSeparator() + " with output: " + getOutput();
        }
        return message;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getOutput() {
        return output;
    }

    public List<String> getCommand() {
        return command;
    }
}

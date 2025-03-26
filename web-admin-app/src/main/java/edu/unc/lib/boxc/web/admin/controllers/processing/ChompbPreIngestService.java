package edu.unc.lib.boxc.web.admin.controllers.processing;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Service for interacting with the chompb command-line tool for pre-ingest processing
 * @author lfarrell
 */
public class ChompbPreIngestService {
    private static final Logger log = LoggerFactory.getLogger(ChompbPreIngestService.class);
    private GlobalPermissionEvaluator globalPermissionEvaluator;
    private Path baseProjectsPath;
    private String serviceKeyPath;
    private String serviceUser;
    private String chompbCommand = "chompb";
    private static final Set<String> VALID_FILENAMES = Set.of("data.json", "data.csv");
    private ExecutorService executorService;

    private static final int MAX_TIMEOUT_HOURS = 5;

    /**
     * List all the chompb projects in the base projects path
     *
     * @param agent
     * @return output of the list projects command, which is a json string
     */
    public String getProjectLists(AgentPrincipals agent) {
        assertHasPermission(agent);

        return executeChompbCommand(chompbCommand, "-w", baseProjectsPath.toAbsolutePath().toString(), "list_projects");
    }

    /**
     * Start the chompb process for a specific project
     * @param agent
     * @param projectName
     * @param email
     */
    public void startCropping(AgentPrincipals agent, String projectName, String email) {
        assertHasPermission(agent);
        log.info("Starting cropping for project {} for user {}", projectName, agent.getUsername());

        executeBackgroundCommand(chompbCommand, "process_source_files",
                    "--action", "velocicroptor",
                    "-w", baseProjectsPath.resolve(projectName).toAbsolutePath().toString(),
                    "-k", serviceKeyPath,
                    "--user", serviceUser,
                    "--email", email);
    }

    protected String executeChompbCommand(String... command) {
        StringBuilder output = new StringBuilder();
        String outputString;

        try {
            log.debug("Executing chompb command: {}", String.join(" ", command));
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);

            Process process = builder.start();

            // Use a separate thread to read the output concurrently
            log.debug("Creating output reader task");
            var outputReaderTask = getOutputReaderTask(process.getInputStream(), output);

            // If any errors occurred while reading the output, they will be thrown here
            waitForProcess(process, Arrays.asList(command), outputReaderTask, output);

            outputReaderTask.join();

            int exitCode = process.exitValue();
            log.debug("Process exit code: {}", exitCode);

            if (exitCode != 0) {
                throw new RepositoryException("Command exited with errors: " + command
                        + "\n" + output + "\n" + exitCode);
            }

            outputString = output.toString().trim();
            log.debug("Finished executing chompb command");
        } catch (IOException e) {
            throw new RepositoryException("Failed to execute chompb command", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RepositoryException("Interrupted while waiting for chompb command to complete", e);
        }

        log.debug("Chompb command output: {}", outputString);
        return outputString;
    }

    private static CompletableFuture<Void> getOutputReaderTask(InputStream inputStream, StringBuilder output) {
        return CompletableFuture.runAsync(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
                log.debug("Finished reading output: {}", output);
            } catch (IOException e) {
                log.error("Error reading command output", e);
                throw new RepositoryException("Error reading command output", e);
            }
        });
    }

    private static void waitForProcess(Process process, List<String> command, CompletableFuture<Void> outputFuture,
                                       StringBuilder output)
            throws InterruptedException {
        log.debug("Waiting for process for {} hours: {}", MAX_TIMEOUT_HOURS, command);
        boolean completed = process.waitFor(MAX_TIMEOUT_HOURS, TimeUnit.HOURS);
        if (!completed) {
            log.warn("Command timed out, attempting to end process: {}", command);
            process.destroyForcibly();
            if (outputFuture != null) {
                log.debug("Waiting for output future to complete");
                outputFuture.join();
            }
            throw new RepositoryException("Command timed out after " + MAX_TIMEOUT_HOURS + " hours: "
                    + command + "\n" + output.toString());
        }
        log.debug("Process completed normally");
    }

    protected void executeBackgroundCommand(String... command) {
        final var joinedCommand = String.join(" ", command);
        log.debug("Submitting background chompb command: {}", joinedCommand);
        try {
            executorService.submit(() -> {
                log.debug("Background thread started for command: {}", joinedCommand);
                try {
                    executeChompbCommand(command);
                    log.debug("Background command completed successfully");
                } catch (Exception e) {
                    log.error("Background command failed: {}", command, e);
                    throw e;
                }
            });
        } catch (RejectedExecutionException e) {
            throw new RepositoryException("Failed to schedule background command", e);
        }
    }

    /**
     * Get the processing results for a specific project and job
     *
     * @param agent
     * @param projectName
     * @param jobName
     * @param filename
     * @return Contents of the file as a InputStream
     */
    public InputStream getProcessingResults(AgentPrincipals agent, String projectName, String jobName, String filename) throws IOException {
        assertHasPermission(agent);
        assertValidProcessingResultFilename(filename);

        var projectPath = buildProjectPath(projectName);
        // Build path to results file
        Path resultsPath = projectPath.resolve("processing/results")
                .resolve(jobName)
                .resolve("report")
                .resolve(filename)
                .normalize();
        if (!resultsPath.startsWith(projectPath)) {
            throw new AccessRestrictionException("Cannot access specified file");
        }
        // Read the file and return it as a stream
        return Files.newInputStream(resultsPath);
    }

    private void assertValidProcessingResultFilename(String filename) {
        if (!VALID_FILENAMES.contains(filename) && !(filename.startsWith("images/") && filename.endsWith(".jpg"))) {
            throw new IllegalArgumentException("Invalid filename: " + filename);
        }
    }

    private void assertHasPermission(AgentPrincipals agent) {
        if (!globalPermissionEvaluator.hasGlobalPermission(agent.getPrincipals(), Permission.ingest)) {
            throw new AccessRestrictionException("User " + agent.getUsername() + " does not have permission to use chompb");
        }
    }

    private Path buildProjectPath(String projectName) {
        return baseProjectsPath.resolve(projectName);
    }

    public void setGlobalPermissionEvaluator(GlobalPermissionEvaluator globalPermissionEvaluator) {
        this.globalPermissionEvaluator = globalPermissionEvaluator;
    }

    public void setBaseProjectsPath(Path baseProjectsPath) {
        this.baseProjectsPath = baseProjectsPath;
    }

    public void setServiceKeyPath(String serviceKeyPath) {
        this.serviceKeyPath = serviceKeyPath;
    }

    public void setServiceUser(String serviceUser) {
        this.serviceUser = serviceUser;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setChompbCommand(String chompbCommand) {
        this.chompbCommand = chompbCommand;
    }
}

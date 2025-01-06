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
import java.util.Set;
import java.util.concurrent.ExecutorService;

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
    private static final Set<String> VALID_FILENAMES = Set.of("data.json", "data.csv");
    private ExecutorService executorService;

    /**
     * List all the chompb projects in the base projects path
     *
     * @param agent
     * @return output of the list projects command, which is a json string
     */
    public String getProjectLists(AgentPrincipals agent) {
        assertHasPermission(agent);

        return executeChompbCommand("chompb", "-w", baseProjectsPath.toAbsolutePath().toString(), "list_projects");
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

        executeBackgroundCommand("chompb", "process_source_files",
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
            InputStream is = process.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = br.readLine()) != null) {
                output.append(line).append("\n");
            }

            outputString = output.toString().trim();
            if (process.waitFor() != 0) {
                throw new RepositoryException("Command exited with status code " + process.waitFor() + ": " + outputString);
            }
        } catch (IOException e) {
            throw new RepositoryException("Failed to execute chompb command", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RepositoryException("Interrupted while waiting for chompb command to complete", e);
        }

        log.debug("Chompb command output: {}", outputString);
        return outputString;
    }

    protected void executeBackgroundCommand(String... command) {
        log.debug("Submitting background chompb command: {}", String.join(" ", command));
        executorService.submit(() -> {
            executeChompbCommand(command);
        });
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
}

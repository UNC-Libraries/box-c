package edu.unc.lib.boxc.web.admin.controllers.processing;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author lfarrell
 */
public class ChompbPreIngestService {
    private GlobalPermissionEvaluator globalPermissionEvaluator;
    private Path baseProjectsPath;

    /**
     * List all of the chompb projects in the base projects path
     *
     * @param agent
     * @return output of the list projects command, which is a json string
     */
    public String getProjectLists(AgentPrincipals agent) {
        assertHasPermission(agent);

        return executeChompbCommand("chompb", "-w", baseProjectsPath.toAbsolutePath().toString(), "list_projects");
    }

    protected String executeChompbCommand(String... command) {
        StringBuilder output = new StringBuilder();
        String outputString;

        try {
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
                throw new RuntimeException("Command exited with status code " + process.waitFor() + ": " + outputString);
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute chompb command", e);
        }

        return outputString;
    }

    /**
     * Get the processing results for a specific project and job
     *
     * @param agent
     * @param projectName
     * @param jobName
     * @return String representation of json processing results
     */
    public String getProcessingResults(AgentPrincipals agent, String projectName, String jobName) throws IOException {
        assertHasPermission(agent);

        var projectPath = buildProjectPath(projectName);
        // Build path to results file
        var resultsPath = projectPath.resolve("processing/results").resolve(jobName).resolve("report/data.json");
        // Read the file and return it as a string
        return Files.readString(resultsPath);
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
}

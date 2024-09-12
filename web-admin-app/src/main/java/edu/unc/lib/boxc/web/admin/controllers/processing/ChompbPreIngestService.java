package edu.unc.lib.boxc.web.admin.controllers.processing;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
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
     * @param principals
     * @return
     */
    public String getProjectLists(AccessGroupSet principals) {
        if (!globalPermissionEvaluator.hasGlobalPermission(principals, Permission.ingest)) {
            return null;
        }

        StringBuilder output = new StringBuilder();

        try {
            ProcessBuilder builder = new ProcessBuilder("chompb", "-w", baseProjectsPath.toAbsolutePath().toString(), "list_projects");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            InputStream is = process.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = br.readLine()) != null) {
                output.append(line).append("\n");
            }
            if (process.waitFor() != 0) {
                throw new Exception("Command exited with status code " + process.waitFor() + ": " + output);
            }
        } catch (Exception e) {
            return e.getMessage();
        }

        return output.toString();
    }

    public void setGlobalPermissionEvaluator(GlobalPermissionEvaluator globalPermissionEvaluator) {
        this.globalPermissionEvaluator = globalPermissionEvaluator;
    }

    public void setBaseProjectsPath(Path baseProjectsPath) {
        this.baseProjectsPath = baseProjectsPath;
    }
}

package edu.unc.lib.boxc.web.admin.controllers.processing;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;

/**
 * @author lfarrell
 */
public class ChompbPreIngestService {
    private GlobalPermissionEvaluator globalPermissionEvaluator;

    public String getProjectList(AccessGroupSet principals) {
        if (!globalPermissionEvaluator.hasGlobalPermission(principals, Permission.ingest)) {
            return null;
        }

        return "";
    }

    public void setGlobalPermissionEvaluator(GlobalPermissionEvaluator globalPermissionEvaluator) {
        this.globalPermissionEvaluator = globalPermissionEvaluator;
    }
}

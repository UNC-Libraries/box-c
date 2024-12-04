package edu.unc.lib.boxc.web.common.auth;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import org.apache.commons.lang3.StringUtils;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.ON_CAMPUS_PRINC;

/**
 * Utility class for determining permissions for patron actions
 * @author bbpennel
 */
public class PatronActionPermissionsUtil {
    private PatronActionPermissionsUtil() {
    }

    /**
     * Determine if the provided agent has permission to download a bulk export zip of the provided object
     * @param aclService
     * @param pid
     * @param agent
     * @return
     */
    public static boolean hasBulkDownloadPermission(AccessControlService aclService, PID pid, AgentPrincipals agent) {
        AccessGroupSet principals = agent.getPrincipals();
        if (StringUtils.isBlank(agent.getUsername()) && !principals.contains(ON_CAMPUS_PRINC)) {
            return false;
        }
        return aclService.hasAccess(pid, principals, Permission.viewOriginal);
    }
}

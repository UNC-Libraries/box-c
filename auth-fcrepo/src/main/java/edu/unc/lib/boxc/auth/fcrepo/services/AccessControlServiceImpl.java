package edu.unc.lib.boxc.auth.fcrepo.services;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * Implementation of the service to evaluate and retrieve box-c access controls in
 * a fcrepo repository.
 *
 * @author bbpennel
 *
 */
public class AccessControlServiceImpl implements AccessControlService {

    private static final Logger log = LoggerFactory.getLogger(AccessControlServiceImpl.class);

    private InheritedPermissionEvaluator permissionEvaluator;

    private GlobalPermissionEvaluator globalPermissionEvaluator;

    @Override
    public boolean hasAccess(PID pid, Set<String> principals, Permission permission) {
        // Check if there are any global agents, if so evaluate immediately against requested permission
        if (globalPermissionEvaluator.hasGlobalPermission(principals, permission)) {
            return true;
        }
        boolean result = permissionEvaluator.hasPermission(pid, principals, permission);
        if (log.isDebugEnabled()) {
            log.debug("Evaluated permission {} for user with principals {} against {}, result was {}",
                    permission, principals, pid, result);
        }

        return result;
    }

    @Override
    public void assertHasAccess(PID pid, Set<String> principals, Permission permission)
            throws AccessRestrictionException {
        assertHasAccess(null, pid, principals, permission);
    }

    @Override
    public void assertHasAccess(String message, PID pid, Set<String> principals, Permission permission)
            throws AccessRestrictionException {
        if (!hasAccess(pid, principals, permission)) {
            throw new AccessRestrictionException(message);
        }
    }

    public void setPermissionEvaluator(InheritedPermissionEvaluator permissionEvaluator) {
        this.permissionEvaluator = permissionEvaluator;
    }

    public void setGlobalPermissionEvaluator(GlobalPermissionEvaluator globalPermissionEvaluator) {
        this.globalPermissionEvaluator = globalPermissionEvaluator;
    }

}

package edu.unc.lib.boxc.web.sword.filters;

import static edu.unc.lib.boxc.web.common.auth.RemoteUserUtil.getRemoteUser;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.auth.api.AccessPrincipalConstants;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.web.common.auth.filters.StoreUserAccessControlFilter;
import edu.unc.lib.boxc.web.sword.SwordConfigurationImpl;

/**
 * Extension of basic access control filter which specifically handles sword depositors, adding in the
 * <depositor-namespace>:<user-name> group and the generic groups
 *
 * @author bbpennel
 *
 */
public class DepositorAccessControlFilter extends StoreUserAccessControlFilter {
    private static final Logger log = LoggerFactory.getLogger(DepositorAccessControlFilter.class);
    private static final String DEPOSITOR_ROLE = "sword-depositor";
    private SwordConfigurationImpl swordConfig;

    @Override
    protected AccessGroupSet getUserGroups(HttpServletRequest request) {
        log.debug("Getting groups from depositor filter");
        if (request.isUserInRole(FORWARDING_ROLE)) {
            return this.getForwardedGroups(request);
        } else if (request.isUserInRole(DEPOSITOR_ROLE)) {
            return this.getDepositorGroups(request);
        } else {
            return this.getGrouperGroups(request);
        }
    }

    protected AccessGroupSet getDepositorGroups(HttpServletRequest request) {
        String user = getRemoteUser(request);
        log.debug("SWORD depositor user {} logged in", user);
        AccessGroupSet accessGroups = new AccessGroupSetImpl();
        accessGroups.addAccessGroup(AccessPrincipalConstants.PUBLIC_PRINC);
        if (user != null) {
            accessGroups.addAccessGroup(swordConfig.getDepositorNamespace() + request.getRemoteUser());
            accessGroups.addAccessGroup(AccessPrincipalConstants.AUTHENTICATED_PRINC);
        }
        return accessGroups;
    }

    public void setSwordConfig(SwordConfigurationImpl swordConfig) {
        this.swordConfig = swordConfig;
    }
}

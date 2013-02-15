package edu.unc.lib.dl.cdr.sword.server.filters;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.filter.StoreUserAccessControlFilter;
import edu.unc.lib.dl.acl.util.AccessGroupConstants;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.cdr.sword.server.SwordConfigurationImpl;

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
		log.debug("SWORD depositor user " + request.getRemoteUser() + " logged in");
		AccessGroupSet accessGroups = new AccessGroupSet();
		accessGroups.addAccessGroup(AccessGroupConstants.PUBLIC_GROUP);
		accessGroups.addAccessGroup(AccessGroupConstants.AUTHENTICATED_GROUP);
		if (request.getRemoteUser() != null)
			accessGroups.addAccessGroup(swordConfig.getDepositorNamespace() + request.getRemoteUser());
		return accessGroups;
	}

	public void setSwordConfig(SwordConfigurationImpl swordConfig) {
		this.swordConfig = swordConfig;
	}
}

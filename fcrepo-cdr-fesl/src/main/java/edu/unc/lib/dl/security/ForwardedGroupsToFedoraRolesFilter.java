package edu.unc.lib.dl.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When this filter gets a request from a user with the "forwardingGroups" role, it incorporates groups forwarded via a
 * request header into Fedora's list of roles (a Fedora auxilary request attribute).
 * 
 * This filter requires two init parameters, the forwardedGroupsHeader and the forwardedGroupsSeparator.
 * 
 * @author count0
 * 
 */
public class ForwardedGroupsToFedoraRolesFilter implements Filter {
	private static final Logger LOG = LoggerFactory.getLogger(ForwardedGroupsToFedoraRolesFilter.class);
	private static final String FEDORA_ROLE_ALLOWED_TO_FORWARD_GROUPS = "canForwardGroups";
	private static final String FEDORA_ROLE_KEY = "fedoraRole";
	private static final String FEDORA_ATTRIBUTES_KEY = "FEDORA_AUX_SUBJECT_ATTRIBUTES";

	private String header = null;
	private String separator = null;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.header = filterConfig.getInitParameter("header");
		if (this.header == null || this.header.trim().length() == 0) {
			String msg = "The parameter 'header' must be supplied with a single header name.";
			LOG.error(msg);
		}
		this.separator = filterConfig.getInitParameter("separator");
		if (this.separator == null || this.separator.trim().length() == 0) {
			String msg = "The parameter 'separator' must be supplied.";
			LOG.error(msg);
		} else {
			this.separator = java.util.regex.Pattern.quote(this.separator);
		}
		if (this.header == null || this.separator == null) {
			throw new ServletException("Filter is missing one of the init parameters: header, separator");
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		String groups = req.getHeader(this.header);
		LOG.debug(this.header + ": " + groups);
		LOG.debug("remote user: " + req.getRemoteUser());
		LOG.debug("uri: " + req.getRequestURI());
		if (groups != null) {
			Set<String> forwardedRoles = new HashSet<String>();
			for (String cdrRole : groups.split(this.separator)) {
				forwardedRoles.add(cdrRole);
			}
			populateFedoraAttributes(forwardedRoles, req);
		}
		chain.doFilter(request, response);
	}

	/**
	 * Add roles and other subject attributes where Fedora expects them - a Map called FEDORA_AUX_SUBJECT_ATTRIBUTES.
	 * Roles will be put in "fedoraRole" and others will be named as-is.
	 * 
	 * @param subject
	 *           the subject from authentication.
	 * @param userRoles
	 *           the set of user roles.
	 * @param request
	 *           the request in which to place the attributes.
	 */
	private void populateFedoraAttributes(Set<String> forwardedGroups, HttpServletRequest request) {
		@SuppressWarnings("unchecked")
		Map<String, Set<String>> fedAttributes = (Map<String, Set<String>>) request.getAttribute(FEDORA_ATTRIBUTES_KEY);
		if (fedAttributes == null) {
			fedAttributes = new HashMap<String, Set<String>>();
			request.setAttribute(FEDORA_ATTRIBUTES_KEY, fedAttributes);
		}
		// get the fedora roles attribute or create it.
		Set<String> roles = (Set<String>) fedAttributes.get(FEDORA_ROLE_KEY);
		if (roles == null) {
			if (request.isUserInRole(FEDORA_ROLE_ALLOWED_TO_FORWARD_GROUPS)) {
				roles = new HashSet<String>();
				roles.add(FEDORA_ROLE_ALLOWED_TO_FORWARD_GROUPS);
				fedAttributes.put(FEDORA_ROLE_KEY, roles);
			} else {
				LOG.warn("The header "+header+" was found, but this user is not allowed to forward them: " + request.getRemoteUser());
				return;
			}
		}
		if (roles.contains(FEDORA_ROLE_ALLOWED_TO_FORWARD_GROUPS)) {
			roles.clear();
			roles.addAll(forwardedGroups);
		}
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
	}

}

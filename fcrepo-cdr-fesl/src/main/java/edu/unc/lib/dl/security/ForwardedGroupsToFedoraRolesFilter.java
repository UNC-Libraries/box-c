/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
	private String separatorQuoted = null;

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public void init() throws ServletException {
		this.separatorQuoted = java.util.regex.Pattern.quote(this.separator);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {
		HttpServletRequest req = (HttpServletRequest) request;
		String groups = req.getHeader(this.header);
		LOG.debug(this.header + ": " + groups);
		LOG.debug("remote user: " + req.getRemoteUser());
		LOG.debug("uri: " + req.getRequestURI());
		Set<String> forwardedRoles = new HashSet<String>();

		// TODO what if the groups header is NULL!!!  make sure public is set regardless.
		if (groups != null) {
			for (String cdrRole : groups.split(this.separatorQuoted)) {
				forwardedRoles.add(cdrRole);
			}
		}
		populateFedoraAttributes(forwardedRoles, req);
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
			boolean canWrite = request.isUserInRole("canWrite");
			// purge admin roles from forwarded groups
			forwardedGroups.remove("administrator");
			roles.clear();
			if(canWrite) roles.add("canWrite");
			roles.addAll(forwardedGroups);
		}
		roles.add("public");
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub
		
	}

}

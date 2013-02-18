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
package edu.unc.lib.dl.acl.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.context.ServletContextAware;

import edu.unc.lib.dl.acl.util.AccessGroupConstants;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.httpclient.HttpClientUtil;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Filter which retrieves the users shibboleth and grouper session information in order to construct their profile as
 * needed.
 * 
 * @author bbpennel
 * 
 */
public class StoreUserAccessControlFilter extends OncePerRequestFilter implements ServletContextAware {
	private static final Logger log = LoggerFactory.getLogger(StoreUserAccessControlFilter.class);
	
	protected static String FORWARDING_ROLE = "group-forwarding";

	@Override
	public void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException,
			ServletException {
		log.debug("In StoreUserAccessControlFilter");
		// Skip processing static content
		if (!req.getServletPath().startsWith("/static/")) {
			storeUserGroupData(req);
		}
		chain.doFilter(req, res);
		GroupsThreadStore.clearStore();
	}

	protected void storeUserGroupData(HttpServletRequest request) {
		try {
			String userName = request.getRemoteUser();
			if (userName == null)
				userName = "";
			else
				userName = userName.trim();
			GroupsThreadStore.storeUsername(userName);
			
			AccessGroupSet accessGroups = getUserGroups(request);
			request.setAttribute("accessGroupSet", accessGroups);
			GroupsThreadStore.storeGroups(accessGroups);
			if (log.isDebugEnabled())
				log.debug("Setting cdr groups for request processing thread: " + GroupsThreadStore.getGroupString());
		} catch (Exception e) {
			log.debug("Error while retrieving the users profile", e);
		}
	}
	
	protected AccessGroupSet getUserGroups(HttpServletRequest request) {
		if (request.isUserInRole(FORWARDING_ROLE)) {
			return this.getForwardedGroups(request);
		} else {
			return this.getGrouperGroups(request);
		}
	}
	
	protected AccessGroupSet getForwardedGroups(HttpServletRequest request) {
		String forwardedGroups = (String) request.getHeader(HttpClientUtil.FORWARDED_GROUPS_HEADER);
		if (log.isDebugEnabled())
			log.debug("Forwarding user " + request.getRemoteUser() + " logged in with forwarded groups " + forwardedGroups);
		if (forwardedGroups == null)
			return new AccessGroupSet();
		
		if (forwardedGroups.trim().length() > 0) {
			return new AccessGroupSet(forwardedGroups);
		}
		return new AccessGroupSet();
	}
	
	protected AccessGroupSet getGrouperGroups(HttpServletRequest request) {
		String shibGroups = (String) request.getHeader(HttpClientUtil.SHIBBOLETH_GROUPS_HEADER);
		AccessGroupSet accessGroups = null;
		String userName = request.getRemoteUser();
		if (log.isDebugEnabled())
			log.debug("Normal user " + userName + " logged in with groups " + shibGroups);
		
		if (shibGroups == null || shibGroups.trim().length() == 0)
			accessGroups = new AccessGroupSet();
		else {
			accessGroups = new AccessGroupSet(shibGroups);
		}
		
		accessGroups.addAccessGroup(AccessGroupConstants.PUBLIC_GROUP);
		if (userName != null && userName.length() > 0) {
			accessGroups.addAccessGroup(AccessGroupConstants.AUTHENTICATED_GROUP);
		}
		
		return accessGroups;
	}
}

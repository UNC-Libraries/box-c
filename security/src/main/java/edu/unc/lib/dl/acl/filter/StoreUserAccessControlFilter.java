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
 * Filter which retrieves the users shibboleth and grouper session information in order to construct
 * their profile as needed.
 * @author bbpennel
 *
 */
public class StoreUserAccessControlFilter extends OncePerRequestFilter implements ServletContextAware {
	private static final Logger log = LoggerFactory.getLogger(StoreUserAccessControlFilter.class);
	
	@Override
	public void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) 
			throws IOException, ServletException {
		log.debug("In StoreUserAccessControlFilter");
		//Don't check security for static files
		if (!(req.getServletPath().startsWith("/js/") || req.getServletPath().startsWith("/css/")
				|| req.getServletPath().startsWith("/images/"))){
			storeUserGroupData(req);
		}
		chain.doFilter(req, res);
		GroupsThreadStore.clearStore();
	}
	
	public void storeUserGroupData(HttpServletRequest request) {
		try {
			String userName = request.getRemoteUser();
			if (userName == null)
				userName = "";
			String shibGroups = (String) request.getHeader(HttpClientUtil.SHIBBOLETH_GROUPS_HEADER);
			if (shibGroups == null)
				shibGroups = "";
			
			log.debug("User " + userName + " logged in with groups " + shibGroups);
			
			if (shibGroups.trim().length() > 0) {
				AccessGroupSet accessGroups = new AccessGroupSet(shibGroups);
				accessGroups.addAccessGroup(AccessGroupConstants.PUBLIC_GROUP);
				if (userName != null && userName.trim().length() > 0)
					accessGroups.addAccessGroup(AccessGroupConstants.AUTHENTICATED_GROUP);
				request.setAttribute("accessGroupSet", accessGroups);
				GroupsThreadStore.storeGroups(accessGroups);
				log.debug("Setting cdr groups for request processing thread: "+shibGroups);
			} else {
				GroupsThreadStore.clearGroups();
			}
		} catch (Exception e) {
			log.debug("Error while retrieving the users profile", e);
		}
	}
}

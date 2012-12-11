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
import edu.unc.lib.dl.security.access.UserSecurityProfile;

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
public class UserSecurityProfileFilter extends OncePerRequestFilter implements ServletContextAware {
	private static final Logger LOG = LoggerFactory.getLogger(UserSecurityProfileFilter.class);
	
	@Override
	public void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) 
			throws IOException, ServletException {
		LOG.debug("In UserSecurityProfileFilter");
		//Don't check security for static files
		if (!(req.getServletPath().startsWith("/js/") || req.getServletPath().startsWith("/css/")
				|| req.getServletPath().startsWith("/images/"))){
			UserSecurityProfile user = getUserSecurityProfile(req);
			req.getSession().setAttribute("user", user);
		}
		chain.doFilter(req, res);
	}
	
	public UserSecurityProfile getUserSecurityProfile(HttpServletRequest request) {
		UserSecurityProfile user = (UserSecurityProfile)request.getSession().getAttribute("user");
		
		try {
			String userName = request.getRemoteUser();
			if (userName == null)
				userName = "";
			String members = (String) request.getHeader("isMemberOf");
			if (members == null)
				members = "";
			
			if (user == null || !userName.equals(user.getUserName()) || !members.equals(user.getIsMemeberOf())){
				LOG.debug("Creating user security profile for " + userName + " as " + members);
				user = new UserSecurityProfile();
				if (userName != null) {
					userName = userName.trim();
				}
				user.setUserName(userName);
				
				user.setAccessGroups(members);
				//Add the public group to everyone
				user.getAccessGroups().addAccessGroup(AccessGroupConstants.PUBLIC_GROUP);
			}
		} catch (Exception e) {
			LOG.debug("Error while retrieving the users profile", e);
		}

		return user;
	}
}

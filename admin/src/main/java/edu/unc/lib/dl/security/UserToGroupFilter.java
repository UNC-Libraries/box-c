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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUtils;

import org.apache.log4j.Logger;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.filter.OncePerRequestFilter;

public class UserToGroupFilter extends OncePerRequestFilter implements
		ServletContextAware {

	class FilteredRequest extends HttpServletRequestWrapper {
		String groups;
		
        public FilteredRequest(ServletRequest request, String groups) {
                super((HttpServletRequest)request);
        		this.groups = groups;        		
        }

        public String getParameter(String paramName) {
        		logger.debug("in getParameter: "+paramName+ " groups: "+groups);
        	
                if(("cdrRoles".equals(paramName)) && (groups != null)) {
                        return groups;
                }
                return super.getParameter(paramName);
        }
    }

	
	@Override
	public void doFilterInternal(HttpServletRequest req,
			HttpServletResponse res, FilterChain chain) throws IOException,
			ServletException {
		// before we allow the request to proceed, we'll first get the user's
		// role and see if it's an administrator

		String groups = null;
		List<String> groupList = new ArrayList<String>(1);
		boolean permitted = hasAccess(req, groupList);
		groups = groupList.get(0);
		
		logger.debug("hasAccess groups: "+groups);
				
		if (permitted) {
			chain.doFilter(new FilteredRequest(req, groups), res);
		} else {

			StringBuffer hostUrl = req.getRequestURL();
			
			req.setAttribute("nopermission", req.getRequestURI());
			req.setAttribute("hostUrl", hostUrl.toString());
			req.getRequestDispatcher("/WEB-INF/jsp/nopermission.jsp").forward(
					req, res);
		}
	}

	public boolean hasAccess(HttpServletRequest request, List<String> groupList) {

		try {
			String path = request.getRequestURI();
			if (path != null) {
				path = path.trim();
			} else {
				logger.debug("Path is null; denying access");

				return false;
			}
			String user = request.getRemoteUser();
			if (user != null) {
				user = user.trim();
				logger.debug("remoteUser: "+user);
			} else {
				logger.debug("remoteUser is NULL");
			}

			logger.debug("requestURI: " + path);

			
			String members = (String) request.getHeader("isMemberOf");
			groupList.add(members);
			
			logger.debug("isMemberOf: " + members);

			Map<String, List<String>> usersAndGroups = null;

			if ((members != null) && (!members.equals(""))) {
				String[] groups = members.split(";");
				usersAndGroups = new HashMap<String, List<String>>();
				List<String> roles = new ArrayList<String>();

				for (String group : groups) {
					roles.add(group);
				}
				usersAndGroups.put(user, roles);
			}

			List<Map<String, String>> pathsAndGroups = loadAccessControl(request);

			if ((pathsAndGroups != null) && (pathsAndGroups.size() > 0)) {
				for (Object pathAndGroup : pathsAndGroups) {

					Object[] keys = ((Map) pathAndGroup).keySet().toArray();

					String temp = (String) keys[0];

					logger.debug("processing path: " + temp);

					if (path.startsWith(temp)) { // found a match, so get role
						String role = (String) ((Map) pathAndGroup).get(temp);

						logger.debug("need to match role: " + role);

						if (role.equals("IS_AUTHENTICATED_ANONYMOUSLY")) { // public
							// access

							logger
									.debug("Anonymous authentication; allowing access");
							return true;
						}

						if (user == null) {
							logger
									.debug("Remote user not found; denying access");
							return false;
						}

						List<String> roles = null;

						if (usersAndGroups != null) {
							roles = (ArrayList<String>) usersAndGroups
									.get(user);
						}

						if (roles != null) {
							for (String aRole : roles) {
								if (role.equals(aRole)) {
									logger
											.debug("Had role for path; allowing access");
									return true;
								}
							}
							logger
									.debug("Did not have role for path; denying access");

							return false;
						} else {
							logger
									.debug("User without roles; denying access");
							return false;
						}
					}
				}
			}
			logger.debug("Default action; denying access");

		} catch (Exception e) {
			logger.info(e);
		}

		return false;
	}


	private List<Map<String, String>> loadAccessControl(
			HttpServletRequest request) {
		List<Map<String, String>> results = new ArrayList<Map<String, String>>();

		try {
			ServletContext servletContext = request.getSession()
					.getServletContext();

			if (servletContext == null)
				logger.debug("servletContext is NULL");

			InputStream is = servletContext
					.getResourceAsStream("/WEB-INF/classes/controlledPaths.txt");
			if (is != null) {
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader reader = new BufferedReader(isr);
				String input = "";

				while ((input = reader.readLine()) != null) {
					String path = input.substring(0, input.indexOf('*')).trim();
					logger.debug("path = " + path);

					String role = input.substring(input.indexOf(' ') + 1)
							.trim();
					logger.debug("role = " + role);
					Map<String, String> map = new HashMap<String, String>();

					map.put(path, role);
					results.add(map);
				}
				reader.close();
			} else {
				logger.debug("input stream is NULL");
			}
		} catch (IOException e) {
			logger.error("Could not load access control information", e);
			e.printStackTrace();
			return null;
		} catch (Exception e) {
			logger.error("Access control file input error", e);
			e.printStackTrace();
			return null;
		}

		return results;
	}
}
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
package edu.unc.lib.dl.ui.access;

import java.io.IOException;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.filter.OncePerRequestFilter;

import edu.unc.lib.dl.acl.fcrepo4.GlobalPermissionEvaluator;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;

/**
 * Stores global access level information. This information is only suitable for informing UI decisions and access to
 * CDR application level information, not in place of FRACAS.  Optionally, it will also deny access if the filter does
 * not determine the user to be an admin.
 *
 * @author bbpennel
 *
 */
public class StoreAccessLevelFilter extends OncePerRequestFilter implements ServletContextAware {
    private static final long MAX_CACHE_AGE = 1000 * 60 * 10;

    @Autowired
    private GlobalPermissionEvaluator globalPermissionEvaluator;

    @Autowired
    private SolrQueryLayerService queryLayer;

    private boolean requireViewAdmin = false;
    private String nonAdminRedirectUrl = null;

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String username = GroupsThreadStore.getUsername();
        HttpSession session = request.getSession(true);
        AccessLevel accessLevel = null;

        if (username == null || username.length() == 0) {
            session.removeAttribute("accessLevel");
        } else {
            accessLevel = (AccessLevel) session.getAttribute("accessLevel");
            // Invalidate and refresh cache if user changed or the cache is too old
            if (accessLevel == null || !username.equals(accessLevel.getUsername())
                    || (System.currentTimeMillis() - accessLevel.getCacheAge()) > MAX_CACHE_AGE) {
                session.removeAttribute("accessLevel");
                accessLevel = new AccessLevel(username);
                session.setAttribute("accessLevel", accessLevel);
                AccessGroupSet groups = GroupsThreadStore.getGroups();
                if (globalPermissionEvaluator.hasGlobalPrincipal(groups)) {
                    Set<UserRole> roles = globalPermissionEvaluator.getGlobalUserRoles(groups);
                    if (roles.contains(UserRole.administrator)) {
                        accessLevel.setHighestRole(UserRole.administrator);
                    } else {
                        accessLevel.setHighestRole(UserRole.canAccess);
                    }
                } else {
                    // Check for viewAdmin
                    boolean viewAdmin = queryLayer.hasAdminViewPermission(groups);
                    if (viewAdmin) {
                        // See which exact type of admin we're dealing with
                        // Comment out until we start using these
                        // if (queryLayer.hasRole(groups, UserRole.curator))
                        // accessLevel.setHighestRole(UserRole.curator);
                        // else if (queryLayer.hasRole(groups, UserRole.processor))
                        // accessLevel.setHighestRole(UserRole.processor);
                        // else accessLevel.setHighestRole(UserRole.observer);
                        accessLevel.setHighestRole(UserRole.canAccess);
                    }
                }
            }
        }

        // Enforce admin requirement if it is set.
        if (requireViewAdmin && (accessLevel == null || !accessLevel.isViewAdmin())) {
            response.setStatus(401);
            if (nonAdminRedirectUrl != null) {
                RequestDispatcher dispatcher = request.getRequestDispatcher(nonAdminRedirectUrl);
                dispatcher.forward(request, response);
            }
            return;
        }

        chain.doFilter(request, response);
    }

    public void setQueryLayer(SolrQueryLayerService queryLayer) {
        this.queryLayer = queryLayer;
    }

    public void setRequireViewAdmin(boolean requireViewAdmin) {
        this.requireViewAdmin = requireViewAdmin;
    }

    public void setNonAdminRedirectUrl(String nonAdminRedirectUrl) {
        this.nonAdminRedirectUrl = nonAdminRedirectUrl;
    }
}

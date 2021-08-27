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
package edu.unc.lib.boxc.web.access.controllers;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.web.common.exceptions.ResourceNotFoundException;
import edu.unc.lib.boxc.web.common.view.XSLViewResolver;

/**
 * Allows admin users to trigger reloads of various UI specific mappings.
 * @author bbpennel
 */
@Controller
@RequestMapping("/refreshMappings")
public class RefreshMappingsController {
    private static final Logger LOG = LoggerFactory.getLogger(RefreshMappingsController.class);

    @Autowired(required = true)
    private XSLViewResolver xslViewResolver;

    @Autowired
    private GlobalPermissionEvaluator globalPermissionEvaluator;

    @RequestMapping(method = RequestMethod.GET)
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        if (!globalPermissionEvaluator.hasGlobalPermission(principals, Permission.reindex)) {
            throw new ResourceNotFoundException();
        }

        try {
            xslViewResolver.refreshViews();
        } catch (Exception e) {
            response.getWriter().append("Failed to refresh transform mappings, check logs.");
            LOG.error("Failed to refresh transform mappings", e);
        }

        response.getWriter().append("Mappings refresh was successful.");
    }

    public void setXslViewResolver(XSLViewResolver xslViewResolver) {
        this.xslViewResolver = xslViewResolver;
    }
}

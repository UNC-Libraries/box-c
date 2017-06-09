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
package edu.unc.lib.dl.ui.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;
import edu.unc.lib.dl.ui.util.ExternalContentSettings;

/**
 * Simple controller for transferring users to an allowed subset of external pages, with support for
 * redirecting or embedding.
 * @author bbpennel
 */
@Controller
@RequestMapping("/external")
public class ExternalContentController {
    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(ExternalContentController.class);

    @RequestMapping(method = RequestMethod.POST)
    public String handlePost(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String page = request.getParameter("page");
        //Support for redirecting post requests?
        //Map parameters = request.getParameterMap();
        return forwardToContent(page, model, request, response);
    }

    @RequestMapping(method = RequestMethod.GET)
    public String handleRequest(Model model, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String page = request.getParameter("page");
        return forwardToContent(page, model, request, response);
    }

    private String forwardToContent(String page, Model model, HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String contentUrl = ExternalContentSettings.getUrl(page);
        //If no page was specified or if an invalid page was requested, then issue a 404 error.
        if (page == null || contentUrl == null) {
            throw new ResourceNotFoundException();
        }
        if (contentUrl.indexOf("redirect:") == 0) {
            return contentUrl;
        }
        model.addAttribute("contentUrl", contentUrl);
        return "externalTemplate";
    }
}

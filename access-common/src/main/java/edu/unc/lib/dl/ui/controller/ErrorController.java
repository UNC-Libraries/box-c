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

import java.net.SocketException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for forwarding users to specific error pages.
 * @author bbpennel
 */
@Controller
public class ErrorController {
    private static final Logger LOG = LoggerFactory.getLogger(ErrorController.class);

    @RequestMapping("/403")
    public String handle403(Model model, HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        model.addAttribute("pageSubtitle", "Error");
        return "error/403";
    }

    @RequestMapping("/404")
    public String handle404(Model model, HttpServletResponse response) {
        LOG.debug("Page not found, forwarding to 404 page");
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        model.addAttribute("pageSubtitle", "Page not found");
        return "error/404";
    }

    @RequestMapping("/exception")
    public String handleException(Model model, HttpServletRequest request, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");

        if (throwable.getCause() instanceof SocketException || throwable instanceof IllegalStateException) {
            LOG.debug("Connection aborted for {}",
                    request.getAttribute("javax.servlet.forward.request_uri"), throwable);
        } else {
            LOG.error("An uncaught exception occurred at " + request.getAttribute("javax.servlet.forward.request_uri"),
                    throwable);
        }

        model.addAttribute("pageSubtitle", "Error");
        return "error/exception";
    }
}

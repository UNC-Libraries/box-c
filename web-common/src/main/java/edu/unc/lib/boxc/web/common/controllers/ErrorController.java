package edu.unc.lib.boxc.web.common.controllers;

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

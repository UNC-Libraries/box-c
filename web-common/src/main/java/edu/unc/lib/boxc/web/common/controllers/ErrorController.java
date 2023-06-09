package edu.unc.lib.boxc.web.common.controllers;

import java.net.SocketException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Object> handle403() {
        return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    @RequestMapping("/404")
    public ResponseEntity<Object> handle404() {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @RequestMapping("/exception")
    public ResponseEntity<Object> handleException(HttpServletRequest request) {
        Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");

        if (throwable.getCause() instanceof SocketException || throwable instanceof IllegalStateException) {
            LOG.debug("Connection aborted for {}",
                    request.getAttribute("javax.servlet.forward.request_uri"), throwable);
        } else {
            LOG.error("An uncaught exception occurred at " + request.getAttribute("javax.servlet.forward.request_uri"),
                    throwable);
        }
        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

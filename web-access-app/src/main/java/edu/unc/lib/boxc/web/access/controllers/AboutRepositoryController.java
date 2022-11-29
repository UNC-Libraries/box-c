package edu.unc.lib.boxc.web.access.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Handles requests to the about the repository page
 * @author lfarrell
 */
@Controller
@RequestMapping("/aboutRepository")
public class AboutRepositoryController {
    @RequestMapping(method = RequestMethod.GET)
    public String handleRequest() {
        return "aboutRepository";
    }
}
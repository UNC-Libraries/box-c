package edu.unc.lib.boxc.web.admin.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 
 * @author bbpennel
 *
 */
@Controller
public class AccessErrorController {
    @RequestMapping(value = "login", method = RequestMethod.GET)
    public String toLoginPage() {
        return "error/login";
    }

    @RequestMapping(value = "noAccess", method = RequestMethod.GET)
    public String nonAdminPage() {
        return "error/nonAdmin";
    }
}

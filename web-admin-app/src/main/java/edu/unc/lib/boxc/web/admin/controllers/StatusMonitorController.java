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
public class StatusMonitorController {
    @RequestMapping(value = "statusMonitor", method = RequestMethod.GET)
    public String statusMonitor() {
        return "report/statusMonitor";
    }
}
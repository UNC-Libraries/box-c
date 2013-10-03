package edu.unc.lib.dl.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class StatusMonitorController {
	@RequestMapping(value = "statusMonitor", method = RequestMethod.GET)
	public String statusMonitor() {
		return "report/statusMonitor";
	}
}
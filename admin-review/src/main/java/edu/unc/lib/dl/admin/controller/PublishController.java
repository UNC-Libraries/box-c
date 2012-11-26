package edu.unc.lib.dl.admin.controller;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.security.access.UserSecurityProfile;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.util.ContentModelHelper;

@Controller
public class PublishController {
	private static final Logger log = LoggerFactory.getLogger(PublishController.class);
	private ManagementClient managementClient;
	private OperationsMessageSender messageSender;
	
	@RequestMapping(value = "{prefix}/{id}/publish", method = RequestMethod.GET)
	public String publishObject(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id, Model model,
			HttpServletRequest request) {

		PID pid = new PID(idPrefix + ":" + id);
		log.debug("Publishing object " + pid);
		
		try {
			// Update relation
			managementClient.addLiteralStatement(pid, ContentModelHelper.CDRProperty.isPublished.toString(), "yes", null);
			// Send message to trigger solr update
			messageSender.sendPublishOperation(((UserSecurityProfile)request.getSession().getAttribute("user")).getUserName(), Arrays.asList(pid), true);
		} catch (FedoraException e) {
			log.error("Failed to update relation on " + pid, e);
		}
		
		
		return "";
	}
}

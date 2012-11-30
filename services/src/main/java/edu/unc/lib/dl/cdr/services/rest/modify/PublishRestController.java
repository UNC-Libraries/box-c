package edu.unc.lib.dl.cdr.services.rest.modify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.util.ContentModelHelper;

@Controller
public class PublishRestController {
	private static final Logger log = LoggerFactory.getLogger(PublishRestController.class);
	
	@Autowired(required=true)
	private ManagementClient managementClient;
	@Autowired(required=true)
	private OperationsMessageSender messageSender;

	@RequestMapping(value = "edit/{prefix}/{id}/publish", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> publishObject(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id, Model model,
			HttpServletRequest request) {
		PID pid = new PID(idPrefix + ":" + id);
		return this.publishObject(pid, true, request.getRemoteUser());
	}
	
	@RequestMapping(value = "edit/{prefix}/{id}/unpublish", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> unpublishObject(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id, Model model,
			HttpServletRequest request) {
		PID pid = new PID(idPrefix + ":" + id);
		return this.publishObject(pid, false, request.getRemoteUser());
	}
	
	private Map<String, ? extends Object> publishObject(PID pid, boolean publish, String username) {
		Map<String, Object> result = new HashMap<String,Object>();
		result.put("pid", pid);
		result.put("action", (publish)? "publish": "unpublish");
		log.debug("Publishing object " + pid);
		
		try {
			// Update relation
			managementClient.setExclusiveLiteral(pid, ContentModelHelper.CDRProperty.isPublished.toString(), (publish)? "yes": "no", null);
			result.put("timestamp", System.currentTimeMillis());
			// Send message to trigger solr update
			String messageId = messageSender.sendPublishOperation(username, Arrays.asList(pid), true);
			result.put("messageId", messageId);
		} catch (FedoraException e) {
			log.error("Failed to update relation on " + pid, e);
			result.put("error", e.toString());
		}
		
		return result;
	}
}

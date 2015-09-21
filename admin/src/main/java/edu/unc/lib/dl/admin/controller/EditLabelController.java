package edu.unc.lib.dl.admin.controller;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.jdom2.Element;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.util.PremisEventLogger;

@Controller
public class EditLabelController {
	@Resource(name="managementClient")
	private ManagementClient client;
	
	@RequestMapping(value = "editlabel/{pid}", method = RequestMethod.POST)
	public @ResponseBody
	Object saveLabel(@PathVariable("pid") String pid,
			@RequestParam("label") String label) throws IngestException {
		
		if (label != null && label.trim().length() > 0) {
			try {
				PremisEventLogger logger = new PremisEventLogger(pid);
				PID pidObject = new PID(pid);

				this.client.modifyObject(pidObject, label, null, null, null);
				
				Element event = logger.logEvent(PremisEventLogger.Type.MIGRATION, "Object renamed to " + label, pidObject);
				PremisEventLogger.addDetailedOutcome(event, "success", "Object renamed successfully", null);
				this.client.writePremisEventsToFedoraObject(logger, pidObject);
			} catch (FedoraException e) {
				throw new IngestException("Could not update label for " + pid, e);
			}
		} else {
			Map <String, String> response = new HashMap<>();
			response.put("message", "error");
			return response ;
		}
		Map <String, String> response = new HashMap<>();
		response.put("message", "success");
		return response;
	}
}

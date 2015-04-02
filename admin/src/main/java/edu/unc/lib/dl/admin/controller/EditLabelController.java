package edu.unc.lib.dl.admin.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ManagementClient.State;
import edu.unc.lib.dl.ingest.IngestException;


@Controller
public class EditLabelController {
	
	@Autowired
	private ManagementClient client;
	
	@RequestMapping(value = "editlabel/{pid}", method = RequestMethod.POST)
	public @ResponseBody
	Object saveLabel(@PathVariable("pid") String pid,
			@RequestParam("label") String label) throws IngestException {
		
		if (label != null && label.trim().length() > 0) {
			try {
				this.client.modifyObject(new PID(pid), label, null, null, null);
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

package edu.unc.lib.dl.cdr.services.rest.modify;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.PID;

/**
 * API controller which permanently deletes/destroys objects in the repository.
 * 
 * @author bbpennel
 *
 */
@Controller
public class DestroyObjectController {
	private static final Logger log = LoggerFactory.getLogger(DestroyObjectController.class);

	@Autowired(required = true)
	@Qualifier("forwardedManagementClient")
	private ManagementClient managementClient;
	
	@RequestMapping(value = "edit/destroy/{id}", method = RequestMethod.POST)
	public @ResponseBody
	Map<String, ? extends Object> moveToTrash(@PathVariable("id") String id) {
		PID pid = new PID(id);
		
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("pid", pid);
		result.put("action", "destroy");
		
		try {
			log.debug("Destroying object {}", id);
			managementClient.purgeObject(pid, "Object destroyed by CDR API", false);
			result.put("timestamp", System.currentTimeMillis());
		} catch (AuthorizationException e) {
			result.put("error", "Insufficient privileges to perform operation on object " + id);
		} catch (FedoraException e) {
			log.error("Failed to destroy object {}", pid, e);
			result.put("error", e.toString());
		}
		
		return result;
	}
}

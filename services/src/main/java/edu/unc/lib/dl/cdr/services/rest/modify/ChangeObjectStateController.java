package edu.unc.lib.dl.cdr.services.rest.modify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.ManagementClient.State;
import edu.unc.lib.dl.fedora.PID;

/**
 * API controller for changing an object's state, specifically to mark it as either deleted or active
 * 
 * @author bbpennel
 *
 */
@Controller
public class ChangeObjectStateController {
	private static final Logger log = LoggerFactory.getLogger(ChangeObjectStateController.class);
	
	@Autowired(required = true)
	@Qualifier("forwardedManagementClient")
	private ManagementClient managementClient;
	
	@RequestMapping(value = "edit/restore/{id}", method = RequestMethod.POST)
	public @ResponseBody
	Map<String, ? extends Object> removeFromTrash(@PathVariable("id") String id) {
		return this.changeObjectState(id, false);
	}
	
	@RequestMapping(value = "edit/delete/{id}", method = RequestMethod.POST)
	public @ResponseBody
	Map<String, ? extends Object> moveToTrash(@PathVariable("id") String id) {
		return this.changeObjectState(id, true);
	}
	
	private Map<String, ? extends Object> changeObjectState(String id, boolean markAsDeleted) {
		PID pid = new PID(id);
		
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("pid", id);
		result.put("action", (markAsDeleted) ? "delete" : "restore");
		

		try {
			State newState = markAsDeleted? State.DELETED : State.ACTIVE;
			log.debug("Changing the state of object {} to {}", id, newState);
			managementClient.modifyObject(pid, null, null, newState, null);
			result.put("timestamp", System.currentTimeMillis());
		} catch (AuthorizationException e) {
			result.put("error", "Insufficient privileges to perform operation on object " + id);
		} catch (FedoraException e) {
			log.error("Failed to perform modifyObject on {}", pid, e);
			result.put("error", e.toString());
		}

		return result;
	}
	
	@RequestMapping(value = "edit/restore", method = RequestMethod.POST)
	public @ResponseBody
	List<? extends Object> removeBatchFromTrash(@RequestParam("ids") String ids) {
		return this.changeBatchObjectState(ids, false);
	}
	
	@RequestMapping(value = "edit/delete", method = RequestMethod.POST)
	public @ResponseBody
	List<? extends Object> moveBatchToTrash(@RequestParam("ids") String ids) {
		return this.changeBatchObjectState(ids, true);
	}
	
	public List<? extends Object> changeBatchObjectState(String ids, boolean moveToTrash) {
		if (ids == null)
			return null;
		List<Object> results = new ArrayList<Object>();
		for (String id : ids.split("\n")) {
			results.add(this.changeObjectState(id, moveToTrash));
		}
		return results;
	}
}

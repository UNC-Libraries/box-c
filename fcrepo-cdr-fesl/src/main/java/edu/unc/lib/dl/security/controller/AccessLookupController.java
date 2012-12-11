package edu.unc.lib.dl.security.controller;

import java.util.HashMap;
import java.util.Map;

import org.fcrepo.server.errors.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fedora.FedoraAccessControlService;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.security.GroupRolesFactory;

@Controller
public class AccessLookupController {
	private static final Logger log = LoggerFactory.getLogger(AccessLookupController.class);

	private GroupRolesFactory groupRolesFactory;

	/**
	 * Returns a JSON representation of all the roles and groups for the provided pid
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "fesl/{id}/getAccess", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getAccess(@PathVariable("id") String id) {
		try {
			Map<String, Object> result = new HashMap<String, Object>();
			result.put(FedoraAccessControlService.ROLES_TO_GROUPS, groupRolesFactory.getAllRolesAndGroups(new PID(id)));
			return result;
		} catch (ObjectNotFoundException e) {
			log.debug("Requested object " + id + " was not found");
			return null;
		}
	}

	/**
	 * Returns true or false depending on if the provided groups have the specified permission on the selected pid. The
	 * groups can either be forwarded via headers or as a GET parameter.
	 * 
	 * @param id
	 * @param permissionName
	 * @param groups
	 * @return
	 */
	@RequestMapping(value = "fesl/{id}/hasAccess/{permissionName}", method = RequestMethod.GET)
	public @ResponseBody
	boolean hasAccess(@PathVariable("id") String id, @PathVariable("permissionName") String permissionName,
			@RequestParam("groups") String groups) {

		AccessGroupSet accessGroups;
		if (groups != null) {
			accessGroups = new AccessGroupSet(groups);
		} else {
			accessGroups = GroupsThreadStore.getGroups();
		}
		return this.hasAccess(id, permissionName, accessGroups);
	}

	private boolean hasAccess(String id, String permissionName, AccessGroupSet accessGroups) {
		PID pid = new PID(id);
		try {
			Permission permission = Permission.getPermission(permissionName);
			if (permission == null)
				return false;
			return ObjectAccessControlsBean.createObjectAccessControlBean(pid,
					groupRolesFactory.getAllRolesAndGroups(pid)).hasPermission(accessGroups, permission);
		} catch (ObjectNotFoundException e) {
			log.debug("Requested object " + id + " was not found");
			return false;
		}
	}

	public void setGroupRolesFactory(GroupRolesFactory groupRolesFactory) {
		this.groupRolesFactory = groupRolesFactory;
	}
}

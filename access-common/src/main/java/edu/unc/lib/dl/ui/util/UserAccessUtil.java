package edu.unc.lib.dl.ui.util;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fedora.InvalidDatastreamException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

/**
 * Utility for determining if a particular user has access to an object or datastream.
 * 
 * @author bbpennel
 */
public class UserAccessUtil {
	private Permission defaultPermission = Permission.viewDescription;
	private AccessControlService accessControlService;
	// Cache of answers as to whether or not a user has access to a particular object or object's datastream
	// <PID, <group, Answer>>
	private WeakHashMap<String, Map<String, Boolean>> pids2User2Access = new WeakHashMap<String, Map<String, Boolean>>(
			256);

	private AtomicLong lastCleared = new AtomicLong(0);
	// Time interval between cache clears
	private long clearInterval = 1000 * 60 * 4;

	public boolean hasAccess(String id, String user, AccessGroupSet groups) {
		clearCacheOnInterval();
		
		// Check for cached version
		Map<String, Boolean> user2Access = this.pids2User2Access.get(id);
		if (user2Access != null) {
			Boolean answer = user2Access.get(user);
			if (answer != null)
				return answer;
		}

		// Determine what permission we are looking for
		PID pid = new PID(id);
		String[] idParts = pid.getPid().split("/");
		Permission permission = null;
		if (idParts.length > 1) {
			id = idParts[0];
			Datastream datastream = Datastream.getDatastream(idParts[1]);
			if (datastream == null) {
				throw new InvalidDatastreamException(idParts[1] + " is not a valid datastream identifer");
			}
			permission = Permission.getPermissionByDatastreamCategory(datastream.getCategory());
		} else {
			permission = defaultPermission;
		}

		// Get access info from fedora
		ObjectAccessControlsBean aclBean = accessControlService.getObjectAccessControls(pid);
		boolean answer = aclBean.hasPermission(groups, permission);

		if (user2Access == null) {
			user2Access = new HashMap<String, Boolean>();
			this.pids2User2Access.put(pid.getPid(), user2Access);
		}

		user2Access.put(user, answer);
		return answer;
	}
	
	private void clearCacheOnInterval() {
		synchronized(lastCleared) {
			long currentTime = System.currentTimeMillis();
			if (currentTime - lastCleared.get() > clearInterval) {
				if (this.pids2User2Access.size() > 0)
					this.pids2User2Access.clear();
				lastCleared.set(currentTime);
			}
		}
	}

	public void updateAccess(String pid, String user, AccessGroupSet groups) {
		this.pids2User2Access.remove(pid);
		this.hasAccess(pid, user, groups);
	}

	public void clear() {
		synchronized(lastCleared) {
			this.pids2User2Access.clear();
			lastCleared.set(System.currentTimeMillis());
		}
	}

	public void setDefaultPermission(Permission defaultPermission) {
		this.defaultPermission = defaultPermission;
	}

	public void setAccessControlService(AccessControlService accessControlService) {
		this.accessControlService = accessControlService;
	}
}

package edu.unc.lib.dl.acl.service;

import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.fedora.PID;

/**
 * Interface for services retrieving access control information for objects.
 * 
 * @author count0
 * 
 */
public interface AccessControlService {
	/**
	 * Returns an ObjectAccesscontrolBean containing the access control for the object specified by the provided pid.
	 * 
	 * @param pid
	 * @return
	 */
	public ObjectAccessControlsBean getObjectAccessControls(PID pid);
}

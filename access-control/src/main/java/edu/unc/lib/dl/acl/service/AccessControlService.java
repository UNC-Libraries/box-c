package edu.unc.lib.dl.acl.service;

import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.fedora.PID;

public interface AccessControlService {
	public ObjectAccessControlsBean getObjectAccessControls(PID pid);
}

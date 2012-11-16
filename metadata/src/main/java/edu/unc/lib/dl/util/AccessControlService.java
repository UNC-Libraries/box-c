package edu.unc.lib.dl.util;

import edu.unc.lib.dl.fedora.PID;

public interface AccessControlService {
	public ObjectAccessControlsBean getObjectAccessControls(PID pid);
}

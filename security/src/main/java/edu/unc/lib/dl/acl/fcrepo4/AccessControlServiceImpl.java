/**
 * Copyright 2016 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.acl.fcrepo4;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fedora.PID;

/**
 * 
 * @author bbpennel
 *
 */
public class AccessControlServiceImpl implements AccessControlService {

	public AccessControlServiceImpl() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public ObjectAccessControlsBean getObjectAccessControls(PID pid) {
		// TODO stub
		return new ObjectAccessControlsBeanImpl();
	}

	@Override
	public boolean hasAccess(PID pid, AccessGroupSet groups, Permission permission) {
		// TODO Auto-generated method stub
		return true;
	}

}

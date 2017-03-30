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

import java.util.Date;
import java.util.List;
import java.util.Set;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;

/**
 * 
 * @author bbpennel
 *
 */
public class ObjectAccessControlsBeanImpl implements ObjectAccessControlsBean {

	private final static String query = "SELECT  ?pid WHERE { " + 
			"  ?pid  <http://pcdm.org/models#hasFile>|<http://pcdm.org/models#hasMember>* <http://localhost:8080/fcrepo/rest/content/c2/8c/73/7a/c28c737a-0120-4f3a-bdff-25ed6747bd5c> .\n" + 
			"  \n" + 
			"}";
	
	public ObjectAccessControlsBeanImpl() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Date getLastActiveEmbargoUntilDate() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<UserRole> getRoles(AccessGroupSet groups) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasPermission(AccessGroupSet groups, Permission permission) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public Set<String> getPermissionsByGroups(AccessGroupSet groups) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getGroupsByPermission(Permission permission) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getGroupsByUserRole(UserRole userRole) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> roleGroupsToUnprefixedList() {
		// TODO Auto-generated method stub
		return null;
	}

}

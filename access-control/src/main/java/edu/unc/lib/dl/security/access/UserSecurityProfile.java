/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.dl.security.access;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashMap;

/**
 * Object stores a single users access control information, including what groups they are
 * a member of and what objects they have already authenticated to in this session.
 * @author bbpennel
 */
public class UserSecurityProfile implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private AccessGroupSet accessGroups;
	private String isMemberOf;
	private String userName;
	private UserDatastreamAccessCache datastreamAccessCache;
	
	public UserSecurityProfile(){
		datastreamAccessCache = new UserDatastreamAccessCache();
	}

	public AccessGroupSet getAccessGroups() {
		return accessGroups;
	}

	public void setAccessGroups(AccessGroupSet accessGroups) {
		this.accessGroups = accessGroups;
	}
	
	public void setAccessGroups(String isMemberOf){
		this.isMemberOf = isMemberOf;
		if (isMemberOf == null){
			accessGroups = new AccessGroupSet();
			return;
		}
		String[] groups = isMemberOf.split(";");
		accessGroups = new AccessGroupSet(groups);
	}
	
	public String getIsMemeberOf(){
		return isMemberOf;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}
	
	public UserDatastreamAccessCache getDatastreamAccessCache() {
		return datastreamAccessCache;
	}

	public void setDatastreamAccessCache(UserDatastreamAccessCache datastreamAccessCache) {
		this.datastreamAccessCache = datastreamAccessCache;
	}

	public static class UserDatastreamAccessCache extends HashMap<String,EnumSet<AccessType>> {
		private static final long serialVersionUID = 1L;

		public void put(String id, AccessType accessType){
			if (this.containsKey(id)){
				EnumSet<AccessType> accessTypes = this.get(id);
				accessTypes.add(accessType);
			} else {
				EnumSet<AccessType> accessTypes = EnumSet.noneOf(AccessType.class);
				accessTypes.add(accessType);
				this.put(id, accessTypes);
			}
		}
		
		public boolean contains(String id, AccessType accessType){
			EnumSet<AccessType> accessTypes = this.get(id);
			if (accessTypes != null){
				return accessTypes.contains(accessType);
			}
			return false;
		}
	}
}

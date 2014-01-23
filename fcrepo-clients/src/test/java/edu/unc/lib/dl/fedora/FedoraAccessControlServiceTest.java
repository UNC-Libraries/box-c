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
package edu.unc.lib.dl.fedora;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fedora.PID;

public class FedoraAccessControlServiceTest extends Assert {

	@Test
	public void getAccessControlTest() {
		
		Map<String, List<String>> roleMappings = new HashMap<String,List<String>>();
		roleMappings.put(UserRole.patron.getURI().toString(), Arrays.asList("group1", "group2"));
		roleMappings.put(UserRole.curator.getURI().toString(), Arrays.asList("cur1"));
		roleMappings.put(UserRole.metadataPatron.getURI().toString(), Arrays.asList("group3"));
		
		PID pid = new PID("uuid:test");
		
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(pid, roleMappings, null, null, null, null);
		
		AccessGroupSet groups = new AccessGroupSet("group1;group3");
		Set<UserRole> userRoles = aclBean.getRoles(groups);
		
		assertEquals(2, userRoles.size());
		assertTrue(userRoles.contains(UserRole.patron));
		assertTrue(userRoles.contains(UserRole.metadataPatron));
		assertFalse(userRoles.contains(UserRole.curator));
	}
	
	@Test
	public void emptyRoleResultTest() {
		Map<String, List<String>> roleMappings = new HashMap<String,List<String>>();
		
		PID pid = new PID("uuid:test");
		
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(pid, roleMappings, null, null, null, null);
		
		AccessGroupSet groups = new AccessGroupSet("group1;group3");
		Set<UserRole> userRoles = aclBean.getRoles(groups);
		
		assertEquals(0, userRoles.size());
	}
}

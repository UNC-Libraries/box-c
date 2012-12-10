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
		
		ObjectAccessControlsBean aclBean = ObjectAccessControlsBean.createObjectAccessControlBean(pid, roleMappings);
		
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
		
		ObjectAccessControlsBean aclBean = ObjectAccessControlsBean.createObjectAccessControlBean(pid, roleMappings);
		
		AccessGroupSet groups = new AccessGroupSet("group1;group3");
		Set<UserRole> userRoles = aclBean.getRoles(groups);
		
		assertEquals(0, userRoles.size());
	}
}

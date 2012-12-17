package edu.unc.lib.dl.acl.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.fedora.PID;

/**
 * 
 * @author bbpennel
 *
 */
public class ObjectAccessControlsBeanTest extends Assert {

	@Test
	public void constructFromBlankList() {
		List<String> roleGroupList = Arrays.asList("");
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roleGroupList);

		assertEquals(0, aclBean.getActiveRoleGroups().size());
	}

	@Test
	public void constructFromList() {
		List<String> roleGroupList = Arrays.asList("http://cdr.unc.edu/definitions/roles#patron|unc:app:lib:cdr:patron",
				"http://cdr.unc.edu/definitions/roles#administrator|unc:app:lib:cdr:admin");
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roleGroupList);

		assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:patron"), Permission.viewDescription));
	}

	private Map<String, List<String>> getRoleGroups() {
		Map<String, List<String>> roles = new HashMap<String, List<String>>();
		roles.put(UserRole.patron.getURI().toString(), Arrays.asList("unc:app:lib:cdr:patron"));
		roles.put(UserRole.metadataPatron.getURI().toString(), Arrays.asList("public", "authenticated"));
		roles.put(UserRole.curator.getURI().toString(), Arrays.asList("unc:app:lib:cdr:curator"));
		
		return roles;
	}
	
	@Test
	public void activeEmbargoTest() {
		Map<String, List<String>> roles = getRoleGroups();
		
		List<String> embargoes = Arrays.asList("3000-01-01");
		
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, embargoes);
		
		assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.curator));
		assertFalse(aclBean.getActiveRoleGroups().containsKey(UserRole.patron));
		assertFalse(aclBean.getActiveRoleGroups().containsKey(UserRole.metadataPatron));
		
		assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:curator"), Permission.viewDescription));
		assertFalse(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:patron"), Permission.viewDescription));
	}
	
	@Test
	public void inactiveEmbargoTest() {
		Map<String, List<String>> roles = getRoleGroups();
		
		List<String> embargoes = Arrays.asList("1970-01-01");
		
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, embargoes);
		
		assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.curator));
		assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.patron));
		assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.metadataPatron));
		
		assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:curator"), Permission.viewDescription));
		assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:patron"), Permission.viewDescription));
	}
	
	@Test
	public void multipleInactiveEmbargoTest() {
		Map<String, List<String>> roles = getRoleGroups();
		
		List<String> embargoes = Arrays.asList("1970-01-01", "1984-01-01");
		
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, embargoes);
		
		assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.curator));
		assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.patron));
		
		assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:curator"), Permission.viewDescription));
		assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:patron"), Permission.viewDescription));
	}
	
	@Test
	public void multipleMixedEmbargoTest() {
		Map<String, List<String>> roles = getRoleGroups();
		
		List<String> embargoes = Arrays.asList("1970-01-01", "3000-01-01");
		
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, embargoes);
		
		assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.curator));
		assertFalse(aclBean.getActiveRoleGroups().containsKey(UserRole.patron));
		
		assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:curator"), Permission.viewDescription));
		assertFalse(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:patron"), Permission.viewDescription));
	}
	
	@Test
	public void nullEmbargoesTest() {
		Map<String, List<String>> roles = getRoleGroups();
		new ObjectAccessControlsBean(new PID("uuid:test"), roles, null);
	}
	
	@Test
	public void getRolesTest() {
		Map<String, List<String>> roles = getRoleGroups();
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, null);
		Set<UserRole> filteredRoles = aclBean.getRoles(new AccessGroupSet("unc:app:lib:cdr:curator"));
		assertTrue(filteredRoles.contains(UserRole.curator));
		assertFalse(filteredRoles.contains(UserRole.patron));
		
		assertEquals(1, filteredRoles.size());
	}
	
	@Test
	public void getMultipleRolesTest() {
		Map<String, List<String>> roles = getRoleGroups();
		roles.put(UserRole.observer.getURI().toString(), Arrays.asList("unc:app:lib:cdr:patron"));
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, null);
		
		Set<UserRole> filteredRoles = aclBean.getRoles(new AccessGroupSet("unc:app:lib:cdr:patron"));
		assertFalse(filteredRoles.contains(UserRole.curator));
		assertTrue(filteredRoles.contains(UserRole.patron));
		assertTrue(filteredRoles.contains(UserRole.observer));
	}
	
	@Test
	public void getRolesNoMatchesTest() {
		Map<String, List<String>> roles = getRoleGroups();
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, null);
		
		Set<UserRole> filteredRoles = aclBean.getRoles(new AccessGroupSet("unc:app:lib:cdr:observer"));
		assertFalse(filteredRoles.contains(UserRole.curator));
		assertFalse(filteredRoles.contains(UserRole.patron));
		assertFalse(filteredRoles.contains(UserRole.observer));
	}
	
	@Test
	public void roleGroupsToListTest() {
		Map<String, List<String>> roles = getRoleGroups();
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, null);
		
		List<String> roleGroups = aclBean.roleGroupsToList();
		assertTrue(roleGroups.contains(UserRole.curator.getURI().toString() + "|unc:app:lib:cdr:curator"));
		assertTrue(roleGroups.contains(UserRole.patron.getURI().toString() + "|unc:app:lib:cdr:patron"));
		assertTrue(roleGroups.contains(UserRole.metadataPatron.getURI().toString() + "|public"));
		assertTrue(roleGroups.contains(UserRole.metadataPatron.getURI().toString() + "|authenticated"));
		
		
		assertEquals(4, roleGroups.size());
	}
	
	@Test
	public void constructFromRoleGroupList() {
		Map<String, List<String>> roles = getRoleGroups();
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, null);
		
		List<String> roleGroups = aclBean.roleGroupsToList();
		aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roleGroups);
		
		assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.curator));
		assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.patron));
		assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.metadataPatron));
	}
}

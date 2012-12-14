package edu.unc.lib.dl.acl.util;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.fedora.PID;

public class ObjectAccessControlsBeanTest extends Assert {

	@Test
	public void constructFromBlankList() {
		List<String> roleGroupList = Arrays.asList("");
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roleGroupList);
		
		assertEquals(0, aclBean.roleGroupsToList());
	}
	
	@Test
	public void constructFromList() {
		List<String> roleGroupList = Arrays.asList("http://cdr.unc.edu/definitions/roles#patron|unc:app:lib:cdr:patron", "http://cdr.unc.edu/definitions/roles#administrator|unc:app:lib:cdr:admin");
		ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roleGroupList);
		
		assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:patron"), Permission.viewDescription));
	}
}

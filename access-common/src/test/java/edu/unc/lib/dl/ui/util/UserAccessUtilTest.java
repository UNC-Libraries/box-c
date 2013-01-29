package edu.unc.lib.dl.ui.util;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fedora.InvalidDatastreamException;
import edu.unc.lib.dl.fedora.PID;
import static org.mockito.Mockito.*;

public class UserAccessUtilTest extends Assert {

	@Test
	public void hasAccessTest() {
		ObjectAccessControlsBean aclBean = mock(ObjectAccessControlsBean.class);
		when(aclBean.hasPermission(any(AccessGroupSet.class), any(Permission.class))).thenReturn(true);
		
		AccessControlService acs = mock(AccessControlService.class);
		when(acs.getObjectAccessControls(any(PID.class))).thenReturn(aclBean);
		
		UserAccessUtil userAccessUtil = new UserAccessUtil();
		userAccessUtil.setAccessControlService(acs);
		
		assertTrue(userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class)));
		
		verify(aclBean).hasPermission(any(AccessGroupSet.class), any(Permission.class));
	}
	
	@Test
	public void hasAccessFailTest() {
		ObjectAccessControlsBean aclBean = mock(ObjectAccessControlsBean.class);
		when(aclBean.hasPermission(any(AccessGroupSet.class), any(Permission.class))).thenReturn(false);
		
		AccessControlService acs = mock(AccessControlService.class);
		when(acs.getObjectAccessControls(any(PID.class))).thenReturn(aclBean);
		
		UserAccessUtil userAccessUtil = new UserAccessUtil();
		userAccessUtil.setAccessControlService(acs);
		
		assertFalse(userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class)));
		
		verify(aclBean).hasPermission(any(AccessGroupSet.class), any(Permission.class));
	}
	
	@Test
	public void cachedHasAccessTest() {
		ObjectAccessControlsBean aclBean = mock(ObjectAccessControlsBean.class);
		when(aclBean.hasPermission(any(AccessGroupSet.class), any(Permission.class))).thenReturn(true);
		
		AccessControlService acs = mock(AccessControlService.class);
		when(acs.getObjectAccessControls(any(PID.class))).thenReturn(aclBean);
		
		UserAccessUtil userAccessUtil = new UserAccessUtil();
		userAccessUtil.setAccessControlService(acs);
		
		userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class));
		
		assertTrue(userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class)));
		assertTrue(userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class)));
		assertTrue(userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class)));
		
		verify(aclBean, times(1)).hasPermission(any(AccessGroupSet.class), any(Permission.class));
	}
	
	@Test
	public void cachedHasAccessFailTest() {
		ObjectAccessControlsBean aclBean = mock(ObjectAccessControlsBean.class);
		when(aclBean.hasPermission(any(AccessGroupSet.class), any(Permission.class))).thenReturn(false);
		
		AccessControlService acs = mock(AccessControlService.class);
		when(acs.getObjectAccessControls(any(PID.class))).thenReturn(aclBean);
		
		UserAccessUtil userAccessUtil = new UserAccessUtil();
		userAccessUtil.setAccessControlService(acs);
		
		userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class));
		
		assertFalse(userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class)));
		assertFalse(userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class)));
		
		verify(aclBean, times(1)).hasPermission(any(AccessGroupSet.class), any(Permission.class));
	}
	
	@Test
	public void hasAccessMultipleTest() {
		ObjectAccessControlsBean aclBean = mock(ObjectAccessControlsBean.class);
		when(aclBean.hasPermission(any(AccessGroupSet.class), any(Permission.class))).thenReturn(true);
		
		AccessControlService acs = mock(AccessControlService.class);
		when(acs.getObjectAccessControls(any(PID.class))).thenReturn(aclBean);
		
		UserAccessUtil userAccessUtil = new UserAccessUtil();
		userAccessUtil.setAccessControlService(acs);
		
		assertTrue(userAccessUtil.hasAccess("uuid:test1", "user1", mock(AccessGroupSet.class)));
		assertTrue(userAccessUtil.hasAccess("uuid:test1", "user2", mock(AccessGroupSet.class)));
		
		when(aclBean.hasPermission(any(AccessGroupSet.class), any(Permission.class))).thenReturn(false);
		assertFalse(userAccessUtil.hasAccess("uuid:test1", "user3", mock(AccessGroupSet.class)));
		
		assertTrue(userAccessUtil.hasAccess("uuid:test1", "user1", mock(AccessGroupSet.class)));
		verify(aclBean, times(3)).hasPermission(any(AccessGroupSet.class), any(Permission.class));
		
		when(aclBean.hasPermission(any(AccessGroupSet.class), any(Permission.class))).thenReturn(true);
		assertTrue(userAccessUtil.hasAccess("uuid:test2", "user1", mock(AccessGroupSet.class)));
		assertTrue(userAccessUtil.hasAccess("uuid:test2", "user2", mock(AccessGroupSet.class)));
		assertTrue(userAccessUtil.hasAccess("uuid:test2", "user1", mock(AccessGroupSet.class)));
		
		verify(aclBean, times(5)).hasPermission(any(AccessGroupSet.class), any(Permission.class));
	}
	
	
	@Test
	public void hasAccessDatastreamTest() {
		ObjectAccessControlsBean aclBean = mock(ObjectAccessControlsBean.class);
		when(aclBean.hasPermission(any(AccessGroupSet.class), eq(Permission.viewDescription))).thenReturn(true);
		when(aclBean.hasPermission(any(AccessGroupSet.class), eq(Permission.viewOriginal))).thenReturn(false);
		
		AccessControlService acs = mock(AccessControlService.class);
		when(acs.getObjectAccessControls(any(PID.class))).thenReturn(aclBean);
		
		UserAccessUtil userAccessUtil = new UserAccessUtil();
		userAccessUtil.setAccessControlService(acs);
		
		assertTrue(userAccessUtil.hasAccess("uuid:test1", "user1", mock(AccessGroupSet.class)));
		assertFalse(userAccessUtil.hasAccess("uuid:test1/DATA_FILE", "user1", mock(AccessGroupSet.class)));
		assertTrue(userAccessUtil.hasAccess("uuid:test1", "user1", mock(AccessGroupSet.class)));
		assertFalse(userAccessUtil.hasAccess("uuid:test1/DATA_FILE", "user1", mock(AccessGroupSet.class)));
		
		verify(aclBean, times(2)).hasPermission(any(AccessGroupSet.class), any(Permission.class));
	}
	
	@Test(expected=InvalidDatastreamException.class)
	public void invalidDatastreamTest() {
		ObjectAccessControlsBean aclBean = mock(ObjectAccessControlsBean.class);
		when(aclBean.hasPermission(any(AccessGroupSet.class), eq(Permission.viewDescription))).thenReturn(true);
		when(aclBean.hasPermission(any(AccessGroupSet.class), eq(Permission.viewOriginal))).thenReturn(false);
		
		AccessControlService acs = mock(AccessControlService.class);
		when(acs.getObjectAccessControls(any(PID.class))).thenReturn(aclBean);
		
		UserAccessUtil userAccessUtil = new UserAccessUtil();
		userAccessUtil.setAccessControlService(acs);
		
		userAccessUtil.hasAccess("uuid:test1/INVALID_DATASTREAM", "user1", mock(AccessGroupSet.class));
	}
}

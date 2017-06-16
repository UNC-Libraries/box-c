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
package edu.unc.lib.dl.ui.util;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fedora.InvalidDatastreamException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.ui.service.SolrQueryLayerService;
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
        
        SolrQueryLayerService queryLayer = mock(SolrQueryLayerService.class);
        when(queryLayer.isAccessible(any(SimpleIdRequest.class))).thenReturn(true);
        userAccessUtil.setSolrQueryLayer(queryLayer);
        
        assertTrue(userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class)));
        
        //verify(aclBean).hasPermission(any(AccessGroupSet.class), any(Permission.class));
        verify(queryLayer).isAccessible(any(SimpleIdRequest.class));
    }
    
    @Test
    public void hasAccessFailTest() {
        ObjectAccessControlsBean aclBean = mock(ObjectAccessControlsBean.class);
        when(aclBean.hasPermission(any(AccessGroupSet.class), any(Permission.class))).thenReturn(false);
        
        AccessControlService acs = mock(AccessControlService.class);
        when(acs.getObjectAccessControls(any(PID.class))).thenReturn(aclBean);
        
        UserAccessUtil userAccessUtil = new UserAccessUtil();
        userAccessUtil.setAccessControlService(acs);
        
        SolrQueryLayerService queryLayer = mock(SolrQueryLayerService.class);
        when(queryLayer.isAccessible(any(SimpleIdRequest.class))).thenReturn(false);
        userAccessUtil.setSolrQueryLayer(queryLayer);
        
        assertFalse(userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class)));
        
        //verify(aclBean).hasPermission(any(AccessGroupSet.class), any(Permission.class));
        verify(queryLayer).isAccessible(any(SimpleIdRequest.class));
    }
    
    @Test
    public void cachedHasAccessTest() {
        ObjectAccessControlsBean aclBean = mock(ObjectAccessControlsBean.class);
        when(aclBean.hasPermission(any(AccessGroupSet.class), any(Permission.class))).thenReturn(true);
        
        AccessControlService acs = mock(AccessControlService.class);
        when(acs.getObjectAccessControls(any(PID.class))).thenReturn(aclBean);
        
        UserAccessUtil userAccessUtil = new UserAccessUtil();
        userAccessUtil.setAccessControlService(acs);
        
        SolrQueryLayerService queryLayer = mock(SolrQueryLayerService.class);
        when(queryLayer.isAccessible(any(SimpleIdRequest.class))).thenReturn(true);
        userAccessUtil.setSolrQueryLayer(queryLayer);
        
        userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class));
        
        assertTrue(userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class)));
        assertTrue(userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class)));
        assertTrue(userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class)));
        
        verify(queryLayer, times(1)).isAccessible(any(SimpleIdRequest.class));
        //verify(aclBean, times(1)).hasPermission(any(AccessGroupSet.class), any(Permission.class));
    }
    
    @Test
    public void cachedHasAccessFailTest() {
        ObjectAccessControlsBean aclBean = mock(ObjectAccessControlsBean.class);
        when(aclBean.hasPermission(any(AccessGroupSet.class), any(Permission.class))).thenReturn(false);
        
        AccessControlService acs = mock(AccessControlService.class);
        when(acs.getObjectAccessControls(any(PID.class))).thenReturn(aclBean);
        
        UserAccessUtil userAccessUtil = new UserAccessUtil();
        userAccessUtil.setAccessControlService(acs);
        
        SolrQueryLayerService queryLayer = mock(SolrQueryLayerService.class);
        when(queryLayer.isAccessible(any(SimpleIdRequest.class))).thenReturn(false);
        userAccessUtil.setSolrQueryLayer(queryLayer);
        
        userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class));
        
        assertFalse(userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class)));
        assertFalse(userAccessUtil.hasAccess("uuid:test", "user", mock(AccessGroupSet.class)));
        
        verify(queryLayer, times(1)).isAccessible(any(SimpleIdRequest.class));
        //verify(aclBean, times(1)).hasPermission(any(AccessGroupSet.class), any(Permission.class));
    }
    
    @Test
    public void hasAccessMultipleTest() {
        ObjectAccessControlsBean aclBean = mock(ObjectAccessControlsBean.class);
        when(aclBean.hasPermission(any(AccessGroupSet.class), any(Permission.class))).thenReturn(true);
        
        AccessControlService acs = mock(AccessControlService.class);
        when(acs.getObjectAccessControls(any(PID.class))).thenReturn(aclBean);
        
        UserAccessUtil userAccessUtil = new UserAccessUtil();
        userAccessUtil.setAccessControlService(acs);
        
        SolrQueryLayerService queryLayer = mock(SolrQueryLayerService.class);
        when(queryLayer.isAccessible(any(SimpleIdRequest.class))).thenReturn(true);
        userAccessUtil.setSolrQueryLayer(queryLayer);
        
        assertTrue(userAccessUtil.hasAccess("uuid:test1", "user1", mock(AccessGroupSet.class)));
        assertTrue(userAccessUtil.hasAccess("uuid:test1", "user2", mock(AccessGroupSet.class)));
        
        when(queryLayer.isAccessible(any(SimpleIdRequest.class))).thenReturn(false);
        when(aclBean.hasPermission(any(AccessGroupSet.class), any(Permission.class))).thenReturn(false);
        assertFalse(userAccessUtil.hasAccess("uuid:test1", "user3", mock(AccessGroupSet.class)));
        
        assertTrue(userAccessUtil.hasAccess("uuid:test1", "user1", mock(AccessGroupSet.class)));
        verify(queryLayer, times(3)).isAccessible(any(SimpleIdRequest.class));
        //verify(aclBean, times(3)).hasPermission(any(AccessGroupSet.class), any(Permission.class));
        
        when(queryLayer.isAccessible(any(SimpleIdRequest.class))).thenReturn(true);
        when(aclBean.hasPermission(any(AccessGroupSet.class), any(Permission.class))).thenReturn(true);
        assertTrue(userAccessUtil.hasAccess("uuid:test2", "user1", mock(AccessGroupSet.class)));
        assertTrue(userAccessUtil.hasAccess("uuid:test2", "user2", mock(AccessGroupSet.class)));
        assertTrue(userAccessUtil.hasAccess("uuid:test2", "user1", mock(AccessGroupSet.class)));
        
        verify(queryLayer, times(5)).isAccessible(any(SimpleIdRequest.class));
        //verify(aclBean, times(5)).hasPermission(any(AccessGroupSet.class), any(Permission.class));
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
        
        SolrQueryLayerService queryLayer = mock(SolrQueryLayerService.class);
        when(queryLayer.isAccessible(any(SimpleIdRequest.class))).thenReturn(true);
        userAccessUtil.setSolrQueryLayer(queryLayer);
        
        assertTrue(userAccessUtil.hasAccess("uuid:test1", "user1", mock(AccessGroupSet.class)));
        assertFalse(userAccessUtil.hasAccess("uuid:test1/DATA_FILE", "user1", mock(AccessGroupSet.class)));
        assertTrue(userAccessUtil.hasAccess("uuid:test1", "user1", mock(AccessGroupSet.class)));
        assertFalse(userAccessUtil.hasAccess("uuid:test1/DATA_FILE", "user1", mock(AccessGroupSet.class)));
        
        verify(queryLayer, times(2)).isAccessible(any(SimpleIdRequest.class));
        //verify(aclBean, times(2)).hasPermission(any(AccessGroupSet.class), any(Permission.class));
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
        
        SolrQueryLayerService queryLayer = mock(SolrQueryLayerService.class);
        when(queryLayer.isAccessible(any(SimpleIdRequest.class))).thenReturn(true);
        userAccessUtil.setSolrQueryLayer(queryLayer);
        
        userAccessUtil.hasAccess("uuid:test1/INVALID_DATASTREAM", "user1", mock(AccessGroupSet.class));
    }
}

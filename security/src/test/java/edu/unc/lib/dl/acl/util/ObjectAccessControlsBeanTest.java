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
package edu.unc.lib.dl.acl.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;

/**
 *
 * @author bbpennel
 *
 */
public class ObjectAccessControlsBeanTest extends Assert {
//
//    @Test
//    public void constructFromBlankList() {
//        List<String> roleGroupList = Arrays.asList("");
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roleGroupList);
//
//        assertEquals(0, aclBean.getActiveRoleGroups().size());
//    }
//
//    @Test
//    public void constructFromList() {
//        List<String> roleGroupList = Arrays.asList("http://cdr.unc.edu/definitions/roles#patron|unc:app:lib:cdr:patron",
//                "http://cdr.unc.edu/definitions/roles#administrator|unc:app:lib:cdr:admin");
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roleGroupList);
//
//        assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:patron"), Permission.viewDescription));
//    }
//
//    private Map<String, List<String>> getRoleGroups() {
//        Map<String, List<String>> roles = new HashMap<String, List<String>>();
//        roles.put(UserRole.patron.getURI().toString(), Arrays.asList("unc:app:lib:cdr:patron"));
//        roles.put(UserRole.metadataPatron.getURI().toString(), Arrays.asList("public", "authenticated"));
//        roles.put(UserRole.curator.getURI().toString(), Arrays.asList("unc:app:lib:cdr:curator"));
//
//        return roles;
//    }
//
//    @Test
//    public void activeEmbargoTest() {
//        Map<String, List<String>> roles = getRoleGroups();
//
//        List<String> embargoes = Arrays.asList("3000-01-01");
//
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, null, embargoes,
//                null, null);
//
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.curator));
//        assertFalse(aclBean.getActiveRoleGroups().containsKey(UserRole.patron));
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.metadataPatron));
//
//        // All groups previously assigned to patron roles should have been grouped into the list permission
//        Set<String> listRoles = aclBean.getActiveRoleGroups().get(UserRole.metadataPatron);
//        assertTrue(listRoles.contains("unc:app:lib:cdr:patron"));
//        assertTrue(listRoles.contains("public"));
//        assertTrue(listRoles.contains("authenticated"));
//
//        assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:curator"), Permission.viewDescription));
//        assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:patron"), Permission.viewDescription));
//    }
//
//    @Test
//    public void inactiveEmbargoTest() {
//        Map<String, List<String>> roles = getRoleGroups();
//
//        List<String> embargoes = Arrays.asList("1970-01-01");
//
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, null, embargoes,
//                null, null);
//
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.curator));
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.patron));
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.metadataPatron));
//
//        assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:curator"), Permission.viewDescription));
//        assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:patron"), Permission.viewDescription));
//    }
//
//    @Test
//    public void multipleInactiveEmbargoTest() {
//        Map<String, List<String>> roles = getRoleGroups();
//
//        List<String> embargoes = Arrays.asList("1970-01-01", "1984-01-01");
//
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, null, embargoes,
//                null, null);
//
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.curator));
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.patron));
//
//        assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:curator"), Permission.viewDescription));
//        assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:patron"), Permission.viewDescription));
//    }
//
//    @Test
//    public void multipleMixedEmbargoTest() {
//        Map<String, List<String>> roles = getRoleGroups();
//
//        List<String> embargoes = Arrays.asList("1970-01-01", "3000-01-01");
//
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, null, embargoes,
//                null, null);
//
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.curator));
//        assertFalse(aclBean.getActiveRoleGroups().containsKey(UserRole.patron));
//
//        assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:curator"), Permission.viewDescription));
//        assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:patron"), Permission.viewDescription));
//    }
//
//    @Test
//    public void nullEmbargoesTest() {
//        Map<String, List<String>> roles = getRoleGroups();
//        new ObjectAccessControlsBean(new PID("uuid:test"), roles, null, null, null, null);
//    }
//
//    @Test
//    public void getRolesTest() {
//        Map<String, List<String>> roles = getRoleGroups();
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, null, null, null,
//                null);
//        Set<UserRole> filteredRoles = aclBean.getRoles(new AccessGroupSet("unc:app:lib:cdr:curator"));
//        assertTrue(filteredRoles.contains(UserRole.curator));
//        assertFalse(filteredRoles.contains(UserRole.patron));
//
//        assertEquals(1, filteredRoles.size());
//    }
//
//    @Test
//    public void getMultipleRolesTest() {
//        Map<String, List<String>> roles = getRoleGroups();
//        roles.put(UserRole.observer.getURI().toString(), Arrays.asList("unc:app:lib:cdr:patron"));
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, null, null, null,
//                null);
//
//        Set<UserRole> filteredRoles = aclBean.getRoles(new AccessGroupSet("unc:app:lib:cdr:patron"));
//        assertFalse(filteredRoles.contains(UserRole.curator));
//        assertTrue(filteredRoles.contains(UserRole.patron));
//        assertTrue(filteredRoles.contains(UserRole.observer));
//    }
//
//    @Test
//    public void getRolesNoMatchesTest() {
//        Map<String, List<String>> roles = getRoleGroups();
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, null, null, null,
//                null);
//
//        Set<UserRole> filteredRoles = aclBean.getRoles(new AccessGroupSet("unc:app:lib:cdr:observer"));
//        assertFalse(filteredRoles.contains(UserRole.curator));
//        assertFalse(filteredRoles.contains(UserRole.patron));
//        assertFalse(filteredRoles.contains(UserRole.observer));
//    }
//
//    @Test
//    public void roleGroupsToListTest() {
//        Map<String, List<String>> roles = getRoleGroups();
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, null, null, null,
//                null);
//
//        List<String> roleGroups = aclBean.roleGroupsToList();
//        assertTrue(roleGroups.contains(UserRole.curator.getURI().toString() + "|unc:app:lib:cdr:curator"));
//        assertTrue(roleGroups.contains(UserRole.patron.getURI().toString() + "|unc:app:lib:cdr:patron"));
//        assertTrue(roleGroups.contains(UserRole.metadataPatron.getURI().toString() + "|public"));
//        assertTrue(roleGroups.contains(UserRole.metadataPatron.getURI().toString() + "|authenticated"));
//
//        assertEquals(4, roleGroups.size());
//    }
//
//    @Test
//    public void constructFromRoleGroupList() {
//        Map<String, List<String>> roles = getRoleGroups();
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, null, null, null,
//                null);
//
//        List<String> roleGroups = aclBean.roleGroupsToList();
//        aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roleGroups);
//
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.curator));
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.patron));
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.metadataPatron));
//    }
//
//    @Test
//    public void invalidRole() {
//        Map<String, List<String>> roles = getRoleGroups();
//        roles.put("http://cdr.unc.edu/definitions/acl#inheritPermissions", Arrays.asList("true"));
//
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, null, null, null,
//                null);
//
//        List<String> roleGroups = aclBean.roleGroupsToList();
//        aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roleGroups);
//
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.curator));
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.patron));
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.metadataPatron));
//        assertFalse(aclBean.getActiveRoleGroups().containsKey(null));
//    }
//
//    @Test
//    public void noBlankAdmin() {
//        List<String> roleGroupList = Arrays.asList("http://cdr.unc.edu/definitions/roles#patron|unc:app:lib:cdr:patron");
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roleGroupList);
//
//        assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:patron"), Permission.viewDescription));
//
//        Set<String> groupsByPermission = aclBean.getGroupsByPermission(Permission.viewDescription);
//        assertTrue(groupsByPermission.contains("unc:app:lib:cdr:patron"));
//    }
//
//    @Test
//    public void fromParentNoInherit() {
//        List<String> roleGroupList = Arrays.asList("http://cdr.unc.edu/definitions/roles#patron|unc:app:lib:cdr:patron");
//        ObjectAccessControlsBean parentAclBean = new ObjectAccessControlsBean(new PID("uuid:parent"), roleGroupList);
//
//        Map<String, List<String>> triples = new HashMap<String, List<String>>();
//        triples.put(ContentModelHelper.CDRProperty.inheritPermissions.toString(), Arrays.asList("false"));
//        triples.put(UserRole.patron.toString(), Arrays.asList("testgroup"));
//        triples.put(ContentModelHelper.FedoraProperty.state.toString(),
//                Arrays.asList(ContentModelHelper.FedoraProperty.Active.toString()));
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(parentAclBean, new PID("uuid:test"), triples);
//
//        assertFalse(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:patron"), Permission.viewDescription));
//        assertTrue(aclBean.hasPermission(new AccessGroupSet("testgroup"), Permission.viewDescription));
//    }
//
//    @Test
//    public void fromParentInherit() {
//        List<String> roleGroupList = Arrays.asList("http://cdr.unc.edu/definitions/roles#patron|unc:app:lib:cdr:patron",
//                UserRole.list.toString() + "|public");
//        ObjectAccessControlsBean parentAclBean = new ObjectAccessControlsBean(new PID("uuid:parent"), roleGroupList);
//
//        Map<String, List<String>> triples = new HashMap<String, List<String>>();
//        triples.put(ContentModelHelper.CDRProperty.inheritPermissions.toString(), Arrays.asList("true"));
//        triples.put(ContentModelHelper.FedoraProperty.state.toString(),
//                Arrays.asList(ContentModelHelper.FedoraProperty.Active.toString()));
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(parentAclBean, new PID("uuid:test"), triples);
//
//        assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:patron"), Permission.viewDescription));
//
//        Set<String> groupsByPermission = aclBean.getGroupsByPermission(Permission.viewDescription);
//        assertTrue(groupsByPermission.contains("unc:app:lib:cdr:patron"));
//        Set<String> listGroups = aclBean.getGroupsByUserRole(UserRole.list);
//        assertNull(listGroups);
//    }
//
//    @Test
//    public void fromParentMerge() {
//        List<String> roleGroupList = Arrays.asList(UserRole.patron.toString() + "|unc:app:lib:cdr:patron",
//                UserRole.list.toString() + "|public");
//        ObjectAccessControlsBean parentAclBean = new ObjectAccessControlsBean(new PID("uuid:parent"), roleGroupList);
//        Map<String, List<String>> triples = new HashMap<String, List<String>>();
//        triples.put(ContentModelHelper.CDRProperty.inheritPermissions.toString(), Arrays.asList("true"));
//        triples.put(UserRole.curator.toString(), Arrays.asList("testgroup"));
//        triples.put(UserRole.patron.toString(), Arrays.asList("testpatron"));
//        triples.put(UserRole.list.toString(), Arrays.asList("listgroup"));
//        triples.put(ContentModelHelper.FedoraProperty.state.toString(),
//                Arrays.asList(ContentModelHelper.FedoraProperty.Active.toString()));
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(parentAclBean, new PID("uuid:test"), triples);
//
//        assertTrue(aclBean.hasPermission(new AccessGroupSet("unc:app:lib:cdr:patron"), Permission.viewDescription));
//
//        Set<String> groupsByPermission = aclBean.getGroupsByPermission(Permission.viewDescription);
//        assertTrue(groupsByPermission.contains("unc:app:lib:cdr:patron"));
//        assertTrue(groupsByPermission.contains("testgroup"));
//        assertTrue(groupsByPermission.contains("testpatron"));
//
//        Set<String> listGroups = aclBean.getGroupsByUserRole(UserRole.list);
//        assertFalse(listGroups.contains("public"));
//        assertTrue(listGroups.contains("listgroup"));
//
//        groupsByPermission = aclBean.getGroupsByPermission(Permission.viewAdminUI);
//        assertTrue(groupsByPermission.contains("testgroup"));
//        assertEquals(1, groupsByPermission.size());
//    }
//
//    @Test
//    public void twoGrantsForSameGroup() {
//        Map<String, List<String>> roles = new HashMap<String, List<String>>();
//        roles.put(UserRole.patron.getURI().toString(), Arrays.asList("patron"));
//        roles.put(UserRole.curator.getURI().toString(), Arrays.asList("admingroup"));
//        roles.put(UserRole.observer.getURI().toString(), Arrays.asList("admingroup"));
//
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, null, null, null,
//                null);
//
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.curator));
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.observer));
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.patron));
//        assertFalse(aclBean.getActiveRoleGroups().containsKey(UserRole.metadataPatron));
//
//        assertTrue(aclBean.hasPermission(new AccessGroupSet("admingroup"), Permission.moveToTrash));
//
//    }
//
//    @Test
//    public void globalAndLocalGrantSameRole() {
//        Map<String, List<String>> roles = new HashMap<String, List<String>>();
//        roles.put(UserRole.patron.getURI().toString(), Arrays.asList("patron"));
//        roles.put(UserRole.curator.getURI().toString(), Arrays.asList("admingroup"));
//
//        Map<String, List<String>> globalRoles = new HashMap<String, List<String>>();
//        globalRoles.put(UserRole.curator.getURI().toString(), Arrays.asList("globalcure"));
//
//        ObjectAccessControlsBean aclBean = new ObjectAccessControlsBean(new PID("uuid:test"), roles, globalRoles, null,
//                null, null);
//
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.curator));
//        assertTrue(aclBean.getActiveRoleGroups().containsKey(UserRole.patron));
//
//        assertTrue(aclBean.hasPermission(new AccessGroupSet("globalcure"), Permission.moveToTrash));
//        assertTrue(aclBean.hasPermission(new AccessGroupSet("admingroup"), Permission.moveToTrash));
//    }
}

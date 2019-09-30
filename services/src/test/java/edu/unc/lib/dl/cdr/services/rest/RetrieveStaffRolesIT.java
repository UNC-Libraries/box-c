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
package edu.unc.lib.dl.cdr.services.rest;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.USER_NAMESPACE;
import static edu.unc.lib.dl.acl.util.UserRole.canAccess;
import static edu.unc.lib.dl.acl.util.UserRole.canIngest;
import static edu.unc.lib.dl.acl.util.UserRole.canManage;
import static edu.unc.lib.dl.acl.util.UserRole.unitOwner;
import static edu.unc.lib.dl.cdr.services.rest.AccessControlRetrievalController.ASSIGNED_ROLES;
import static edu.unc.lib.dl.cdr.services.rest.AccessControlRetrievalController.INHERITED_ROLES;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.activemq.util.ByteArrayInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.RoleAssignment;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.cdr.services.rest.modify.AbstractAPIIT;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.ContentRootObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.test.AclModelBuilder;
import edu.unc.lib.dl.test.RepositoryObjectTreeIndexer;

/**
 *
 * @author bbpennel
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/access-control-retrieval-it-servlet.xml")
})
public class RetrieveStaffRolesIT extends AbstractAPIIT {
    private static final String USER_PRINC = "user";
    private static final String USER_NS_PRINC = USER_NAMESPACE + USER_PRINC;
    private static final String GRP_PRINC = "group";
    private static final String SUPER_GROUP_PRINC = "adminGroup";

    private static final String origBodyString = "Original data";
    private static final String origFilename = "original.txt";
    private static final String origMimetype = "text/plain";

    @Autowired
    private String baseAddress;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;
    @Autowired
    private RepositoryPIDMinter pidMinter;
    @Autowired
    private RepositoryObjectTreeIndexer treeIndexer;

    private ContentRootObject rootObj;

    @Before
    public void init_() throws Exception {
        AccessGroupSet testPrincipals = new AccessGroupSet(GRP_PRINC);
        GroupsThreadStore.storeUsername(USER_PRINC);
        GroupsThreadStore.storeGroups(testPrincipals);

        PID rootPid = pidMinter.mintContentPid();
        repositoryObjectFactory.createContentRootObject(rootPid.getRepositoryUri(), null);
        rootObj = repositoryObjectLoader.getContentRootObject(rootPid);
    }

    @After
    public void teardown() throws Exception {
        GroupsThreadStore.clearStore();
    }

    @Test
    public void testInsufficientPermissions() throws Exception {
        PID unitPid = pidMinter.mintContentPid();
        // Creating admin unit with no permissions granted
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(unitPid, null);
        rootObj.addMember(unit);
        treeIndexer.indexAll(baseAddress);

        mvc.perform(get("/acl/staff/" + unitPid.getId()))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void testAsGlobalAdminNoAssigned() throws Exception {
        AccessGroupSet testPrincipals = new AccessGroupSet(SUPER_GROUP_PRINC);
        GroupsThreadStore.storeGroups(testPrincipals);

        PID pid = pidMinter.mintContentPid();
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(pid, null);
        rootObj.addMember(unit);
        treeIndexer.indexAll(baseAddress);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, List<RoleAssignment>> respMap = getRolesFromResponse(result);

        assertNoInheritedRoles(respMap);
        assertNoAssignedRoles(respMap);
    }

    @Test
    public void testObjectNotFound() throws Exception {
        PID pid = pidMinter.mintContentPid();
        treeIndexer.indexAll(baseAddress);

        mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void testAdminUnitWithManager() throws Exception {
        AdminUnit unit = setupAdminUnitWithGroup();
        PID pid = unit.getPid();
        treeIndexer.indexAll(baseAddress);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, List<RoleAssignment>> respMap = getRolesFromResponse(result);

        assertNoInheritedRoles(respMap);
        assertHasAssignedRole(GRP_PRINC, canManage, respMap);
    }

    @Test
    public void testAdminUnitWithMultipleRoles() throws Exception {
        PID unitPid = pidMinter.mintContentPid();
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(unitPid,
                new AclModelBuilder("Admin Unit Can Manage")
                .addCanManage(GRP_PRINC)
                .addUnitOwner(USER_NS_PRINC)
                .model);
        rootObj.addMember(unit);
        treeIndexer.indexAll(baseAddress);

        MvcResult result = mvc.perform(get("/acl/staff/" + unitPid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, List<RoleAssignment>> respMap = getRolesFromResponse(result);

        assertNoInheritedRoles(respMap);
        assertHasAssignedRole(GRP_PRINC, canManage, respMap);
        assertHasAssignedRole(USER_PRINC, unitOwner, respMap);
    }

    @Test
    public void testCollectionNoAssigned() throws Exception {
        AdminUnit unit = setupAdminUnitWithGroup();
        PID pid = pidMinter.mintContentPid();
        CollectionObject coll = repositoryObjectFactory.createCollectionObject(pid, null);
        unit.addMember(coll);

        treeIndexer.indexAll(baseAddress);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, List<RoleAssignment>> respMap = getRolesFromResponse(result);

        assertNoAssignedRoles(respMap);
        assertHasInheritedRole(GRP_PRINC, canManage, respMap);
    }

    @Test
    public void testCollectionWithAssignedStaffRole() throws Exception {
        AdminUnit unit = setupAdminUnitWithGroup();
        PID pid = pidMinter.mintContentPid();
        CollectionObject coll = repositoryObjectFactory.createCollectionObject(pid,
                new AclModelBuilder("Collection with access")
                .addCanAccess(USER_NS_PRINC)
                .model);
        unit.addMember(coll);

        treeIndexer.indexAll(baseAddress);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, List<RoleAssignment>> respMap = getRolesFromResponse(result);

        assertHasAssignedRole(USER_PRINC, canAccess, respMap);
        assertHasInheritedRole(GRP_PRINC, canManage, respMap);
    }

    @Test
    public void testCollectionWithPatronRole() throws Exception {
        AdminUnit unit = setupAdminUnitWithGroup();
        PID pid = pidMinter.mintContentPid();
        CollectionObject coll = repositoryObjectFactory.createCollectionObject(pid,
                new AclModelBuilder("Collection with patron")
                .addCanViewOriginals(USER_NS_PRINC)
                .model);
        unit.addMember(coll);

        treeIndexer.indexAll(baseAddress);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, List<RoleAssignment>> respMap = getRolesFromResponse(result);

        assertNoAssignedRoles(respMap);
        assertHasInheritedRole(GRP_PRINC, canManage, respMap);
    }

    @Test
    public void testFolderWithInheritedStaffRoles() throws Exception {
        AdminUnit unit = setupAdminUnitWithGroup();
        CollectionObject coll = repositoryObjectFactory.createCollectionObject(
                new AclModelBuilder("Collection with access")
                .addCanAccess(USER_NS_PRINC)
                .model);
        unit.addMember(coll);
        PID pid = pidMinter.mintContentPid();
        FolderObject folder = repositoryObjectFactory.createFolderObject(pid, null);
        coll.addMember(folder);
        treeIndexer.indexAll(baseAddress);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, List<RoleAssignment>> respMap = getRolesFromResponse(result);

        assertNoAssignedRoles(respMap);
        assertHasInheritedRole(GRP_PRINC, canManage, respMap);
        assertHasInheritedRole(USER_PRINC, canAccess, respMap);
    }

    /*
     * Verify that a group assigned roles at the unit and collection level will favor
     * the unit level assignment if it has higher level permissions.
     */
    @Test
    public void testFolderFirstAssignmentPrecedence() throws Exception {
        AdminUnit unit = setupAdminUnitWithGroup();
        CollectionObject coll = repositoryObjectFactory.createCollectionObject(
                new AclModelBuilder("Collection with access")
                .addCanAccess(GRP_PRINC)
                .model);
        unit.addMember(coll);
        PID pid = pidMinter.mintContentPid();
        FolderObject folder = repositoryObjectFactory.createFolderObject(pid, null);
        coll.addMember(folder);
        treeIndexer.indexAll(baseAddress);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, List<RoleAssignment>> respMap = getRolesFromResponse(result);

        assertNoAssignedRoles(respMap);
        assertHasInheritedRole(GRP_PRINC, canManage, respMap);
    }

    /*
     * Verify that a group assigned roles at the unit and collection level will favor
     * the collection level assignment if it has higher level permissions.
     */
    @Test
    public void testFolderOverrideParent() throws Exception {
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(
                new AclModelBuilder("Admin Unit Group Can Access")
                .addCanAccess(GRP_PRINC)
                .model);
        rootObj.addMember(unit);
        CollectionObject coll = repositoryObjectFactory.createCollectionObject(
                new AclModelBuilder("Collection Group Can Ingest")
                .addCanIngest(GRP_PRINC)
                .model);
        unit.addMember(coll);
        PID pid = pidMinter.mintContentPid();
        FolderObject folder = repositoryObjectFactory.createFolderObject(pid, null);
        coll.addMember(folder);
        treeIndexer.indexAll(baseAddress);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, List<RoleAssignment>> respMap = getRolesFromResponse(result);

        assertNoAssignedRoles(respMap);
        assertHasInheritedRole(GRP_PRINC, canIngest, respMap);
    }

    @Test
    public void testWork() throws Exception {
        WorkObject work = setupWorkStructure();
        PID pid = work.getPid();
        treeIndexer.indexAll(baseAddress);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, List<RoleAssignment>> respMap = getRolesFromResponse(result);

        assertNoAssignedRoles(respMap);
        assertHasInheritedRole(GRP_PRINC, canManage, respMap);
    }

    @Test
    public void testFileObject() throws Exception {
        WorkObject work = setupWorkStructure();
        InputStream contentStream = new ByteArrayInputStream(origBodyString.getBytes());
        FileObject fileObj = work.addDataFile(contentStream, origFilename, origMimetype, null, null);
        PID pid = fileObj.getPid();
        treeIndexer.indexAll(baseAddress);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, List<RoleAssignment>> respMap = getRolesFromResponse(result);

        assertNoAssignedRoles(respMap);
        assertHasInheritedRole(GRP_PRINC, canManage, respMap);
    }

    private AdminUnit setupAdminUnitWithGroup() {
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(
                new AclModelBuilder("Admin Unit Can Manage")
                .addCanManage(GRP_PRINC)
                .model);
        rootObj.addMember(unit);
        return unit;
    }

    private WorkObject setupWorkStructure() {
        AdminUnit unit = setupAdminUnitWithGroup();
        CollectionObject coll = repositoryObjectFactory.createCollectionObject(null);
        unit.addMember(coll);
        PID pid = pidMinter.mintContentPid();
        WorkObject work = repositoryObjectFactory.createWorkObject(pid, null);
        coll.addMember(work);
        return work;
    }

    private void assertHasInheritedRole(String princ, UserRole role,
            Map<String, List<RoleAssignment>> respMap) {
        List<RoleAssignment> inherited = respMap.get(INHERITED_ROLES);
        assertTrue("Response did not contain required inherited role " + princ + " " + role,
                inherited.contains(new RoleAssignment(princ, role)));
    }

    private void assertHasAssignedRole(String princ, UserRole role,
            Map<String, List<RoleAssignment>> respMap) {
        List<RoleAssignment> assigned = respMap.get(ASSIGNED_ROLES);
        assertTrue("Response did not contain required assigned role " + princ + " " + role,
                assigned.contains(new RoleAssignment(princ, role)));
    }

    private void assertNoInheritedRoles(Map<String, List<RoleAssignment>> respMap) {
        List<RoleAssignment> inherited = respMap.get(INHERITED_ROLES);
        assertTrue("Inherited role map was expected to be empty", inherited.isEmpty());
    }

    private void assertNoAssignedRoles(Map<String, List<RoleAssignment>> respMap) {
        List<RoleAssignment> assigned = respMap.get(ASSIGNED_ROLES);
        assertTrue("Assigned role map was expected to be empty", assigned.isEmpty());
    }

    protected Map<String, List<RoleAssignment>> getRolesFromResponse(MvcResult result) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, List<RoleAssignment>>>() {});
    }
}

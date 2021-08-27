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
package edu.unc.lib.boxc.web.services.rest;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.USER_NAMESPACE;
import static edu.unc.lib.boxc.auth.api.UserRole.canAccess;
import static edu.unc.lib.boxc.auth.api.UserRole.canIngest;
import static edu.unc.lib.boxc.auth.api.UserRole.canManage;
import static edu.unc.lib.boxc.auth.api.UserRole.unitOwner;
import static edu.unc.lib.boxc.web.services.rest.AccessControlRetrievalController.ASSIGNED_ROLES;
import static edu.unc.lib.boxc.web.services.rest.AccessControlRetrievalController.INHERITED_ROLES;
import static edu.unc.lib.boxc.web.services.rest.AccessControlRetrievalController.ROLES_KEY;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unc.lib.boxc.auth.api.UserRole;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.RoleAssignment;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.test.AclModelBuilder;
import edu.unc.lib.boxc.web.services.rest.modify.AbstractAPIIT;

/**
 *
 * @author bbpennel
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/spring-test/acl-service-context.xml"),
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

    @Before
    public void init_() throws Exception {
        AccessGroupSet testPrincipals = new AccessGroupSetImpl(GRP_PRINC);
        GroupsThreadStore.storeUsername(USER_PRINC);
        GroupsThreadStore.storeGroups(testPrincipals);
        setupContentRoot();
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
        contentRoot.addMember(unit);
        treeIndexer.indexAll(baseAddress);

        mvc.perform(get("/acl/staff/" + unitPid.getId()))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void testAsGlobalAdminNoAssigned() throws Exception {
        AccessGroupSet testPrincipals = new AccessGroupSetImpl(SUPER_GROUP_PRINC);
        GroupsThreadStore.storeGroups(testPrincipals);

        PID pid = pidMinter.mintContentPid();
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(pid, null);
        contentRoot.addMember(unit);
        treeIndexer.indexAll(baseAddress);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, Map<String, List<RoleAssignment>>> respMap = getRolesFromResponse(result);

        assertNoInheritedRoles(respMap);
        assertNoAssignedRoles(respMap);
    }

    @Test
    public void testObjectNotFound() throws Exception {
        PID pid = pidMinter.mintContentPid();
        treeIndexer.indexAll(baseAddress);

        mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().isNotFound())
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

        Map<String, Map<String, List<RoleAssignment>>> respMap = getRolesFromResponse(result);

        assertNoInheritedRoles(respMap);
        assertHasAssignedRole(GRP_PRINC, canManage, pid, respMap);
    }

    @Test
    public void testAdminUnitWithMultipleRoles() throws Exception {
        PID unitPid = pidMinter.mintContentPid();
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(unitPid,
                new AclModelBuilder("Admin Unit Can Manage")
                .addCanManage(GRP_PRINC)
                .addUnitOwner(USER_NS_PRINC)
                .model);
        contentRoot.addMember(unit);
        treeIndexer.indexAll(baseAddress);

        MvcResult result = mvc.perform(get("/acl/staff/" + unitPid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, Map<String, List<RoleAssignment>>> respMap = getRolesFromResponse(result);

        assertNoInheritedRoles(respMap);
        assertHasAssignedRole(GRP_PRINC, canManage, unitPid, respMap);
        assertHasAssignedRole(USER_PRINC, unitOwner, unitPid, respMap);
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

        Map<String, Map<String, List<RoleAssignment>>> respMap = getRolesFromResponse(result);

        assertNoAssignedRoles(respMap);
        assertHasInheritedRole(GRP_PRINC, canManage, unit.getPid(), respMap);
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

        Map<String, Map<String, List<RoleAssignment>>> respMap = getRolesFromResponse(result);

        assertHasAssignedRole(USER_PRINC, canAccess, pid, respMap);
        assertHasInheritedRole(GRP_PRINC, canManage, unit.getPid(), respMap);
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

        Map<String, Map<String, List<RoleAssignment>>> respMap = getRolesFromResponse(result);

        assertNoAssignedRoles(respMap);
        assertHasInheritedRole(GRP_PRINC, canManage, unit.getPid(), respMap);
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

        Map<String, Map<String, List<RoleAssignment>>> respMap = getRolesFromResponse(result);

        assertNoAssignedRoles(respMap);
        assertHasInheritedRole(GRP_PRINC, canManage, unit.getPid(), respMap);
        assertHasInheritedRole(USER_PRINC, canAccess, coll.getPid(), respMap);
    }

    /*
     * Verify that a group assigned roles at the unit and collection level will
     * both show up with the correct pid
     */
    @Test
    public void testFolderDuplicateRolesForPrincipal() throws Exception {
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(
                new AclModelBuilder("Admin Unit Group Can Access")
                .addCanAccess(GRP_PRINC)
                .model);
        contentRoot.addMember(unit);
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

        Map<String, Map<String, List<RoleAssignment>>> respMap = getRolesFromResponse(result);

        assertNoAssignedRoles(respMap);
        assertHasInheritedRole(GRP_PRINC, canAccess, unit.getPid(), respMap);
        assertHasInheritedRole(GRP_PRINC, canIngest, coll.getPid(), respMap);
    }

    @Test
    public void testWork() throws Exception {
        AdminUnit unit = setupAdminUnitWithGroup();
        WorkObject work = setupWorkStructure(unit);
        PID pid = work.getPid();
        treeIndexer.indexAll(baseAddress);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, Map<String, List<RoleAssignment>>> respMap = getRolesFromResponse(result);

        assertNoAssignedRoles(respMap);
        assertHasInheritedRole(GRP_PRINC, canManage, unit.getPid(), respMap);
    }

    @Test
    public void testFileObject() throws Exception {
        AdminUnit unit = setupAdminUnitWithGroup();
        WorkObject work = setupWorkStructure(unit);

        Path contentPath = Files.createTempFile("test", ".txt");
        FileUtils.writeStringToFile(contentPath.toFile(), origBodyString, "UTF-8");
        FileObject fileObj = work.addDataFile(contentPath.toUri(), origFilename, origMimetype, null, null);
        PID pid = fileObj.getPid();
        treeIndexer.indexAll(baseAddress);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        Map<String, Map<String, List<RoleAssignment>>> respMap = getRolesFromResponse(result);

        assertNoAssignedRoles(respMap);
        assertHasInheritedRole(GRP_PRINC, canManage, unit.getPid(), respMap);
    }

    private AdminUnit setupAdminUnitWithGroup() {
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(
                new AclModelBuilder("Admin Unit Can Manage")
                .addCanManage(GRP_PRINC)
                .model);
        contentRoot.addMember(unit);
        return unit;
    }

    private WorkObject setupWorkStructure(AdminUnit unit) {
        CollectionObject coll = repositoryObjectFactory.createCollectionObject(null);
        unit.addMember(coll);
        PID pid = pidMinter.mintContentPid();
        WorkObject work = repositoryObjectFactory.createWorkObject(pid, null);
        coll.addMember(work);
        return work;
    }

    private void assertHasInheritedRole(String princ, UserRole role, PID pid,
            Map<String, Map<String, List<RoleAssignment>>> respMap) {
        List<RoleAssignment> inherited = respMap.get(INHERITED_ROLES).get(ROLES_KEY);
        assertTrue("Response did not contain required inherited role " + princ + " " + role,
                inherited.contains(new RoleAssignment(princ, role, pid)));
    }

    private void assertHasAssignedRole(String princ, UserRole role, PID pid,
            Map<String, Map<String, List<RoleAssignment>>> respMap) {
        List<RoleAssignment> assigned = respMap.get(ASSIGNED_ROLES).get(ROLES_KEY);
        assertTrue("Response did not contain required assigned role " + princ + " " + role,
                assigned.contains(new RoleAssignment(princ, role, pid)));
    }

    private void assertNoInheritedRoles(Map<String, Map<String, List<RoleAssignment>>> respMap) {
        List<RoleAssignment> inherited = respMap.get(INHERITED_ROLES).get(ROLES_KEY);
        assertTrue("Inherited role map was expected to be empty", inherited.isEmpty());
    }

    private void assertNoAssignedRoles(Map<String, Map<String, List<RoleAssignment>>> respMap) {
        List<RoleAssignment> assigned = respMap.get(ASSIGNED_ROLES).get(ROLES_KEY);
        assertTrue("Assigned role map was expected to be empty", assigned.isEmpty());
    }

    protected Map<String, Map<String, List<RoleAssignment>>> getRolesFromResponse(MvcResult result) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<Map<String, Map<String, List<RoleAssignment>>>>() {});
    }
}

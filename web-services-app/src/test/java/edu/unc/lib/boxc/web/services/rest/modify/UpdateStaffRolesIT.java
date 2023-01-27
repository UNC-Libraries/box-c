package edu.unc.lib.boxc.web.services.rest.modify;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.USER_NAMESPACE;
import static edu.unc.lib.boxc.auth.api.Permission.assignStaffRoles;
import static edu.unc.lib.boxc.auth.api.UserRole.canAccess;
import static edu.unc.lib.boxc.auth.api.UserRole.canManage;
import static edu.unc.lib.boxc.auth.api.UserRole.unitOwner;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.boxc.auth.api.UserRole;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.RoleAssignment;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.fcrepo.test.AclModelBuilder;
import edu.unc.lib.boxc.web.services.rest.modify.UpdateStaffAccessController.UpdateStaffRequest;

/**
 * @author bbpennel
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/update-staff-it-servlet.xml")
})
public class UpdateStaffRolesIT extends AbstractAPIIT {
    private static final String USER_NAME = "adminuser";
    private static final String USER_URI = USER_NAMESPACE + USER_NAME;
    private static final String USER_GROUPS = "edu:lib:admin_grp";

    @BeforeEach
    public void setup() throws Exception {
        AccessGroupSet testPrincipals = new AccessGroupSetImpl(USER_GROUPS);

        GroupsThreadStore.storeUsername(USER_NAME);
        GroupsThreadStore.storeGroups(testPrincipals);

        setupContentRoot();
    }

    @AfterEach
    public void teardown() throws Exception {
        GroupsThreadStore.clearStore();
    }

    @Test
    public void testInsufficientPermissions() throws Exception {
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(null);
        contentRoot.addMember(unit);
        PID pid = unit.getPid();

        treeIndexer.indexAll(baseAddress);

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSetImpl.class), eq(assignStaffRoles));

        List<RoleAssignment> assignments = asList(
                new RoleAssignment(USER_NAME, canAccess, pid));

        mvc.perform(put("/edit/acl/staff/" + pid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializeAssignments(assignments)))
                .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    public void testInvalidUnitOwnerAssignment() throws Exception {
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(null);
        contentRoot.addMember(unit);
        CollectionObject coll = repositoryObjectFactory.createCollectionObject(null);
        unit.addMember(coll);
        PID pid = coll.getPid();

        treeIndexer.indexAll(baseAddress);

        List<RoleAssignment> assignments = asList(
                new RoleAssignment(USER_NAME, unitOwner));

        MvcResult result = mvc.perform(put("/edit/acl/staff/" + pid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializeAssignments(assignments)))
                .andExpect(status().isBadRequest())
            .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(pid.getId(), respMap.get("pid"));
        assertEquals("editStaffRoles", respMap.get("action"));
        assertTrue(((String) respMap.get("error")).matches(".*contained invalid acl properties:[\\S\\s]+unitOwner"));
    }

    @Test
    public void testTargetDoesNotExist() throws Exception {
        PID pid = pidMinter.mintContentPid();

        List<RoleAssignment> assignments = asList(
                new RoleAssignment(USER_NAME, canManage, pid));

        mvc.perform(put("/edit/acl/staff/" + pid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializeAssignments(assignments)))
                .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    public void testNoAssignments() throws Exception {
        PID pid = pidMinter.mintContentPid();
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(pid,
                new AclModelBuilder("Admin Unit Can Manage")
                .addCanManage(USER_NAME)
                .model);
        contentRoot.addMember(unit);

        treeIndexer.indexAll(baseAddress);

        List<RoleAssignment> assignments = Collections.emptyList();

        MvcResult result = mvc.perform(put("/edit/acl/staff/" + pid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializeAssignments(assignments)))
                .andExpect(status().isOk())
            .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(pid.getId(), respMap.get("pid"));
        assertEquals("editStaffRoles", respMap.get("action"));

        AdminUnit updated = repositoryObjectLoader.getAdminUnit(pid);
        // Verify that existing assignment was cleared
        assertNoAssignment(USER_NAME, canManage, updated);
    }

    @Test
    public void testInvalidRolesBody() throws Exception {
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(null);
        contentRoot.addMember(unit);
        PID pid = unit.getPid();

        treeIndexer.indexAll(baseAddress);

        String assignments = "[ { \"no\" : \"thanks\" } ]";

        mvc.perform(put("/edit/acl/staff/" + pid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignments.getBytes()))
                .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void testNoRolesBody() throws Exception {
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(null);
        contentRoot.addMember(unit);
        PID pid = unit.getPid();

        treeIndexer.indexAll(baseAddress);

        mvc.perform(put("/edit/acl/staff/" + pid.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void testNoPrincipal() throws Exception {
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(null);
        contentRoot.addMember(unit);
        PID pid = unit.getPid();

        treeIndexer.indexAll(baseAddress);

        List<RoleAssignment> assignments = asList(
                new RoleAssignment("", canManage));

        mvc.perform(put("/edit/acl/staff/" + pid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializeAssignments(assignments)))
                .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void testInvalidRole() throws Exception {
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(null);
        contentRoot.addMember(unit);
        PID pid = unit.getPid();

        treeIndexer.indexAll(baseAddress);

        String assignments = "[ { \"principal\" : \"user\", \"role\" : \"dunno\" } ]";

        mvc.perform(put("/edit/acl/staff/" + pid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(assignments.getBytes()))
                .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void testAssignRoles() throws Exception {
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(null);
        contentRoot.addMember(unit);
        PID pid = unit.getPid();

        treeIndexer.indexAll(baseAddress);

        List<RoleAssignment> assignments = asList(
                new RoleAssignment(USER_NAME, canManage, pid));

        MvcResult result = mvc.perform(put("/edit/acl/staff/" + pid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializeAssignments(assignments)))
                .andExpect(status().isOk())
            .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(pid.getId(), respMap.get("pid"));
        assertEquals("editStaffRoles", respMap.get("action"));

        AdminUnit updated = repositoryObjectLoader.getAdminUnit(pid);
        assertHasAssignment(USER_URI, canManage, updated);
    }

    @Test
    public void testAssignGroup() throws Exception {
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(null);
        contentRoot.addMember(unit);
        PID pid = unit.getPid();

        treeIndexer.indexAll(baseAddress);

        List<RoleAssignment> assignments = asList(
                new RoleAssignment(USER_GROUPS, canManage));

        MvcResult result = mvc.perform(put("/edit/acl/staff/" + pid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializeAssignments(assignments)))
                .andExpect(status().isOk())
            .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(pid.getId(), respMap.get("pid"));
        assertEquals("editStaffRoles", respMap.get("action"));

        AdminUnit updated = repositoryObjectLoader.getAdminUnit(pid);
        assertHasAssignment(USER_GROUPS, canManage, updated);
    }

    @Test
    public void testAssignMultipleRolesToSamePrincipal() throws Exception {
        AdminUnit unit = repositoryObjectFactory.createAdminUnit(null);
        contentRoot.addMember(unit);
        PID pid = unit.getPid();

        treeIndexer.indexAll(baseAddress);

        List<RoleAssignment> assignments = asList(
                new RoleAssignment(USER_NAME, canManage),
                new RoleAssignment(USER_NAME, canAccess)
                );

        mvc.perform(put("/edit/acl/staff/" + pid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(serializeAssignments(assignments)))
                .andExpect(status().isBadRequest())
            .andReturn();
    }

    private void assertHasAssignment(String princ, UserRole role, ContentObject obj) {
        Resource resc = obj.getResource();
        assertTrue(resc.hasProperty(role.getProperty(), princ),
                "Expected role " + role.name() + " was not assigned for " + princ);
    }

    private void assertNoAssignment(String princ, UserRole role, ContentObject obj) {
        Resource resc = obj.getResource();
        assertFalse(resc.hasProperty(role.getProperty(), princ),
                "Unexpected role " + role.name() + " was assigned for " + princ);
    }

    private byte[] serializeAssignments(List<RoleAssignment> assignments) throws Exception {
        UpdateStaffRequest updateRequest = new UpdateStaffRequest();
        updateRequest.setRoles(assignments);

        return makeRequestBody(updateRequest);
    }
}

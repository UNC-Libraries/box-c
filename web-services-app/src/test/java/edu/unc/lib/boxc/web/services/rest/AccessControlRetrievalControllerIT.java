package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.UserRole;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.RoleAssignment;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.auth.fcrepo.services.InheritedAclFactory;
import edu.unc.lib.boxc.auth.fcrepo.services.ObjectAclFactory;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.DepositRecord;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.web.common.auth.PatronPrincipalProvider;
import edu.unc.lib.boxc.web.services.rest.exceptions.RestResponseEntityExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static edu.unc.lib.boxc.web.services.rest.AccessControlRetrievalController.ASSIGNED_ROLES;
import static edu.unc.lib.boxc.web.services.rest.AccessControlRetrievalController.INHERITED_ROLES;
import static edu.unc.lib.boxc.web.services.rest.AccessControlRetrievalController.ROLES_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AccessControlRetrievalControllerIT {
    private AutoCloseable closeable;
    private MockMvc mvc;
    private PID pid;
    private PID parentPid;
    private RoleAssignment roleAssignment, parentRoleAssignment;
    @Mock
    private AccessControlService aclService;
    @Mock
    private ObjectAclFactory objectAclFactory;
    @Mock
    private InheritedAclFactory inheritedAclFactory;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private PatronPrincipalProvider patronPrincipalProvider;
    @Mock
    private AdminUnit adminUnit;
    @Mock
    private CollectionObject collectionObject;
    @Mock
    private FileObject fileObject;
    @Mock
    private DepositRecord depositRecord;
    @Mock
    private RepositoryObject parentObject;
    @InjectMocks
    private AccessControlRetrievalController controller;
    private final static String USERNAME = "test_user";
    private final static String ADMIN_GROUP = "adminGroup";
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl(ADMIN_GROUP);

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        pid = makePid();
        parentPid = makePid();
        roleAssignment = new RoleAssignment(ADMIN_GROUP, UserRole.canManage, pid);
        parentRoleAssignment = new RoleAssignment(ADMIN_GROUP, UserRole.canProcess, parentPid);
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testNoPermission() throws Exception {
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(any(), eq(pid), any(), eq(viewHidden));

        mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void testGetStaffRolesWithAdminUnit() throws Exception {
        var roles = List.of(roleAssignment);
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(adminUnit);
        when(objectAclFactory.getStaffRoleAssignments(eq(pid))).thenReturn(roles);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().isOk())
                .andReturn();

        verify(objectAclFactory).getStaffRoleAssignments(eq(pid));
        verify(inheritedAclFactory, never()).getStaffRoleAssignments(eq(pid));

        var respJson = MvcTestHelpers.getResponseAsJson(result);

        var assignedRoles = respJson.get(ASSIGNED_ROLES).get(ROLES_KEY).get(0);
        assertEquals(roleAssignment.getPrincipal(), assignedRoles.get("principal").textValue());
        assertEquals(roleAssignment.getRole().name(), assignedRoles.get("role").textValue());
        assertEquals(roleAssignment.getAssignedTo(), assignedRoles.get("assignedTo").textValue());

        var inheritedRoles = respJson.get(INHERITED_ROLES).get(ROLES_KEY);
        assertTrue(inheritedRoles.isEmpty());
    }

    @Test
    public void testGetStaffRolesWithCollectionObject() throws Exception {
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(collectionObject);
        when(collectionObject.getParent()).thenReturn(parentObject);
        when(parentObject.getPid()).thenReturn(parentPid);
        when(objectAclFactory.getStaffRoleAssignments(eq(pid))).thenReturn(List.of(roleAssignment));
        when(inheritedAclFactory.getStaffRoleAssignments(eq(parentPid))).thenReturn(List.of(parentRoleAssignment));

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().isOk())
                .andReturn();

        verify(objectAclFactory).getStaffRoleAssignments(eq(pid));
        verify(inheritedAclFactory).getStaffRoleAssignments(eq(parentPid));

        var respJson = MvcTestHelpers.getResponseAsJson(result);

        var assignedRoles = respJson.get(ASSIGNED_ROLES).get(ROLES_KEY).get(0);
        assertEquals(roleAssignment.getPrincipal(), assignedRoles.get("principal").textValue());
        assertEquals(roleAssignment.getRole().name(), assignedRoles.get("role").textValue());
        assertEquals(roleAssignment.getAssignedTo(), assignedRoles.get("assignedTo").textValue());

        var inheritedRoles = respJson.get(INHERITED_ROLES).get(ROLES_KEY).get(0);
        assertEquals(parentRoleAssignment.getPrincipal(), inheritedRoles.get("principal").textValue());
        assertEquals(parentRoleAssignment.getRole().name(), inheritedRoles.get("role").textValue());
        assertEquals(parentRoleAssignment.getAssignedTo(), inheritedRoles.get("assignedTo").textValue());


    }

    @Test
    public void testGetStaffRolesWithContentObject() throws Exception {
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(fileObject);
        when(fileObject.getParent()).thenReturn(parentObject);
        when(inheritedAclFactory.getStaffRoleAssignments(eq(pid))).thenReturn(List.of(roleAssignment));

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().isOk())
                .andReturn();

        verify(objectAclFactory, never()).getStaffRoleAssignments(eq(pid));
        verify(inheritedAclFactory).getStaffRoleAssignments(eq(pid));

        var respJson = MvcTestHelpers.getResponseAsJson(result);

        var assignedRoles = respJson.get(ASSIGNED_ROLES).get(ROLES_KEY);
        assertTrue(assignedRoles.isEmpty());

        var inheritedRoles = respJson.get(INHERITED_ROLES).get(ROLES_KEY).get(0);
        assertEquals(roleAssignment.getPrincipal(), inheritedRoles.get("principal").textValue());
        assertEquals(roleAssignment.getRole().name(), inheritedRoles.get("role").textValue());
        assertEquals(roleAssignment.getAssignedTo(), inheritedRoles.get("assignedTo").textValue());
    }

    @Test
    public void testGetStaffRolesWithNonContentObject() throws Exception {
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(depositRecord);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();

        verify(objectAclFactory, never()).getStaffRoleAssignments(eq(pid));
        verify(inheritedAclFactory, never()).getStaffRoleAssignments(eq(pid));

        var respJson = MvcTestHelpers.getResponseAsJson(result);
        assertNull(respJson.get(INHERITED_ROLES));
        assertNull(respJson.get(ASSIGNED_ROLES));
        var error = respJson.get("error");
        assertEquals("Cannot retrieve staff roles for object " + pid.getId() + " of type "
                + depositRecord.getClass().getName(), error.textValue());
    }
}

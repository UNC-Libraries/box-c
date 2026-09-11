package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.AccessPrincipalConstants;
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
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.web.common.auth.IPAddressPatronPrincipalConfig;
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

import java.util.Date;
import java.util.List;

import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static edu.unc.lib.boxc.web.services.rest.AccessControlRetrievalController.ALLOWED_PATRON_PRINCIPALS;
import static edu.unc.lib.boxc.web.services.rest.AccessControlRetrievalController.ASSIGNED_ROLES;
import static edu.unc.lib.boxc.web.services.rest.AccessControlRetrievalController.INHERITED_ROLES;
import static edu.unc.lib.boxc.web.services.rest.AccessControlRetrievalController.ROLES_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private WorkObject parentObject;
    @Mock
    private IPAddressPatronPrincipalConfig config;
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

        when(parentObject.getPid()).thenReturn(parentPid);
        when(patronPrincipalProvider.getConfiguredPatronPrincipals()).thenReturn(List.of(config));
        when(config.getName()).thenReturn(USERNAME);
        when(config.getPrincipal()).thenReturn(ADMIN_GROUP);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
        GroupsThreadStore.clearStore();
    }

    @Test
    public void testGetStaffRolesNoPermission() throws Exception {
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

    @Test
    public void testAllowedPrincipalsNoPermission() throws Exception {
        mvc.perform(get("/acl/patron/allowedPrincipals"))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void testAllowedPrincipalsSuccess() throws Exception {
        AccessGroupSet groups = GroupsThreadStore.getGroups();
        groups.add(AccessPrincipalConstants.ADMIN_ACCESS_PRINC);
        GroupsThreadStore.storeGroups(groups);

        MvcResult result = mvc.perform(get("/acl/patron/allowedPrincipals"))
                .andExpect(status().isOk())
                .andReturn();

        var respJson = MvcTestHelpers.getResponseAsJson(result);
        var body = respJson.get(0);
        assertEquals(USERNAME, body.get("name").textValue());
        assertEquals(ADMIN_GROUP, body.get("principal").textValue());
    }

    @Test
    public void testGetPatronAccessNoPermission() throws Exception {
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(any(), eq(pid), any(), eq(viewHidden));

        mvc.perform(get("/acl/patron/" + pid.getId()))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void testGetPatronAccessWithAdminUnit() throws Exception {
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(adminUnit);

        MvcResult result = mvc.perform(get("/acl/patron/" + pid.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();

        var respJson = MvcTestHelpers.getResponseAsJson(result);
        assertNull(respJson.get(INHERITED_ROLES));
        assertNull(respJson.get(ASSIGNED_ROLES));
        assertNull(respJson.get(ALLOWED_PATRON_PRINCIPALS));
        var error = respJson.get("error");
        assertEquals("Cannot retrieve patron access for a unit", error.textValue());
    }

    @Test
    public void testGetPatronAccessWithContentObject() throws Exception {
        var date = new Date(1407470400000L); // 2014-08-08
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(fileObject);
        when(fileObject.getParent()).thenReturn(parentObject);
        when(objectAclFactory.getPatronRoleAssignments(eq(pid))).thenReturn(List.of(roleAssignment));
        when(objectAclFactory.isMarkedForDeletion(eq(pid))).thenReturn(false);
        when(objectAclFactory.getEmbargoUntil(eq(pid))).thenReturn(date);
        when(inheritedAclFactory.getPatronAccess(eq(parentPid))).thenReturn(List.of(parentRoleAssignment));
        when(inheritedAclFactory.isMarkedForDeletion(eq(parentPid))).thenReturn(false);
        when(inheritedAclFactory.getEmbargoUntil(eq(parentPid))).thenReturn(date);

        MvcResult result = mvc.perform(get("/acl/patron/" + pid.getId()))
                .andExpect(status().isOk())
                .andReturn();

        var respJson = MvcTestHelpers.getResponseAsJson(result);

        var assigned = respJson.get(ASSIGNED_ROLES);
        assertEquals("2014-08-08", assigned.get("embargo").textValue());
        assertFalse(assigned.get("deleted").asBoolean());
        var assignedRoles = assigned.get(ROLES_KEY).get(0);
        assertEquals(roleAssignment.getPrincipal(), assignedRoles.get("principal").textValue());
        assertEquals(roleAssignment.getRole().name(), assignedRoles.get("role").textValue());
        assertEquals(roleAssignment.getAssignedTo(), assignedRoles.get("assignedTo").textValue());


        var inherited = respJson.get(INHERITED_ROLES);
        assertEquals("2014-08-08", inherited.get("embargo").textValue());
        assertFalse(inherited.get("deleted").asBoolean());
        var inheritedRoles = inherited.get(ROLES_KEY).get(0);
        assertEquals(parentRoleAssignment.getPrincipal(), inheritedRoles.get("principal").textValue());
        assertEquals(parentRoleAssignment.getRole().name(), inheritedRoles.get("role").textValue());
        assertEquals(parentRoleAssignment.getAssignedTo(), inheritedRoles.get("assignedTo").textValue());

        var allowedPatronPrincipals = respJson.get(ALLOWED_PATRON_PRINCIPALS).get(0);
        assertEquals(USERNAME, allowedPatronPrincipals.get("name").textValue());
        assertEquals(ADMIN_GROUP, allowedPatronPrincipals.get("principal").textValue());
    }

    @Test
    public void testGetPatronAccessWithNonContentObject() throws Exception {
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(depositRecord);

        MvcResult result = mvc.perform(get("/acl/patron/" + pid.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();

        verify(objectAclFactory, never()).getStaffRoleAssignments(eq(pid));
        verify(inheritedAclFactory, never()).getStaffRoleAssignments(eq(pid));

        var respJson = MvcTestHelpers.getResponseAsJson(result);
        assertNull(respJson.get(INHERITED_ROLES));
        assertNull(respJson.get(ASSIGNED_ROLES));
        assertNull(respJson.get(ALLOWED_PATRON_PRINCIPALS));
        var error = respJson.get("error");
        assertEquals("Cannot retrieve patron access for object " + pid.getId() + " of type "
                + depositRecord.getClass().getName(), error.textValue());
    }
}

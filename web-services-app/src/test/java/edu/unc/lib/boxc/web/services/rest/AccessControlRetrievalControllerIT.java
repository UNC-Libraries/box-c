package edu.unc.lib.boxc.web.services.rest;

import edu.unc.lib.boxc.auth.api.Permission;
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

import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static edu.unc.lib.boxc.auth.api.Permission.viewOriginal;
import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
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
    private RoleAssignment roleAssignment;
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
    private final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestResponseEntityExceptionHandler())
                .build();
        pid = makePid();
        parentPid = makePid();
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
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(adminUnit);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().isOk())
                .andReturn();

        verify(objectAclFactory).getStaffRoleAssignments(eq(pid));
        verify(inheritedAclFactory, never()).getStaffRoleAssignments(eq(pid));
    }

    @Test
    public void testGetStaffRolesWithCollectionObject() throws Exception {
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(collectionObject);
        when(collectionObject.getParent()).thenReturn(parentObject);
        when(parentObject.getPid()).thenReturn(parentPid);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().isOk())
                .andReturn();

        verify(objectAclFactory).getStaffRoleAssignments(eq(pid));
        verify(inheritedAclFactory).getStaffRoleAssignments(eq(parentPid));
    }

    @Test
    public void testGetStaffRolesWithContentObject() throws Exception {
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(fileObject);
        when(fileObject.getParent()).thenReturn(parentObject);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().isOk())
                .andReturn();

        verify(objectAclFactory, never()).getStaffRoleAssignments(eq(pid));
        verify(inheritedAclFactory).getStaffRoleAssignments(eq(pid));
    }

    @Test
    public void testGetStaffRolesWithNonContentObject() throws Exception {
        when(repositoryObjectLoader.getRepositoryObject(eq(pid))).thenReturn(depositRecord);

        MvcResult result = mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().isBadRequest())
                .andReturn();

        verify(objectAclFactory, never()).getStaffRoleAssignments(eq(pid));
        verify(inheritedAclFactory, never()).getStaffRoleAssignments(eq(pid));
    }
}

package edu.unc.lib.boxc.web.common.auth;

import edu.unc.lib.boxc.auth.api.AccessPrincipalConstants;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static edu.unc.lib.boxc.auth.api.Permission.viewOriginal;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
public class PatronActionPermissionsUtilTest {
    private static final String WORK_ID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    @Mock
    private AccessControlService aclService;
    private PID pid;
    private final static String USERNAME = "test_user";
    private AccessGroupSet groups = new AccessGroupSetImpl("adminGroup");
    private AgentPrincipals agent;

    private AutoCloseable autoCloseable;

    @BeforeEach
    public void setup() {
        autoCloseable = openMocks(this);
        pid = PIDs.get(WORK_ID);
        agent = new AgentPrincipalsImpl(USERNAME, groups);
    }

    @AfterEach
    public void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    public void testHasBulkDownloadPermissionPermit() {
        when(aclService.hasAccess(eq(pid), any(), eq(viewOriginal))).thenReturn(true);
        assertTrue(PatronActionPermissionsUtil.hasBulkDownloadPermission(aclService, pid, agent));
    }

    @Test
    public void testHasBulkDownloadPermissionNotAuthenticatedOrOnCampusReject() {
        when(aclService.hasAccess(eq(pid), any(), eq(viewOriginal))).thenReturn(true);
        var unauthAgent = new AgentPrincipalsImpl(null, null);
        assertFalse(PatronActionPermissionsUtil.hasBulkDownloadPermission(aclService, pid, unauthAgent));
    }

    @Test
    public void testHasBulkDownloadPermissionOnCampusPermit() {
        when(aclService.hasAccess(eq(pid), any(), eq(viewOriginal))).thenReturn(true);
        var unauthAgent = new AgentPrincipalsImpl(null, new AccessGroupSetImpl(AccessPrincipalConstants.ON_CAMPUS_PRINC));
        assertTrue(PatronActionPermissionsUtil.hasBulkDownloadPermission(aclService, pid, unauthAgent));
    }

    @Test
    public void testHasBulkDownloadPermissionNoPermissionReject() {
        when(aclService.hasAccess(eq(pid), any(), eq(viewOriginal))).thenReturn(false);
        assertFalse(PatronActionPermissionsUtil.hasBulkDownloadPermission(aclService, pid, agent));
    }
}

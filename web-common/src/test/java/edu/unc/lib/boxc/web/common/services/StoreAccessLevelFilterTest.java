package edu.unc.lib.boxc.web.common.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Arrays;
import java.util.HashSet;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.AccessPrincipalConstants;
import edu.unc.lib.boxc.auth.api.UserRole;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.web.common.auth.AccessLevel;
import edu.unc.lib.boxc.web.common.auth.filters.StoreAccessLevelFilter;

/**
 * @author bbpennel
 */
public class StoreAccessLevelFilterTest {
    private AutoCloseable closeable;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HttpSession session;
    @Mock
    private FilterChain filterChain;
    @Mock
    private SolrQueryLayerService queryLayer;
    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;
    @Captor
    private ArgumentCaptor<AccessLevel> accessLevelCaptor;
    private AccessGroupSet principals;

    @InjectMocks
    private StoreAccessLevelFilter filter;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        principals = new AccessGroupSetImpl();
        GroupsThreadStore.storeGroups(principals);

        when(request.getSession(true)).thenReturn(session);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
        GroupsThreadStore.clearStore();
    }

    @Test
    public void noUsername() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        verify(session).removeAttribute("accessLevel");
        verify(session, never()).setAttribute(anyString(), any(AccessLevel.class));

        assertAdminAccessPrincipalNotGranted();
    }

    @Test
    public void accessFromLocalPermissions() throws Exception {
        GroupsThreadStore.storeUsername("user");
        when(queryLayer.hasAdminViewPermission(any(AccessGroupSetImpl.class))).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(session).setAttribute(anyString(), accessLevelCaptor.capture());
        AccessLevel level = accessLevelCaptor.getValue();
        assertEquals(UserRole.canAccess, level.getHighestRole());

        verify(filterChain).doFilter(request, response);

        assertHasAdminAccessPrincipal();
    }

    @Test
    public void noAccess() throws Exception {
        filter.setRequireViewAdmin(true);
        GroupsThreadStore.storeUsername("user");
        when(queryLayer.hasAdminViewPermission(any(AccessGroupSetImpl.class))).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(session).setAttribute(anyString(), accessLevelCaptor.capture());
        AccessLevel level = accessLevelCaptor.getValue();
        assertNull(level.getHighestRole());

        verify(response).setStatus(401);
        verify(filterChain, never()).doFilter(request, response);

        assertAdminAccessPrincipalNotGranted();
    }

    @Test
    public void accessFromGlobalPermissions() throws Exception {
        GroupsThreadStore.storeUsername("user");
        when(globalPermissionEvaluator.hasGlobalPrincipal(any())).thenReturn(true);
        when(globalPermissionEvaluator.getGlobalUserRoles(any())).thenReturn(new HashSet<>(
                Arrays.asList(UserRole.canIngest)));

        filter.doFilterInternal(request, response, filterChain);

        verify(session).setAttribute(anyString(), accessLevelCaptor.capture());
        AccessLevel level = accessLevelCaptor.getValue();
        assertEquals(UserRole.canAccess, level.getHighestRole());

        verify(filterChain).doFilter(request, response);

        assertHasAdminAccessPrincipal();
    }

    @Test
    public void adminAccessFromGlobalPermissions() throws Exception {
        GroupsThreadStore.storeUsername("user");
        when(globalPermissionEvaluator.hasGlobalPrincipal(any())).thenReturn(true);
        when(globalPermissionEvaluator.getGlobalUserRoles(any())).thenReturn(new HashSet<>(
                Arrays.asList(UserRole.administrator)));

        filter.doFilterInternal(request, response, filterChain);

        verify(session).setAttribute(anyString(), accessLevelCaptor.capture());
        AccessLevel level = accessLevelCaptor.getValue();
        assertEquals(UserRole.administrator, level.getHighestRole());

        verify(filterChain).doFilter(request, response);

        assertHasAdminAccessPrincipal();
    }

    private void assertHasAdminAccessPrincipal() {
        assertTrue(GroupsThreadStore.getPrincipals().contains(AccessPrincipalConstants.ADMIN_ACCESS_PRINC),
                "Did not set admin_access principal for the request");
    }

    private void assertAdminAccessPrincipalNotGranted() {
        assertFalse(GroupsThreadStore.getPrincipals().contains(AccessPrincipalConstants.ADMIN_ACCESS_PRINC),
                "Was granted admin_access principal, which must not be present");
    }
}

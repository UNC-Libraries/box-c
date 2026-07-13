package edu.unc.lib.boxc.auth.fcrepo.services;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.model.api.ids.PID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class AccessControlServiceImplTest {
    private AccessControlServiceImpl service;

    private InheritedPermissionEvaluator permissionEvaluator;
    private GlobalPermissionEvaluator globalPermissionEvaluator;

    @BeforeEach
    public void setUp() {
        service = new AccessControlServiceImpl();
        permissionEvaluator = mock(InheritedPermissionEvaluator.class);
        globalPermissionEvaluator = mock(GlobalPermissionEvaluator.class);

        service.setPermissionEvaluator(permissionEvaluator);
        service.setGlobalPermissionEvaluator(globalPermissionEvaluator);
    }

    @Test
    public void hasAccessReturnsTrueWhenGlobalPermissionGranted() {
        PID pid = mock(PID.class);
        Set<String> principals = Set.of("group1");
        Permission permission = mock(Permission.class);

        when(globalPermissionEvaluator.hasGlobalPermission(principals, permission)).thenReturn(true);

        boolean result = service.hasAccess(pid, principals, permission);

        assertTrue(result);
        verify(globalPermissionEvaluator).hasGlobalPermission(principals, permission);
        verifyNoInteractions(permissionEvaluator);
    }

    @Test
    public void hasAccessDelegatesInheritedPermissionEvaluatorWhenNoGlobalPermission() {
        PID pid = mock(PID.class);
        Set<String> principals = Set.of("group1");
        Permission permission = mock(Permission.class);

        when(globalPermissionEvaluator.hasGlobalPermission(principals, permission)).thenReturn(false);
        when(permissionEvaluator.hasPermission(pid, principals, permission)).thenReturn(true);

        boolean result = service.hasAccess(pid, principals, permission);

        assertTrue(result);
        verify(globalPermissionEvaluator).hasGlobalPermission(principals, permission);
        verify(permissionEvaluator).hasPermission(pid, principals, permission);
    }

    @Test
    public void hasAccessReturnsFalseWhenNoGlobalPermissionAndInheritedPermissionDenied() {
        PID pid = mock(PID.class);
        Set<String> principals = Set.of("group1");
        Permission permission = mock(Permission.class);

        when(globalPermissionEvaluator.hasGlobalPermission(principals, permission)).thenReturn(false);
        when(permissionEvaluator.hasPermission(pid, principals, permission)).thenReturn(false);

        boolean result = service.hasAccess(pid, principals, permission);

        assertFalse(result);
        verify(globalPermissionEvaluator).hasGlobalPermission(principals, permission);
        verify(permissionEvaluator).hasPermission(pid, principals, permission);
    }

    @Test
    public void assertHasAccessDoesNotThrowWhenAccessGranted() {
        PID pid = mock(PID.class);
        Set<String> principals = Set.of("group1");
        Permission permission = mock(Permission.class);

        when(globalPermissionEvaluator.hasGlobalPermission(principals, permission)).thenReturn(false);
        when(permissionEvaluator.hasPermission(pid, principals, permission)).thenReturn(true);

        assertDoesNotThrow(() -> service.assertHasAccess(pid, principals, permission));
    }

    @Test
    public void assertHasAccessThrowsWhenAccessDenied() {
        PID pid = mock(PID.class);
        Set<String> principals = Set.of("group1");
        Permission permission = mock(Permission.class);

        when(globalPermissionEvaluator.hasGlobalPermission(principals, permission)).thenReturn(false);
        when(permissionEvaluator.hasPermission(pid, principals, permission)).thenReturn(false);

        assertThrows(AccessRestrictionException.class,
                () -> service.assertHasAccess(pid, principals, permission));
    }

    @Test
    public void assertHasAccessWithMessageThrowsWithCustomMessageWhenDenied() {
        PID pid = mock(PID.class);
        Set<String> principals = Set.of("group1");
        Permission permission = mock(Permission.class);
        String message = "denial message";

        when(globalPermissionEvaluator.hasGlobalPermission(principals, permission)).thenReturn(false);
        when(permissionEvaluator.hasPermission(pid, principals, permission)).thenReturn(false);

        AccessRestrictionException ex = assertThrows(
                AccessRestrictionException.class,
                () -> service.assertHasAccess(message, pid, principals, permission)
        );

        assertEquals(message, ex.getMessage());
    }

    @Test
    public void assertHasAccessWithMessageDoesNotThrowWhenAccessGranted() {
        PID pid = mock(PID.class);
        Set<String> principals = Set.of("group1");
        Permission permission = mock(Permission.class);
        String message = "message";

        when(globalPermissionEvaluator.hasGlobalPermission(principals, permission)).thenReturn(true);

        assertDoesNotThrow(() -> service.assertHasAccess(message, pid, principals, permission));
    }
}

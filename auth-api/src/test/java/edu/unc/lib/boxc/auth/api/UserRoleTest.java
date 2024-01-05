package edu.unc.lib.boxc.auth.api;

import edu.unc.lib.boxc.model.api.rdf.CdrAcl;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author bbpennel
 */
public class UserRoleTest {
    @Test
    public void canViewReducedQualityPermissionsTest() {
        var subject = UserRole.canViewReducedQuality;
        var expectedPermissions = Set.of(
                Permission.viewMetadata, Permission.viewAccessCopies, Permission.viewReducedResolutionImages);
        assertSetMatchesExactly(expectedPermissions, subject.getPermissions());
    }

    @Test
    public void canViewReducedQualityPermissionNamesTest() {
        var subject = UserRole.canViewReducedQuality;
        var expectedNames = Set.of(Permission.viewMetadata.name(), Permission.viewAccessCopies.name(),
                Permission.viewReducedResolutionImages.name());
        assertSetMatchesExactly(expectedNames, subject.getPermissionNames());
    }

    @Test
    public void administratorPermissionsTest() {
        var subject = UserRole.administrator;
        var expectedPermissions = Set.of(Permission.values());
        assertSetMatchesExactly(expectedPermissions, subject.getPermissions());
    }

    @Test
    public void getUserRolesWithNoPermissionsTest() {
        // Listing no permissions returns all user roles
        assertSetMatchesExactly(Set.of(UserRole.values()), UserRole.getUserRoles(Collections.emptyList()));
    }

    @Test
    public void getUserRolesMatchesMultipleRolesTest() {
        var expected = Set.of(UserRole.canIngest, UserRole.canManage,
                UserRole.unitOwner, UserRole.administrator);
        var result = UserRole.getUserRoles(Arrays.asList(Permission.viewAccessCopies, Permission.ingest));
        assertSetMatchesExactly(expected, result);
    }

    @Test
    public void getUserRolesWithPermissionOrderMembersTest() {
        var expected = Set.of(UserRole.canProcess, UserRole.canManage,
                UserRole.unitOwner, UserRole.administrator);
        var result = UserRole.getUserRolesWithPermission(Permission.orderMembers);
        assertSetMatchesExactly(expected, result);
    }

    @Test
    public void getStaffRolesTest() {
        var expected = Arrays.asList(UserRole.canAccess, UserRole.canIngest, UserRole.canDescribe,
                UserRole.canProcess, UserRole.canManage, UserRole.unitOwner, UserRole.administrator);
        assertIterableEquals(expected, UserRole.getStaffRoles());
    }

    @Test
    public void getPatronRolesTest() {
        var expected = Arrays.asList(UserRole.none, UserRole.canDiscover, UserRole.canViewMetadata,
                UserRole.canViewAccessCopies, UserRole.canViewReducedQuality, UserRole.canViewOriginals);
        assertIterableEquals(expected, UserRole.getPatronRoles());
    }

    @Test
    public void getRoleByPropertyValidTest() {
        assertEquals(UserRole.canAccess, UserRole.getRoleByProperty(CdrAcl.canAccess.getURI()));
    }

    @Test
    public void getRoleByPropertyNotFoundTest() {
        assertNull(UserRole.getRoleByProperty("http://example.com/ohno"));
    }

    @Test
    public void getPredicateTest() {
        assertEquals("canManage", UserRole.canManage.getPredicate());
    }

    @Test
    public void getPropertyTest() {
        assertEquals(CdrAcl.canManage, UserRole.canManage.getProperty());
    }

    @Test
    public void getURITest() {
        assertEquals(CdrAcl.canManage, UserRole.canManage.getProperty());
    }

    @Test
    public void isStaffRoleTrueTest() {
        assertTrue(UserRole.canManage.isStaffRole());
    }

    @Test
    public void isStaffRoleFalseTest() {
        assertFalse(UserRole.canViewReducedQuality.isStaffRole());
    }

    @Test
    public void isPatronRoleFalseTest() {
        assertFalse(UserRole.canManage.isPatronRole());
    }

    @Test
    public void isPatronRoleTrueTest() {
        assertTrue(UserRole.canViewReducedQuality.isPatronRole());
    }

    @Test
    public void equalsTrueTest() {
        assertTrue(UserRole.none.equals(CdrAcl.none.getURI()));
    }

    @Test
    public void equalsFalseTest() {
        assertFalse(UserRole.none.equals("hello"));
    }

    // Compare that two sets are exactly equal, order insensitive.
    // junits assertIterableEquals is not reliable with sets since it depends on order, and we aren't importing hamcrest
    private <T> void assertSetMatchesExactly(Set<T> expected, Set<T> actual) {
        var message = "Actual set values did not match expected:\nActual: " + actual + "\nExpected: " + expected;
        assertTrue(actual.containsAll(expected), message);
        assertEquals(expected.size(), actual.size(), message);
    }
}

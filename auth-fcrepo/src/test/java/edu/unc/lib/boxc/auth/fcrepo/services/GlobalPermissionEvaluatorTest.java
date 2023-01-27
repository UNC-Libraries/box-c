package edu.unc.lib.boxc.auth.fcrepo.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.auth.api.AccessPrincipalConstants;
import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.UserRole;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;

public class GlobalPermissionEvaluatorTest {

    private final static String PRINC_GRP1 = "group1";
    private final static String PRINC_GRP2 = "group2";

    private Set<String> principals;

    private Properties configProperties;

    private GlobalPermissionEvaluator evaluator;

    @BeforeEach
    public void init() {
        initMocks(this);

        configProperties = new Properties();

        principals = new HashSet<>(Arrays.asList(PRINC_GRP1));
    }

    @Test
    public void hasGlobalPermissionTest() {
        addGlobalAssignment(UserRole.administrator, PRINC_GRP1);

        evaluator = new GlobalPermissionEvaluatorImpl(configProperties);

        assertTrue(evaluator.hasGlobalPermission(principals, Permission.destroy));
    }

    @Test
    public void illegalRoleTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            configProperties.setProperty(
                    GlobalPermissionEvaluatorImpl.GLOBAL_PROP_PREFIX + "boxy", PRINC_GRP1);

            evaluator = new GlobalPermissionEvaluatorImpl(configProperties);
        });
    }

    @Test
    public void illegalPatronRoleTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            addGlobalAssignment(UserRole.canViewOriginals, PRINC_GRP1);

            evaluator = new GlobalPermissionEvaluatorImpl(configProperties);
        });
    }

    @Test
    public void noPermissionTest() {
        addGlobalAssignment(UserRole.canAccess, PRINC_GRP1);

        evaluator = new GlobalPermissionEvaluatorImpl(configProperties);

        assertFalse(evaluator.hasGlobalPermission(principals, Permission.destroy));
    }

    @Test
    public void illegalPatronPrincipalTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            addGlobalAssignment(UserRole.canAccess, AccessPrincipalConstants.AUTHENTICATED_PRINC);

            new GlobalPermissionEvaluatorImpl(configProperties);
        });
    }

    @Test
    public void nonroleProperties() {
        configProperties.setProperty("some.property", "value");
        addGlobalAssignment(UserRole.canAccess, PRINC_GRP1);
        configProperties.setProperty("some.other.property", "val");

        evaluator = new GlobalPermissionEvaluatorImpl(configProperties);

        assertTrue(evaluator.hasGlobalPermission(principals, Permission.viewHidden));
    }

    @Test
    public void noRoleProperties() {
        configProperties.setProperty("some.property", "value");

        evaluator = new GlobalPermissionEvaluatorImpl(configProperties);

        assertFalse(evaluator.hasGlobalPermission(principals, Permission.viewHidden));
    }

    @Test
    public void multipleRolesPerPrincipal() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            addGlobalAssignment(UserRole.canAccess, PRINC_GRP1);
            addGlobalAssignment(UserRole.canManage, PRINC_GRP1);

            evaluator = new GlobalPermissionEvaluatorImpl(configProperties);
        });
    }

    @Test
    public void noPrincipalsTest() {
        addGlobalAssignment(UserRole.administrator, PRINC_GRP1);

        evaluator = new GlobalPermissionEvaluatorImpl(configProperties);

        assertFalse(evaluator.hasGlobalPermission(Collections.emptySet(), Permission.viewHidden));
    }

    @Test
    public void multiplePrincipalsTest() {
        addGlobalAssignment(UserRole.canManage, PRINC_GRP1);
        addGlobalAssignment(UserRole.canDescribe, PRINC_GRP2);

        evaluator = new GlobalPermissionEvaluatorImpl(configProperties);

        assertTrue(evaluator.hasGlobalPermission(principals, Permission.markForDeletion));

        Set<String> principals2 = new HashSet<>(Arrays.asList(PRINC_GRP2));
        assertTrue(evaluator.hasGlobalPermission(principals2, Permission.editDescription));
        assertFalse(evaluator.hasGlobalPermission(principals2, Permission.markForDeletion));

        // Check that if multiple globals present the higher one wins
        Set<String> principalsCombined = new HashSet<>(Arrays.asList(PRINC_GRP2, PRINC_GRP1));
        assertTrue(evaluator.hasGlobalPermission(principalsCombined, Permission.editDescription));
        assertTrue(evaluator.hasGlobalPermission(principalsCombined, Permission.markForDeletion));
    }

    @Test
    public void multiplePrincipalsSameRoleTest() {
        addGlobalAssignment(UserRole.canDescribe, PRINC_GRP1 + "," + PRINC_GRP2);

        evaluator = new GlobalPermissionEvaluatorImpl(configProperties);

        assertTrue(evaluator.hasGlobalPermission(principals, Permission.editDescription));
        assertFalse(evaluator.hasGlobalPermission(principals, Permission.markForDeletion));

        Set<String> principals2 = new HashSet<>(Arrays.asList(PRINC_GRP2));
        assertTrue(evaluator.hasGlobalPermission(principals2, Permission.editDescription));
        assertFalse(evaluator.hasGlobalPermission(principals2, Permission.markForDeletion));

        // Check that if multiple globals present the higher one wins
        Set<String> principalsCombined = new HashSet<>(Arrays.asList(PRINC_GRP2, PRINC_GRP1));
        assertTrue(evaluator.hasGlobalPermission(principalsCombined, Permission.editDescription));
        assertFalse(evaluator.hasGlobalPermission(principalsCombined, Permission.markForDeletion));
    }

    @Test
    public void hasGlobalPrincipalTest() {
        addGlobalAssignment(UserRole.canManage, PRINC_GRP1);

        evaluator = new GlobalPermissionEvaluatorImpl(configProperties);

        assertTrue(evaluator.hasGlobalPrincipal(principals));
    }

    @Test
    public void noGlobalPrincipalTest() {
        addGlobalAssignment(UserRole.canManage, PRINC_GRP2);

        evaluator = new GlobalPermissionEvaluatorImpl(configProperties);

        assertFalse(evaluator.hasGlobalPrincipal(principals));
    }

    @Test
    public void getGlobalUserRoles() {
        addGlobalAssignment(UserRole.canManage, PRINC_GRP1);
        addGlobalAssignment(UserRole.canDescribe, PRINC_GRP2);

        evaluator = new GlobalPermissionEvaluatorImpl(configProperties);

        Set<UserRole> roles = evaluator.getGlobalUserRoles(principals);

        assertTrue(roles.contains(UserRole.canManage));
    }

    @Test
    public void getGlobalMultipleUserRoles() {
        principals = new HashSet<>(Arrays.asList(PRINC_GRP1, PRINC_GRP2));

        addGlobalAssignment(UserRole.canManage, PRINC_GRP1);
        addGlobalAssignment(UserRole.canDescribe, PRINC_GRP2);

        evaluator = new GlobalPermissionEvaluatorImpl(configProperties);

        Set<UserRole> roles = evaluator.getGlobalUserRoles(principals);

        assertTrue(roles.contains(UserRole.canManage));
        assertTrue(roles.contains(UserRole.canDescribe));
    }

    @Test
    public void getNoGlobalUserRoles() {
        addGlobalAssignment(UserRole.canDescribe, PRINC_GRP2);

        evaluator = new GlobalPermissionEvaluatorImpl(configProperties);

        Set<UserRole> roles = evaluator.getGlobalUserRoles(principals);

        assertTrue(roles.isEmpty());
    }

    private void addGlobalAssignment(UserRole role, String principal) {
        configProperties.setProperty(
                GlobalPermissionEvaluatorImpl.GLOBAL_PROP_PREFIX + role.name(), principal);
    }
}

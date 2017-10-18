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
package edu.unc.lib.dl.acl.fcrepo4;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;

public class GlobalPermissionEvaluatorTest {

    private final static String PRINC_GRP1 = "group1";
    private final static String PRINC_GRP2 = "group2";

    private Set<String> principals;

    private Properties configProperties;

    private GlobalPermissionEvaluator evaluator;

    @Before
    public void init() {
        initMocks(this);

        configProperties = new Properties();

        principals = new HashSet<>(Arrays.asList(PRINC_GRP1));
    }

    @Test
    public void hasGlobalPermissionTest() {
        addGlobalAssignment(UserRole.administrator, PRINC_GRP1);

        evaluator = new GlobalPermissionEvaluator(configProperties);

        assertTrue(evaluator.hasGlobalPermission(principals, Permission.destroy));
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalRoleTest() {
        configProperties.setProperty(
                GlobalPermissionEvaluator.GLOBAL_PROP_PREFIX + "boxy", PRINC_GRP1);

        evaluator = new GlobalPermissionEvaluator(configProperties);
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalPatronRoleTest() {
        addGlobalAssignment(UserRole.canViewOriginals, PRINC_GRP1);

        evaluator = new GlobalPermissionEvaluator(configProperties);
    }

    @Test
    public void noPermissionTest() {
        addGlobalAssignment(UserRole.canAccess, PRINC_GRP1);

        evaluator = new GlobalPermissionEvaluator(configProperties);

        assertFalse(evaluator.hasGlobalPermission(principals, Permission.destroy));
    }

    @Test
    public void nonroleProperties() {
        configProperties.setProperty("some.property", "value");
        addGlobalAssignment(UserRole.canAccess, PRINC_GRP1);
        configProperties.setProperty("some.other.property", "val");

        evaluator = new GlobalPermissionEvaluator(configProperties);

        assertTrue(evaluator.hasGlobalPermission(principals, Permission.viewHidden));
    }

    @Test
    public void noRoleProperties() {
        configProperties.setProperty("some.property", "value");

        evaluator = new GlobalPermissionEvaluator(configProperties);

        assertFalse(evaluator.hasGlobalPermission(principals, Permission.viewHidden));
    }

    @Test(expected = IllegalStateException.class)
    public void multipleRolesPerPrincipal() {
        addGlobalAssignment(UserRole.canAccess, PRINC_GRP1);
        addGlobalAssignment(UserRole.canManage, PRINC_GRP1);

        evaluator = new GlobalPermissionEvaluator(configProperties);
    }

    @Test
    public void noPrincipalsTest() {
        addGlobalAssignment(UserRole.administrator, PRINC_GRP1);

        evaluator = new GlobalPermissionEvaluator(configProperties);

        assertFalse(evaluator.hasGlobalPermission(Collections.emptySet(), Permission.viewHidden));
    }

    @Test
    public void multiplePrincipalsTest() {
        addGlobalAssignment(UserRole.canManage, PRINC_GRP1);
        addGlobalAssignment(UserRole.canDescribe, PRINC_GRP2);

        evaluator = new GlobalPermissionEvaluator(configProperties);

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
    public void hasGlobalPrincipalTest() {
        addGlobalAssignment(UserRole.canManage, PRINC_GRP1);

        evaluator = new GlobalPermissionEvaluator(configProperties);

        assertTrue(evaluator.hasGlobalPrincipal(principals));
    }

    @Test
    public void noGlobalPrincipalTest() {
        addGlobalAssignment(UserRole.canManage, PRINC_GRP2);

        evaluator = new GlobalPermissionEvaluator(configProperties);

        assertFalse(evaluator.hasGlobalPrincipal(principals));
    }

    @Test
    public void getGlobalUserRoles() {
        addGlobalAssignment(UserRole.canManage, PRINC_GRP1);
        addGlobalAssignment(UserRole.canDescribe, PRINC_GRP2);

        evaluator = new GlobalPermissionEvaluator(configProperties);

        Set<UserRole> roles = evaluator.getGlobalUserRoles(principals);

        assertTrue(roles.contains(UserRole.canManage));
    }

    @Test
    public void getGlobalMultipleUserRoles() {
        principals = new HashSet<>(Arrays.asList(PRINC_GRP1, PRINC_GRP2));

        addGlobalAssignment(UserRole.canManage, PRINC_GRP1);
        addGlobalAssignment(UserRole.canDescribe, PRINC_GRP2);

        evaluator = new GlobalPermissionEvaluator(configProperties);

        Set<UserRole> roles = evaluator.getGlobalUserRoles(principals);

        assertTrue(roles.contains(UserRole.canManage));
        assertTrue(roles.contains(UserRole.canDescribe));
    }

    @Test
    public void getNoGlobalUserRoles() {
        addGlobalAssignment(UserRole.canDescribe, PRINC_GRP2);

        evaluator = new GlobalPermissionEvaluator(configProperties);

        Set<UserRole> roles = evaluator.getGlobalUserRoles(principals);

        assertTrue(roles.isEmpty());
    }

    private void addGlobalAssignment(UserRole role, String principal) {
        configProperties.setProperty(
                GlobalPermissionEvaluator.GLOBAL_PROP_PREFIX + role.name(), principal);
    }
}

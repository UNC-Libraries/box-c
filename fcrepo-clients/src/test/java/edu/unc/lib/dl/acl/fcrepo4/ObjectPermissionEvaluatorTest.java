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

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fedora.PID;

/**
 *
 * @author bbpennel
 *
 */
public class ObjectPermissionEvaluatorTest {

    @Mock
    private ObjectAclFactory aclFactory;
    @Mock
    private PID pid;

    private ObjectPermissionEvaluator evaluator;

    private final static String PRINC_GRP1 = "group1";
    private final static String PRINC_GRP2 = "group2";

    private Set<String> principals;

    private Map<String, Set<String>> objPrincRoles;

    @Before
    public void init() {
        initMocks(this);

        evaluator = new ObjectPermissionEvaluator();
        evaluator.setAclFactory(aclFactory);

        principals = new HashSet<>(Arrays.asList(PRINC_GRP1));
        objPrincRoles = new HashMap<>();
        when(aclFactory.getPrincipalRoles(any(PID.class))).thenReturn(objPrincRoles);
    }

    @Test
    public void hasStaffPermissionTest() throws Exception {
        objPrincRoles.put(PRINC_GRP1, roleSet(UserRole.canManage));

        assertTrue(evaluator
                .hasStaffPermission(pid, principals, Permission.markForDeletion));
    }

    @Test
    public void hasStaffPermissionMultiplePrincipalsTest() throws Exception {
        principals.add(PRINC_GRP2);

        objPrincRoles.put(PRINC_GRP1, roleSet(UserRole.canManage));

        assertTrue(evaluator
                .hasStaffPermission(pid, principals, Permission.editDescription));
    }

    @Test
    public void hasStaffPermissionNoRolesTest() throws Exception {

        assertFalse(evaluator
                .hasStaffPermission(pid, principals, Permission.markForDeletion));
    }

    @Test
    public void hasStaffPermissionDeniedTest() throws Exception {
        principals.add(PRINC_GRP2);

        objPrincRoles.put(PRINC_GRP1, roleSet(UserRole.canManage));

        assertFalse(evaluator
                .hasStaffPermission(pid, principals, Permission.destroy));
    }

    @Test
    public void hasStaffPermissionMultipleRolesTest() throws Exception {
        objPrincRoles.put(PRINC_GRP1, roleSet(UserRole.canView,
                UserRole.canManage));

        assertTrue(evaluator
                .hasStaffPermission(pid, principals, Permission.editDescription));
    }

    @Test(expected = IllegalArgumentException.class)
    public void hasStaffPermissionNullPermissionsTest() throws Exception {
        evaluator.hasStaffPermission(pid, principals, null);
    }

    @Test
    public void getPatronPrincipalsWithPermissionTest() throws Exception {
        objPrincRoles.put(PRINC_GRP1, roleSet(UserRole.canViewMetadata));

        Set<String> permittedPrincipals = evaluator
                .getPatronPrincipalsWithPermission(pid, principals, Permission.viewMetadata);

        assertEquals(1, permittedPrincipals.size());
        assertTrue(permittedPrincipals.contains(PRINC_GRP1));
    }

    @Test
    public void getMultiplePatronPrincipalsWithPermissionTest() throws Exception {
        principals.add(PRINC_GRP2);

        objPrincRoles.put(PRINC_GRP1, roleSet(UserRole.canViewMetadata));

        Set<String> permittedPrincipals = evaluator
                .getPatronPrincipalsWithPermission(pid, principals, Permission.viewMetadata);

        assertEquals(1, permittedPrincipals.size());
        assertTrue(permittedPrincipals.contains(PRINC_GRP1));
    }

    @Test
    public void getPatronPrincipalsWithPermissionMultipleRolesTest() throws Exception {
        principals.add(PRINC_GRP2);

        objPrincRoles.put(PRINC_GRP1, roleSet(
                UserRole.canViewMetadata, UserRole.canViewOriginals));
        objPrincRoles.put(PRINC_GRP2, roleSet(UserRole.canViewMetadata));

        Set<String> permittedPrincipals = evaluator
                .getPatronPrincipalsWithPermission(pid, principals, Permission.viewMetadata);

        assertEquals(2, permittedPrincipals.size());
        assertTrue(permittedPrincipals.contains(PRINC_GRP1));
        assertTrue(permittedPrincipals.contains(PRINC_GRP2));
    }

    @Test
    public void getNoPatronPrincipalsWithPermissionTest() throws Exception {
        principals = new HashSet<>();

        objPrincRoles.put(PRINC_GRP1, roleSet(UserRole.canViewMetadata));

        Set<String> permittedPrincipals = evaluator
                .getPatronPrincipalsWithPermission(pid, principals, Permission.viewMetadata);

        assertEquals(0, permittedPrincipals.size());
    }

    @Test
    public void getPatronPrincipalsWithPermissionNoPatronRolesTest() throws Exception {
        objPrincRoles.put(PRINC_GRP1, roleSet(UserRole.canManage));
        objPrincRoles.put(PRINC_GRP2, roleSet(UserRole.canManage));

        Set<String> permittedPrincipals = evaluator
                .getPatronPrincipalsWithPermission(pid, principals, Permission.viewMetadata);

        assertEquals(0, permittedPrincipals.size());
    }

    @Test
    public void getPatronPrincipalsWithPermissionNoRolesTest() throws Exception {
        Set<String> permittedPrincipals = evaluator
                .getPatronPrincipalsWithPermission(pid, principals, Permission.viewMetadata);

        assertEquals(0, permittedPrincipals.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getPatronPrincipalsWithPermissionNoPidTest() throws Exception {
        evaluator.getPatronPrincipalsWithPermission(null, principals, Permission.viewMetadata);
    }

    @Test
    public void hasPatronAccessNoModificationsTest() throws Exception {
        when(aclFactory.getPatronAccess(any(PID.class))).thenReturn(PatronAccess.parent);

        assertTrue(evaluator.hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void hasPatronAccessNoneTest() throws Exception {
        when(aclFactory.getPatronAccess(any(PID.class))).thenReturn(PatronAccess.none);

        assertFalse(evaluator.hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void hasPatronAccessUnauthenticatedTest() throws Exception {
        when(aclFactory.getPatronAccess(any(PID.class))).thenReturn(PatronAccess.authenticated);

        assertFalse(evaluator.hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void hasPatronAccessAuthenticatedTest() throws Exception {
        principals = new HashSet<>(Arrays.asList(AUTHENTICATED_PRINC));
        when(aclFactory.getPatronAccess(any(PID.class))).thenReturn(PatronAccess.authenticated);

        assertTrue(evaluator.hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void hasPatronAccessDeletedTest() throws Exception {
        when(aclFactory.getPatronAccess(any(PID.class))).thenReturn(PatronAccess.parent);
        when(aclFactory.isMarkedForDeletion(any(PID.class))).thenReturn(true);

        assertFalse(evaluator.hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void hasPatronAccessEmbargoedTest() throws Exception {
        when(aclFactory.getPatronAccess(any(PID.class))).thenReturn(PatronAccess.parent);

        // Set the embargo to tomorrow so that it will not be expired
        Date tomorrow = Date.from(ZonedDateTime.now().plusDays(1).toInstant());
        when(aclFactory.getEmbargoUntil(any(PID.class))).thenReturn(tomorrow);

        assertFalse(evaluator.hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void hasPatronAccessMetadataEmbargoedTest() throws Exception {
        when(aclFactory.getPatronAccess(any(PID.class))).thenReturn(PatronAccess.parent);

        // Set the embargo to tomorrow so that it will not be expired
        Date tomorrow = Date.from(ZonedDateTime.now().plusDays(1).toInstant());
        when(aclFactory.getEmbargoUntil(any(PID.class))).thenReturn(tomorrow);

        assertTrue(evaluator.hasPatronAccess(pid, principals, Permission.viewMetadata));
    }

    @Test
    public void hasPatronAccessExpiredEmbargoTest() throws Exception {
        when(aclFactory.getPatronAccess(any(PID.class))).thenReturn(PatronAccess.parent);

        // Set the embargo to tomorrow so that it will not be expired
        Date yesterday = Date.from(ZonedDateTime.now().plusDays(-1).toInstant());
        when(aclFactory.getEmbargoUntil(any(PID.class))).thenReturn(yesterday);

        assertTrue(evaluator.hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    private static Set<String> roleSet(UserRole... roles) {
        return Arrays.stream(roles)
                .map(p -> p.getPropertyString())
                .collect(Collectors.toSet());
    }
}

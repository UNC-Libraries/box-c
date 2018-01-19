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
import static edu.unc.lib.dl.acl.util.UserRole.canAccess;
import static edu.unc.lib.dl.acl.util.UserRole.canManage;
import static edu.unc.lib.dl.acl.util.UserRole.canViewMetadata;
import static edu.unc.lib.dl.acl.util.UserRole.canViewOriginals;
import static edu.unc.lib.dl.acl.util.UserRole.unitOwner;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.CONTENT_ROOT_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.ContentPathFactory;
import edu.unc.lib.dl.fedora.PID;

public class InheritedAclFactoryTest {

    private static final String PATRON_PRINC = "everyone";
    private static final String MANAGE_PRINC = "manageGrp";
    private static final String OWNER_PRINC = "owner";

    @Mock
    private ContentPathFactory pathFactory;

    @Mock
    private ObjectPermissionEvaluator objectPermissionEvaluator;

    @Mock
    private ObjectAclFactory objectAclFactory;

    private InheritedAclFactory aclFactory;

    private List<PID> ancestorPids;

    private PID pid;

    @Before
    public void init() {
        initMocks(this);

        aclFactory = new InheritedAclFactory();
        aclFactory.setObjectAclFactory(objectAclFactory);
        aclFactory.setObjectPermissionEvaluator(objectPermissionEvaluator);
        aclFactory.setPathFactory(pathFactory);

        ancestorPids = new ArrayList<>();
        ancestorPids.add(PIDs.get(CONTENT_ROOT_ID));

        when(pathFactory.getAncestorPids(any(PID.class))).thenReturn(ancestorPids);

        pid = PIDs.get(UUID.randomUUID().toString());

        Map<String, Set<String>> objPrincRoles = new HashMap<>();
        when(objectAclFactory.getPrincipalRoles(any(PID.class)))
                .thenReturn(objPrincRoles);

        when(objectAclFactory.getPatronAccess(any(PID.class)))
                .thenReturn(PatronAccess.parent);
    }

    @Test
    public void unitBasePrincRolesTest() {
        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertPrincipalHasRoles("Assumed patron assignment should be present for unit",
                princRoles, PATRON_PRINC, canViewOriginals);
    }

    @Test
    public void collectionGetPrincRolesTest() {
        addPidToAncestors();

        Map<String, Set<String>> objPrincRoles = new HashMap<>();
        addPrincipalRoles(objPrincRoles, MANAGE_PRINC, UserRole.canManage);

        when(objectAclFactory.getPrincipalRoles(eq(pid)))
                .thenReturn(objPrincRoles);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertPrincipalHasRoles("Only one role should be present on collection",
                princRoles, MANAGE_PRINC, canManage);
    }

    @Test
    public void collectionInheritedGetPrincRolesTest() {
        PID unitPid = addPidToAncestors();

        Map<String, Set<String>> unitPrincRoles = new HashMap<>();
        addPrincipalRoles(unitPrincRoles, MANAGE_PRINC, UserRole.canManage);

        when(objectAclFactory.getPrincipalRoles(eq(unitPid)))
                .thenReturn(unitPrincRoles);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertPrincipalHasRoles("Only one role should be inherited on collection",
                princRoles, MANAGE_PRINC, canManage);
    }

    @Test
    public void collectionMergedInheritedPrincRolesTest() {
        PID unitPid = addPidToAncestors();

        Map<String, Set<String>> unitPrincRoles = new HashMap<>();
        addPrincipalRoles(unitPrincRoles, MANAGE_PRINC, UserRole.canAccess);
        addPrincipalRoles(unitPrincRoles, OWNER_PRINC, UserRole.unitOwner);
        when(objectAclFactory.getPrincipalRoles(eq(unitPid)))
                .thenReturn(unitPrincRoles);

        Map<String, Set<String>> collPrincRoles = new HashMap<>();
        addPrincipalRoles(collPrincRoles, MANAGE_PRINC, UserRole.canManage);
        when(objectAclFactory.getPrincipalRoles(eq(pid)))
                .thenReturn(collPrincRoles);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertEquals("Incorrect number of principal assignments on collection",
                2, princRoles.size());

        assertPrincipalHasRoles("Incorrect inherited/merged roles for the manger principal",
                princRoles, MANAGE_PRINC, canManage, canAccess);
        assertPrincipalHasRoles("Owner principal not set correctly",
                princRoles, OWNER_PRINC, unitOwner);
    }

    @Test
    public void collectionNoPrincRoles() {
        addPidToAncestors();

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertEquals("No assignments should be present on collection", 0, princRoles.size());
    }

    @Test
    public void contentInheritedRolesTest() {
        PID unitPid = addPidToAncestors();
        PID collPid = addPidToAncestors();

        Map<String, Set<String>> unitPrincRoles = new HashMap<>();
        addPrincipalRoles(unitPrincRoles, OWNER_PRINC, unitOwner);
        when(objectAclFactory.getPrincipalRoles(eq(unitPid)))
                .thenReturn(unitPrincRoles);

        Map<String, Set<String>> collPrincRoles = new HashMap<>();
        addPrincipalRoles(collPrincRoles, MANAGE_PRINC, canManage);
        addPrincipalRoles(collPrincRoles, PATRON_PRINC, canViewMetadata);
        when(objectAclFactory.getPrincipalRoles(eq(collPid)))
                .thenReturn(collPrincRoles);

        when(objectPermissionEvaluator.hasPatronAccess(eq(pid), eq(PATRON_PRINC), any(Permission.class)))
                .thenReturn(true);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertEquals("Incorrect number of principal assignments on content",
                3, princRoles.size());
        assertPrincipalHasRoles("Incorrect inherited roles for the manger principal",
                princRoles, MANAGE_PRINC, canManage);
        assertPrincipalHasRoles("Incorrect inherited patron roles for the patron principal",
                princRoles, PATRON_PRINC, canViewMetadata);
        assertPrincipalHasRoles("Owner principal role not set correctly",
                princRoles, OWNER_PRINC, unitOwner);
    }

    @Test
    public void contentRemovePatronAccessTest() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        Map<String, Set<String>> collPrincRoles = new HashMap<>();
        addPrincipalRoles(collPrincRoles, PATRON_PRINC, canViewMetadata);
        when(objectAclFactory.getPrincipalRoles(eq(collPid)))
                .thenReturn(collPrincRoles);

        when(objectPermissionEvaluator.hasPatronAccess(eq(pid), anyString(), any(Permission.class)))
                .thenReturn(false);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertEquals("All access to content object should be removed", 0, princRoles.size());
    }

    @Test
    public void contentReducePermissionsTest() {
        addPidToAncestors();
        PID collectionPid = addPidToAncestors();

        Map<String, Set<String>> collPrincRoles = new HashMap<>();
        addPrincipalRoles(collPrincRoles, PATRON_PRINC, canViewMetadata);
        addPrincipalRoles(collPrincRoles, AUTHENTICATED_PRINC, canViewOriginals);
        when(objectAclFactory.getPrincipalRoles(eq(collectionPid)))
                .thenReturn(collPrincRoles);

        // revoke one patron but not the other
        when(objectPermissionEvaluator.hasPatronAccess(eq(pid), eq(PATRON_PRINC), any(Permission.class)))
                .thenReturn(false);
        when(objectPermissionEvaluator.hasPatronAccess(eq(pid), eq(AUTHENTICATED_PRINC), any(Permission.class)))
                .thenReturn(true);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertEquals("Only one patron principal should be present", 1, princRoles.size());
        assertPrincipalHasRoles("Authenticated principal should still be assigned",
                princRoles, AUTHENTICATED_PRINC, canViewOriginals);
    }

    @Test
    public void embargoReducedRoleTest() {
        addPidToAncestors();
        PID collectionPid = addPidToAncestors();

        Map<String, Set<String>> collPrincRoles = new HashMap<>();
        addPrincipalRoles(collPrincRoles, PATRON_PRINC, canViewOriginals);
        when(objectAclFactory.getPrincipalRoles(eq(collectionPid)))
                .thenReturn(collPrincRoles);

        when(objectPermissionEvaluator.hasPatronAccess(eq(pid), eq(PATRON_PRINC), eq(Permission.viewMetadata)))
                .thenReturn(true);
        when(objectPermissionEvaluator.hasPatronAccess(eq(pid), eq(PATRON_PRINC), eq(Permission.viewOriginal)))
                .thenReturn(false);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertEquals("Only one patron principal should be present", 1, princRoles.size());
        assertPrincipalHasRoles("Patron should be reduced to have view metadata role",
                princRoles, PATRON_PRINC, canViewMetadata);
    }

    @Test
    public void ignoreRolesAssignedToContentTest() {
        PID unitPid = addPidToAncestors();
        addPidToAncestors();

        Map<String, Set<String>> unitPrincRoles = new HashMap<>();
        addPrincipalRoles(unitPrincRoles, OWNER_PRINC, unitOwner);
        when(objectAclFactory.getPrincipalRoles(eq(unitPid)))
                .thenReturn(unitPrincRoles);

        Map<String, Set<String>> collPrincRoles = new HashMap<>();
        addPrincipalRoles(collPrincRoles, PATRON_PRINC, canViewMetadata);
        when(objectAclFactory.getPrincipalRoles(eq(pid)))
                .thenReturn(collPrincRoles);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertEquals("Only owner should be returned for content object", 1, princRoles.size());
        assertPrincipalHasRoles("Owner principal role not set correctly",
                princRoles, OWNER_PRINC, unitOwner);
    }

    @Test
    public void notMarkedForDeletionTest() {
        addPidToAncestors();

        when(objectAclFactory.isMarkedForDeletion(any(PID.class)))
                .thenReturn(false);

        assertFalse(aclFactory.isMarkedForDeletion(pid));
    }

    @Test
    public void inheritMarkedForDeletionTest() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        when(objectAclFactory.isMarkedForDeletion(eq(collPid)))
                .thenReturn(true);

        assertTrue(aclFactory.isMarkedForDeletion(pid));
    }

    @Test
    public void isMarkedForDeletionTest() {
        addPidToAncestors();
        addPidToAncestors();

        when(objectAclFactory.isMarkedForDeletion(eq(pid)))
                .thenReturn(true);

        assertTrue(aclFactory.isMarkedForDeletion(pid));
    }

    @Test
    public void noEmbargoTest() {
        addPidToAncestors();

        when(objectAclFactory.getEmbargoUntil(any(PID.class))).thenReturn(null);

        assertNull("No embargo should return null", aclFactory.getEmbargoUntil(pid));
    }

    @Test
    public void hasEmbargoTest() {
        addPidToAncestors();

        Date embargoDate = new Date();
        when(objectAclFactory.getEmbargoUntil(eq(pid))).thenReturn(embargoDate);

        assertEquals(embargoDate, aclFactory.getEmbargoUntil(pid));
    }

    @Test
    public void inheritEmbargoTest() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        Date embargoDate = new Date();
        when(objectAclFactory.getEmbargoUntil(eq(collPid))).thenReturn(embargoDate);

        assertEquals(embargoDate, aclFactory.getEmbargoUntil(pid));
    }

    @Test
    public void overrideInheritedEmbargoTest() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        Date collEmbargoDate = new Date();
        when(objectAclFactory.getEmbargoUntil(eq(collPid))).thenReturn(collEmbargoDate);

        Date tomorrowEmbargo = Date.from(ZonedDateTime.now().plusDays(1).toInstant());
        when(objectAclFactory.getEmbargoUntil(eq(pid))).thenReturn(tomorrowEmbargo);

        assertEquals(tomorrowEmbargo, aclFactory.getEmbargoUntil(pid));
    }

    @Test
    public void overrideLocalEmbargoTest() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        Date collEmbargoDate = Date.from(ZonedDateTime.now().plusDays(1).toInstant());
        when(objectAclFactory.getEmbargoUntil(eq(collPid))).thenReturn(collEmbargoDate);

        Date objEmbargoDate = new Date();
        when(objectAclFactory.getEmbargoUntil(eq(pid))).thenReturn(objEmbargoDate);

        assertEquals(collEmbargoDate, aclFactory.getEmbargoUntil(pid));
    }

    @Test
    public void unsetPatronAccess() {
        addPidToAncestors();
        addPidToAncestors();

        assertEquals(PatronAccess.parent, aclFactory.getPatronAccess(pid));
    }

    @Test
    public void inheritNoPatronAccessTest() {
        addPidToAncestors();
        addPidToAncestors();
        PID parentPid = addPidToAncestors();

        when(objectAclFactory.getPatronAccess(eq(parentPid)))
                .thenReturn(PatronAccess.none);

        assertEquals(PatronAccess.none, aclFactory.getPatronAccess(pid));
    }

    @Test
    public void authenticatedPatronAccessTest() {
        addPidToAncestors();
        addPidToAncestors();
        PID parentPid = addPidToAncestors();

        when(objectAclFactory.getPatronAccess(eq(parentPid)))
                .thenReturn(PatronAccess.authenticated);

        assertEquals(PatronAccess.authenticated, aclFactory.getPatronAccess(pid));
    }

    @Test
    public void testRootObjectGetPrincipals() throws Exception {
        PID rootPid = PIDs.get(CONTENT_ROOT_ID);
        // Root has no ancestors
        ancestorPids.clear();

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(rootPid);

        assertEquals(0, princRoles.size());
    }

    private static void assertPrincipalHasRoles(String message, Map<String, Set<String>> princRoles,
            String principal, UserRole... expectedRoles) {
        try {
            Set<String> roles = princRoles.get(principal);
            assertNotNull(roles);
            assertEquals(expectedRoles.length, roles.size());
            for (UserRole expectedRole : expectedRoles) {
                assertTrue(roles.contains(expectedRole.getPropertyString()));
            }
        } catch (Error e) {
            throw new AssertionError(message, e);
        }
    }

    private void addPrincipalRoles(Map<String, Set<String>> objPrincRoles,
            String princ, UserRole... roles) {
        Set<String> roleSet = Arrays.stream(roles)
            .map(r -> r.getPropertyString())
            .collect(Collectors.toSet());
        objPrincRoles.put(princ, roleSet);
    }

    private PID addPidToAncestors() {
        PID ancestor = PIDs.get(UUID.randomUUID().toString());
        ancestorPids.add(ancestor);
        return ancestor;
    }
}

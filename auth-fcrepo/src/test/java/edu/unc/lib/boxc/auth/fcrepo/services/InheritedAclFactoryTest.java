package edu.unc.lib.boxc.auth.fcrepo.services;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PATRON_NAMESPACE;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.auth.api.UserRole.canAccess;
import static edu.unc.lib.boxc.auth.api.UserRole.canManage;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewAccessCopies;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewMetadata;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewOriginals;
import static edu.unc.lib.boxc.auth.api.UserRole.unitOwner;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.CONTENT_ROOT_ID;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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

import edu.unc.lib.boxc.auth.api.UserRole;
import edu.unc.lib.boxc.auth.api.models.RoleAssignment;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

public class InheritedAclFactoryTest {

    private static final String MANAGE_PRINC = "manageGrp";
    private static final String OWNER_PRINC = "owner";
    private static final String PATRON_GROUP = PATRON_NAMESPACE + "special";

    @Mock
    private ContentPathFactory pathFactory;

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
        aclFactory.setPathFactory(pathFactory);

        ancestorPids = new ArrayList<>();
        ancestorPids.add(PIDs.get(CONTENT_ROOT_ID));

        when(pathFactory.getAncestorPids(any(PID.class))).thenReturn(ancestorPids);

        pid = PIDs.get(UUID.randomUUID().toString());
    }

    @Test
    public void unitBasePrincRoles() {
        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertPrincipalHasRoles("Assumed patron assignment should be present for unit",
                princRoles, PUBLIC_PRINC, canViewOriginals);
    }

    @Test
    public void collectionGetPrincRoles() {
        addPidToAncestors();

        addPrincipalRoles(pid, MANAGE_PRINC, UserRole.canManage);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertPrincipalHasRoles("Only one role should be present on collection",
                princRoles, MANAGE_PRINC, canManage);
    }

    @Test
    public void collectionInheritedGetPrincRoles() {
        PID unitPid = addPidToAncestors();

        addPrincipalRoles(unitPid, MANAGE_PRINC, UserRole.canManage);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertPrincipalHasRoles("Only one role should be inherited on collection",
                princRoles, MANAGE_PRINC, canManage);
    }

    @Test
    public void collectionMergedInheritedPrincRoles() {
        PID unitPid = addPidToAncestors();

        addPrincipalRoles(unitPid, MANAGE_PRINC, UserRole.canAccess);
        addPrincipalRoles(unitPid, OWNER_PRINC, UserRole.unitOwner);

        addPrincipalRoles(pid, MANAGE_PRINC, UserRole.canManage);

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
    public void contentInheritedRoles() {
        PID unitPid = addPidToAncestors();
        PID collPid = addPidToAncestors();

        addPrincipalRoles(unitPid, OWNER_PRINC, unitOwner);

        addPrincipalRoles(collPid, MANAGE_PRINC, canManage);
        addPrincipalRoles(collPid, PUBLIC_PRINC, canViewMetadata);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertEquals("Incorrect number of principal assignments on content",
                3, princRoles.size());
        assertPrincipalHasRoles("Incorrect inherited roles for the manger principal",
                princRoles, MANAGE_PRINC, canManage);
        assertPrincipalHasRoles("Incorrect inherited patron roles for the patron principal",
                princRoles, PUBLIC_PRINC, canViewMetadata);
        assertPrincipalHasRoles("Owner principal role not set correctly",
                princRoles, OWNER_PRINC, unitOwner);
    }

    @Test
    public void contentRemovePatronAccess() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        addPrincipalRoles(collPid, PUBLIC_PRINC, canViewMetadata);

        addPrincipalRoles(pid, PUBLIC_PRINC, UserRole.none);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertEquals("All access to content object should be removed", 0, princRoles.size());
    }

    @Test
    public void contentInheritNoPatronAccess() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        addPrincipalRoles(collPid, PUBLIC_PRINC, UserRole.none);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertEquals("All access to content object should be removed", 0, princRoles.size());
    }

    @Test
    public void contentReducePermissions() {
        addPidToAncestors();
        PID collectionPid = addPidToAncestors();

        addPrincipalRoles(collectionPid, PUBLIC_PRINC, canViewMetadata);
        addPrincipalRoles(collectionPid, AUTHENTICATED_PRINC, canViewOriginals);

        // Revoke one patron
        addPrincipalRoles(pid, PUBLIC_PRINC, UserRole.none);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertEquals("Only one patron principal should be present", 1, princRoles.size());
        assertPrincipalHasRoles("Authenticated principal should still be assigned",
                princRoles, AUTHENTICATED_PRINC, canViewOriginals);
    }

    @Test
    public void embargoDoesNotAffectRole() {
        addPidToAncestors();
        PID collectionPid = addPidToAncestors();

        addPrincipalRoles(collectionPid, PUBLIC_PRINC, canViewOriginals);

        when(objectAclFactory.getEmbargoUntil(pid)).thenReturn(getNextYear());

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertEquals("Only one patron principal should be present", 1, princRoles.size());
        assertPrincipalHasRoles("Embargo should not impact the roles returned",
                princRoles, PUBLIC_PRINC, canViewOriginals);
    }

    @Test
    public void ignoreRolesAssignedToContent() {
        PID unitPid = addPidToAncestors();
        addPidToAncestors();

        addPrincipalRoles(unitPid, OWNER_PRINC, unitOwner);

        addPrincipalRoles(pid, PUBLIC_PRINC, canViewMetadata);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertEquals("Only owner should be returned for content object", 1, princRoles.size());
        assertPrincipalHasRoles("Owner principal role not set correctly",
                princRoles, OWNER_PRINC, unitOwner);
    }

    @Test
    public void contentInheritedPatronGroup() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        addPrincipalRoles(collPid, PATRON_GROUP, canViewOriginals);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertEquals("Incorrect number of principal assignments on content",
                1, princRoles.size());
        assertPrincipalHasRoles("Incorrect inherited patron roles for the patron group",
                princRoles, PATRON_GROUP, canViewOriginals);
    }

    @Test
    public void contentDowngradedPatronGroup() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        addPrincipalRoles(collPid, PATRON_GROUP, canViewOriginals);
        addPrincipalRoles(pid, PATRON_GROUP, canViewMetadata);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertEquals("Incorrect number of principal assignments on content",
                1, princRoles.size());
        assertPrincipalHasRoles("Incorrect inherited patron roles for the patron group",
                princRoles, PATRON_GROUP, canViewMetadata);
    }

    @Test
    public void contentRevokedPatronGroup() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        addPrincipalRoles(collPid, PATRON_GROUP, canViewOriginals);
        addPrincipalRoles(pid, PATRON_GROUP, UserRole.none);

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(pid);

        assertEquals("Incorrect number of principal assignments on content",
                0, princRoles.size());
    }

    @Test
    public void notMarkedForDeletion() {
        addPidToAncestors();

        when(objectAclFactory.isMarkedForDeletion(any(PID.class)))
                .thenReturn(false);

        assertFalse(aclFactory.isMarkedForDeletion(pid));
    }

    @Test
    public void inheritMarkedForDeletion() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        when(objectAclFactory.isMarkedForDeletion(eq(collPid)))
                .thenReturn(true);

        assertTrue(aclFactory.isMarkedForDeletion(pid));
    }

    @Test
    public void isMarkedForDeletion() {
        addPidToAncestors();
        addPidToAncestors();

        when(objectAclFactory.isMarkedForDeletion(eq(pid)))
                .thenReturn(true);

        assertTrue(aclFactory.isMarkedForDeletion(pid));
    }

    @Test
    public void noEmbargo() {
        addPidToAncestors();

        when(objectAclFactory.getEmbargoUntil(any(PID.class))).thenReturn(null);

        assertNull("No embargo should return null", aclFactory.getEmbargoUntil(pid));
    }

    @Test
    public void hasEmbargo() {
        addPidToAncestors();

        Date embargoDate = new Date();
        when(objectAclFactory.getEmbargoUntil(eq(pid))).thenReturn(embargoDate);

        assertEquals(embargoDate, aclFactory.getEmbargoUntil(pid));
    }

    @Test
    public void inheritEmbargo() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        Date embargoDate = new Date();
        when(objectAclFactory.getEmbargoUntil(eq(collPid))).thenReturn(embargoDate);

        assertEquals(embargoDate, aclFactory.getEmbargoUntil(pid));
    }

    @Test
    public void overrideInheritedEmbargo() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        Date collEmbargoDate = new Date();
        when(objectAclFactory.getEmbargoUntil(eq(collPid))).thenReturn(collEmbargoDate);

        Date tomorrowEmbargo = Date.from(ZonedDateTime.now().plusDays(1).toInstant());
        when(objectAclFactory.getEmbargoUntil(eq(pid))).thenReturn(tomorrowEmbargo);

        assertEquals(tomorrowEmbargo, aclFactory.getEmbargoUntil(pid));
    }

    @Test
    public void overrideLocalEmbargo() {
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

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertTrue("No patron assignments expected", assignments.isEmpty());
    }

    @Test
    public void patronAccessPatronSetAfterCollection() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();
        PID parentPid = addPidToAncestors();

        addPrincipalRoles(collPid, AUTHENTICATED_PRINC, canViewOriginals);
        addPrincipalRoles(parentPid, PUBLIC_PRINC, canViewMetadata);

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(1, assignments.size());

        // Only the inherited authenticated principal should be present
        RoleAssignment assignment = assignments.get(0);
        assertEquals(canViewOriginals, assignment.getRole());
        assertEquals(AUTHENTICATED_PRINC, assignment.getPrincipal());
    }

    @Test
    public void patronAccessCollection() {
        addPidToAncestors();

        addPrincipalRoles(pid, PUBLIC_PRINC, canViewMetadata);

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(1, assignments.size());

        RoleAssignment assignment = assignments.get(0);
        assertEquals(canViewMetadata, assignment.getRole());
        assertEquals(PUBLIC_PRINC, assignment.getPrincipal());
    }

    @Test
    public void patronAccessInherited() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        addPrincipalRoles(collPid, PUBLIC_PRINC, canViewOriginals);

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(1, assignments.size());

        RoleAssignment assignment = assignments.get(0);
        assertEquals(canViewOriginals, assignment.getRole());
        assertEquals(PUBLIC_PRINC, assignment.getPrincipal());
    }

    @Test
    public void patronAccessDowngrade() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();
        PID parentPid = addPidToAncestors();

        addPrincipalRoles(collPid, PUBLIC_PRINC, canViewOriginals);
        addPrincipalRoles(parentPid, PUBLIC_PRINC, canViewMetadata);

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(1, assignments.size());

        RoleAssignment assignment = assignments.get(0);
        assertEquals(canViewMetadata, assignment.getRole());
        assertEquals(PUBLIC_PRINC, assignment.getPrincipal());
    }

    @Test
    public void patronAccessRevoked() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        addPrincipalRoles(collPid, PUBLIC_PRINC, canViewOriginals);
        addPrincipalRoles(pid, PUBLIC_PRINC, UserRole.none);

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertTrue("All assignments should be revoked", assignments.isEmpty());
    }

    @Test
    public void patronAccessInheritRevoked() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();
        PID parentPid = addPidToAncestors();

        addPrincipalRoles(collPid, PUBLIC_PRINC, canViewOriginals);
        addPrincipalRoles(parentPid, PUBLIC_PRINC, UserRole.none);

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertTrue("All assignments should be revoked", assignments.isEmpty());
    }

    @Test
    public void patronAccessMultiplePrincipals() {
        addPidToAncestors();

        addPrincipalRoles(pid, PUBLIC_PRINC, canViewMetadata);
        addPrincipalRoles(pid, AUTHENTICATED_PRINC, canViewOriginals);

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(2, assignments.size());

        RoleAssignment assignment1 = getAssignmentByPrincipal(assignments, PUBLIC_PRINC);
        assertEquals(canViewMetadata, assignment1.getRole());
        RoleAssignment assignment2 = getAssignmentByPrincipal(assignments, AUTHENTICATED_PRINC);
        assertEquals(canViewOriginals, assignment2.getRole());
    }

    @Test
    public void patronAccessActiveEmbargo() {
        addPidToAncestors();

        addPrincipalRoles(pid, PUBLIC_PRINC, canViewOriginals);
        addPrincipalRoles(pid, AUTHENTICATED_PRINC, canViewOriginals);

        when(objectAclFactory.getEmbargoUntil(pid)).thenReturn(getNextYear());

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(2, assignments.size());

        RoleAssignment assignment1 = getAssignmentByPrincipal(assignments, PUBLIC_PRINC);
        assertEquals(canViewMetadata, assignment1.getRole());
        RoleAssignment assignment2 = getAssignmentByPrincipal(assignments, AUTHENTICATED_PRINC);
        assertEquals(canViewMetadata, assignment2.getRole());
    }

    @Test
    public void patronAccessEmbargoMetadataAssigned() {
        addPidToAncestors();

        addPrincipalRoles(pid, PUBLIC_PRINC, canViewMetadata);

        when(objectAclFactory.getEmbargoUntil(pid)).thenReturn(getNextYear());

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(1, assignments.size());

        RoleAssignment assignment1 = getAssignmentByPrincipal(assignments, PUBLIC_PRINC);
        assertEquals(canViewMetadata, assignment1.getRole());
    }

    @Test
    public void patronAccessExpiredEmbargo() {
        addPidToAncestors();

        addPrincipalRoles(pid, PUBLIC_PRINC, canViewOriginals);
        addPrincipalRoles(pid, AUTHENTICATED_PRINC, canViewOriginals);

        when(objectAclFactory.getEmbargoUntil(pid)).thenReturn(getLastYear());

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(2, assignments.size());

        RoleAssignment assignment1 = getAssignmentByPrincipal(assignments, PUBLIC_PRINC);
        assertEquals(canViewOriginals, assignment1.getRole());
        RoleAssignment assignment2 = getAssignmentByPrincipal(assignments, AUTHENTICATED_PRINC);
        assertEquals(canViewOriginals, assignment2.getRole());
    }

    @Test
    public void patronAccessInheritedEmbargo() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        addPrincipalRoles(collPid, PUBLIC_PRINC, canViewOriginals);

        when(objectAclFactory.getEmbargoUntil(collPid)).thenReturn(getNextYear());

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(1, assignments.size());

        RoleAssignment assignment = getAssignmentByPrincipal(assignments, PUBLIC_PRINC);
        assertEquals(canViewMetadata, assignment.getRole());
    }

    @Test
    public void patronAccessDirectDelete() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        addPrincipalRoles(collPid, PUBLIC_PRINC, canViewOriginals);
        addPrincipalRoles(collPid, AUTHENTICATED_PRINC, canViewOriginals);

        when(objectAclFactory.isMarkedForDeletion(pid)).thenReturn(true);

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(0, assignments.size());
    }

    @Test
    public void patronAccessInheritedDelete() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        addPrincipalRoles(collPid, PUBLIC_PRINC, canViewOriginals);
        addPrincipalRoles(collPid, AUTHENTICATED_PRINC, canViewOriginals);

        when(objectAclFactory.isMarkedForDeletion(collPid)).thenReturn(true);

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(0, assignments.size());
    }

    @Test
    public void patronAccessGroupInheritedFromCollection() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        addPrincipalRoles(collPid, PUBLIC_PRINC, canViewMetadata);
        addPrincipalRoles(collPid, AUTHENTICATED_PRINC, canViewAccessCopies);
        addPrincipalRoles(collPid, PATRON_GROUP, canViewOriginals);
        addPrincipalRoles(collPid, PATRON_NAMESPACE + "second", canViewAccessCopies);

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(4, assignments.size());

        RoleAssignment assignment1 = getAssignmentByPrincipal(assignments, PUBLIC_PRINC);
        assertEquals(canViewMetadata, assignment1.getRole());
        RoleAssignment assignment2 = getAssignmentByPrincipal(assignments, AUTHENTICATED_PRINC);
        assertEquals(canViewAccessCopies, assignment2.getRole());
        RoleAssignment assignment3 = getAssignmentByPrincipal(assignments, PATRON_GROUP);
        assertEquals(canViewOriginals, assignment3.getRole());
        RoleAssignment assignment4 = getAssignmentByPrincipal(assignments, PATRON_NAMESPACE + "second");
        assertEquals(canViewAccessCopies, assignment4.getRole());
    }

    @Test
    public void patronAccessOnlyGroupOnCollection() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        addPrincipalRoles(collPid, PATRON_GROUP, canViewOriginals);

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(1, assignments.size());

        RoleAssignment assignment = assignments.get(0);
        assertEquals(canViewOriginals, assignment.getRole());
        assertEquals(PATRON_GROUP, assignment.getPrincipal());
    }

    @Test
    public void patronAccessGroupDowngraded() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();
        PID parentPid = addPidToAncestors();

        addPrincipalRoles(collPid, PATRON_GROUP, canViewOriginals);
        addPrincipalRoles(parentPid, PATRON_GROUP, canViewMetadata);

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(1, assignments.size());

        RoleAssignment assignment = assignments.get(0);
        assertEquals(canViewMetadata, assignment.getRole());
        assertEquals(PATRON_GROUP, assignment.getPrincipal());
    }

    @Test
    public void patronAccessGroupFromCollectionStaffOnlyOnChild() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        addPrincipalRoles(collPid, PUBLIC_PRINC, canViewMetadata);
        addPrincipalRoles(collPid, AUTHENTICATED_PRINC, canViewAccessCopies);
        addPrincipalRoles(collPid, PATRON_GROUP, canViewOriginals);

        addPrincipalRoles(pid, PUBLIC_PRINC, UserRole.none);
        addPrincipalRoles(pid, AUTHENTICATED_PRINC, UserRole.none);

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(0, assignments.size());
    }

    @Test
    public void patronAccessGroupRegrantedOnStaffOnlyChild() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        addPrincipalRoles(collPid, PUBLIC_PRINC, canViewMetadata);
        addPrincipalRoles(collPid, AUTHENTICATED_PRINC, canViewAccessCopies);
        addPrincipalRoles(collPid, PATRON_GROUP, canViewOriginals);
        addPrincipalRoles(collPid, PATRON_NAMESPACE + "notreupped", canViewOriginals);

        addPrincipalRoles(pid, PUBLIC_PRINC, UserRole.none);
        addPrincipalRoles(pid, AUTHENTICATED_PRINC, UserRole.none);
        addPrincipalRoles(pid, PATRON_GROUP, canViewOriginals);

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(1, assignments.size());

        RoleAssignment assignment = assignments.get(0);
        assertEquals(canViewOriginals, assignment.getRole());
        assertEquals(PATRON_GROUP, assignment.getPrincipal());
    }

    @Test
    public void patronAccessGroupOnlyOnChild() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        addPrincipalRoles(collPid, PUBLIC_PRINC, canViewMetadata);
        addPrincipalRoles(collPid, AUTHENTICATED_PRINC, canViewAccessCopies);

        addPrincipalRoles(pid, PATRON_GROUP, canViewOriginals);

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(2, assignments.size());

        RoleAssignment assignment1 = getAssignmentByPrincipal(assignments, PUBLIC_PRINC);
        assertEquals(canViewMetadata, assignment1.getRole());
        RoleAssignment assignment2 = getAssignmentByPrincipal(assignments, AUTHENTICATED_PRINC);
        assertEquals(canViewAccessCopies, assignment2.getRole());
    }

    @Test
    public void patronAccessGroupInheritedDelete() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();
        PID parentPid = addPidToAncestors();

        addPrincipalRoles(collPid, PUBLIC_PRINC, canViewOriginals);
        addPrincipalRoles(collPid, AUTHENTICATED_PRINC, canViewOriginals);
        addPrincipalRoles(collPid, PATRON_GROUP, canViewOriginals);

        when(objectAclFactory.isMarkedForDeletion(parentPid)).thenReturn(true);

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(0, assignments.size());
    }

    @Test
    public void patronAccessGroupEmbargoed() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();
        PID parentPid = addPidToAncestors();

        addPrincipalRoles(collPid, PATRON_GROUP, canViewOriginals);

        when(objectAclFactory.getEmbargoUntil(parentPid)).thenReturn(getNextYear());

        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        assertEquals(1, assignments.size());

        RoleAssignment assignment = assignments.get(0);
        assertEquals(canViewMetadata, assignment.getRole());
        assertEquals(PATRON_GROUP, assignment.getPrincipal());
    }

    @Test
    public void rootObjectGetPrincipals() throws Exception {
        PID rootPid = PIDs.get(CONTENT_ROOT_ID);
        // Root has no ancestors
        ancestorPids.clear();

        Map<String, Set<String>> princRoles = aclFactory.getPrincipalRoles(rootPid);

        assertEquals(0, princRoles.size());
    }

    @Test
    public void getStaffRoleAssignmentsDirectAssignment() {
        PID unitPid = makePid();

        mockObjStaffRoleAssignments(new RoleAssignment(OWNER_PRINC, UserRole.unitOwner, unitPid));

        List<RoleAssignment> assignments = aclFactory.getStaffRoleAssignments(unitPid);
        assertEquals(1, assignments.size());

        RoleAssignment assignment = getAssignmentByPidAndRole(assignments, unitPid, UserRole.unitOwner);
        assertEquals(OWNER_PRINC, assignment.getPrincipal());
    }

    @Test
    public void getStaffRoleAssignmentsInheritedAndDirectAssignment() {
        PID unitPid = addPidToAncestors();
        PID collPid = makePid();

        mockObjStaffRoleAssignments(new RoleAssignment(OWNER_PRINC, UserRole.unitOwner, unitPid));
        mockObjStaffRoleAssignments(new RoleAssignment(MANAGE_PRINC, UserRole.canManage, collPid));

        List<RoleAssignment> assignments = aclFactory.getStaffRoleAssignments(collPid);
        assertEquals(2, assignments.size());

        RoleAssignment assignment1 = getAssignmentByPidAndRole(assignments, unitPid, UserRole.unitOwner);
        assertEquals(OWNER_PRINC, assignment1.getPrincipal());

        RoleAssignment assignment2 = getAssignmentByPidAndRole(assignments, collPid, UserRole.canManage);
        assertEquals(MANAGE_PRINC, assignment2.getPrincipal());
    }

    @Test
    public void getStaffRoleAssignmentsInheritedOnFolder() {
        PID unitPid = addPidToAncestors();
        PID collPid = addPidToAncestors();
        PID folderPid = makePid();

        mockObjStaffRoleAssignments(new RoleAssignment(OWNER_PRINC, UserRole.unitOwner, unitPid));
        mockObjStaffRoleAssignments(new RoleAssignment(MANAGE_PRINC, UserRole.canManage, collPid));

        List<RoleAssignment> assignments = aclFactory.getStaffRoleAssignments(folderPid);
        assertEquals(2, assignments.size());

        RoleAssignment assignment1 = getAssignmentByPidAndRole(assignments, unitPid, UserRole.unitOwner);
        assertEquals(OWNER_PRINC, assignment1.getPrincipal());

        RoleAssignment assignment2 = getAssignmentByPidAndRole(assignments, collPid, UserRole.canManage);
        assertEquals(MANAGE_PRINC, assignment2.getPrincipal());
    }

    @Test
    public void getStaffRoleAssignmentsNoAssignments() {
        addPidToAncestors();
        PID collPid = makePid();

        List<RoleAssignment> assignments = aclFactory.getStaffRoleAssignments(collPid);
        assertTrue(assignments.isEmpty());
    }

    @Test
    public void getStaffRoleAssignmentsMultipleInheritedAndDirectAssignment() {
        PID unitPid = addPidToAncestors();
        PID collPid = makePid();

        mockObjStaffRoleAssignments(
                new RoleAssignment(OWNER_PRINC, UserRole.unitOwner, unitPid),
                new RoleAssignment(MANAGE_PRINC, UserRole.canAccess, unitPid));
        mockObjStaffRoleAssignments(new RoleAssignment(MANAGE_PRINC, UserRole.canManage, collPid));

        List<RoleAssignment> assignments = aclFactory.getStaffRoleAssignments(collPid);
        assertEquals(3, assignments.size());

        RoleAssignment assignment1 = getAssignmentByPidAndRole(assignments, unitPid, UserRole.unitOwner);
        assertEquals(OWNER_PRINC, assignment1.getPrincipal());

        RoleAssignment assignment2 = getAssignmentByPidAndRole(assignments, unitPid, UserRole.canAccess);
        assertEquals(MANAGE_PRINC, assignment2.getPrincipal());

        RoleAssignment assignment3 = getAssignmentByPidAndRole(assignments, collPid, UserRole.canManage);
        assertEquals(MANAGE_PRINC, assignment3.getPrincipal());
    }

    @Test
    public void getPatronRoleAssignmentsDirect() {
        addPidToAncestors();
        PID collPid = makePid();

        mockObjPatronRoleAssignments(new RoleAssignment(PUBLIC_PRINC, UserRole.canViewOriginals, collPid));

        List<RoleAssignment> assignments = aclFactory.getPatronRoleAssignments(collPid);
        assertEquals(1, assignments.size());

        RoleAssignment assignment = getAssignmentByPidAndRole(assignments, collPid, UserRole.canViewOriginals);
        assertEquals(PUBLIC_PRINC, assignment.getPrincipal());
    }

    @Test
    public void getPatronRoleAssignmentsInherited() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();
        PID folderPid = makePid();

        mockObjPatronRoleAssignments(new RoleAssignment(PUBLIC_PRINC, UserRole.canViewAccessCopies, collPid),
                new RoleAssignment(AUTHENTICATED_PRINC, UserRole.canViewOriginals, collPid));
        mockObjPatronRoleAssignments(new RoleAssignment(PUBLIC_PRINC, UserRole.canViewMetadata, folderPid));

        List<RoleAssignment> assignments = aclFactory.getPatronRoleAssignments(folderPid);
        assertEquals(3, assignments.size());

        RoleAssignment assignment1 = getAssignmentByPidAndRole(assignments, collPid, UserRole.canViewAccessCopies);
        assertEquals(PUBLIC_PRINC, assignment1.getPrincipal());
        RoleAssignment assignment2 = getAssignmentByPidAndRole(assignments, collPid, UserRole.canViewOriginals);
        assertEquals(AUTHENTICATED_PRINC, assignment2.getPrincipal());

        RoleAssignment assignment3 = getAssignmentByPidAndRole(assignments, folderPid, UserRole.canViewMetadata);
        assertEquals(PUBLIC_PRINC, assignment3.getPrincipal());
    }

    @Test
    public void getPatronRoleAssignmentsNone() {
        addPidToAncestors();
        PID collPid = makePid();

        List<RoleAssignment> assignments = aclFactory.getPatronRoleAssignments(collPid);
        assertTrue("No assignments should be returned", assignments.isEmpty());
    }

    private RoleAssignment getAssignmentByPrincipal(List<RoleAssignment> assignments, String principal) {
        return assignments.stream()
                .filter(a -> a.getPrincipal().equals(principal))
                .findFirst()
                .orElse(null);
    }

    private RoleAssignment getAssignmentByPidAndRole(List<RoleAssignment> assignments, PID pid, UserRole role) {
        return assignments.stream()
                .filter(a -> a.getRole().equals(role) && a.getAssignedTo().equals(pid.getId()))
                .findFirst()
                .orElse(null);
    }

    private void mockObjStaffRoleAssignments(RoleAssignment... assignments) {
        PID pid = PIDs.get(assignments[0].getAssignedTo());
        List<RoleAssignment> assigned = asList(assignments);
        when(objectAclFactory.getStaffRoleAssignments(pid)).thenReturn(assigned);
    }

    private void mockObjPatronRoleAssignments(RoleAssignment... assignments) {
        PID pid = PIDs.get(assignments[0].getAssignedTo());
        List<RoleAssignment> assigned = asList(assignments);
        when(objectAclFactory.getPatronRoleAssignments(pid)).thenReturn(assigned);
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

    private void addPrincipalRoles(PID pid, String principal, UserRole... roles) {
        Map<String, Set<String>> princRoles = objectAclFactory.getPrincipalRoles(pid);
        if (princRoles == null || princRoles.isEmpty()) {
            princRoles = new HashMap<>();
            when(objectAclFactory.getPrincipalRoles(pid)).thenReturn(princRoles);
        }

        princRoles.put(principal, Arrays.stream(roles)
                .map(UserRole::getPropertyString)
                .collect(Collectors.toSet()));
    }

    private PID addPidToAncestors() {
        PID ancestor = makePid();
        ancestorPids.add(ancestor);
        return ancestor;
    }

    private PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

    private Date getNextYear() {
        Date dt = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        c.add(Calendar.DATE, 365);
        return c.getTime();
    }

    private Date getLastYear() {
        Date dt = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        c.add(Calendar.DATE, -365);
        return c.getTime();
    }
}

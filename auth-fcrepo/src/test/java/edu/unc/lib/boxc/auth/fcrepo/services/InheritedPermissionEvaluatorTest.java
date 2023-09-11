package edu.unc.lib.boxc.auth.fcrepo.services;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PATRON_NAMESPACE;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.auth.api.UserRole.canIngest;
import static edu.unc.lib.boxc.auth.api.UserRole.canManage;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewAccessCopies;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewMetadata;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewOriginals;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.CONTENT_ROOT_ID;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.UserRole;
import edu.unc.lib.boxc.model.api.exceptions.OrphanedObjectException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;

public class InheritedPermissionEvaluatorTest {

    private static final String PATRON_PRINC = "everyone";
    private static final String STAFF_PRINC = "adminGrp";
    private static final String PATRON_GROUP = PATRON_NAMESPACE + "special";

    private static final Set<String> PATRON_PRINCIPLES = new HashSet<>(
            asList(PUBLIC_PRINC));
    private static final Set<String> AUTH_PRINCIPLES = new HashSet<>(
            asList(PUBLIC_PRINC, AUTHENTICATED_PRINC));
    private static final Set<String> STAFF_PRINCIPLES = new HashSet<>(
            asList(PUBLIC_PRINC, AUTHENTICATED_PRINC, STAFF_PRINC));
    private static final Set<String> PATRON_GROUP_PRINCIPLES = new HashSet<>(
            asList(PUBLIC_PRINC, PATRON_GROUP));

    private AutoCloseable closeable;

    @Mock
    private ContentPathFactory pathFactory;

    @Mock
    private ObjectAclFactory objectAclFactory;

    private InheritedPermissionEvaluator evaluator;

    private List<PID> ancestorPids;

    private PID pid;

    @BeforeEach
    public void init() {
        closeable = openMocks(this);

        evaluator = new InheritedPermissionEvaluator();
        evaluator.setPathFactory(pathFactory);
        evaluator.setObjectAclFactory(objectAclFactory);

        ancestorPids = new ArrayList<>();
        ancestorPids.add(PIDs.get(CONTENT_ROOT_ID));

        when(pathFactory.getAncestorPids(any(PID.class))).thenReturn(ancestorPids);

        pid = PIDs.get(UUID.randomUUID().toString());
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void unitHasPatronPermissionTest() {
        assertTrue(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void unitPatronRequestingStaffTest() {
        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.ingest));
    }

    @Test
    public void unitHasStaffPermission() {
        mockFactoryPrincipalRoles(pid, STAFF_PRINC, canIngest);

        assertTrue(evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.ingest));
    }

    @Test
    public void unitNoStaffPermissions() {
        assertFalse(evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.destroy));
    }

    @Test
    public void unitInsufficientPermissions() {
        mockFactoryPrincipalRoles(pid, STAFF_PRINC, canIngest);

        assertFalse(evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.destroy));
    }

    @Test
    public void unitHasPatronPermission() {
        assertTrue(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void unitHasPatronPermissionDeletedTest() {
        when(objectAclFactory.isMarkedForDeletion(pid)).thenReturn(true);

        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void collectionHasPatron() {
        // Add unit pid into ancestors
        addPidToAncestors();

        mockFactoryPrincipalRoles(pid, PATRON_PRINC, canViewMetadata);

        assertTrue(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void collectionMultiplePatronsHasPermissions() {
        // Add unit pid into ancestors
        addPidToAncestors();

        mockFactoryPrincipalRoles(pid, PATRON_PRINC, canViewMetadata);
        mockFactoryPrincipalRoles(pid, AUTHENTICATED_PRINC, canViewOriginals);

        assertTrue(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewOriginal));
        assertTrue(evaluator.hasPermission(pid, AUTH_PRINCIPLES, Permission.viewOriginal));
    }

    @Test
    public void collectionPatronGroupHasPermissions() {
        // Add unit pid into ancestors
        addPidToAncestors();

        mockFactoryPrincipalRoles(pid, PATRON_PRINC, canViewMetadata);
        mockFactoryPrincipalRoles(pid, PATRON_GROUP, canViewOriginals);

        assertTrue(evaluator.hasPermission(pid, PATRON_GROUP_PRINCIPLES, Permission.viewOriginal));
    }

    @Test
    public void collectionInsufficientPermissions() {
        addPidToAncestors();

        mockFactoryPrincipalRoles(pid, PATRON_PRINC, canViewMetadata);

        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewOriginal));
    }

    @Test
    public void collectionNoPatronsNoPatronAccess() {
        addPidToAncestors();

        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewOriginal));
    }

    @Test
    public void collectionPatronRevokedNoPatronPermission() {
        addPidToAncestors();

        mockFactoryPrincipalRoles(pid, PATRON_PRINC, UserRole.none);

        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewOriginal));
    }

    @Test
    public void collectionDeletedNoPatronPermissions() {
        addPidToAncestors();

        when(objectAclFactory.isMarkedForDeletion(pid)).thenReturn(true);
        mockFactoryPrincipalRoles(pid, PATRON_PRINC, canViewMetadata);

        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void collectionDeletedHasPatronPermissionForStaff() {
        addPidToAncestors();

        when(objectAclFactory.isMarkedForDeletion(pid)).thenReturn(true);
        mockFactoryPrincipalRoles(pid, STAFF_PRINC, canManage);

        assertTrue(evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.viewOriginal));
    }

    @Test
    public void collectionDeletedHasStaffPermission() {
        addPidToAncestors();

        when(objectAclFactory.isMarkedForDeletion(pid)).thenReturn(true);
        mockFactoryPrincipalRoles(pid, STAFF_PRINC, canManage);

        assertTrue(evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.move));
    }

    @Test
    public void collectionEmbargoedPatronMetadataOnly() {
        addPidToAncestors();

        when(objectAclFactory.getEmbargoUntil(pid)).thenReturn(getNextYear());
        mockFactoryPrincipalRoles(pid, PUBLIC_PRINC, canViewOriginals);

        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewOriginal));
        assertTrue(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void collectionExpiredEmbargoedHasPatronViewOriginal() {
        addPidToAncestors();

        when(objectAclFactory.getEmbargoUntil(pid)).thenReturn(getLastYear());
        mockFactoryPrincipalRoles(pid, PUBLIC_PRINC, canViewOriginals);

        assertTrue(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewOriginal));
    }

    @Test
    public void collectionEmbargoedStaffHavePermission() {
        addPidToAncestors();

        when(objectAclFactory.getEmbargoUntil(pid)).thenReturn(getNextYear());
        mockFactoryPrincipalRoles(pid, STAFF_PRINC, canManage);

        assertTrue(evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.viewOriginal));
        assertTrue(evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.editDescription));
    }

    @Test
    public void collectionHasStaffPermission() {

        addPidToAncestors();

        mockFactoryPrincipalRoles(pid, STAFF_PRINC, canIngest);

        assertTrue(evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.ingest));
    }

    @Test
    public void collectionNoStaffPermission() {

        addPidToAncestors();

        assertFalse(evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.ingest));
    }

    @Test
    public void collectionInheritedStaffPermission() {
        PID unitPid = addPidToAncestors();

        // Grant permission on the unit rather than the collection
        mockFactoryPrincipalRoles(unitPid, STAFF_PRINC, canIngest);

        assertTrue(evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.ingest));
    }

    @Test
    public void collectionElevatesStaffPermission() {
        PID unitPid = addPidToAncestors();

        // Reassign staff a higher roll in the collection
        mockFactoryPrincipalRoles(unitPid, STAFF_PRINC, canIngest);
        mockFactoryPrincipalRoles(pid, STAFF_PRINC, canManage);

        assertTrue(evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.changePatronAccess));
    }

    @Test
    public void collectionAttemptToReduceStaffPermission() {
        PID unitPid = addPidToAncestors();

        // Attempt to set reduced role for staff at the collection leve
        mockFactoryPrincipalRoles(unitPid, STAFF_PRINC, canManage);
        mockFactoryPrincipalRoles(pid, STAFF_PRINC, canIngest);

        assertTrue(evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.changePatronAccess));
    }

    @Test
    public void contentHasPatronPermission() {

        addPidToAncestors();
        // Add collection pid into ancestors
        PID collectionPid = addPidToAncestors();

        mockFactoryPrincipalRoles(collectionPid, PATRON_PRINC, canViewMetadata);

        assertTrue(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void contentNoCollectionPermission() {

        addPidToAncestors();
        // Add collection pid into ancestors
        addPidToAncestors();

        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void contentNoCollectionPatronsAddInPatronNoPermission() {

        addPidToAncestors();
        addPidToAncestors();

        mockFactoryPrincipalRoles(pid, PATRON_PRINC, canViewMetadata);

        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void contentNoCollectionPatronsAddInPatronGroupNoPermission() {

        addPidToAncestors();
        addPidToAncestors();

        mockFactoryPrincipalRoles(pid, PATRON_GROUP, canViewMetadata);

        assertFalse(evaluator.hasPermission(pid, PATRON_GROUP_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void contentRemovePatronAccess() {
        addPidToAncestors();
        PID collectionPid = addPidToAncestors();

        mockFactoryPrincipalRoles(collectionPid, PATRON_PRINC, canViewMetadata);
        mockFactoryPrincipalRoles(pid, PATRON_PRINC, UserRole.none);

        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void contentRemovePatronGroupAccess() {
        addPidToAncestors();
        PID collectionPid = addPidToAncestors();

        mockFactoryPrincipalRoles(collectionPid, PATRON_GROUP, canViewMetadata);
        mockFactoryPrincipalRoles(pid, PATRON_GROUP, UserRole.none);

        assertFalse(evaluator.hasPermission(pid, PATRON_GROUP_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void contentRegularPatronsNoneGroupInherited() {
        addPidToAncestors();
        PID collectionPid = addPidToAncestors();

        mockFactoryPrincipalRoles(collectionPid, PUBLIC_PRINC, canViewMetadata);
        mockFactoryPrincipalRoles(collectionPid, AUTHENTICATED_PRINC, canViewOriginals);
        mockFactoryPrincipalRoles(collectionPid, PATRON_GROUP, canViewOriginals);
        mockFactoryPrincipalRoles(pid, PUBLIC_PRINC, UserRole.none);
        mockFactoryPrincipalRoles(pid, AUTHENTICATED_PRINC, UserRole.none);

        assertFalse(evaluator.hasPermission(pid, PATRON_GROUP_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void contentRegularPatronsNoneGroupReUpped() {
        addPidToAncestors();
        PID collectionPid = addPidToAncestors();

        mockFactoryPrincipalRoles(collectionPid, PUBLIC_PRINC, canViewMetadata);
        mockFactoryPrincipalRoles(collectionPid, AUTHENTICATED_PRINC, canViewOriginals);
        mockFactoryPrincipalRoles(collectionPid, PATRON_GROUP, canViewOriginals);
        mockFactoryPrincipalRoles(pid, PUBLIC_PRINC, UserRole.none);
        mockFactoryPrincipalRoles(pid, AUTHENTICATED_PRINC, UserRole.none);
        mockFactoryPrincipalRoles(pid, PATRON_GROUP, canViewOriginals);

        assertTrue(evaluator.hasPermission(pid, PATRON_GROUP_PRINCIPLES, Permission.viewOriginal));
    }

    @Test
    public void contentPatronGroupDowngraded() {
        addPidToAncestors();
        PID collectionPid = addPidToAncestors();

        mockFactoryPrincipalRoles(collectionPid, PATRON_GROUP, canViewOriginals);
        mockFactoryPrincipalRoles(pid, PATRON_GROUP, canViewMetadata);

        assertTrue(evaluator.hasPermission(pid, PATRON_GROUP_PRINCIPLES, Permission.viewMetadata));
        assertFalse(evaluator.hasPermission(pid, PATRON_GROUP_PRINCIPLES, Permission.viewOriginal));
    }

    @Test
    public void contentAttemptToElevatePatronAccess() {
        addPidToAncestors();
        PID collectionPid = addPidToAncestors();

        mockFactoryPrincipalRoles(collectionPid, PATRON_PRINC, canViewAccessCopies);
        mockFactoryPrincipalRoles(pid, PATRON_PRINC, canViewOriginals);

        assertTrue(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewAccessCopies));
        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewOriginal));
    }

    @Test
    public void contentEmbargoedPermission() {
        addPidToAncestors();
        PID collectionPid = addPidToAncestors();

        when(objectAclFactory.getEmbargoUntil(pid)).thenReturn(getNextYear());
        mockFactoryPrincipalRoles(collectionPid, PATRON_PRINC, canViewOriginals);

        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewOriginal));
        assertTrue(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void contentInheritsEmbargoPermission() {
        addPidToAncestors();
        PID collectionPid = addPidToAncestors();

        when(objectAclFactory.getEmbargoUntil(collectionPid)).thenReturn(getNextYear());
        mockFactoryPrincipalRoles(collectionPid, PATRON_PRINC, canViewOriginals);

        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewOriginal));
        assertTrue(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void contentDeletedNoPatronPermissions() {
        addPidToAncestors();
        addPidToAncestors();

        when(objectAclFactory.isMarkedForDeletion(pid)).thenReturn(true);
        mockFactoryPrincipalRoles(pid, PATRON_PRINC, canViewMetadata);

        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void contentInheritsDeletedNoPatronPermissions() {
        addPidToAncestors();
        PID collPid = addPidToAncestors();

        when(objectAclFactory.isMarkedForDeletion(collPid)).thenReturn(true);
        mockFactoryPrincipalRoles(collPid, PATRON_PRINC, canViewMetadata);

        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void contentInheritStaffPermission() {
        PID unitPid = addPidToAncestors();
        addPidToAncestors();

        mockFactoryPrincipalRoles(unitPid, STAFF_PRINC, canIngest);

        assertTrue(evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.ingest));
        assertTrue(evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void contentIgnoreLocallyAssignedStaffPermission() {
        addPidToAncestors();
        addPidToAncestors();

        mockFactoryPrincipalRoles(pid, STAFF_PRINC, canIngest);

        assertFalse(evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.ingest));
    }

    @Test
    public void orphanedObjectFailure() {
        Assertions.assertThrows(OrphanedObjectException.class, () -> {
            when(pathFactory.getAncestorPids(any(PID.class))).thenReturn(Collections.emptyList());

            evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.ingest);
        });
    }

    @Test
    public void rootHasPatronPermission() {
        when(pathFactory.getAncestorPids(any(PID.class))).thenReturn(new ArrayList<>());

        assertFalse(evaluator.hasPermission(RepositoryPaths.getContentRootPid(),
                PATRON_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void rootHasStaffPermission() {
        when(pathFactory.getAncestorPids(any(PID.class))).thenReturn(new ArrayList<>());

        assertFalse(evaluator.hasPermission(RepositoryPaths.getContentRootPid(),
                STAFF_PRINCIPLES, Permission.editDescription));
    }

    private PID addPidToAncestors() {
        PID ancestor = PIDs.get(UUID.randomUUID().toString());
        ancestorPids.add(ancestor);
        return ancestor;
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

    private void mockFactoryPrincipalRoles(PID pid, String principal, UserRole... roles) {
        Map<String, Set<String>> princRoles = objectAclFactory.getPrincipalRoles(pid);
        if (princRoles == null || princRoles.isEmpty()) {
            princRoles = new HashMap<>();
            when(objectAclFactory.getPrincipalRoles(pid)).thenReturn(princRoles);
        }

        princRoles.put(principal, Arrays.stream(roles)
                .map(UserRole::getPropertyString)
                .collect(Collectors.toSet()));
    }
}

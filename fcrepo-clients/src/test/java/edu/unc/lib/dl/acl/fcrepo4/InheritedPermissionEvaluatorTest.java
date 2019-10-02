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
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.dl.acl.util.UserRole.canIngest;
import static edu.unc.lib.dl.acl.util.UserRole.canManage;
import static edu.unc.lib.dl.acl.util.UserRole.canViewMetadata;
import static edu.unc.lib.dl.acl.util.UserRole.canViewOriginals;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.CONTENT_ROOT_ID;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.ContentPathFactory;
import edu.unc.lib.dl.fedora.PID;

public class InheritedPermissionEvaluatorTest {

    private static final String PATRON_PRINC = "everyone";
    private static final String STAFF_PRINC = "adminGrp";

    private static final Set<String> PATRON_PRINCIPLES = new HashSet<>(
            asList(PUBLIC_PRINC));
    private static final Set<String> AUTH_PRINCIPLES = new HashSet<>(
            asList(PUBLIC_PRINC, AUTHENTICATED_PRINC));
    private static final Set<String> STAFF_PRINCIPLES = new HashSet<>(
            asList(PUBLIC_PRINC, AUTHENTICATED_PRINC, STAFF_PRINC));

    @Mock
    private ContentPathFactory pathFactory;

    @Mock
    private ObjectAclFactory objectAclFactory;

    private InheritedPermissionEvaluator evaluator;



    private List<PID> ancestorPids;

    private PID pid;

    @Before
    public void init() {
        initMocks(this);

        evaluator = new InheritedPermissionEvaluator();
        evaluator.setPathFactory(pathFactory);
        evaluator.setObjectAclFactory(objectAclFactory);

        ancestorPids = new ArrayList<>();
        ancestorPids.add(PIDs.get(CONTENT_ROOT_ID));

        when(pathFactory.getAncestorPids(any(PID.class))).thenReturn(ancestorPids);

        pid = PIDs.get(UUID.randomUUID().toString());
    }

    @Test
    public void unitHasPatronPermissionTest() {

        assertTrue(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void unitPatronRequestingStaffTest() {

        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.ingest));
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

    @Test
    public void unitHasStaffPermission() {
        mockFactoryPrincipalRoles(pid, STAFF_PRINC, canIngest);

        assertTrue(evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.ingest));
    }

    @Test
    public void unitNoStaffPermissions() {
        mockFactoryPrincipalRoles(pid, STAFF_PRINC, canIngest);

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

    /*
     *  + collectionEmbargoedNoPatronViewOriginal
 + collectionEmbargoedHasPatronViewMetadata
 collectionExpiredEmbargoedHasPatronViewOriginal
 + collectionEmbargoedHasStaffPermission
 + collectionEmbargoedStaffPrincipalHasPatronPermission
     */


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
    public void contentRemovePatronAccess() {
        addPidToAncestors();
        PID collectionPid = addPidToAncestors();

        mockFactoryPrincipalRoles(collectionPid, PATRON_PRINC, canViewMetadata);
        mockFactoryPrincipalRoles(pid, PATRON_PRINC, UserRole.none);

        assertFalse(evaluator.hasPermission(pid, PATRON_PRINCIPLES, Permission.viewMetadata));
    }

    @Test
    public void contentRequestStaffPermission() {
        addPidToAncestors();
        addPidToAncestors();

        assertFalse(evaluator.hasPermission(pid, STAFF_PRINCIPLES, Permission.ingest));
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
}

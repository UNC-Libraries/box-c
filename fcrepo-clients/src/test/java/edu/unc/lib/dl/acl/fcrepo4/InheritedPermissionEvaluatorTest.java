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

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.CONTENT_ROOT_ID;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.ContentPathFactory;
import edu.unc.lib.dl.fedora.PID;

public class InheritedPermissionEvaluatorTest {

    private static final String PATRON_PRINC = "everyone";
    private static final String STAFF_PRINC = "adminGrp";

    @Mock
    private ContentPathFactory pathFactory;

    @Mock
    private ObjectPermissionEvaluator objectPermissionEvaluator;

    private InheritedPermissionEvaluator evaluator;

    private Set<String> principals;

    private List<PID> ancestorPids;

    private PID pid;

    @Before
    public void init() {
        initMocks(this);

        evaluator = new InheritedPermissionEvaluator();
        evaluator.setObjectPermissionEvaluator(objectPermissionEvaluator);
        evaluator.setPathFactory(pathFactory);

        principals = new HashSet<>(Arrays.asList(PATRON_PRINC));

        ancestorPids = new ArrayList<>();
        ancestorPids.add(PIDs.get(CONTENT_ROOT_ID));

        when(pathFactory.getAncestorPids(any(PID.class))).thenReturn(ancestorPids);

        pid = PIDs.get(UUID.randomUUID().toString());
    }

    @Test
    public void unitHasPatronPermissionTest() {

        assertTrue(evaluator.hasPermission(pid, principals, Permission.viewMetadata));
    }

    @Test
    public void unitPatronRequestingStaffTest() {

        assertFalse(evaluator.hasPermission(pid, principals, Permission.ingest));
    }

    @Test
    public void unitHasStaffPermissionTest() {

        when(objectPermissionEvaluator.hasStaffPermission(
                eq(pid), nonEmptySet(), any(Permission.class))).thenReturn(true);

        principals = new HashSet<>(Arrays.asList(STAFF_PRINC));

        assertTrue(evaluator.hasPermission(pid, principals, Permission.ingest));
    }

    @Test
    public void collectionHasPatronTest() {

        // Add unit pid into ancestors
        addPidToAncestors();

        when(objectPermissionEvaluator.getPatronPrincipalsWithPermission(
                eq(pid), nonEmptySet(), eq(Permission.viewMetadata)))
                .thenReturn(principals);

        assertTrue(evaluator.hasPermission(pid, principals, Permission.viewMetadata));
    }

    @Test
    public void collectionNoPrincipalsWithPatronRoleTest() {

        // Add unit pid into ancestors
        addPidToAncestors();

        when(objectPermissionEvaluator.getPatronPrincipalsWithPermission(
                eq(pid), nonEmptySet(), eq(Permission.viewMetadata)))
                .thenReturn(Collections.emptySet());

        assertFalse(evaluator.hasPermission(pid, principals, Permission.viewMetadata));
    }

    @Test
    public void collectionHasStaffPermission() {

        addPidToAncestors();

        when(objectPermissionEvaluator.hasStaffPermission(
                eq(pid), nonEmptySet(), any(Permission.class))).thenReturn(true);

        principals = new HashSet<>(Arrays.asList(STAFF_PRINC));

        assertTrue(evaluator.hasPermission(pid, principals, Permission.ingest));
    }

    @Test
    public void collectionNoStaffPermission() {

        addPidToAncestors();

        when(objectPermissionEvaluator.hasStaffPermission(
                eq(pid), nonEmptySet(), any(Permission.class))).thenReturn(false);

        principals = new HashSet<>(Arrays.asList(STAFF_PRINC));

        assertFalse(evaluator.hasPermission(pid, principals, Permission.ingest));
    }

    @Test
    public void collectionInheritedStaffPermission() {
        PID unitPid = addPidToAncestors();

        // Grant permission on the unit rather than the collection
        when(objectPermissionEvaluator.hasStaffPermission(
                eq(unitPid), nonEmptySet(), any(Permission.class))).thenReturn(true);

        principals = new HashSet<>(Arrays.asList(STAFF_PRINC));

        assertTrue(evaluator.hasPermission(pid, principals, Permission.ingest));
    }

    @Test
    public void contentHasPatronPermission() {

        addPidToAncestors();
        // Add collection pid into ancestors
        PID collectionPid = addPidToAncestors();

        when(objectPermissionEvaluator.getPatronPrincipalsWithPermission(
                eq(collectionPid), nonEmptySet(), eq(Permission.viewMetadata)))
                .thenReturn(principals);

        when(objectPermissionEvaluator.hasPatronAccess(eq(pid), nonEmptySet(), eq(Permission.viewMetadata)))
                .thenReturn(true);

        assertTrue(evaluator.hasPermission(pid, principals, Permission.viewMetadata));
    }

    @Test
    public void contentNoCollectionPermission() {

        addPidToAncestors();
        // Add collection pid into ancestors
        PID collectionPid = addPidToAncestors();

        when(objectPermissionEvaluator.getPatronPrincipalsWithPermission(
                eq(collectionPid), nonEmptySet(), eq(Permission.viewMetadata)))
                .thenReturn(Collections.emptySet());

        when(objectPermissionEvaluator.hasPatronAccess(eq(pid), nonEmptySet(), eq(Permission.viewMetadata)))
                .thenReturn(true);

        assertFalse(evaluator.hasPermission(pid, principals, Permission.viewMetadata));
    }

    @Test
    public void contentRemovePatronAccess() {
        addPidToAncestors();
        PID collectionPid = addPidToAncestors();

        when(objectPermissionEvaluator.getPatronPrincipalsWithPermission(
                eq(collectionPid), nonEmptySet(), eq(Permission.viewMetadata)))
                .thenReturn(principals);

        when(objectPermissionEvaluator.hasPatronAccess(eq(pid), nonEmptySet(), eq(Permission.viewMetadata)))
                .thenReturn(false);

        assertFalse(evaluator.hasPermission(pid, principals, Permission.viewMetadata));
    }

    @Test
    public void contentRequestStaffPermission() {
        addPidToAncestors();
        addPidToAncestors();

        principals = new HashSet<>(Arrays.asList(STAFF_PRINC));

        assertFalse(evaluator.hasPermission(pid, principals, Permission.ingest));
    }

    private PID addPidToAncestors() {
        PID ancestor = PIDs.get(UUID.randomUUID().toString());
        ancestorPids.add(ancestor);
        return ancestor;
    }

    private static Set<String> nonEmptySet() {
        return argThat(new NonEmptySetMatcher());
    }

    private static class NonEmptySetMatcher extends ArgumentMatcher<Set<String>> {
        @SuppressWarnings("unchecked")
        @Override
        public boolean matches(Object set) {
            return ((Set<String>) set).size() > 0;
        }
     }
}

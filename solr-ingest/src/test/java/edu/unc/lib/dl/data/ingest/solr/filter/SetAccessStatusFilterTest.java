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
package edu.unc.lib.dl.data.ingest.solr.filter;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.fcrepo4.InheritedAclFactory;
import edu.unc.lib.dl.acl.fcrepo4.ObjectAclFactory;
import edu.unc.lib.dl.acl.util.RoleAssignment;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.search.solr.util.FacetConstants;

/**
 * @author harring
 */
public class SetAccessStatusFilterTest {

    private static final String PID_STRING = "uuid:07d9594f-310d-4095-ab67-79a1056e7430";

    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private IndexDocumentBean idb;
    @Mock
    private InheritedAclFactory inheritedAclFactory;
    @Mock
    private ObjectAclFactory objAclFactory;
    @Mock
    private ContentObject contentObj;
    @Mock
    private Date date;

    @Mock
    private PID pid;

    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    private SetAccessStatusFilter filter;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        when(pid.toString()).thenReturn(PID_STRING);

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);

        filter = new SetAccessStatusFilter();
        filter.setObjectAclFactory(objAclFactory);
        filter.setInheritedAclFactory(inheritedAclFactory);
    }

    @Test
    public void testIsMarkedForDeletion() throws Exception {

        when(inheritedAclFactory.isMarkedForDeletion(any(PID.class))).thenReturn(true);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.MARKED_FOR_DELETION));
        assertFalse(listCaptor.getValue().contains(FacetConstants.EMBARGOED));
    }

    @Test
    public void testIsObjectEmbargoed() throws Exception {

        addInheritedRoleAssignment(pid, PUBLIC_PRINC, UserRole.canViewOriginals);
        addInheritedRoleAssignment(pid, AUTHENTICATED_PRINC, UserRole.canViewOriginals);

        when(objAclFactory.getEmbargoUntil(pid)).thenReturn(getNextYear());

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertFalse(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
        assertTrue(listCaptor.getValue().contains(FacetConstants.EMBARGOED));
        assertFalse(listCaptor.getValue().contains(FacetConstants.EMBARGOED_PARENT));
        assertFalse(listCaptor.getValue().contains(FacetConstants.MARKED_FOR_DELETION));
    }

    @Test
    public void testIsParentEmbargoed() throws Exception {
        addInheritedRoleAssignment(pid, PUBLIC_PRINC, UserRole.canViewMetadata);
        addInheritedRoleAssignment(pid, AUTHENTICATED_PRINC, UserRole.canViewMetadata);

        when(inheritedAclFactory.getEmbargoUntil(pid)).thenReturn(getNextYear());

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.EMBARGOED_PARENT));
        assertFalse(listCaptor.getValue().contains(FacetConstants.EMBARGOED));
        assertFalse(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
    }

    @Test
    public void testHasStaffOnlyAccess() throws Exception {
        addPrincipalRoles(pid, PUBLIC_PRINC, UserRole.none);
        addPrincipalRoles(pid, AUTHENTICATED_PRINC, UserRole.none);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
    }

    @Test
    public void testHasPublicAccess() throws Exception {

        addInheritedRoleAssignment(pid, PUBLIC_PRINC, UserRole.canViewOriginals);
        addInheritedRoleAssignment(pid, AUTHENTICATED_PRINC, UserRole.canViewOriginals);

        addPrincipalRoles(pid, PUBLIC_PRINC, UserRole.canViewOriginals);
        addPrincipalRoles(pid, AUTHENTICATED_PRINC, UserRole.canViewOriginals);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
    }

    @Test
    public void testHasInheritedPublicAccess() throws Exception {

        addInheritedRoleAssignment(pid, PUBLIC_PRINC, UserRole.canViewOriginals);
        addInheritedRoleAssignment(pid, AUTHENTICATED_PRINC, UserRole.canViewOriginals);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
    }

    @Test
    public void testHasPartialAccess() throws Exception {

        addInheritedRoleAssignment(pid, PUBLIC_PRINC, UserRole.canViewMetadata);
        addInheritedRoleAssignment(pid, AUTHENTICATED_PRINC, UserRole.canViewOriginals);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertFalse(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
    }

    @Test
    public void testHasMixedRevokedAccess() throws Exception {

        addInheritedRoleAssignment(pid, PUBLIC_PRINC, UserRole.none);
        addInheritedRoleAssignment(pid, AUTHENTICATED_PRINC, UserRole.canViewOriginals);

        addPrincipalRoles(pid, PUBLIC_PRINC, UserRole.none);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertFalse(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
    }

    @Test
    public void testParentHasStaffOnlyAccess() throws Exception {
        addInheritedRoleAssignment(pid, "managerGroup", UserRole.canManage);

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.PARENT_HAS_STAFF_ONLY_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
    }

    @Test
    public void testNoAccessControlsSet() throws Exception {

        filter.filter(dip);

        verify(idb).setStatus(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(FacetConstants.PARENT_HAS_STAFF_ONLY_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.STAFF_ONLY_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.PUBLIC_ACCESS));
        assertFalse(listCaptor.getValue().contains(FacetConstants.EMBARGOED));
        assertFalse(listCaptor.getValue().contains(FacetConstants.MARKED_FOR_DELETION));
    }

    private void addPrincipalRoles(PID pid, String principal, UserRole... roles) {
        Map<String, Set<String>> princRoles = objAclFactory.getPrincipalRoles(pid);
        if (princRoles == null || princRoles.isEmpty()) {
            princRoles = new HashMap<>();
            when(objAclFactory.getPrincipalRoles(pid)).thenReturn(princRoles);
        }

        princRoles.put(principal, Arrays.stream(roles)
                .map(UserRole::getPropertyString)
                .collect(Collectors.toSet()));
    }

    private void addInheritedRoleAssignment(PID pid, String principal, UserRole role) {
        List<RoleAssignment> assignments = inheritedAclFactory.getPatronAccess(pid);
        if (assignments == null || assignments.isEmpty()) {
            assignments = new ArrayList<>();
            when(inheritedAclFactory.getPatronAccess(pid)).thenReturn(assignments);
        }

        assignments.add(new RoleAssignment(principal, role));
    }

    private Date getNextYear() {
        Date dt = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(dt);
        c.add(Calendar.DATE, 365);
        return c.getTime();
    }
}

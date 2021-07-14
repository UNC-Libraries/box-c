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

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.auth.api.UserRole.canDescribe;
import static edu.unc.lib.boxc.auth.api.UserRole.canManage;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewAccessCopies;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewMetadata;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewOriginals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.dl.acl.fcrepo4.InheritedAclFactory;
import edu.unc.lib.boxc.auth.api.AccessPrincipalConstants;
import edu.unc.lib.boxc.auth.fcrepo.model.RoleAssignment;
import edu.unc.lib.boxc.auth.api.UserRole;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

/**
 *
 * @author bbpennel
 *
 */
public class SetAccessControlFilterTest {

    private static final String PRINC1 = "group1";
    private static final String PRINC2 = "group2";

    @Mock
    private DocumentIndexingPackage dip;
    @Mock
    private IndexDocumentBean idb;
    @Mock
    private InheritedAclFactory aclFactory;

    @Mock
    private PID pid;

    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    private SetAccessControlFilter filter;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);

        filter = new SetAccessControlFilter();
        filter.setAclFactory(aclFactory);
    }

    @Test
    public void testHasPatronPrincipal() throws Exception {
        addPatronAccess(PUBLIC_PRINC, canViewOriginals);

        filter.filter(dip);

        verify(idb).setAdminGroup(listCaptor.capture());
        assertTrue("Patron principal must not have admin viewing rights",
                listCaptor.getValue().isEmpty());

        verify(idb).setReadGroup(listCaptor.capture());
        assertPrincipalsPresent("Patron principal must have patron viewing rights",
                listCaptor.getValue(), PUBLIC_PRINC);

        verify(idb).setRoleGroup(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(
                canViewOriginals.name() + "|" + PUBLIC_PRINC));
    }

    @Test
    public void testHasStaffPrincipal() throws Exception {
        addStaffAssignments(PRINC1, canManage);

        filter.filter(dip);

        verify(idb).setAdminGroup(listCaptor.capture());
        assertPrincipalsPresent("Staff principal must have admin viewing rights",
                listCaptor.getValue(), PRINC1);

        verify(idb).setReadGroup(listCaptor.capture());
        assertPrincipalsPresent("Staff principal must have patron viewing rights",
                listCaptor.getValue(), PRINC1);

        verify(idb).setRoleGroup(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(
                canManage.name() + "|" + PRINC1));
    }

    @Test
    public void testHasNoPrincipals() throws Exception {
        filter.filter(dip);

        verify(idb).setAdminGroup(listCaptor.capture());
        assertTrue("No admin rights should be granted",
                listCaptor.getValue().isEmpty());

        verify(idb).setReadGroup(listCaptor.capture());
        assertTrue("No read rights should be granted",
                listCaptor.getValue().isEmpty());

        verify(idb).setRoleGroup(listCaptor.capture());
        assertTrue("No role assignments should be present",
                listCaptor.getValue().isEmpty());
    }

    @Test
    public void testHasMultipleStaffAndPatrons() throws Exception {
        addPatronAccess(PUBLIC_PRINC, canViewMetadata);
        addPatronAccess(AUTHENTICATED_PRINC, canViewOriginals);
        addStaffAssignments(PRINC1, canDescribe);
        addStaffAssignments(PRINC2, canManage);

        filter.filter(dip);

        verify(idb).setAdminGroup(listCaptor.capture());
        assertPrincipalsPresent("Only staff principals should be granted admin rights",
                listCaptor.getValue(), PRINC1, PRINC2);

        verify(idb).setReadGroup(listCaptor.capture());
        assertPrincipalsPresent("All principals should be granted read rights",
                listCaptor.getValue(), PUBLIC_PRINC, AUTHENTICATED_PRINC, PRINC1, PRINC2);

        verify(idb).setRoleGroup(listCaptor.capture());
        List<String> roleGroups = listCaptor.getValue();
        assertEquals(4, roleGroups.size());
        assertTrue(roleGroups.contains(canViewMetadata.name() + "|" + PUBLIC_PRINC));
        assertTrue(roleGroups.contains(canViewOriginals.name() + "|" + AUTHENTICATED_PRINC));
        assertTrue(roleGroups.contains(canDescribe.name() + "|" + PRINC1));
        assertTrue(roleGroups.contains(canManage.name() + "|" + PRINC2));
    }

    @Test
    public void testPrincipalHasMultipleRoles() throws Exception {
        addStaffAssignments(PRINC1, canDescribe, canManage);

        filter.filter(dip);

        verify(idb).setAdminGroup(listCaptor.capture());
        assertPrincipalsPresent("Principal should only appear once in admin rights",
                listCaptor.getValue(), PRINC1);

        verify(idb).setReadGroup(listCaptor.capture());
        assertPrincipalsPresent("Principal should only appear once in read rights",
                listCaptor.getValue(), PRINC1);

        verify(idb).setRoleGroup(listCaptor.capture());
        assertEquals("Principal should appear with each role granted",
                2, listCaptor.getValue().size());
        assertTrue(listCaptor.getValue().contains(
                canManage.name() + "|" + PRINC1));
        assertTrue(listCaptor.getValue().contains(
                canDescribe.name() + "|" + PRINC1));
    }

    @Test
    public void testHasMultiplePrincipals() throws Exception {
        addPatronAccess(PUBLIC_PRINC, canViewOriginals);
        addStaffAssignments(PRINC2, canManage);

        filter.filter(dip);

        verify(idb).setAdminGroup(listCaptor.capture());
        assertPrincipalsPresent("Only staff principal should be granted admin rights",
                listCaptor.getValue(), PRINC2);

        verify(idb).setReadGroup(listCaptor.capture());
        assertPrincipalsPresent("Both principals should be granted read rights",
                listCaptor.getValue(), PUBLIC_PRINC, PRINC2);

        verify(idb).setRoleGroup(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(
                canViewOriginals.name() + "|" + PUBLIC_PRINC));
        assertTrue(listCaptor.getValue().contains(
                canManage.name() + "|" + PRINC2));
    }

    @Test
    public void testHasCustomPatronPrincipal() throws Exception {
        addPatronAccess(PUBLIC_PRINC, canViewMetadata);
        addPatronAccess(AUTHENTICATED_PRINC, canViewAccessCopies);
        String customGroup = AccessPrincipalConstants.IP_PRINC_NAMESPACE + "custom";
        addPatronAccess(customGroup, canViewOriginals);

        filter.filter(dip);

        verify(idb).setReadGroup(listCaptor.capture());
        assertPrincipalsPresent("All principals should be granted read rights",
                listCaptor.getValue(), PUBLIC_PRINC, AUTHENTICATED_PRINC, customGroup);

        verify(idb).setRoleGroup(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(
                canViewMetadata.name() + "|" + PUBLIC_PRINC));
        assertTrue(listCaptor.getValue().contains(
                canViewAccessCopies.name() + "|" + AUTHENTICATED_PRINC));
        assertTrue(listCaptor.getValue().contains(
                canViewOriginals.name() + "|" + customGroup));
    }

    @Test
    public void testContentRoot() throws Exception {
        ContentRootObject contentRoot = mock(ContentRootObject.class);
        when(dip.getContentObject()).thenReturn(contentRoot);

        filter.filter(dip);

        verify(idb).setAdminGroup(listCaptor.capture());
        List<String> adminPrincipals = listCaptor.getValue();
        assertEquals(1, adminPrincipals.size());
        assertTrue("Admin access principal must be have admin viewing rights for content root",
                adminPrincipals.contains(AccessPrincipalConstants.ADMIN_ACCESS_PRINC));

        verify(idb).setReadGroup(listCaptor.capture());
        List<String> patronPrincipals = listCaptor.getValue();
        assertEquals(1, patronPrincipals.size());
        assertPrincipalsPresent("Patron principal must have patron viewing rights for content root",
                listCaptor.getValue(), PUBLIC_PRINC);

        verify(idb).setRoleGroup(listCaptor.capture());
        assertTrue("No role grants should be present for ContentRoot",
                listCaptor.getValue().isEmpty());
    }

    private void assertPrincipalsPresent(String message, List<String> values, String... principals) {
        assertEquals(message, principals.length, values.size());
        for (String principal : principals) {
            assertTrue(values.contains(principal));
        }
    }

    private void addStaffAssignments(String principal, UserRole... roles) {
        List<RoleAssignment> assignments = aclFactory.getStaffRoleAssignments(pid);
        if (assignments.isEmpty()) {
            assignments = new ArrayList<>();
            when(aclFactory.getStaffRoleAssignments(pid)).thenReturn(assignments);
        }
        for (UserRole role: roles) {
            assignments.add(new RoleAssignment(principal, role, pid));
        }
    }

    private void addPatronAccess(String principal, UserRole role) {
        List<RoleAssignment> assignments = aclFactory.getPatronAccess(pid);
        if (assignments.isEmpty()) {
            assignments = new ArrayList<>();
            when(aclFactory.getPatronAccess(pid)).thenReturn(assignments);
        }
        assignments.add(new RoleAssignment(principal, role, pid));
    }
}

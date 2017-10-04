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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;
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
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;

/**
 *
 * @author bbpennel
 *
 */
public class SetAccessControlFilterTest {

    private static final String PID_STRING = "uuid:07d9594f-310d-4095-ab67-79a1056e7430";

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

    private Map<String, Set<String>> principalRoles;

    private SetAccessControlFilter filter;

    @Before
    public void setup() throws Exception {
        initMocks(this);

        when(pid.getPid()).thenReturn(PID_STRING);

        when(dip.getDocument()).thenReturn(idb);
        when(dip.getPid()).thenReturn(pid);

        principalRoles = new HashMap<>();
        when(aclFactory.getPrincipalRoles(any(PID.class))).thenReturn(principalRoles);

        filter = new SetAccessControlFilter();
        filter.setAclFactory(aclFactory);
    }

    @Test
    public void testHasPatronPrincipal() throws Exception {
        addPrincipalRoles(PRINC1, UserRole.canViewOriginals);

        filter.filter(dip);

        verify(idb).setAdminGroup(listCaptor.capture());
        assertTrue("Patron principal must not have admin viewing rights",
                listCaptor.getValue().isEmpty());

        verify(idb).setReadGroup(listCaptor.capture());
        assertPrincipalsPresent("Patron principal must have patron viewing rights",
                listCaptor.getValue(), PRINC1);

        verify(idb).setRoleGroup(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(
                UserRole.canViewOriginals.name() + "|" + PRINC1));
    }

    @Test
    public void testHasStaffPrincipal() throws Exception {
        addPrincipalRoles(PRINC1, UserRole.canManage);

        filter.filter(dip);

        verify(idb).setAdminGroup(listCaptor.capture());
        assertPrincipalsPresent("Staff principal must have admin viewing rights",
                listCaptor.getValue(), PRINC1);

        verify(idb).setReadGroup(listCaptor.capture());
        assertPrincipalsPresent("Staff principal must have patron viewing rights",
                listCaptor.getValue(), PRINC1);

        verify(idb).setRoleGroup(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(
                UserRole.canManage.name() + "|" + PRINC1));
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
    public void testHasMultiplePrincipals() throws Exception {
        addPrincipalRoles(PRINC1, UserRole.canViewOriginals);
        addPrincipalRoles(PRINC2, UserRole.canManage);

        filter.filter(dip);

        verify(idb).setAdminGroup(listCaptor.capture());
        assertPrincipalsPresent("Only staff principal should be granted admin rights",
                listCaptor.getValue(), PRINC2);

        verify(idb).setReadGroup(listCaptor.capture());
        assertPrincipalsPresent("Both principals should be granted read rights",
                listCaptor.getValue(), PRINC1, PRINC2);

        verify(idb).setRoleGroup(listCaptor.capture());
        assertTrue(listCaptor.getValue().contains(
                UserRole.canViewOriginals.name() + "|" + PRINC1));
        assertTrue(listCaptor.getValue().contains(
                UserRole.canManage.name() + "|" + PRINC2));
    }

    @Test
    public void testPrincipalHasMultipleRoles() throws Exception {
        addPrincipalRoles(PRINC1, UserRole.canViewOriginals,
                UserRole.canDescribe, UserRole.canManage);

        filter.filter(dip);

        verify(idb).setAdminGroup(listCaptor.capture());
        assertPrincipalsPresent("Principal should only appear once in admin rights",
                listCaptor.getValue(), PRINC1);

        verify(idb).setReadGroup(listCaptor.capture());
        assertPrincipalsPresent("Principal should only appear once in read rights",
                listCaptor.getValue(), PRINC1);

        verify(idb).setRoleGroup(listCaptor.capture());
        assertEquals("Principal should appear with each role granted",
                3, listCaptor.getValue().size());
        assertTrue(listCaptor.getValue().contains(
                UserRole.canViewOriginals.name() + "|" + PRINC1));
        assertTrue(listCaptor.getValue().contains(
                UserRole.canManage.name() + "|" + PRINC1));
        assertTrue(listCaptor.getValue().contains(
                UserRole.canDescribe.name() + "|" + PRINC1));
    }

    private void assertPrincipalsPresent(String message, List<String> values, String... principals) {
        assertEquals(message, principals.length, values.size());
        for (String principal : principals) {
            assertTrue(values.contains(principal));
        }
    }

    private void addPrincipalRoles(String principal, UserRole...roles) {
        Set<String> roleStrings = Arrays.stream(roles)
                .map(r -> r.getPropertyString())
                .collect(Collectors.toSet());

        principalRoles.put(principal, roleStrings);
    }
}

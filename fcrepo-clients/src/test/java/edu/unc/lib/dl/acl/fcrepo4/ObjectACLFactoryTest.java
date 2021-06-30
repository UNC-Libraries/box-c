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

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;
import edu.unc.lib.boxc.model.fcrepo.objects.AbstractContentObject;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectLoaderImpl;
import edu.unc.lib.dl.acl.util.RoleAssignment;

/**
 *
 * @author bbpennel
 *
 */
public class ObjectACLFactoryTest {

    private static final long CACHE_TIME_TO_LIVE = 100l;
    private static final long CACHE_MAX_SIZE = 5;

    private static final String MANAGE_GRP = "manage_group";
    private static final String PATRON_GRP = "patron_group";
    private static final String USER_PRINC = "username";

    private static final String PID_BASE_URI = "http://example.com/content/";
    private static final LocalDateTime TOMORROW = LocalDateTime.now().plusDays(1);

    private ObjectAclFactory aclFactory;

    @Mock
    private RepositoryObjectLoaderImpl repositoryObjectLoader;
    @Mock
    private AbstractContentObject repoObj;
    private Model objModel;
    private Resource objResc;

    private PID pid;

    @Before
    public void init() {
        initMocks(this);

        aclFactory = new ObjectAclFactory();
        aclFactory.setRepositoryObjectLoader(repositoryObjectLoader);
        aclFactory.setCacheTimeToLive(CACHE_TIME_TO_LIVE);
        aclFactory.setCacheMaxSize(CACHE_MAX_SIZE);

        aclFactory.init();

        pid = makePid();
        objModel = ModelFactory.createDefaultModel();
        objResc = objModel.getResource(pid.getRepositoryPath());
        when(repoObj.getModel()).thenReturn(objModel);
        when(repositoryObjectLoader.getRepositoryObject(pid)).thenReturn(repoObj);
    }

    @Test
    public void getPrincipalRolesTest() throws Exception {
        objResc.addLiteral(CdrAcl.canManage, MANAGE_GRP);

        Map<String, Set<String>> results = aclFactory.getPrincipalRoles(pid);

        assertEquals("Incorrect number of principals returned", 1, results.size());
        Set<String> roles = results.get(MANAGE_GRP);
        assertNotNull(roles);
        assertEquals("Incorrect number of roles for principal", 1, roles.size());
        assertTrue(roles.contains(CdrAcl.canManage.toString()));
    }

    @Test
    public void getPrincipalRoleNoPropertiesTest() throws Exception {
        Map<String, Set<String>> results = aclFactory.getPrincipalRoles(pid);

        assertEquals("No principals should be returned", 0, results.size());
    }

    @Test
    public void getPrincipalRoleNoRolesTest() throws Exception {
        objResc.addLiteral(CdrAcl.embargoUntil, TOMORROW);

        Map<String, Set<String>> results = aclFactory.getPrincipalRoles(pid);

        assertEquals("No principals should be returned", 0, results.size());
    }

    @Test
    public void getMultiplePrincipalsTest() throws Exception {
        objResc.addLiteral(CdrAcl.canManage, MANAGE_GRP);
        objResc.addLiteral(CdrAcl.canDiscover, PATRON_GRP);
        objResc.addLiteral(CdrAcl.unitOwner, USER_PRINC);

        Map<String, Set<String>> results = aclFactory.getPrincipalRoles(pid);

        assertEquals("Incorrect number of principals returned", 3, results.size());
        Set<String> roles = results.get(MANAGE_GRP);
        assertEquals("Incorrect number of roles for manager grp", 1, roles.size());
        assertTrue(roles.contains(CdrAcl.canManage.toString()));

        Set<String> roles2 = results.get(PATRON_GRP);
        assertEquals("Incorrect number of roles for patron grp", 1, roles2.size());
        assertTrue(roles2.contains(CdrAcl.canDiscover.toString()));

        Set<String> roles3 = results.get(USER_PRINC);
        assertEquals("Incorrect number of roles for user", 1, roles3.size());
        assertTrue(roles3.contains(CdrAcl.unitOwner.toString()));
    }

    @Test
    public void getPrincipalWithMultipleRolesTest() throws Exception {
        objResc.addLiteral(CdrAcl.canManage, MANAGE_GRP);
        objResc.addLiteral(CdrAcl.canDescribe, MANAGE_GRP);

        Map<String, Set<String>> results = aclFactory.getPrincipalRoles(pid);

        assertEquals("Incorrect number of principals returned", 1, results.size());
        Set<String> roles = results.get(MANAGE_GRP);
        assertNotNull(roles);
        assertEquals("Incorrect number of roles for principal", 2, roles.size());
        assertTrue(roles.contains(CdrAcl.canManage.toString()));
        assertTrue(roles.contains(CdrAcl.canDescribe.toString()));
    }

    @Test
    public void isMarkedForDeletionTest() throws Exception {
        objResc.addLiteral(CdrAcl.markedForDeletion, true);

        boolean result = aclFactory.isMarkedForDeletion(pid);

        assertTrue(result);
    }

    @Test
    public void isTombstoneTest() throws Exception {
        objResc.addProperty(RDF.type, Cdr.Tombstone);

        boolean result = aclFactory.isMarkedForDeletion(pid);

        assertTrue(result);
    }

    @Test
    public void isMarkedForDeletionFalseTest() throws Exception {
        objResc.addLiteral(CdrAcl.canViewOriginals, PUBLIC_PRINC);

        boolean result = aclFactory.isMarkedForDeletion(pid);

        assertFalse(result);
    }

    @Test
    public void getEmbargoUntilTest() throws Exception {
        objResc.addLiteral(CdrAcl.canManage, MANAGE_GRP);
        objResc.addLiteral(CdrAcl.embargoUntil, TOMORROW);

        Date embargoDate = aclFactory.getEmbargoUntil(pid);

        assertNotNull(embargoDate);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
        assertEquals(dtf.format(TOMORROW), dt.format(embargoDate));
    }

    @Test
    public void getNoEmbargoUntilTest() throws Exception {
        objResc.addLiteral(CdrAcl.canManage, MANAGE_GRP);

        Date embargoDate = aclFactory.getEmbargoUntil(pid);

        assertNull(embargoDate);
    }

    @Test
    public void getInvalidEmbargoUntilTest() throws Exception {
        objResc.addLiteral(CdrAcl.embargoUntil, "notadate");

        Date embargoDate = aclFactory.getEmbargoUntil(pid);

        assertNull(embargoDate);
    }

    @Test(expected = NotImplementedException.class)
    public void getPatronAccessTest() throws Exception {
        aclFactory.getPatronAccess(pid);
    }

    @Test
    public void cacheValueUsedTest() throws Exception {
        objResc.addLiteral(CdrAcl.canManage, MANAGE_GRP);

        Map<String, Set<String>> results = aclFactory.getPrincipalRoles(pid);
        assertEquals("Incorrect number of principals returned", 1, results.size());

        verify(repositoryObjectLoader).getRepositoryObject(pid);

        // Second run, should not change the number of sparql invocations
        results = aclFactory.getPrincipalRoles(pid);

        verify(repositoryObjectLoader).getRepositoryObject(pid);

        assertEquals("Incorrect number of principals on second run", 1, results.size());
        Set<String> roles = results.get(MANAGE_GRP);
        assertEquals("Incorrect number of roles for principal on second run", 1, roles.size());
        assertTrue(roles.contains(CdrAcl.canManage.toString()));
    }

    @Test
    public void cacheValueExpiredTest() throws Exception {
        objResc.addLiteral(CdrAcl.canManage, MANAGE_GRP);

        Map<String, Set<String>> results = aclFactory.getPrincipalRoles(pid);
        verify(repositoryObjectLoader).getRepositoryObject(pid);
        assertEquals("Incorrect number of principals returned", 1, results.size());

        // Wait for expiration time
        Thread.sleep(CACHE_TIME_TO_LIVE * 2);

        results = aclFactory.getPrincipalRoles(pid);
        assertEquals("Incorrect number of principals returned", 1, results.size());
        assertEquals("Incorrect number of roles for principal", 1, results.get(MANAGE_GRP).size());

        verify(repositoryObjectLoader, times(2)).getRepositoryObject(pid);
    }

    @Test
    public void multiplePidCaching() throws Exception {
        objResc.addLiteral(CdrAcl.canManage, MANAGE_GRP);

        Map<String, Set<String>> results = aclFactory.getPrincipalRoles(pid);
        assertEquals("Incorrect number of principals returned for pid1", 1, results.size());
        Set<String> roles = results.get(MANAGE_GRP);
        assertEquals("Incorrect number of roles for principal for pid1", 1, roles.size());
        assertTrue(roles.contains(CdrAcl.canManage.toString()));

        PID pid2 = makePid();
        Model model2 = ModelFactory.createDefaultModel();
        Resource resc2 = model2.getResource(pid2.getRepositoryPath());
        AbstractContentObject repoObj2 = mock(AbstractContentObject.class);
        when(repoObj2.getModel()).thenReturn(model2);
        when(repositoryObjectLoader.getRepositoryObject(pid2)).thenReturn(repoObj2);

        resc2.addLiteral(CdrAcl.canAccess, PATRON_GRP);
        resc2.addLiteral(CdrAcl.canIngest, USER_PRINC);

        results = aclFactory.getPrincipalRoles(pid2);
        assertEquals("Incorrect number of principals returned for pid2", 2, results.size());
        assertEquals("Incorrect number of roles for patron for pid2", 1, results.get(PATRON_GRP).size());
        assertEquals("Incorrect number of roles for user for pid2", 1, results.get(USER_PRINC).size());

        verify(repositoryObjectLoader, times(2)).getRepositoryObject(any(PID.class));

        results = aclFactory.getPrincipalRoles(pid);
        assertEquals("Incorrect number of principals cached for pid1", 1, results.size());
        results = aclFactory.getPrincipalRoles(pid2);
        assertEquals("Incorrect number of principals cached for pid2", 2, results.size());

        verify(repositoryObjectLoader, times(2)).getRepositoryObject(any(PID.class));
    }

    @Test
    public void testGetStaffRolesSingleRoleValid() {
        objResc.addLiteral(CdrAcl.canManage, MANAGE_GRP);

        List<RoleAssignment> assignments = aclFactory.getStaffRoleAssignments(pid);

        assertEquals(1, assignments.size());
        RoleAssignment assignment = assignments.get(0);
        assertEquals(pid.getId(), assignment.getAssignedTo());
        assertEquals(CdrAcl.canManage, assignment.getRole().getProperty());
        assertEquals(MANAGE_GRP, assignment.getPrincipal());
    }

    @Test
    public void testGetStaffRolesMultipleSamePrincipalValid() {
        objResc.addLiteral(CdrAcl.canManage, MANAGE_GRP);
        objResc.addLiteral(CdrAcl.canAccess, MANAGE_GRP);

        List<RoleAssignment> assignments = aclFactory.getStaffRoleAssignments(pid);

        assertEquals(2, assignments.size());

        RoleAssignment assignment1 = getAssignmentByRole(assignments, CdrAcl.canManage);
        assertEquals(pid.getId(), assignment1.getAssignedTo());
        assertEquals(CdrAcl.canManage, assignment1.getRole().getProperty());
        assertEquals(MANAGE_GRP, assignment1.getPrincipal());

        RoleAssignment assignment2 = getAssignmentByRole(assignments, CdrAcl.canAccess);
        assertEquals(pid.getId(), assignment2.getAssignedTo());
        assertEquals(CdrAcl.canAccess, assignment2.getRole().getProperty());
        assertEquals(MANAGE_GRP, assignment2.getPrincipal());
    }

    @Test
    public void testGetStaffRolesMultipleValid() {
        objResc.addLiteral(CdrAcl.canManage, MANAGE_GRP);
        objResc.addLiteral(CdrAcl.canAccess, USER_PRINC);

        List<RoleAssignment> assignments = aclFactory.getStaffRoleAssignments(pid);

        assertEquals(2, assignments.size());

        RoleAssignment assignment1 = getAssignmentByRole(assignments, CdrAcl.canManage);
        assertEquals(pid.getId(), assignment1.getAssignedTo());
        assertEquals(CdrAcl.canManage, assignment1.getRole().getProperty());
        assertEquals(MANAGE_GRP, assignment1.getPrincipal());

        RoleAssignment assignment2 = getAssignmentByRole(assignments, CdrAcl.canAccess);
        assertEquals(pid.getId(), assignment2.getAssignedTo());
        assertEquals(CdrAcl.canAccess, assignment2.getRole().getProperty());
        assertEquals(USER_PRINC, assignment2.getPrincipal());
    }

    @Test
    public void testGetStaffRolesOnlyPatronAssignments() {
        objResc.addLiteral(CdrAcl.canViewOriginals, USER_PRINC);

        List<RoleAssignment> assignments = aclFactory.getStaffRoleAssignments(pid);

        assertTrue(assignments.isEmpty());
    }

    @Test
    public void testGetStaffRolesNoAssignments() {
        List<RoleAssignment> assignments = aclFactory.getStaffRoleAssignments(pid);

        assertTrue(assignments.isEmpty());
    }

    @Test
    public void testGetPatronRolesSingleRoleValid() {
        objResc.addLiteral(CdrAcl.canViewOriginals, USER_PRINC);

        List<RoleAssignment> assignments = aclFactory.getPatronRoleAssignments(pid);

        assertEquals(1, assignments.size());
        RoleAssignment assignment = assignments.iterator().next();
        assertEquals(pid.getId(), assignment.getAssignedTo());
        assertEquals(CdrAcl.canViewOriginals, assignment.getRole().getProperty());
        assertEquals(USER_PRINC, assignment.getPrincipal());
    }

    @Test
    public void testGetPatronRolesOnlyStaffAssignments() {
        objResc.addLiteral(CdrAcl.canManage, USER_PRINC);

        List<RoleAssignment> assignments = aclFactory.getPatronRoleAssignments(pid);

        assertTrue(assignments.isEmpty());
    }

    @Test
    public void testGetPatronRolesNoAssignments() {
        List<RoleAssignment> assignments = aclFactory.getPatronRoleAssignments(pid);

        assertTrue(assignments.isEmpty());
    }

    private RoleAssignment getAssignmentByRole(List<RoleAssignment> assignments, Property role) {
        return assignments.stream()
                .filter(a -> a.getRole().getProperty().equals(role))
                .findFirst()
                .orElse(null);
    }

    private PID makePid() {
        PID pid = mock(PID.class);
        when(pid.getRepositoryPath()).thenReturn(PID_BASE_URI + UUID.randomUUID().toString());
        return pid;
    }
}

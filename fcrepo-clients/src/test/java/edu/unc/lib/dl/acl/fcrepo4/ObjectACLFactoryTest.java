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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.CdrAcl;
import edu.unc.lib.dl.sparql.SparqlQueryService;

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

    private ObjectAclFactory aclFactory;

    @Mock
    private SparqlQueryService queryService;

    @Mock
    private QueryExecution mockQueryExec;
    @Mock
    private ResultSet mockResultSet;
    @Mock
    private QuerySolution mockQuerySoln;
    @Mock
    private RDFNode mockObjNode;
    @Mock
    private Literal mockObjLiteral;

    private PID pid;

    @Before
    public void init() {
        initMocks(this);

        aclFactory = new ObjectAclFactory();
        aclFactory.setQueryService(queryService);
        aclFactory.setCacheTimeToLive(CACHE_TIME_TO_LIVE);
        aclFactory.setCacheMaxSize(CACHE_MAX_SIZE);

        aclFactory.init();

        when(queryService.executeQuery(anyString())).thenReturn(mockQueryExec);
        // Boilerplate to setup single query result interaction
        when(mockQueryExec.execSelect()).thenReturn(mockResultSet);
        when(mockResultSet.hasNext()).thenReturn(true, false);
        when(mockResultSet.nextSolution()).thenReturn(mockQuerySoln);
        when(mockQuerySoln.get(eq("obj"))).thenReturn(mockObjNode);
        when(mockObjNode.isLiteral()).thenReturn(true);
        when(mockObjNode.asLiteral()).thenReturn(mockObjLiteral);

        pid = makePid();
    }

    @Test
    public void getPrincipalRolesTest() throws Exception {
        when(mockResultSet.hasNext()).thenReturn(true, true, false);
        when(mockQuerySoln.getResource(eq("pred")))
                .thenReturn(CdrAcl.canManage, CdrAcl.embargoUntil);
        when(mockObjLiteral.getLexicalForm()).thenReturn(MANAGE_GRP, "2050");

        Map<String, Set<String>> results = aclFactory.getPrincipalRoles(pid);

        assertEquals("Incorrect number of principals returned", 1, results.size());
        Set<String> roles = results.get(MANAGE_GRP);
        assertNotNull(roles);
        assertEquals("Incorrect number of roles for principal", 1, roles.size());
        assertTrue(roles.contains(CdrAcl.canManage.toString()));
    }

    @Test
    public void getPrincipalRoleNoPropertiesTest() throws Exception {
        when(mockResultSet.hasNext()).thenReturn(false);

        Map<String, Set<String>> results = aclFactory.getPrincipalRoles(pid);

        assertEquals("No principals should be returned", 0, results.size());
    }

    @Test
    public void getPrincipalRoleNoRolesTest() throws Exception {
        when(mockQuerySoln.getResource(eq("pred")))
                .thenReturn(CdrAcl.embargoUntil);
        when(mockObjLiteral.getLexicalForm()).thenReturn("2050");

        Map<String, Set<String>> results = aclFactory.getPrincipalRoles(pid);

        assertEquals("No principals should be returned", 0, results.size());
    }

    @Test
    public void getMultiplePrincipalsTest() throws Exception {
        when(mockResultSet.hasNext()).thenReturn(true, true, true, false);
        when(mockQuerySoln.getResource(eq("pred")))
                .thenReturn(CdrAcl.canManage, CdrAcl.canDiscover, CdrAcl.unitOwner);
        when(mockObjLiteral.getLexicalForm())
                .thenReturn(MANAGE_GRP, PATRON_GRP, USER_PRINC);

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
        when(mockResultSet.hasNext()).thenReturn(true, true, false);
        when(mockQuerySoln.getResource(eq("pred")))
                .thenReturn(CdrAcl.canManage, CdrAcl.canDescribe);
        when(mockObjLiteral.getLexicalForm()).thenReturn(MANAGE_GRP);

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
        when(mockQuerySoln.getResource(eq("pred"))).thenReturn(CdrAcl.markedForDeletion);
        when(mockObjLiteral.getLexicalForm()).thenReturn("true");

        boolean result = aclFactory.isMarkedForDeletion(pid);

        assertTrue(result);
    }

    @Test
    public void isMarkedForDeletionFalseTest() throws Exception {
        when(mockQuerySoln.getResource(eq("pred"))).thenReturn(CdrAcl.patronAccess);
        when(mockObjLiteral.getLexicalForm()).thenReturn(PatronAccess.everyone.name());

        boolean result = aclFactory.isMarkedForDeletion(pid);

        assertFalse(result);
    }

    @Test
    public void getEmbargoUntilTest() throws Exception {
        String embargoValue = "2050-01-02";

        when(mockResultSet.hasNext()).thenReturn(true, true, false);
        when(mockQuerySoln.getResource(eq("pred")))
                .thenReturn(CdrAcl.canManage, CdrAcl.embargoUntil);
        when(mockObjLiteral.getLexicalForm()).thenReturn(MANAGE_GRP, embargoValue);

        Date embargoDate = aclFactory.getEmbargoUntil(pid);

        assertNotNull(embargoDate);
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
        assertEquals(embargoValue, dt.format(embargoDate));
    }

    @Test
    public void getNoEmbargoUntilTest() throws Exception {
        when(mockResultSet.hasNext()).thenReturn(true, false);
        when(mockQuerySoln.getResource(eq("pred")))
                .thenReturn(CdrAcl.canManage);
        when(mockObjLiteral.getLexicalForm()).thenReturn(MANAGE_GRP);

        Date embargoDate = aclFactory.getEmbargoUntil(pid);

        assertNull(embargoDate);
    }

    @Test
    public void getInvalidEmbargoUntilTest() throws Exception {
        when(mockResultSet.hasNext()).thenReturn(true, false);
        when(mockQuerySoln.getResource(eq("pred")))
                .thenReturn(CdrAcl.embargoUntil);
        when(mockObjLiteral.getLexicalForm()).thenReturn("notadate");

        Date embargoDate = aclFactory.getEmbargoUntil(pid);

        assertNull(embargoDate);
    }

    @Test
    public void getPatronAccessTest() throws Exception {
        when(mockQuerySoln.getResource(eq("pred"))).thenReturn(CdrAcl.patronAccess);
        when(mockObjLiteral.getLexicalForm()).thenReturn(PatronAccess.everyone.name());

        PatronAccess access = aclFactory.getPatronAccess(pid);

        assertEquals("Incorrect patron access setting retrieved", PatronAccess.everyone, access);
    }

    @Test
    public void getPatronAccessNotSpecifiedTest() throws Exception {
        when(mockQuerySoln.getResource(eq("pred"))).thenReturn(CdrAcl.canManage);
        when(mockObjLiteral.getLexicalForm()).thenReturn(MANAGE_GRP);

        PatronAccess access = aclFactory.getPatronAccess(pid);

        assertEquals("Default patron access expected", PatronAccess.parent, access);
    }

    @Test
    public void cacheValueUsedTest() throws Exception {
        when(mockResultSet.hasNext()).thenReturn(true, false, true, false);
        when(mockQuerySoln.getResource(eq("pred")))
                .thenReturn(CdrAcl.canManage);
        when(mockObjLiteral.getLexicalForm()).thenReturn(MANAGE_GRP);

        Map<String, Set<String>> results = aclFactory.getPrincipalRoles(pid);
        assertEquals("Incorrect number of principals returned", 1, results.size());

        verify(queryService).executeQuery(anyString());

        // Second run, should not change the number of sparql invocations
        results = aclFactory.getPrincipalRoles(pid);

        verify(queryService).executeQuery(anyString());

        assertEquals("Incorrect number of principals on second run", 1, results.size());
        Set<String> roles = results.get(MANAGE_GRP);
        assertEquals("Incorrect number of roles for principal on second run", 1, roles.size());
        assertTrue(roles.contains(CdrAcl.canManage.toString()));
    }

    @Test
    public void cacheValueExpiredTest() throws Exception {
        when(mockResultSet.hasNext()).thenReturn(true, false, true, false);
        when(mockQuerySoln.getResource(eq("pred")))
                .thenReturn(CdrAcl.canManage);
        when(mockObjLiteral.getLexicalForm()).thenReturn(MANAGE_GRP);

        Map<String, Set<String>> results = aclFactory.getPrincipalRoles(pid);
        verify(queryService).executeQuery(anyString());
        assertEquals("Incorrect number of principals returned", 1, results.size());

        // Wait for expiration time
        Thread.sleep(CACHE_TIME_TO_LIVE * 2);

        results = aclFactory.getPrincipalRoles(pid);
        assertEquals("Incorrect number of principals returned", 1, results.size());
        assertEquals("Incorrect number of roles for principal", 1, results.get(MANAGE_GRP).size());

        verify(queryService, times(2)).executeQuery(anyString());
    }

    @Test
    public void multiplePidCaching() throws Exception {
        when(mockResultSet.hasNext()).thenReturn(true, false, true, true, false);
        when(mockQuerySoln.getResource(eq("pred")))
                .thenReturn(CdrAcl.canManage, CdrAcl.canAccess, CdrAcl.canIngest);
        when(mockObjLiteral.getLexicalForm()).thenReturn(MANAGE_GRP, PATRON_GRP, USER_PRINC);

        Map<String, Set<String>> results = aclFactory.getPrincipalRoles(pid);
        assertEquals("Incorrect number of principals returned for pid1", 1, results.size());
        Set<String> roles = results.get(MANAGE_GRP);
        assertEquals("Incorrect number of roles for principal for pid1", 1, roles.size());
        assertTrue(roles.contains(CdrAcl.canManage.toString()));

        PID pid2 = makePid();

        results = aclFactory.getPrincipalRoles(pid2);
        assertEquals("Incorrect number of principals returned for pid2", 2, results.size());
        assertEquals("Incorrect number of roles for patron for pid2", 1, results.get(PATRON_GRP).size());
        assertEquals("Incorrect number of roles for user for pid2", 1, results.get(USER_PRINC).size());

        verify(queryService, times(2)).executeQuery(anyString());

        results = aclFactory.getPrincipalRoles(pid);
        assertEquals("Incorrect number of principals cached for pid1", 1, results.size());
        results = aclFactory.getPrincipalRoles(pid2);
        assertEquals("Incorrect number of principals cached for pid2", 2, results.size());

        verify(queryService, times(2)).executeQuery(anyString());
    }

    private PID makePid() {
        PID pid = mock(PID.class);
        when(pid.getRepositoryPath()).thenReturn(PID_BASE_URI + UUID.randomUUID().toString());
        return pid;
    }
}

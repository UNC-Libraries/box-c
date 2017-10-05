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
import static java.lang.Integer.parseInt;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.fuseki.embedded.FusekiEmbeddedServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.DatasetImpl;
import org.apache.jena.vocabulary.RDF;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.CdrAcl;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.sparql.FusekiSparqlQueryServiceImpl;
import edu.unc.lib.dl.sparql.SparqlQueryService;

/**
 *
 * @author bbpennel
 *
 */
public class ObjectPermissionEvaluatorIT {

    private static final String FUSEKI_PORT = System
            .getProperty("fuseki.dynamic.test.port", "48080");

    private static final String FUSEKI_BASE_URI =
            "http://localhost:" + FUSEKI_PORT + "/fuseki/test";

    private final static String PRINC_GRP1 = "group1";
    private final static String PRINC_GRP2 = "group2";

    private static final long CACHE_MAX_SIZE = 100l;
    private static final long CACHE_TIME_TO_LIVE = 100L;

    private ObjectAclFactory aclFactory;

    private FusekiEmbeddedServer server;

    private SparqlQueryService sparqlService;

    private Model fusekiModel;

    private ObjectPermissionEvaluator objectPermissionEvaluator;

    private PID pid;

    @Before
    public void init() {
        fusekiModel = createDefaultModel();
        Dataset ds = new DatasetImpl(fusekiModel);
        server = FusekiEmbeddedServer.create().setPort(parseInt(FUSEKI_PORT))
                .setContextPath("/fuseki").add("/test", ds)
                .build();
        server.start();

        sparqlService = new FusekiSparqlQueryServiceImpl();
        ((FusekiSparqlQueryServiceImpl) sparqlService).setFusekiQueryURL(FUSEKI_BASE_URI);

        aclFactory = new ObjectAclFactory();
        aclFactory.setQueryService(sparqlService);
        aclFactory.setCacheMaxSize(CACHE_MAX_SIZE);
        aclFactory.setCacheTimeToLive(CACHE_TIME_TO_LIVE);
        aclFactory.init();

        objectPermissionEvaluator = new ObjectPermissionEvaluator();
        objectPermissionEvaluator.setAclFactory(aclFactory);

        pid = PIDs.get(UUID.randomUUID().toString());
    }

    @After
    public void tearDownFuseki() throws Exception {
        server.stop();
    }

    @Test
    public void hasStaffPermission() {
        createObject(pid).addLiteral(CdrAcl.canManage, PRINC_GRP1);

        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        assertTrue(objectPermissionEvaluator.hasStaffPermission(pid, principals, Permission.ingest));
    }

    @Test
    public void doesNotHaveStaffPermission() {
        createObject(pid).addLiteral(CdrAcl.canViewOriginals, PRINC_GRP1);

        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        assertFalse(objectPermissionEvaluator.hasStaffPermission(pid, principals, Permission.ingest));
    }

    @Test
    public void getPatronPrincipalsTest() {
        createObject(pid).addLiteral(CdrAcl.canViewOriginals, PRINC_GRP1);

        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        Set<String> permittedPrincipals = objectPermissionEvaluator
                .getPatronPrincipalsWithPermission(pid, principals, Permission.viewMetadata);

        assertEquals(1, permittedPrincipals.size());
        assertTrue(permittedPrincipals.contains(PRINC_GRP1));
    }

    @Test
    public void getPatronPrincipalsWithPermissionNoPatronRolesTest() throws Exception {
        createObject(pid).addLiteral(CdrAcl.canManage, PRINC_GRP1)
                .addLiteral(CdrAcl.canManage, PRINC_GRP2);

        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        Set<String> permittedPrincipals = objectPermissionEvaluator
                .getPatronPrincipalsWithPermission(pid, principals, Permission.viewMetadata);

        assertEquals(0, permittedPrincipals.size());
    }

    @Test
    public void hasPatronAccessNoModificationsTest() throws Exception {
        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        assertTrue(objectPermissionEvaluator
                .hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void hasPatronAccessNoneTest() throws Exception {
        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        createObject(pid).addLiteral(CdrAcl.patronAccess, PatronAccess.none.name());

        assertFalse(objectPermissionEvaluator
                .hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void hasPatronAccessAuthenticatedTest() throws Exception {
        Set<String> principals = new HashSet<>(Arrays.asList(AUTHENTICATED_PRINC));

        createObject(pid).addLiteral(CdrAcl.patronAccess, PatronAccess.authenticated.name());

        assertTrue(objectPermissionEvaluator
                .hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void hasPatronAccessDeletedTest() throws Exception {
        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        createObject(pid).addLiteral(CdrAcl.markedForDeletion, true);

        assertFalse(objectPermissionEvaluator
                .hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void hasPatronAccessEmbargoedTest() throws Exception {
        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        // Set the embargo to tomorrow (via a calendar) so that it will not be expired
        GregorianCalendar tomorrow = GregorianCalendar.from(ZonedDateTime.now().plusDays(1));
        createObject(pid).addLiteral(CdrAcl.embargoUntil, tomorrow);

        assertFalse(objectPermissionEvaluator
                .hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void hasPatronAccessToMetadataEmbargoedTest() throws Exception {
        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        // Set the embargo to tomorrow (via a calendar) so that it will not be expired
        GregorianCalendar tomorrow = GregorianCalendar.from(ZonedDateTime.now().plusDays(1));
        createObject(pid).addLiteral(CdrAcl.embargoUntil, tomorrow);

        assertTrue(objectPermissionEvaluator
                .hasPatronAccess(pid, principals, Permission.viewMetadata));
    }

    @Test
    public void hasPatronAccessExpiredEmbargoTest() throws Exception {
        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        // Set the embargo to tomorrow (via a calendar) so that it will not be expired
        GregorianCalendar yesterday = GregorianCalendar.from(ZonedDateTime.now().plusDays(-1));
        createObject(pid).addLiteral(CdrAcl.embargoUntil, yesterday);

        assertTrue(objectPermissionEvaluator
                .hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    private Resource createObject(PID pid) {
        Resource resc = fusekiModel.getResource(pid.getRepositoryPath());

        resc.addProperty(RDF.type, Fcrepo4Repository.Container);
        resc.addProperty(RDF.type, Fcrepo4Repository.Resource);

        return resc;
    }
}

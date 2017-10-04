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

import static java.lang.Integer.parseInt;
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.jena.fuseki.embedded.FusekiEmbeddedServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.DatasetImpl;
import org.apache.jena.vocabulary.RDF;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.ContentPathFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.sparql.FusekiSparqlQueryServiceImpl;
import edu.unc.lib.dl.sparql.SparqlQueryService;
import edu.unc.lib.dl.test.TestHelper;

/**
 *
 * @author bbpennel
 *
 */
public class AccessControlServiceImplIT {

    private static final String FUSEKI_PORT = System
            .getProperty("fuseki.dynamic.test.port", "48080");

    private static final String FUSEKI_BASE_URI =
            "http://localhost:" + FUSEKI_PORT + "/fuseki/test";

    private static final long CACHE_MAX_SIZE = 100l;
    private static final long CACHE_TIME_TO_LIVE = 100l;

    private static final String ADMIN_UNIT_1_PATH =
            "http://example.com/rest/content/e7/4a/e8/b4/e74ae8b4-873f-49cc-81d1-d728b75fa230";
    private static final String COLL_1_PATH =
            "http://example.com/rest/content/c5/e9/ca/ba/c5e9caba-4773-4d37-9204-89df4f6b28c8";
    private static final String COLL_1_WORK_1_PATH =
            "http://example.com/rest/content/93/e1/15/27/93e11527-dbb6-416a-86db-3092fea52f37";
    private static final String COLL_1_FOLDER_1_PATH =
            "http://example.com/rest/content/b0/c1/92/ba/b0c192ba-6960-49d4-a29c-ef386934b915";
    private static final String COLL_1_WORK_2_PATH =
            "http://example.com/rest/content/d6/e5/f1/ce/d6e5f1ce-c09c-413d-a273-f5cf44660812";
    private static final String COLL_2_PATH =
            "http://example.com/rest/content/b5/78/75/3e/b578753e-4f6c-44b8-a68c-a0c7112308ff";
    private static final String COLL_2_WORK_1_PATH =
            "http://example.com/rest/content/1f/b9/ec/31/1fb9ec31-5bca-48a0-b819-9ba636146336";
    private static final String COLL_2_WORK_2_PATH =
            "http://example.com/rest/content/21/3b/ee/6a/213bee6a-1018-4cc2-8c6f-459864ef23b1";
    private static final String COLL_3_PATH =
            "http://example.com/rest/content/11/01/68/55/11016855-da92-47c8-a42f-bcf0f104620e";
    private static final String ADMIN_UNIT_2_PATH =
            "http://example.com/rest/content/92/47/7c/1a/92477c1a-ad7d-46ce-a708-4132c1eecd40";

    private static final String EVERYONE_PRINC = "everyone";

    private static final String UNIT_OWNER_PRINC = "adminUser";

    private static final String VIEWER_PRINC = "uarms-viewer";

    private static final String UNIT_MANAGER_PRINC = "wilsontech";

    private ContentPathFactory pathFactory;

    private ObjectAclFactory aclFactory;

    private ObjectPermissionEvaluator objectPermissionEvaluator;

    private InheritedPermissionEvaluator permissionEvaluator;

    private GlobalPermissionEvaluator globalPermissionEvaluator;

    private AccessControlServiceImpl aclService;

    private SparqlQueryService sparqlService;

    private static FusekiEmbeddedServer server;

    private static Model fusekiModel;

    @BeforeClass
    public static void setupFuseki() {
        fusekiModel = createDefaultModel();
        String structurePath = AccessControlServiceImplIT.class
                .getResource("/acl/acl-example-structure.ttl").toString();
        fusekiModel.read(structurePath);
        Dataset ds = new DatasetImpl(fusekiModel);
        server = FusekiEmbeddedServer.create().setPort(parseInt(FUSEKI_PORT))
                .setContextPath("/fuseki")
                .add("/test", ds, false)
                .build();
        server.start();
    }

    @AfterClass
    public static void tearDownFuseki() throws Exception {
        server.stop();
    }

    @Before
    public void init() throws Exception {
        TestHelper.setContentBase("http://example.com/rest");

        sparqlService = new FusekiSparqlQueryServiceImpl();
        ((FusekiSparqlQueryServiceImpl) sparqlService).setFusekiQueryURL(FUSEKI_BASE_URI);

        Properties properties = new Properties();
        properties.load(this.getClass().getResourceAsStream("/acl/config.properties"));
        globalPermissionEvaluator = new GlobalPermissionEvaluator(properties);

        pathFactory = new ContentPathFactory();
        pathFactory.setCacheMaxSize(CACHE_MAX_SIZE);
        pathFactory.setCacheTimeToLive(CACHE_TIME_TO_LIVE);
        pathFactory.setQueryService(sparqlService);
        pathFactory.init();

        aclFactory = new ObjectAclFactory();
        aclFactory.setQueryService(sparqlService);
        aclFactory.setCacheMaxSize(CACHE_MAX_SIZE);
        aclFactory.setCacheTimeToLive(CACHE_TIME_TO_LIVE);
        aclFactory.init();

        objectPermissionEvaluator = new ObjectPermissionEvaluator();
        objectPermissionEvaluator.setAclFactory(aclFactory);

        permissionEvaluator = new InheritedPermissionEvaluator();
        permissionEvaluator.setObjectPermissionEvaluator(objectPermissionEvaluator);
        permissionEvaluator.setPathFactory(pathFactory);

        aclService = new AccessControlServiceImpl();
        aclService.setGlobalPermissionEvaluator(globalPermissionEvaluator);
        aclService.setPermissionEvaluator(permissionEvaluator);
    }

    @Test
    public void hasGlobalAdminAccess() {

        final AccessGroupSet principals = new AccessGroupSet("adminGroup");

        getAllContentObjects().forEach(pid -> {
            assertTrue(aclService.hasAccess(pid, principals, Permission.destroy));
            assertTrue(aclService.hasAccess(pid, principals, Permission.editDescription));
            assertTrue(aclService.hasAccess(pid, principals, Permission.viewOriginal));
        });
    }

    @Test
    public void hasGlobalDescribeAccess() {

        final AccessGroupSet principals = new AccessGroupSet("rdm");

        // Ensure that global describe group can describe everywhere but not destroy
        getAllContentObjects().forEach(pid -> {
            assertFalse(aclService.hasAccess(pid, principals, Permission.destroy));
            assertTrue(aclService.hasAccess(pid, principals, Permission.editDescription));
            assertTrue(aclService.hasAccess(pid, principals, Permission.viewOriginal));
        });
    }

    @Test
    public void hasGlobalViewAccess() {

        final AccessGroupSet principals = new AccessGroupSet("auditors");

        // Ensure that the global access group can read everything, but not edit
        getAllContentObjects().forEach(pid -> {
            assertFalse(aclService.hasAccess(pid, principals, Permission.destroy));
            assertFalse(aclService.hasAccess(pid, principals, Permission.editDescription));
            assertTrue(aclService.hasAccess(pid, principals, Permission.viewOriginal));
        });
    }

    @Test
    public void localHigherPermissionOverridesGlobal() {
        final AccessGroupSet principals = new AccessGroupSet("rdm");

        PID collPid = PIDs.get(COLL_2_PATH);
        assertTrue("canManage role on collection 2 should override global canDescribe",
                aclService.hasAccess(collPid, principals, Permission.markForDeletion));
    }

    @Test
    public void unitHasPatronPermissionTest() {
        final AccessGroupSet principals = new AccessGroupSet(EVERYONE_PRINC);
        PID unitPid = PIDs.get(ADMIN_UNIT_1_PATH);

        assertTrue("Everyone should be able to view unit",
                aclService.hasAccess(unitPid, principals, Permission.viewMetadata));
    }

    @Test
    public void everyoneHasAccessToPublicWorkTest() {
        final AccessGroupSet principals = new AccessGroupSet(EVERYONE_PRINC);
        PID pid = PIDs.get(COLL_1_WORK_1_PATH);

        assertTrue("Everyone should be able to access unrestricted work",
                aclService.hasAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void assertEveryoneCanAccess() {
        final AccessGroupSet principals = new AccessGroupSet(EVERYONE_PRINC);
        PID pid = PIDs.get(COLL_1_WORK_1_PATH);

        aclService.assertHasAccess(null, pid, principals, Permission.viewOriginal);
    }

    @Test(expected = AccessRestrictionException.class)
    public void assertEveryoneCannotAccess() {
        final AccessGroupSet principals = new AccessGroupSet(EVERYONE_PRINC);
        PID pid = PIDs.get(COLL_1_WORK_2_PATH);

        aclService.assertHasAccess(null, pid, principals, Permission.viewOriginal);
    }

    @Test
    public void everyoneCannotAccessRestrictedFolderTest() {
        final AccessGroupSet principals = new AccessGroupSet(EVERYONE_PRINC);
        PID pid = PIDs.get(COLL_1_FOLDER_1_PATH);

        assertFalse("Everyone should not be able to access staff only folder",
                aclService.hasAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void everyoneCannotAccessWorkInheritedFromFolderTest() {
        final AccessGroupSet principals = new AccessGroupSet(EVERYONE_PRINC);
        PID pid = PIDs.get(COLL_1_WORK_2_PATH);

        assertFalse("Everyone should not be able to access work in a staff only folder",
                aclService.hasAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void everyoneCannotAccessRestrictedCollTest() {
        final AccessGroupSet principals = new AccessGroupSet(EVERYONE_PRINC);
        PID pid = PIDs.get(COLL_2_PATH);

        assertFalse("Everyone should not be able to access staff only collection",
                aclService.hasAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void everyoneCannotAccessWorkInRestrictedCollTest() {
        final AccessGroupSet principals = new AccessGroupSet(EVERYONE_PRINC);
        PID pid = PIDs.get(COLL_2_WORK_1_PATH);

        assertFalse("Everyone should not be able to work in restricted collection",
                aclService.hasAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void staffViewerCanAccessStaffOnlyWorkInRestrictedCollTest() {
        final AccessGroupSet principals = new AccessGroupSet(VIEWER_PRINC);
        PID pid = PIDs.get(COLL_2_WORK_2_PATH);

        assertTrue("Staff user should be able to access patron restricted work",
                aclService.hasAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void staffViewerCannotModifyCollectionTest() {
        final AccessGroupSet principals = new AccessGroupSet(VIEWER_PRINC);
        PID pid = PIDs.get(COLL_2_PATH);

        assertFalse("Staff user should not be able to modify collection",
                aclService.hasAccess(pid, principals, Permission.editDescription));
    }

    @Test
    public void collStaffCannotViewOtherColl() {
        final AccessGroupSet principals = new AccessGroupSet(VIEWER_PRINC);
        PID pid = PIDs.get(COLL_3_PATH);

        assertFalse("Collection assigned staff user should not be able to access a different collection",
                aclService.hasAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void unitOwnerHasAccessTest() {
        final AccessGroupSet principals = new AccessGroupSet(UNIT_OWNER_PRINC);
        PID unitPid = PIDs.get(ADMIN_UNIT_1_PATH);

        assertTrue("Unit owner should be able to create collections in unit",
                aclService.hasAccess(unitPid, principals, Permission.createCollection));
        assertTrue("Unit owner should be able to modify contained collection",
                aclService.hasAccess(PIDs.get(COLL_1_PATH), principals, Permission.ingest));
        assertTrue("Unit owner should be able to modify restricted work",
                aclService.hasAccess(PIDs.get(COLL_2_WORK_2_PATH), principals, Permission.destroy));
    }

    @Test
    public void unitOwnerCannotModifyOtherUnitTest() {
        final AccessGroupSet principals = new AccessGroupSet(UNIT_OWNER_PRINC);
        PID unitPid = PIDs.get(ADMIN_UNIT_2_PATH);

        assertFalse("Unit owner 1 should not be able to create collections in another unit",
                aclService.hasAccess(unitPid, principals, Permission.createCollection));
    }

    @Test
    public void unitManagerHasAccessTest() {
        final AccessGroupSet principals = new AccessGroupSet(UNIT_MANAGER_PRINC);
        PID unitPid = PIDs.get(ADMIN_UNIT_1_PATH);

        assertTrue("Manager should be able to modify unit",
                aclService.hasAccess(unitPid, principals, Permission.editDescription));
        assertFalse("Manager should not be able to create collections in unit",
                aclService.hasAccess(unitPid, principals, Permission.createCollection));
        assertTrue("Manager should be able to modify contained collection",
                aclService.hasAccess(PIDs.get(COLL_1_PATH), principals, Permission.ingest));
        assertTrue("Manager should be able to modify all contained collections",
                aclService.hasAccess(PIDs.get(COLL_3_PATH), principals, Permission.ingest));
        assertTrue("Manager should be able to modify restricted work in all contained collections",
                aclService.hasAccess(PIDs.get(COLL_2_WORK_2_PATH), principals, Permission.editDescription));
    }

    private List<PID> getAllContentObjects() {
        return fusekiModel.listResourcesWithProperty(RDF.type).toList().stream()
                .map(p -> PIDs.get(p.getURI()))
                .collect(Collectors.toList());
    }
}

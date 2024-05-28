package edu.unc.lib.boxc.integration.acl;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PATRON_NAMESPACE;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.model.fcrepo.test.TestRepositoryDeinitializer;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.AccessControlServiceImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GlobalPermissionEvaluatorImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.InheritedPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.services.ObjectAclFactory;
import edu.unc.lib.boxc.integration.fcrepo.AbstractFedoraIT;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.AclModelBuilder;

/**
 *
 * @author bbpennel
 *
 */
public class AccessControlServiceImplIT extends AbstractFedoraIT {
    private static final long CACHE_MAX_SIZE = 100l;
    private static final long CACHE_TIME_TO_LIVE = 100l;

    private static ContentRootObject contentRoot;
    private static AdminUnit adminUnit1;
    private static CollectionObject collObj1;
    private static FolderObject collObj1Folder1;
    private static WorkObject collObj1Folder1Work1;
    private static FolderObject collObj1Folder2;
    private static WorkObject collObj1Folder2Work1;
    private static WorkObject collObj1Folder2Work2;
    private static WorkObject collObj1Folder2Work3;
    private static WorkObject collObj1Work2;
    private static CollectionObject collObj2;
    private static FolderObject collObj2Folder1;
    private static WorkObject collObj2Folder1Work1;
    private static WorkObject collObj2Work2;
    private static CollectionObject collObj3;
    private static AdminUnit adminUnit2;

    private static final String PATRON_GROUP = PATRON_NAMESPACE + "special";

    private static final String UNIT_OWNER_PRINC = "adminUser";

    private static final String UNIT2_OWNER_PRINC = "adminUser2";

    private static final String VIEWER_PRINC = "uarms-viewer";

    private static final String UNIT_MANAGER_PRINC = "wilsontech";

    private static final String STAFF_PRINC = "rdm";

    @Autowired
    private ContentPathFactory pathFactory;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    private RepositoryInitializer repoInitializer;

    private ObjectAclFactory aclFactory;

    private InheritedPermissionEvaluator permissionEvaluator;

    private GlobalPermissionEvaluator globalPermissionEvaluator;

    private AccessControlServiceImpl aclService;
    private static FcrepoClient staticFcrepoClient;

    @BeforeEach
    public void init() throws Exception {
        Properties properties = new Properties();
        properties.load(this.getClass().getResourceAsStream("/acl/config.properties"));
        globalPermissionEvaluator = new GlobalPermissionEvaluatorImpl(properties);

        aclFactory = new ObjectAclFactory();
        aclFactory.setRepositoryObjectLoader(repositoryObjectLoader);
        aclFactory.setCacheMaxSize(CACHE_MAX_SIZE);
        aclFactory.setCacheTimeToLive(CACHE_TIME_TO_LIVE);
        aclFactory.init();

        permissionEvaluator = new InheritedPermissionEvaluator();
        permissionEvaluator.setPathFactory(pathFactory);
        permissionEvaluator.setObjectAclFactory(aclFactory);

        aclService = new AccessControlServiceImpl();
        aclService.setGlobalPermissionEvaluator(globalPermissionEvaluator);
        aclService.setPermissionEvaluator(permissionEvaluator);

        staticFcrepoClient = fcrepoClient;

        initStructure();
    }

    private void initStructure() throws Exception {
        // Only create once
        if (contentRoot != null) {
            return;
        }

        repoInitializer.initializeRepository();
        contentRoot = repoObjLoader.getContentRootObject(RepositoryPaths.getContentRootPid());

        adminUnit1 = repoObjFactory.createAdminUnit(
                new AclModelBuilder("Admin Unit 1")
                    .addUnitOwner(UNIT_OWNER_PRINC)
                    .addCanManage(UNIT_MANAGER_PRINC)
                    .model);
        contentRoot.addMember(adminUnit1);

        collObj1 = repoObjFactory.createCollectionObject(
                new AclModelBuilder("Coll1 Public Collection")
                    .addCanViewOriginals(PUBLIC_PRINC)
                    .addCanViewOriginals(AUTHENTICATED_PRINC)
                    .addCanViewOriginals(PATRON_GROUP)
                    .model);
        adminUnit1.addMember(collObj1);

        collObj1Folder1 = repoObjFactory.createFolderObject(
                new AclModelBuilder("Folder No Patron PubColl")
                    .addNoneRole(PUBLIC_PRINC)
                    .model);
        collObj1.addMember(collObj1Folder1);

        collObj1Folder1Work1 = collObj1Folder1.addWork(
                new AclModelBuilder("Work Unmodified in Folder No Patron").model);

        collObj1Folder2 = repoObjFactory.createFolderObject(
                new AclModelBuilder("Folder Downgraded Public")
                    .addCanViewMetadata(PUBLIC_PRINC)
                    .model);
        collObj1.addMember(collObj1Folder2);

        collObj1Folder2Work1 = collObj1Folder2.addWork(
                new AclModelBuilder("Work Unmodified in Folder Downgraded Public").model);

        collObj1Folder2Work2 = collObj1Folder2.addWork(
                new AclModelBuilder("Work Staff Only in Folder Downgraded Public")
                    .addNoneRole(PUBLIC_PRINC)
                    .addNoneRole(AUTHENTICATED_PRINC)
                    .model);

        collObj1Folder2Work3 = collObj1Folder2.addWork(
                new AclModelBuilder("Work Re-added special group in Folder Downgraded Public")
                    .addNoneRole(PUBLIC_PRINC)
                    .addNoneRole(AUTHENTICATED_PRINC)
                    .addCanViewOriginals(PATRON_GROUP)
                    .model);

        collObj1Work2 = repoObjFactory.createWorkObject(
                new AclModelBuilder("Work Unmodified PubColl").model);
        collObj1.addMember(collObj1Work2);

        // Staff only collection
        collObj2 = repoObjFactory.createCollectionObject(
                new AclModelBuilder("Staff Only Collection")
                    .addCanAccess(VIEWER_PRINC)
                    .addCanManage(STAFF_PRINC)
                    .model);
        adminUnit1.addMember(collObj2);

        collObj2Folder1 = repoObjFactory.createFolderObject(
                new AclModelBuilder("Folder No Patron Staff Only Coll")
                    .addNoneRole(PUBLIC_PRINC)
                    .model);
        collObj2.addMember(collObj2Folder1);

        collObj2Folder1Work1 = collObj2Folder1.addWork(
                new AclModelBuilder("Work Unmodified in Folder in Staff Coll").model);

        collObj2Work2 = repoObjFactory.createWorkObject(
                new AclModelBuilder("Work Unmodified Staff Coll").model);
        collObj2.addMember(collObj2Work2);

        // Unit staff only collection
        adminUnit2 = repoObjFactory.createAdminUnit(
                new AclModelBuilder("Admin Unit 2")
                    .addUnitOwner(UNIT2_OWNER_PRINC)
                    .model);
        contentRoot.addMember(adminUnit2);

        collObj3 = repoObjFactory.createCollectionObject(
                new AclModelBuilder("Unit Staff Only Collection").model);
        adminUnit2.addMember(collObj3);

        treeIndexer.indexAll(baseAddress);
    }

    @AfterEach
    public void cleanup() throws Exception {
        // Preventing cleanup of repo until all tests complete
    }

    @AfterAll
    public static void cleanupAll() throws Exception {
        TestRepositoryDeinitializer.cleanup(staticFcrepoClient);
    }

    @Test
    public void hasGlobalAdminAccess() {

        final AccessGroupSet principals = new AccessGroupSetImpl("adminGroup");

        getAllContentObjects().forEach(pid -> {
            assertTrue(aclService.hasAccess(pid, principals, Permission.destroy));
            assertTrue(aclService.hasAccess(pid, principals, Permission.editDescription));
            assertTrue(aclService.hasAccess(pid, principals, Permission.viewOriginal));
        });
    }

    @Test
    public void hasGlobalDescribeAccess() {

        final AccessGroupSet principals = new AccessGroupSetImpl(STAFF_PRINC);

        // Ensure that global describe group can describe everywhere but not destroy
        getAllContentObjects().forEach(pid -> {
            assertFalse(aclService.hasAccess(pid, principals, Permission.destroy));
            assertTrue(aclService.hasAccess(pid, principals, Permission.editDescription));
            assertTrue(aclService.hasAccess(pid, principals, Permission.viewOriginal));
        });
    }

    @Test
    public void hasGlobalViewAccess() {

        final AccessGroupSet principals = new AccessGroupSetImpl("auditors");

        // Ensure that the global access group can read everything, but not edit
        getAllContentObjects().forEach(pid -> {
            assertFalse(aclService.hasAccess(pid, principals, Permission.destroy));
            assertFalse(aclService.hasAccess(pid, principals, Permission.editDescription));
            assertTrue(aclService.hasAccess(pid, principals, Permission.viewOriginal));
        });
    }

    @Test
    public void localHigherPermissionOverridesGlobal() {
        final AccessGroupSet principals = new AccessGroupSetImpl(STAFF_PRINC);

        assertTrue(aclService.hasAccess(collObj2.getPid(), principals, Permission.markForDeletion),
                "canManage role on collection 2 should override global canDescribe");
    }

    @Test
    public void unitHasPatronPermissionTest() {
        final AccessGroupSet principals = new AccessGroupSetImpl(PUBLIC_PRINC);

        assertTrue(aclService.hasAccess(adminUnit1.getPid(), principals, Permission.viewMetadata),
                "Everyone should be able to view unit");
    }

    @Test
    public void everyoneHasAccessToPublicWorkTest() {
        final AccessGroupSet principals = new AccessGroupSetImpl(PUBLIC_PRINC);

        assertTrue(aclService.hasAccess(collObj1Work2.getPid(), principals, Permission.viewOriginal),
                "Everyone should be able to access unrestricted work");
    }

    @Test
    public void assertEveryoneCanAccess() {
        final AccessGroupSet principals = new AccessGroupSetImpl(PUBLIC_PRINC);

        aclService.assertHasAccess(null, collObj1Work2.getPid(), principals, Permission.viewOriginal);
    }

    @Test
    public void assertEveryoneCannotAccess() {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            final AccessGroupSet principals = new AccessGroupSetImpl(PUBLIC_PRINC);

            aclService.assertHasAccess(null, collObj1Folder1Work1.getPid(), principals, Permission.viewOriginal);
        });
    }

    @Test
    public void everyoneCannotAccessRestrictedFolderTest() {
        final AccessGroupSet principals = new AccessGroupSetImpl(PUBLIC_PRINC);

        assertFalse(aclService.hasAccess(collObj1Folder1.getPid(), principals, Permission.viewOriginal),
                "Everyone should not be able to access staff only folder");
    }

    @Test
    public void everyoneCannotAccessWorkInheritedFromFolderTest() {
        final AccessGroupSet principals = new AccessGroupSetImpl(PUBLIC_PRINC);

        assertFalse(aclService.hasAccess(collObj2Work2.getPid(), principals, Permission.viewOriginal),
                "Everyone should not be able to access work in a staff only folder");
    }

    @Test
    public void everyoneCannotAccessRestrictedCollTest() {
        final AccessGroupSet principals = new AccessGroupSetImpl(PUBLIC_PRINC);

        assertFalse(aclService.hasAccess(collObj2.getPid(), principals, Permission.viewOriginal),
                "Everyone should not be able to access staff only collection");
    }

    @Test
    public void everyoneCannotAccessWorkInRestrictedCollTest() {
        final AccessGroupSet principals = new AccessGroupSetImpl(PUBLIC_PRINC);

        assertFalse(aclService.hasAccess(collObj2Folder1Work1.getPid(), principals, Permission.viewOriginal),
                "Everyone should not be able to work in restricted collection");
    }

    @Test
    public void everyoneDowngradedAccess() {
        final AccessGroupSet principals = new AccessGroupSetImpl(PUBLIC_PRINC);

        assertTrue(aclService.hasAccess(collObj1Folder2Work1.getPid(), principals, Permission.viewMetadata),
                "Everyone must have view metadata access");
        assertFalse(aclService.hasAccess(collObj1Folder2Work1.getPid(), principals, Permission.viewOriginal),
                "Everyone must not have originals access");
    }

    @Test
    public void authenticatedHigherPermissions() {
        final AccessGroupSet principals = new AccessGroupSetImpl(PUBLIC_PRINC, AUTHENTICATED_PRINC);

        assertTrue(aclService.hasAccess(collObj1Folder2.getPid(), principals, Permission.viewOriginal),
                "Authenticated must have view originals access for folder");
        assertTrue(aclService.hasAccess(collObj1Folder2Work1.getPid(), principals, Permission.viewOriginal),
                "Authenticated must have view originals access for work");
    }

    @Test
    public void specialPatronGroupHigherPermissions() {
        final AccessGroupSet principals = new AccessGroupSetImpl(PUBLIC_PRINC, PATRON_GROUP);

        assertTrue(aclService.hasAccess(collObj1Folder2.getPid(), principals, Permission.viewOriginal),
                "Patron group must have view originals access for folder");
        assertTrue(aclService.hasAccess(collObj1Folder2Work1.getPid(), principals, Permission.viewOriginal),
                "Patron group must have view originals access for work");
    }

    @Test
    public void specialPatronGroupStaffOnly() {
        final AccessGroupSet principals = new AccessGroupSetImpl(PUBLIC_PRINC, PATRON_GROUP);

        assertFalse(aclService.hasAccess(collObj1Folder2Work2.getPid(), principals, Permission.viewMetadata),
                "Patron group must have no access for work");
    }

    @Test
    public void specialPatronGroupStaffOnlyReUpped() {
        final AccessGroupSet principals = new AccessGroupSetImpl(PUBLIC_PRINC, PATRON_GROUP);

        assertTrue(aclService.hasAccess(collObj1Folder2Work3.getPid(), principals, Permission.viewOriginal),
                "Patron group must have originals access for work");
    }

    @Test
    public void staffViewerCanAccessStaffOnlyWorkInRestrictedCollTest() {
        final AccessGroupSet principals = new AccessGroupSetImpl(VIEWER_PRINC);

        assertTrue(aclService.hasAccess(collObj2Work2.getPid(), principals, Permission.viewOriginal),
                "Staff user should be able to access patron restricted work");
    }

    @Test
    public void staffViewerCannotModifyCollectionTest() {
        final AccessGroupSet principals = new AccessGroupSetImpl(VIEWER_PRINC);

        assertFalse(aclService.hasAccess(collObj2.getPid(), principals, Permission.editDescription),
                "Staff user should not be able to modify collection");
    }

    @Test
    public void collStaffCannotViewOtherColl() {
        final AccessGroupSet principals = new AccessGroupSetImpl(VIEWER_PRINC);

        assertFalse(aclService.hasAccess(collObj3.getPid(), principals, Permission.viewOriginal),
                "Collection assigned staff user should not be able to access a different collection");
    }

    @Test
    public void unitOwnerHasAccessTest() {
        final AccessGroupSet principals = new AccessGroupSetImpl(UNIT_OWNER_PRINC);

        assertTrue(aclService.hasAccess(adminUnit1.getPid(), principals, Permission.createCollection),
                "Unit owner should be able to create collections in unit");
        assertTrue(aclService.hasAccess(collObj1.getPid(), principals, Permission.ingest),
                "Unit owner should be able to modify contained collection");
        assertTrue(aclService.hasAccess(collObj2Work2.getPid(), principals, Permission.destroy),
                "Unit owner should be able to modify restricted work");
    }

    @Test
    public void unitOwnerCannotModifyOtherUnitTest() {
        final AccessGroupSet principals = new AccessGroupSetImpl(UNIT_OWNER_PRINC);

        assertFalse(aclService.hasAccess(adminUnit2.getPid(), principals, Permission.createCollection),
                "Unit owner 1 should not be able to create collections in another unit");
    }

    @Test
    public void unitManagerHasAccessTest() {
        final AccessGroupSet principals = new AccessGroupSetImpl(UNIT_MANAGER_PRINC);

        assertTrue(aclService.hasAccess(adminUnit1.getPid(), principals, Permission.editDescription),
                "Manager should be able to modify unit");
        assertFalse(aclService.hasAccess(adminUnit1.getPid(), principals, Permission.destroy),
                "Manager should not be able to create collections in unit");
        assertTrue(aclService.hasAccess(collObj1.getPid(), principals, Permission.ingest),
                "Manager should be able to modify contained collection");
        assertTrue(aclService.hasAccess(collObj2.getPid(), principals, Permission.ingest),
                "Manager should be able to modify all contained collections");
        assertTrue(aclService.hasAccess(collObj2Work2.getPid(), principals, Permission.editDescription),
                "Manager should be able to modify restricted work in all contained collections");
    }

    private List<PID> getAllContentObjects() {
        return queryModel.listResourcesWithProperty(RDF.type, PcdmModels.Object).toList().stream()
                .map(p -> PIDs.get(p.getURI()))
                .collect(Collectors.toList());
    }
}

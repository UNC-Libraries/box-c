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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.AbstractFedoraIT;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.ContentRootObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectCacheLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.ContentPathFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.test.AclModelBuilder;

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
    private static WorkObject collObj1Work2;
    private static CollectionObject collObj2;
    private static FolderObject collObj2Folder1;
    private static WorkObject collObj2Folder1Work1;
    private static WorkObject collObj2Work2;
    private static CollectionObject collObj3;
    private static AdminUnit adminUnit2;

    private static final String EVERYONE_PRINC = "everyone";

    private static final String UNIT_OWNER_PRINC = "adminUser";

    private static final String UNIT2_OWNER_PRINC = "adminUser2";

    private static final String VIEWER_PRINC = "uarms-viewer";

    private static final String UNIT_MANAGER_PRINC = "wilsontech";

    private static final String STAFF_PRINC = "rdm";

    @Autowired
    private ContentPathFactory pathFactory;
    @Autowired
    private RepositoryObjectCacheLoader repositoryObjectCacheLoader;

    private ObjectAclFactory aclFactory;

    private ObjectPermissionEvaluator objectPermissionEvaluator;

    private InheritedPermissionEvaluator permissionEvaluator;

    private GlobalPermissionEvaluator globalPermissionEvaluator;

    private AccessControlServiceImpl aclService;

    @Before
    public void init() throws Exception {
        Properties properties = new Properties();
        properties.load(this.getClass().getResourceAsStream("/acl/config.properties"));
        globalPermissionEvaluator = new GlobalPermissionEvaluator(properties);

        aclFactory = new ObjectAclFactory();
        aclFactory.setRepositoryObjectCacheLoader(repositoryObjectCacheLoader);
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

        initStructure();
    }

    private void initStructure() throws Exception {
        // Only create once
        if (contentRoot != null) {
            return;
        }

        repoObjFactory.createContentRootObject(
                RepositoryPaths.getContentRootPid().getRepositoryUri(), null);
        contentRoot = repoObjLoader.getContentRootObject(RepositoryPaths.getContentRootPid());

        adminUnit1 = repoObjFactory.createAdminUnit(
                new AclModelBuilder("Admin Unit 1")
                    .addUnitOwner(UNIT_OWNER_PRINC)
                    .addCanManage(UNIT_MANAGER_PRINC)
                    .model);
        contentRoot.addMember(adminUnit1);

        collObj1 = repoObjFactory.createCollectionObject(
                new AclModelBuilder("Coll1 Public Collection")
                    .addCanViewOriginals(EVERYONE_PRINC)
                    .model);
        adminUnit1.addMember(collObj1);

        collObj1Folder1 = repoObjFactory.createFolderObject(
                new AclModelBuilder("Folder No Patron PubColl")
                    .addPatronAccess("none")
                    .model);
        collObj1.addMember(collObj1Folder1);

        collObj1Folder1Work1 = collObj1Folder1.addWork(
                new AclModelBuilder("Work Unmodified in Folder No Patron").model);

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
                    .addPatronAccess("none")
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

        final AccessGroupSet principals = new AccessGroupSet(STAFF_PRINC);

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
        final AccessGroupSet principals = new AccessGroupSet(STAFF_PRINC);

        assertTrue("canManage role on collection 2 should override global canDescribe",
                aclService.hasAccess(collObj2.getPid(), principals, Permission.markForDeletion));
    }

    @Test
    public void unitHasPatronPermissionTest() {
        final AccessGroupSet principals = new AccessGroupSet(EVERYONE_PRINC);

        assertTrue("Everyone should be able to view unit",
                aclService.hasAccess(adminUnit1.getPid(), principals, Permission.viewMetadata));
    }

    @Test
    public void everyoneHasAccessToPublicWorkTest() {
        final AccessGroupSet principals = new AccessGroupSet(EVERYONE_PRINC);

        assertTrue("Everyone should be able to access unrestricted work",
                aclService.hasAccess(collObj1Work2.getPid(), principals, Permission.viewOriginal));
    }

    @Test
    public void assertEveryoneCanAccess() {
        final AccessGroupSet principals = new AccessGroupSet(EVERYONE_PRINC);

        aclService.assertHasAccess(null, collObj1Work2.getPid(), principals, Permission.viewOriginal);
    }

    @Test(expected = AccessRestrictionException.class)
    public void assertEveryoneCannotAccess() {
        final AccessGroupSet principals = new AccessGroupSet(EVERYONE_PRINC);

        aclService.assertHasAccess(null, collObj1Folder1Work1.getPid(), principals, Permission.viewOriginal);
    }

    @Test
    public void everyoneCannotAccessRestrictedFolderTest() {
        final AccessGroupSet principals = new AccessGroupSet(EVERYONE_PRINC);

        assertFalse("Everyone should not be able to access staff only folder",
                aclService.hasAccess(collObj1Folder1.getPid(), principals, Permission.viewOriginal));
    }

    @Test
    public void everyoneCannotAccessWorkInheritedFromFolderTest() {
        final AccessGroupSet principals = new AccessGroupSet(EVERYONE_PRINC);

        assertFalse("Everyone should not be able to access work in a staff only folder",
                aclService.hasAccess(collObj2Work2.getPid(), principals, Permission.viewOriginal));
    }

    @Test
    public void everyoneCannotAccessRestrictedCollTest() {
        final AccessGroupSet principals = new AccessGroupSet(EVERYONE_PRINC);

        assertFalse("Everyone should not be able to access staff only collection",
                aclService.hasAccess(collObj2.getPid(), principals, Permission.viewOriginal));
    }

    @Test
    public void everyoneCannotAccessWorkInRestrictedCollTest() {
        final AccessGroupSet principals = new AccessGroupSet(EVERYONE_PRINC);

        assertFalse("Everyone should not be able to work in restricted collection",
                aclService.hasAccess(collObj2Folder1Work1.getPid(), principals, Permission.viewOriginal));
    }

    @Test
    public void staffViewerCanAccessStaffOnlyWorkInRestrictedCollTest() {
        final AccessGroupSet principals = new AccessGroupSet(VIEWER_PRINC);

        assertTrue("Staff user should be able to access patron restricted work",
                aclService.hasAccess(collObj2Work2.getPid(), principals, Permission.viewOriginal));
    }

    @Test
    public void staffViewerCannotModifyCollectionTest() {
        final AccessGroupSet principals = new AccessGroupSet(VIEWER_PRINC);

        assertFalse("Staff user should not be able to modify collection",
                aclService.hasAccess(collObj2.getPid(), principals, Permission.editDescription));
    }

    @Test
    public void collStaffCannotViewOtherColl() {
        final AccessGroupSet principals = new AccessGroupSet(VIEWER_PRINC);

        assertFalse("Collection assigned staff user should not be able to access a different collection",
                aclService.hasAccess(collObj3.getPid(), principals, Permission.viewOriginal));
    }

    @Test
    public void unitOwnerHasAccessTest() {
        final AccessGroupSet principals = new AccessGroupSet(UNIT_OWNER_PRINC);

        assertTrue("Unit owner should be able to create collections in unit",
                aclService.hasAccess(adminUnit1.getPid(), principals, Permission.createCollection));
        assertTrue("Unit owner should be able to modify contained collection",
                aclService.hasAccess(collObj1.getPid(), principals, Permission.ingest));
        assertTrue("Unit owner should be able to modify restricted work",
                aclService.hasAccess(collObj2Work2.getPid(), principals, Permission.destroy));
    }

    @Test
    public void unitOwnerCannotModifyOtherUnitTest() {
        final AccessGroupSet principals = new AccessGroupSet(UNIT_OWNER_PRINC);

        assertFalse("Unit owner 1 should not be able to create collections in another unit",
                aclService.hasAccess(adminUnit2.getPid(), principals, Permission.createCollection));
    }

    @Test
    public void unitManagerHasAccessTest() {
        final AccessGroupSet principals = new AccessGroupSet(UNIT_MANAGER_PRINC);

        assertTrue("Manager should be able to modify unit",
                aclService.hasAccess(adminUnit1.getPid(), principals, Permission.editDescription));
        assertFalse("Manager should not be able to create collections in unit",
                aclService.hasAccess(adminUnit1.getPid(), principals, Permission.createCollection));
        assertTrue("Manager should be able to modify contained collection",
                aclService.hasAccess(collObj1.getPid(), principals, Permission.ingest));
        assertTrue("Manager should be able to modify all contained collections",
                aclService.hasAccess(collObj2.getPid(), principals, Permission.ingest));
        assertTrue("Manager should be able to modify restricted work in all contained collections",
                aclService.hasAccess(collObj2Work2.getPid(), principals, Permission.editDescription));
    }

    private List<PID> getAllContentObjects() {
        return queryModel.listResourcesWithProperty(RDF.type, PcdmModels.Object).toList().stream()
                .map(p -> PIDs.get(p.getURI()))
                .collect(Collectors.toList());
    }
}

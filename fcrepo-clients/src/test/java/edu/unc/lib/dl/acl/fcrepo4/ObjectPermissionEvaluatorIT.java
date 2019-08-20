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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.acl.service.PatronAccess;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.AbstractFedoraIT;
import edu.unc.lib.dl.fcrepo4.ContentRootObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectCacheLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPaths;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.CdrAcl;
import edu.unc.lib.dl.test.AclModelBuilder;

/**
 *
 * @author bbpennel
 *
 */
public class ObjectPermissionEvaluatorIT extends AbstractFedoraIT {
    private final static String PRINC_GRP1 = "group1";
    private final static String PRINC_GRP2 = "group2";

    private static final long CACHE_MAX_SIZE = 100l;
    private static final long CACHE_TIME_TO_LIVE = 100L;

    private ObjectAclFactory aclFactory;

    @Autowired
    private RepositoryObjectCacheLoader repositoryObjectCacheLoader;

    private ObjectPermissionEvaluator objectPermissionEvaluator;

    private PID pid;

    private static ContentRootObject contentRoot;

    @Before
    public void init() {
        aclFactory = new ObjectAclFactory();
        aclFactory.setRepositoryObjectCacheLoader(repositoryObjectCacheLoader);
        aclFactory.setCacheMaxSize(CACHE_MAX_SIZE);
        aclFactory.setCacheTimeToLive(CACHE_TIME_TO_LIVE);
        aclFactory.init();

        objectPermissionEvaluator = new ObjectPermissionEvaluator();
        objectPermissionEvaluator.setAclFactory(aclFactory);

        pid = PIDs.get(UUID.randomUUID().toString());

        if (contentRoot == null) {
            repoObjFactory.createContentRootObject(
                    RepositoryPaths.getContentRootPid().getRepositoryUri(), null);
            contentRoot = repoObjLoader.getContentRootObject(RepositoryPaths.getContentRootPid());
        }
    }

    @Test
    public void hasStaffPermission() {
        repoObjFactory.createAdminUnit(pid,
                new AclModelBuilder("Admin Unit Staff Perm")
                    .addCanManage(PRINC_GRP1)
                    .model);

        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        assertTrue(objectPermissionEvaluator.hasStaffPermission(pid, principals, Permission.ingest));
    }

    @Test
    public void doesNotHaveStaffPermission() {
        repoObjFactory.createCollectionObject(pid,
                new AclModelBuilder("Coll View Orig Perm")
                    .addCanViewOriginals(PRINC_GRP1)
                    .model);

        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        assertFalse(objectPermissionEvaluator.hasStaffPermission(pid, principals, Permission.ingest));
    }

    @Test
    public void getPatronPrincipalsTest() {
        repoObjFactory.createCollectionObject(pid,
                new AclModelBuilder("Coll View Orig Perm")
                    .addCanViewOriginals(PRINC_GRP1)
                    .model);

        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        Set<String> permittedPrincipals = objectPermissionEvaluator
                .getPatronPrincipalsWithPermission(pid, principals, Permission.viewMetadata);

        assertEquals(1, permittedPrincipals.size());
        assertTrue(permittedPrincipals.contains(PRINC_GRP1));
    }

    @Test
    public void getPatronPrincipalsWithPermissionNoPatronRolesTest() throws Exception {
        repoObjFactory.createCollectionObject(pid,
                new AclModelBuilder("Coll View Double Manager")
                    .addCanManage(PRINC_GRP1)
                    .addCanManage(PRINC_GRP2)
                    .model);

        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        Set<String> permittedPrincipals = objectPermissionEvaluator
                .getPatronPrincipalsWithPermission(pid, principals, Permission.viewMetadata);

        assertEquals(0, permittedPrincipals.size());
    }

    @Test
    public void hasPatronAccessNoModificationsTest() throws Exception {
        repoObjFactory.createCollectionObject(pid, null);

        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        assertTrue(objectPermissionEvaluator
                .hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void hasPatronAccessNoneTest() throws Exception {
        repoObjFactory.createCollectionObject(pid,
                new AclModelBuilder("Coll View No Patron")
                    .addPatronAccess(PatronAccess.none.name())
                    .model);
        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        assertFalse(objectPermissionEvaluator
                .hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void hasPatronAccessAuthenticatedTest() throws Exception {
        repoObjFactory.createCollectionObject(pid,
                new AclModelBuilder("Coll View Auth Patron")
                    .addPatronAccess(PatronAccess.authenticated.name())
                    .model);
        Set<String> principals = new HashSet<>(Arrays.asList(AUTHENTICATED_PRINC));

        assertTrue(objectPermissionEvaluator
                .hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void hasPatronAccessDeletedTest() throws Exception {
        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(pid.getRepositoryPath());
        resc.addLiteral(CdrAcl.markedForDeletion, true);
        repoObjFactory.createCollectionObject(pid, model);

        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        assertFalse(objectPermissionEvaluator
                .hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void hasPatronAccessEmbargoedTest() throws Exception {
        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        // Set the embargo to tomorrow (via a calendar) so that it will not be expired
        GregorianCalendar tomorrow = GregorianCalendar.from(ZonedDateTime.now().plusDays(1));
        repoObjFactory.createCollectionObject(pid,
                new AclModelBuilder("Coll View Embargoed")
                    .addEmbargoUntil(tomorrow)
                    .model);

        assertFalse(objectPermissionEvaluator
                .hasPatronAccess(pid, principals, Permission.viewOriginal));
    }

    @Test
    public void hasPatronAccessToMetadataEmbargoedTest() throws Exception {
        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        // Set the embargo to tomorrow (via a calendar) so that it will not be expired
        GregorianCalendar tomorrow = GregorianCalendar.from(ZonedDateTime.now().plusDays(1));
        repoObjFactory.createCollectionObject(pid,
                new AclModelBuilder("Coll View Embargoed")
                    .addEmbargoUntil(tomorrow)
                    .model);

        assertTrue(objectPermissionEvaluator
                .hasPatronAccess(pid, principals, Permission.viewMetadata));
    }

    @Test
    public void hasPatronAccessExpiredEmbargoTest() throws Exception {
        Set<String> principals = new HashSet<>(Arrays.asList(PRINC_GRP1));

        // Set the embargo to tomorrow (via a calendar) so that it will not be expired
        GregorianCalendar yesterday = GregorianCalendar.from(ZonedDateTime.now().plusDays(-1));
        repoObjFactory.createCollectionObject(pid,
                new AclModelBuilder("Coll View Embargoed")
                    .addEmbargoUntil(yesterday)
                    .model);

        assertTrue(objectPermissionEvaluator
                .hasPatronAccess(pid, principals, Permission.viewOriginal));
    }
}

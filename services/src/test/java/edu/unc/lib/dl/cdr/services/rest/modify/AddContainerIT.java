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
package edu.unc.lib.dl.cdr.services.rest.modify;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.dl.acl.util.UserRole.canViewOriginals;
import static edu.unc.lib.dl.acl.util.UserRole.none;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.CONTENT_ROOT_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.DcElements;

/**
 *
 * @author harring
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/spring-test/acl-service-context.xml"),
    @ContextConfiguration("/add-container-it-servlet.xml")
})
public class AddContainerIT extends AbstractAPIIT {
    private static final String UNIT_MANAGER_PRINC = "wilsontech";

    @Before
    public void initRoot() {
        setupContentRoot();
    }

    @Test
    public void testAddCollectionToAdminUnit() throws UnsupportedOperationException, Exception {
        PID parentPid = makePid();

        AdminUnit parent = repositoryObjectFactory.createAdminUnit(parentPid, null);
        contentRoot.addMember(parent);
        treeIndexer.indexAll(baseAddress);

        String label = "collection_label";
        MvcResult result = mvc.perform(post("/edit/create/collection/" + parentPid.getId())
                .param("label", label))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        treeIndexer.indexAll(baseAddress);

        assertChildContainerAdded(parent, label, CollectionObject.class);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(parentPid.getUUID(), respMap.get("pid"));
        assertEquals("create", respMap.get("action"));
        assertPatronDoesNotHaveNonePermission(parent, label);
    }

    @Test
    public void testAddAdminUnit() throws Exception {
        String label = "admin_label";
        MvcResult result = mvc.perform(post("/edit/create/adminUnit/" + CONTENT_ROOT_ID)
                .param("label", label))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        treeIndexer.indexAll(baseAddress);

        assertChildContainerAdded(contentRoot, label, AdminUnit.class);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(CONTENT_ROOT_ID, respMap.get("pid"));
        assertEquals("create", respMap.get("action"));
        assertPatronDoesNotHaveNonePermission(contentRoot, label);
    }

    @Test
    public void testAddFolderStaffOnly() throws Exception {
        AdminUnit adminUnit = repositoryObjectFactory.createAdminUnit(null);
        contentRoot.addMember(adminUnit);
        CollectionObject collObj = repositoryObjectFactory.createCollectionObject(null);
        adminUnit.addMember(collObj);

        treeIndexer.indexAll(baseAddress);

        String label = "folder_label";
        String staffOnly = "true";
        MvcResult result = mvc.perform(post("/edit/create/folder/" + collObj.getPid().getId())
                .param("label", label)
                .param("staffOnly", staffOnly))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        treeIndexer.indexAll(baseAddress);

        assertChildContainerAdded(collObj, label, FolderObject.class);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(collObj.getPid().getId(), respMap.get("pid"));
        assertEquals("create", respMap.get("action"));

        ContentObject member = getMemberByLabel(collObj, label);
        assertHasAssignment(PUBLIC_PRINC, none, member);
        assertHasAssignment(AUTHENTICATED_PRINC, none, member);
    }

    @Test
    public void testAddWork() throws Exception {
        AdminUnit adminUnit = repositoryObjectFactory.createAdminUnit(null);
        contentRoot.addMember(adminUnit);
        CollectionObject collObj = repositoryObjectFactory.createCollectionObject(null);
        adminUnit.addMember(collObj);

        treeIndexer.indexAll(baseAddress);

        String label = "work_label";
        MvcResult result = mvc.perform(post("/edit/create/work/" + collObj.getPid().getId())
                .param("label", label))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        treeIndexer.indexAll(baseAddress);

        assertChildContainerAdded(collObj, label, WorkObject.class);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(collObj.getPid().getId(), respMap.get("pid"));
        assertEquals("create", respMap.get("action"));
        assertPatronDoesNotHaveNonePermission(collObj, label);
    }

    @Test
    public void testAddAdminUnitToCollection() throws UnsupportedOperationException, Exception {
        PID parentPid = makePid();

        CollectionObject parent = repositoryObjectFactory.createCollectionObject(parentPid, null);
        treeIndexer.indexAll(baseAddress);

        assertChildContainerNotAdded(parent);

        String label = "admin_unit";
        MvcResult result = mvc.perform(post("/edit/create/adminUnit/" + parentPid.getUUID())
                .param("label", label)
                .param("patronOnly", (String) null))
            .andExpect(status().isInternalServerError())
            .andReturn();

        treeIndexer.indexAll(baseAddress);

        assertChildContainerNotAdded(parent);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(parentPid.getUUID(), respMap.get("pid"));
        assertEquals("create", respMap.get("action"));
        assertTrue(respMap.containsKey("error"));
    }

    @Test
    public void testAddDefaultCollection() throws UnsupportedOperationException, Exception {
        AdminUnit adminUnit = repositoryObjectFactory.createAdminUnit(null);
        contentRoot.addMember(adminUnit);

        treeIndexer.indexAll(baseAddress);

        String label = "collection";
        String adminId = adminUnit.getPid().getId();
        MvcResult result = mvc.perform(post("/edit/create/collection/" + adminId)
                .param("label", label)
                .param("staffOnly", "false"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        treeIndexer.indexAll(baseAddress);

        assertChildContainerAdded(adminUnit, label, CollectionObject.class);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(adminId, respMap.get("pid"));
        assertEquals("create", respMap.get("action"));
        ContentObject member = getMemberByLabel(adminUnit, label);
        assertHasAssignment(PUBLIC_PRINC, canViewOriginals, member);
        assertHasAssignment(AUTHENTICATED_PRINC, canViewOriginals, member);
    }

    @Test
    public void testAddStaffOnlyCollection() throws UnsupportedOperationException, Exception {
        AdminUnit adminUnit = repositoryObjectFactory.createAdminUnit(null);
        contentRoot.addMember(adminUnit);

        treeIndexer.indexAll(baseAddress);

        String label = "collection";
        String adminId = adminUnit.getPid().getId();
        MvcResult result = mvc.perform(post("/edit/create/collection/" + adminId)
                .param("label", label)
                .param("staffOnly", "true"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        treeIndexer.indexAll(baseAddress);

        assertChildContainerAdded(adminUnit, label, CollectionObject.class);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(adminId, respMap.get("pid"));
        assertEquals("create", respMap.get("action"));
        ContentObject member = getMemberByLabel(adminUnit, label);
        assertHasAssignment(PUBLIC_PRINC, none, member);
        assertHasAssignment(AUTHENTICATED_PRINC, none, member);
    }

    @Test
    public void testAuthorizationFailure() throws Exception {
        GroupsThreadStore.storeGroups(new AccessGroupSet(UNIT_MANAGER_PRINC));

        AdminUnit adminUnit = repositoryObjectFactory.createAdminUnit(null);
        contentRoot.addMember(adminUnit);
        CollectionObject collObj = repositoryObjectFactory.createCollectionObject(null);
        adminUnit.addMember(collObj);
        treeIndexer.indexAll(baseAddress);

        MvcResult result = mvc.perform(post("/edit/create/folder/" + collObj.getPid().getId())
                .param("label", "folder11"))
            .andReturn();

        assertChildContainerNotAdded(collObj);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(collObj.getPid().getId(), respMap.get("pid"));
        assertEquals("create", respMap.get("action"));
        assertTrue(respMap.containsKey("error"));
    }

    private ContentObject getMemberByLabel(ContentContainerObject parent, String label) {
        return parent.getMembers().stream()
                .filter(m -> m.getResource().hasProperty(DcElements.title, label))
                .findFirst().get();
    }

    private void assertChildContainerAdded(ContentContainerObject parent, String label, Class<?> memberClass) {
        ContentObject member = getMemberByLabel(parent, label);
        assertTrue(memberClass.isInstance(member));
    }

    private void assertChildContainerNotAdded(ContentContainerObject parent) {
        assertTrue(parent.getMembers().size() == 0);
    }

    private void assertPatronDoesNotHaveNonePermission(ContentContainerObject parent, String label) {
        ContentObject member = getMemberByLabel(parent, label);

        assertNoAssignment(PUBLIC_PRINC, none, member);
        assertNoAssignment(AUTHENTICATED_PRINC, none, member);
    }

    private void assertNoAssignment(String princ, UserRole role, RepositoryObject obj) {
        Resource resc = obj.getResource();
        assertFalse("Expected role " + role.name() + " was assigned for " + princ,
                resc.hasProperty(role.getProperty(), princ));
    }

    private void assertHasAssignment(String princ, UserRole role, RepositoryObject obj) {
        Resource resc = obj.getResource();
        assertTrue("Expected role " + role.name() + " was not assigned for " + princ,
                resc.hasProperty(role.getProperty(), princ));
    }
}

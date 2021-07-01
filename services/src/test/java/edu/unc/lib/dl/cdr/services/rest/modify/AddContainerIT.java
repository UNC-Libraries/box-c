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

import static edu.unc.lib.boxc.common.xml.SecureXMLFactory.createSAXBuilder;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.CONTENT_ROOT_ID;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.dl.acl.util.UserRole.canViewOriginals;
import static edu.unc.lib.dl.acl.util.UserRole.none;
import static edu.unc.lib.dl.util.DescriptionConstants.COLLECTION_NUMBER_EL;
import static edu.unc.lib.dl.util.DescriptionConstants.COLLECTION_NUMBER_LABEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.apache.jena.rdf.model.Resource;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.rdf.DcElements;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.test.AclModelBuilder;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.util.DescriptionConstants;

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
    public void testAddFolderAccess() throws Exception {
        AdminUnit adminUnit = repositoryObjectFactory.createAdminUnit(new AclModelBuilder("Access")
                .addCanAccess("accessGroup").model);
        GroupsThreadStore.storeGroups(new AccessGroupSet("accessGroup"));
        contentRoot.addMember(adminUnit);
        CollectionObject collObj = repositoryObjectFactory.createCollectionObject(null);
        adminUnit.addMember(collObj);

        treeIndexer.indexAll(baseAddress);

        String label = "folder_label";
        String staffOnly = "false";
        mvc.perform(post("/edit/create/folder/" + collObj.getPid().getId())
                .param("label", label)
                .param("staffOnly", staffOnly))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void testAddFolderStaffOnlyAccess() throws Exception {
        AdminUnit adminUnit = repositoryObjectFactory.createAdminUnit(new AclModelBuilder("Access")
                .addCanAccess("accessGroup").model);
        GroupsThreadStore.storeGroups(new AccessGroupSet("accessGroup"));
        contentRoot.addMember(adminUnit);
        CollectionObject collObj = repositoryObjectFactory.createCollectionObject(null);
        adminUnit.addMember(collObj);

        treeIndexer.indexAll(baseAddress);

        String label = "folder_label";
        String staffOnly = "true";
        mvc.perform(post("/edit/create/folder/" + collObj.getPid().getId())
                .param("label", label)
                .param("staffOnly", staffOnly))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    public void testAddFolderIngestor() throws Exception {
        AdminUnit adminUnit = repositoryObjectFactory.createAdminUnit(new AclModelBuilder("Ingesting")
                .addCanIngest("ingestorGroup").model);

        GroupsThreadStore.storeGroups(new AccessGroupSet("ingestorGroup"));
        contentRoot.addMember(adminUnit);
        CollectionObject collObj = repositoryObjectFactory.createCollectionObject(null);
        adminUnit.addMember(collObj);

        treeIndexer.indexAll(baseAddress);

        String label = "folder_label";
        String staffOnly = "false";
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
        assertPatronDoesNotHaveNonePermission(collObj, label);
    }


    @Test
    public void testAddFolderStaffOnlyIngestor() throws Exception {
        AdminUnit adminUnit = repositoryObjectFactory.createAdminUnit(new AclModelBuilder("Ingesting")
                .addCanIngest("ingestorGroup").model);

        GroupsThreadStore.storeGroups(new AccessGroupSet("ingestorGroup"));
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

        String label = "staff only collection";
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

        assertModsPopulated(member, label, null);
    }

    @Test
    public void testAddCollectionWithCollectionNumber() throws UnsupportedOperationException, Exception {
        AdminUnit adminUnit = repositoryObjectFactory.createAdminUnit(null);
        contentRoot.addMember(adminUnit);

        treeIndexer.indexAll(baseAddress);

        String label = "collection with number";
        String collNum = "12345678";
        String adminId = adminUnit.getPid().getId();
        MvcResult result = mvc.perform(post("/edit/create/collection/" + adminId)
                .param("label", label)
                .param("collectionNumber", collNum))
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

        assertModsPopulated(member, label, collNum);
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

    public void assertModsPopulated(ContentObject member, String label, String expectedCollNum) throws Exception {
        SAXBuilder sb = createSAXBuilder();
        Document doc = sb.build(member.getDescription().getBinaryStream());
        String title = doc.getRootElement().getChild("titleInfo", MODS_V3_NS)
                .getChildText("title", MODS_V3_NS);
        assertEquals("Title did not match expected value", label, title);

        Element idEl = doc.getRootElement().getChild(COLLECTION_NUMBER_EL, MODS_V3_NS);
        if (expectedCollNum != null) {
            assertEquals(COLLECTION_NUMBER_LABEL, idEl.getAttributeValue("displayLabel"));
            assertEquals(DescriptionConstants.COLLECTION_NUMBER_TYPE, idEl.getAttributeValue("type"));
            assertEquals(expectedCollNum, idEl.getText());
        } else {
            assertNull(idEl);
        }
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

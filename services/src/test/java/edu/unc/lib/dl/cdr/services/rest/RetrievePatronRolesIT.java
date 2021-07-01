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
package edu.unc.lib.dl.cdr.services.rest;

import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.USER_NAMESPACE;
import static edu.unc.lib.dl.acl.util.UserRole.canViewMetadata;
import static edu.unc.lib.dl.acl.util.UserRole.canViewOriginals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.objects.AdminUnitImpl;
import edu.unc.lib.boxc.model.fcrepo.objects.CollectionObjectImpl;
import edu.unc.lib.boxc.model.fcrepo.objects.FileObjectImpl;
import edu.unc.lib.boxc.model.fcrepo.objects.FolderObjectImpl;
import edu.unc.lib.boxc.model.fcrepo.objects.WorkObjectImpl;
import edu.unc.lib.boxc.model.fcrepo.test.AclModelBuilder;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.AccessPrincipalConstants;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.IPAddressPatronPrincipalConfig;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.cdr.services.rest.modify.AbstractAPIIT;
import edu.unc.lib.dl.persist.services.acl.PatronAccessDetails;

/**
*
* @author bbpennel
*
*/
@ContextHierarchy({
   @ContextConfiguration("/spring-test/test-fedora-container.xml"),
   @ContextConfiguration("/spring-test/cdr-client-container.xml"),
   @ContextConfiguration("/spring-test/acl-service-context.xml"),
   @ContextConfiguration("/access-control-retrieval-it-servlet.xml")
})
public class RetrievePatronRolesIT extends AbstractAPIIT {
    private static final String USER_PRINC = "user";
    private static final String USER_NS_PRINC = USER_NAMESPACE + USER_PRINC;

    private static final String origBodyString = "Original data";
    private static final String origFilename = "original.txt";
    private static final String origMimetype = "text/plain";

    private AdminUnitImpl adminUnit;
    private CollectionObjectImpl collObj;

    @Before
    public void init_() throws Exception {
        AccessGroupSet testPrincipals = new AccessGroupSet(PUBLIC_PRINC);
        GroupsThreadStore.storeUsername(USER_PRINC);
        GroupsThreadStore.storeGroups(testPrincipals);
        setupContentRoot();
    }

    @After
    public void teardown() throws Exception {
        GroupsThreadStore.clearStore();
    }

    @Test
    public void insufficientPermissions() throws Exception {
        // Creating unit/coll with no permissions granted
        AdminUnitImpl unit = repositoryObjectFactory.createAdminUnit(null);
        contentRoot.addMember(unit);
        PID pid = pidMinter.mintContentPid();
        CollectionObjectImpl coll = repositoryObjectFactory.createCollectionObject(pid, null);
        unit.addMember(coll);

        treeIndexer.indexAll(baseAddress);

        mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void objectNotFound() throws Exception {
        PID pid = pidMinter.mintContentPid();

        treeIndexer.indexAll(baseAddress);

        mvc.perform(get("/acl/staff/" + pid.getId()))
                .andExpect(status().isNotFound())
                .andReturn();
    }

    @Test
    public void getFromUnit() throws Exception {
        createCollectionInUnit(null);

        treeIndexer.indexAll(baseAddress);

        mvc.perform(get("/acl/patron/" + adminUnit.getPid().getId()))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void getFromCollectionWithNoPatrons() throws Exception {
        createCollectionInUnit(null);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/" + collObj.getPid().getId()))
                .andExpect(status().isOk())
                .andReturn();

        PatronRolesResponse result = parseResponse(mvcResult);

        assertInfoEmpty(result.inherited);

        assertInfoEmpty(result.assigned);
    }

    @Test
    public void getFromCollectionWithPatrons() throws Exception {
        createCollectionInUnit(new AclModelBuilder("Collection")
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC)
                .model);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/" + collObj.getPid().getId()))
                .andExpect(status().isOk())
                .andReturn();

        PatronRolesResponse result = parseResponse(mvcResult);
        assertInfoEmpty(result.inherited);

        PatronAccessDetails assigned = result.assigned;
        assertEquals(2, assigned.getRoles().size());
        assertHasRole(assigned, PUBLIC_PRINC, canViewMetadata);
        assertHasRole(assigned, AUTHENTICATED_PRINC, canViewOriginals);
        assertFalse(assigned.isDeleted());
        assertNull(assigned.getEmbargo());

        assertEquals(1, result.allowedPrincipals.size());
        IPAddressPatronPrincipalConfig config = result.allowedPrincipals.get(0);
        assertEquals("unc:patron:ipp:test_group", config.getPrincipal());
        assertEquals("Test Patron Group", config.getName());
        assertNull(config.getIpInclude());
    }

    @Test
    public void getAllowedPrincpals() throws Exception {
        AccessGroupSet testPrincipals = new AccessGroupSet(PUBLIC_PRINC, AccessPrincipalConstants.ADMIN_ACCESS_PRINC);
        GroupsThreadStore.storeGroups(testPrincipals);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/allowedPrincipals"))
                .andExpect(status().isOk())
                .andReturn();

        ObjectMapper mapper = new ObjectMapper();
        List<IPAddressPatronPrincipalConfig> allowed = mapper.readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<List<IPAddressPatronPrincipalConfig>>() {});

        assertEquals(1, allowed.size());
        IPAddressPatronPrincipalConfig config = allowed.get(0);
        assertEquals("unc:patron:ipp:test_group", config.getPrincipal());
        assertEquals("Test Patron Group", config.getName());
        assertNull(config.getIpInclude());
    }

    @Test
    public void getAllowedPrincpalsInsufficientPermissions() throws Exception {
        mvc.perform(get("/acl/patron/allowedPrincipals"))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    public void getFromCollectionWithPatronsInDeletedUnit() throws Exception {
        createCollectionInUnit(
                new AclModelBuilder("Collection")
                    .addCanViewMetadata(PUBLIC_PRINC)
                    .addCanViewOriginals(AUTHENTICATED_PRINC)
                    .model,
                new AclModelBuilder("Deleted Unit")
                    .addUnitOwner(USER_NS_PRINC)
                    .markForDeletion()
                    .model);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/" + collObj.getPid().getId()))
                .andExpect(status().isOk())
                .andReturn();

        PatronRolesResponse result = parseResponse(mvcResult);

        PatronAccessDetails inherited = result.inherited;
        assertNull(inherited.getRoles());
        assertTrue(inherited.isDeleted());
        assertNull(inherited.getEmbargo());

        PatronAccessDetails assigned = result.assigned;
        assertEquals(2, assigned.getRoles().size());
        assertHasRole(assigned, PUBLIC_PRINC, canViewMetadata);
        assertHasRole(assigned, AUTHENTICATED_PRINC, canViewOriginals);
        assertFalse(assigned.isDeleted());
        assertNull(assigned.getEmbargo());
    }

    @Test
    public void getFromCollectionWithPatronsDeleted() throws Exception {
        createCollectionInUnit(new AclModelBuilder("Collection")
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC)
                .markForDeletion()
                .model);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/" + collObj.getPid().getId()))
                .andExpect(status().isOk())
                .andReturn();

        PatronRolesResponse result = parseResponse(mvcResult);
        assertInfoEmpty(result.inherited);

        PatronAccessDetails assigned = result.assigned;
        assertEquals(2, assigned.getRoles().size());
        assertHasRole(assigned, PUBLIC_PRINC, canViewMetadata);
        assertHasRole(assigned, AUTHENTICATED_PRINC, canViewOriginals);
        assertTrue(assigned.isDeleted());
        assertNull(assigned.getEmbargo());
    }

    @Test
    public void getFromCollectionWithPatronsEmbargoed() throws Exception {
        Calendar embargoUntil = getNextYear();
        createCollectionInUnit(new AclModelBuilder("Collection")
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC)
                .addEmbargoUntil(embargoUntil)
                .model);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/" + collObj.getPid().getId()))
                .andExpect(status().isOk())
                .andReturn();

        PatronRolesResponse result = parseResponse(mvcResult);
        assertInfoEmpty(result.inherited);

        PatronAccessDetails assigned = result.assigned;
        assertEquals(2, assigned.getRoles().size());
        assertHasRole(assigned, PUBLIC_PRINC, canViewMetadata);
        assertHasRole(assigned, AUTHENTICATED_PRINC, canViewOriginals);
        assertFalse(assigned.isDeleted());
        assertEquals(embargoUntil.getTime(), assigned.getEmbargo());
    }

    @Test
    public void getFromFolderWithNoPatrons() throws Exception {
        createCollectionInUnit(null);
        FolderObjectImpl folder = repositoryObjectFactory.createFolderObject(null);
        collObj.addMember(folder);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/" + folder.getPid().getId()))
                .andExpect(status().isOk())
                .andReturn();

        PatronRolesResponse result = parseResponse(mvcResult);
        assertInfoEmpty(result.inherited);
        assertInfoEmpty(result.assigned);
    }

    @Test
    public void getFromFolderWithAssignedPatronsNoInherited() throws Exception {
        createCollectionInUnit(null);
        FolderObjectImpl folder = repositoryObjectFactory.createFolderObject(
                new AclModelBuilder("Folder with patrons")
                    .addCanViewMetadata(PUBLIC_PRINC)
                    .addCanViewOriginals(AUTHENTICATED_PRINC)
                    .model);
        collObj.addMember(folder);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/" + folder.getPid().getId()))
                .andExpect(status().isOk())
                .andReturn();

        PatronRolesResponse result = parseResponse(mvcResult);
        assertInfoEmpty(result.inherited);

        PatronAccessDetails assigned = result.assigned;
        assertEquals(2, assigned.getRoles().size());
        assertHasRole(assigned, PUBLIC_PRINC, canViewMetadata);
        assertHasRole(assigned, AUTHENTICATED_PRINC, canViewOriginals);
        assertFalse(assigned.isDeleted());
        assertNull(assigned.getEmbargo());
    }

    @Test
    public void getFromFolderWithInheritedPatrons() throws Exception {
        createCollectionInUnit(new AclModelBuilder("Collection")
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC)
                .model);
        FolderObjectImpl folder = repositoryObjectFactory.createFolderObject(null);
        collObj.addMember(folder);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/" + folder.getPid().getId()))
                .andExpect(status().isOk())
                .andReturn();

        PatronRolesResponse result = parseResponse(mvcResult);
        PatronAccessDetails inherited = result.inherited;
        assertEquals(2, inherited.getRoles().size());
        assertHasRole(inherited, PUBLIC_PRINC, canViewMetadata);
        assertHasRole(inherited, AUTHENTICATED_PRINC, canViewOriginals);
        assertFalse(inherited.isDeleted());
        assertNull(inherited.getEmbargo());

        assertInfoEmpty(result.assigned);
    }

    @Test
    public void getFromFolderWithInheritedAndAssignedPatrons() throws Exception {
        createCollectionInUnit(new AclModelBuilder("Collection")
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC)
                .model);
        FolderObjectImpl folder = repositoryObjectFactory.createFolderObject(
                new AclModelBuilder("Folder with patrons")
                    .addCanViewMetadata(AUTHENTICATED_PRINC)
                    .model);
        collObj.addMember(folder);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/" + folder.getPid().getId()))
                .andExpect(status().isOk())
                .andReturn();

        PatronRolesResponse result = parseResponse(mvcResult);
        PatronAccessDetails inherited = result.inherited;
        assertEquals(2, inherited.getRoles().size());
        assertHasRole(inherited, PUBLIC_PRINC, canViewMetadata);
        assertHasRole(inherited, AUTHENTICATED_PRINC, canViewOriginals);
        assertFalse(inherited.isDeleted());
        assertNull(inherited.getEmbargo());

        PatronAccessDetails assigned = result.assigned;
        assertEquals(1, assigned.getRoles().size());
        assertHasRole(assigned, AUTHENTICATED_PRINC, canViewMetadata);
        assertFalse(assigned.isDeleted());
        assertNull(assigned.getEmbargo());
    }

    @Test
    public void getFromFolderWithInheritedRevoke() throws Exception {
        createCollectionInUnit(new AclModelBuilder("Collection with none")
                .addNoneRole(PUBLIC_PRINC)
                .model);
        FolderObjectImpl folder = repositoryObjectFactory.createFolderObject(
                new AclModelBuilder("Folder with patrons")
                    .addCanViewMetadata(PUBLIC_PRINC)
                    .model);
        collObj.addMember(folder);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/" + folder.getPid().getId()))
                .andExpect(status().isOk())
                .andReturn();

        PatronRolesResponse result = parseResponse(mvcResult);
        assertInfoEmpty(result.inherited);

        PatronAccessDetails assigned = result.assigned;
        assertEquals(1, assigned.getRoles().size());
        assertHasRole(assigned, PUBLIC_PRINC, canViewMetadata);
        assertFalse(assigned.isDeleted());
        assertNull(assigned.getEmbargo());
    }

    @Test
    public void getFromFolderWithInheritedDeletion() throws Exception {
        createCollectionInUnit(new AclModelBuilder("Collection")
                .addCanViewOriginals(PUBLIC_PRINC)
                .markForDeletion()
                .model);
        FolderObjectImpl folder = repositoryObjectFactory.createFolderObject(null);
        collObj.addMember(folder);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/" + folder.getPid().getId()))
                .andExpect(status().isOk())
                .andReturn();

        PatronRolesResponse result = parseResponse(mvcResult);
        PatronAccessDetails inherited = result.inherited;
        assertEquals(0, inherited.getRoles().size());
        assertTrue(inherited.isDeleted());
        assertNull(inherited.getEmbargo());

        assertInfoEmpty(result.assigned);
    }

    @Test
    public void getFromFolderWithInheritedEmbargoed() throws Exception {
        Calendar embargoUntil = getNextYear();
        createCollectionInUnit(new AclModelBuilder("Collection")
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC)
                .addEmbargoUntil(embargoUntil)
                .model);
        FolderObjectImpl folder = repositoryObjectFactory.createFolderObject(null);
        collObj.addMember(folder);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/" + folder.getPid().getId()))
                .andExpect(status().isOk())
                .andReturn();

        PatronRolesResponse result = parseResponse(mvcResult);
        PatronAccessDetails inherited = result.inherited;
        assertEquals(2, inherited.getRoles().size());
        assertHasRole(inherited, PUBLIC_PRINC, canViewMetadata);
        // Auth principal role reflects the applied embargo
        assertHasRole(inherited, AUTHENTICATED_PRINC, canViewMetadata);
        assertFalse(inherited.isDeleted());
        assertEquals(embargoUntil.getTime(), inherited.getEmbargo());

        assertInfoEmpty(result.assigned);
    }

    @Test
    public void getFromFolderDeleted() throws Exception {
        createCollectionInUnit(
                new AclModelBuilder("Collection")
                    .addCanViewOriginals(PUBLIC_PRINC)
                    .model);
        FolderObjectImpl folder = repositoryObjectFactory.createFolderObject(
                new AclModelBuilder("Folder deleted")
                    .addCanViewOriginals(PUBLIC_PRINC)
                    .markForDeletion()
                    .model);
        collObj.addMember(folder);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/" + folder.getPid().getId()))
                .andExpect(status().isOk())
                .andReturn();

        PatronRolesResponse result = parseResponse(mvcResult);
        PatronAccessDetails inherited = result.inherited;
        assertEquals(1, inherited.getRoles().size());
        assertHasRole(inherited, PUBLIC_PRINC, canViewOriginals);
        assertFalse(inherited.isDeleted());
        assertNull(inherited.getEmbargo());

        PatronAccessDetails assigned = result.assigned;
        assertEquals(1, assigned.getRoles().size());
        // Assigned role should still be returned despite deleted status
        assertHasRole(assigned, PUBLIC_PRINC, canViewOriginals);
        assertTrue(assigned.isDeleted());
        assertNull(assigned.getEmbargo());
    }

    @Test
    public void getFromFolderEmbargoed() throws Exception {
        Calendar embargoUntil = getNextYear();
        createCollectionInUnit(
                new AclModelBuilder("Collection")
                    .addCanViewOriginals(PUBLIC_PRINC)
                    .model);
        FolderObjectImpl folder = repositoryObjectFactory.createFolderObject(
                new AclModelBuilder("Folder deleted")
                    .addCanViewOriginals(PUBLIC_PRINC)
                    .addEmbargoUntil(embargoUntil)
                    .model);
        collObj.addMember(folder);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/" + folder.getPid().getId()))
                .andExpect(status().isOk())
                .andReturn();

        PatronRolesResponse result = parseResponse(mvcResult);
        PatronAccessDetails inherited = result.inherited;
        assertEquals(1, inherited.getRoles().size());
        assertHasRole(inherited, PUBLIC_PRINC, canViewOriginals);
        assertFalse(inherited.isDeleted());
        assertNull(inherited.getEmbargo());

        PatronAccessDetails assigned = result.assigned;
        assertEquals(1, assigned.getRoles().size());
        // Role should be unaffected by embargo in assigned
        assertHasRole(assigned, PUBLIC_PRINC, canViewOriginals);
        assertFalse(assigned.isDeleted());
        assertEquals(embargoUntil.getTime(), assigned.getEmbargo());
    }

    @Test
    public void getFromWorkWithAllTheThings() throws Exception {
        Calendar embargoUntil = getNextYear();
        createCollectionInUnit(
                new AclModelBuilder("Collection")
                    .addCanViewOriginals(PUBLIC_PRINC)
                    .addCanViewOriginals(AUTHENTICATED_PRINC)
                    .markForDeletion()
                    .addEmbargoUntil(embargoUntil)
                    .model);
        WorkObjectImpl work = repositoryObjectFactory.createWorkObject(
                new AclModelBuilder("Work")
                    .addCanViewMetadata(PUBLIC_PRINC)
                    .markForDeletion()
                    .addEmbargoUntil(embargoUntil)
                    .model);
        collObj.addMember(work);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/" + work.getPid().getId()))
                .andExpect(status().isOk())
                .andReturn();

        PatronRolesResponse result = parseResponse(mvcResult);
        PatronAccessDetails inherited = result.inherited;
        assertTrue(inherited.getRoles().isEmpty());
        assertTrue(inherited.isDeleted());
        assertEquals(embargoUntil.getTime(), inherited.getEmbargo());

        PatronAccessDetails assigned = result.assigned;
        assertEquals(1, assigned.getRoles().size());
        assertHasRole(assigned, PUBLIC_PRINC, canViewMetadata);
        assertTrue(assigned.isDeleted());
        assertEquals(embargoUntil.getTime(), assigned.getEmbargo());
    }

    @Test
    public void getFromFolderWithCustomPatrons() throws Exception {
        String customGroup = "unc:patron:ipp:test_group";
        createCollectionInUnit(new AclModelBuilder("Collection")
                .addCanViewMetadata(PUBLIC_PRINC)
                .addCanViewOriginals(AUTHENTICATED_PRINC)
                .addCanViewOriginals(customGroup)
                .model);
        FolderObjectImpl folder = repositoryObjectFactory.createFolderObject(
                new AclModelBuilder("Folder with patrons")
                    .addNoneRole(PUBLIC_PRINC)
                    .addCanViewMetadata(AUTHENTICATED_PRINC)
                    .addCanViewOriginals(customGroup)
                    .model);
        collObj.addMember(folder);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/" + folder.getPid().getId()))
                .andExpect(status().isOk())
                .andReturn();

        PatronRolesResponse result = parseResponse(mvcResult);
        PatronAccessDetails inherited = result.inherited;
        assertEquals(3, inherited.getRoles().size());
        assertHasRole(inherited, PUBLIC_PRINC, canViewMetadata);
        assertHasRole(inherited, AUTHENTICATED_PRINC, canViewOriginals);
        assertHasRole(inherited, customGroup, canViewOriginals);
        assertFalse(inherited.isDeleted());
        assertNull(inherited.getEmbargo());

        PatronAccessDetails assigned = result.assigned;
        assertEquals(3, assigned.getRoles().size());
        assertHasRole(assigned, PUBLIC_PRINC, UserRole.none);
        assertHasRole(assigned, AUTHENTICATED_PRINC, canViewMetadata);
        assertHasRole(assigned, customGroup, canViewOriginals);
        assertFalse(assigned.isDeleted());
        assertNull(assigned.getEmbargo());
    }

    @Test
    public void getFromFileWithRevokedRole() throws Exception {
        createCollectionInUnit(
                new AclModelBuilder("Collection")
                    .addCanViewOriginals(PUBLIC_PRINC)
                    .model);
        WorkObjectImpl work = repositoryObjectFactory.createWorkObject(null);
        collObj.addMember(work);

        Path originalPath = Files.createTempFile("file", ".txt");
        FileUtils.writeStringToFile(originalPath.toFile(), origBodyString, "UTF-8");
        FileObjectImpl fileObj = work.addDataFile(originalPath.toUri(), origFilename, origMimetype, null, null,
                new AclModelBuilder("Work")
                    .addNoneRole(PUBLIC_PRINC)
                    .model);

        treeIndexer.indexAll(baseAddress);

        MvcResult mvcResult = mvc.perform(get("/acl/patron/" + fileObj.getPid().getId()))
                .andExpect(status().isOk())
                .andReturn();

        PatronRolesResponse result = parseResponse(mvcResult);
        PatronAccessDetails inherited = result.inherited;
        assertEquals(1, inherited.getRoles().size());
        assertHasRole(inherited, PUBLIC_PRINC, canViewOriginals);
        assertFalse(inherited.isDeleted());
        assertNull(inherited.getEmbargo());

        PatronAccessDetails assigned = result.assigned;
        assertEquals(1, assigned.getRoles().size());
        assertHasRole(assigned, PUBLIC_PRINC, UserRole.none);
        assertFalse(assigned.isDeleted());
        assertNull(assigned.getEmbargo());
    }

    private void assertInfoEmpty(PatronAccessDetails info) {
        assertTrue(info.getRoles() == null || info.getRoles().isEmpty());
        assertFalse(info.isDeleted());
        assertNull(info.getEmbargo());
    }

    private void assertHasRole(PatronAccessDetails info, String princ, UserRole role) {
        assertTrue("Response info does not contain required assigned role " + princ + " " + role,
                info.getRoles().stream()
                .anyMatch(a -> a.getPrincipal().equals(princ) && a.getRole().equals(role)));
    }

    private void createCollectionInUnit(Model collModel, Model unitModel) {
        adminUnit = repositoryObjectFactory.createAdminUnit(unitModel);
        contentRoot.addMember(adminUnit);
        collObj = repositoryObjectFactory.createCollectionObject(collModel);
        adminUnit.addMember(collObj);
    }

    private void createCollectionInUnit(Model collModel) {
        createCollectionInUnit(collModel,
                new AclModelBuilder("Admin Unit with owner")
                .addUnitOwner(USER_NS_PRINC)
                .model);
    }

    private PatronRolesResponse parseResponse(MvcResult result) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(result.getResponse().getContentAsString(),
                new TypeReference<PatronRolesResponse>() {});
    }

    public static class PatronRolesResponse {
        private PatronAccessDetails inherited;
        private PatronAccessDetails assigned;
        private List<IPAddressPatronPrincipalConfig> allowedPrincipals;
        public PatronAccessDetails getInherited() {
            return inherited;
        }
        public void setInherited(PatronAccessDetails inherited) {
            this.inherited = inherited;
        }
        public PatronAccessDetails getAssigned() {
            return assigned;
        }
        public void setAssigned(PatronAccessDetails assigned) {
            this.assigned = assigned;
        }
        public List<IPAddressPatronPrincipalConfig> getAllowedPrincipals() {
            return allowedPrincipals;
        }
        public void setAllowedPrincipals(List<IPAddressPatronPrincipalConfig> allowedPrincipals) {
            this.allowedPrincipals = allowedPrincipals;
        }
    }

    private Calendar getNextYear() {
        Date dt = new Date();

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTime(dt);
        c.add(Calendar.DATE, 365);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }
}

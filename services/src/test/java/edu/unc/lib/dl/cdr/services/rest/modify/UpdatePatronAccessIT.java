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
import static edu.unc.lib.dl.acl.util.AccessPrincipalConstants.USER_NAMESPACE;
import static edu.unc.lib.dl.acl.util.UserRole.canManage;
import static edu.unc.lib.dl.acl.util.UserRole.canViewMetadata;
import static edu.unc.lib.dl.acl.util.UserRole.canViewOriginals;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.ws.rs.core.MediaType;

import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.RoleAssignment;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.acl.PatronAccessDetails;
import edu.unc.lib.dl.rdf.CdrAcl;
import edu.unc.lib.dl.test.AclModelBuilder;

/**
 * @author bbpennel
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/update-patron-it-servlet.xml")
})
public class UpdatePatronAccessIT extends AbstractAPIIT {
    private static final String USER_NAME = "adminuser";
    private static final String USER_URI = USER_NAMESPACE + USER_NAME;
    private static final String USER_GROUPS = "edu:lib:admin_grp";

    private AdminUnit adminUnit;
    private CollectionObject collObj;

    @Before
    public void setup() throws Exception {
        AccessGroupSet testPrincipals = new AccessGroupSet(USER_GROUPS);

        GroupsThreadStore.storeUsername(USER_NAME);
        GroupsThreadStore.storeGroups(testPrincipals);

        setupContentRoot();
    }

    @After
    public void teardown() throws Exception {
        GroupsThreadStore.clearStore();
    }

    @Test
    public void insufficientPermissions() throws Exception {
        // Construct unit without any staff permissions granted
        createCollectionInUnit(null, null);

        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();
        List<RoleAssignment> assignments = asList(
                new RoleAssignment(PUBLIC_PRINC, canViewOriginals));
        accessDetails.setRoles(assignments);

        mvc.perform(put("/edit/acl/patron/" + collObj.getPid().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(makeRequestBody(accessDetails)))
                .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    public void objectNotFound() throws Exception {
        createCollectionInUnit(null);
        // Create pid for non-existent object
        PID pid = pidMinter.mintContentPid();

        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();
        List<RoleAssignment> assignments = asList(
                new RoleAssignment(PUBLIC_PRINC, canViewOriginals));
        accessDetails.setRoles(assignments);

        mvc.perform(put("/edit/acl/patron/" + pid.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(makeRequestBody(accessDetails)))
                .andExpect(status().isNotFound())
            .andReturn();
    }

    @Test
    public void invalidAssignment() throws Exception {
        createCollectionInUnit(null);

        treeIndexer.indexAll(baseAddress);

        // Request to grant staff permission
        PatronAccessDetails accessDetails = new PatronAccessDetails();
        List<RoleAssignment> assignments = asList(
                new RoleAssignment(PUBLIC_PRINC, canManage));
        accessDetails.setRoles(assignments);

        mvc.perform(put("/edit/acl/patron/" + collObj.getPid().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(makeRequestBody(accessDetails)))
                .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void missingRole() throws Exception {
        createCollectionInUnit(null);

        treeIndexer.indexAll(baseAddress);

        // Request to grant staff permission
        PatronAccessDetails accessDetails = new PatronAccessDetails();
        List<RoleAssignment> assignments = asList(
                new RoleAssignment(PUBLIC_PRINC, null));
        accessDetails.setRoles(assignments);

        mvc.perform(put("/edit/acl/patron/" + collObj.getPid().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(makeRequestBody(accessDetails)))
                .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void invalidBodyJson() throws Exception {
        createCollectionInUnit(null);

        treeIndexer.indexAll(baseAddress);

        mvc.perform(put("/edit/acl/patron/" + collObj.getPid().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ Not valid }"))
                .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    public void assignEmbargoAndRoles() throws Exception {
        createCollectionInUnit(null);

        treeIndexer.indexAll(baseAddress);

        Date embargoUntil = getYearsInTheFuture(1);

        // Request to grant staff permission
        PatronAccessDetails accessDetails = new PatronAccessDetails();
        List<RoleAssignment> assignments = asList(
                new RoleAssignment(PUBLIC_PRINC, canViewMetadata),
                new RoleAssignment(AUTHENTICATED_PRINC, canViewOriginals));
        accessDetails.setEmbargo(embargoUntil);
        accessDetails.setRoles(assignments);

        MvcResult mvcResult = mvc.perform(put("/edit/acl/patron/" + collObj.getPid().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(makeRequestBody(accessDetails)))
                .andExpect(status().isOk())
            .andReturn();

        assertResponseSuccess(mvcResult);

        RepositoryObject target = repositoryObjectLoader.getRepositoryObject(collObj.getPid());
        assertHasAssignment(PUBLIC_PRINC, canViewMetadata, target);
        assertHasAssignment(AUTHENTICATED_PRINC, canViewOriginals, target);

        assertHasEmbargo(embargoUntil, target);
    }

    @Test
    public void emptyDetailsToObjectWithNoDetails() throws Exception {
        createCollectionInUnit(null);

        treeIndexer.indexAll(baseAddress);

        // Request to grant staff permission
        PatronAccessDetails accessDetails = new PatronAccessDetails();

        MvcResult mvcResult = mvc.perform(put("/edit/acl/patron/" + collObj.getPid().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(makeRequestBody(accessDetails)))
                .andExpect(status().isOk())
            .andReturn();

        assertResponseWithoutChanges(mvcResult);

        RepositoryObject target = repositoryObjectLoader.getRepositoryObject(collObj.getPid());
        assertNoRoles(target);
    }

    @Test
    public void emptyDetailsToObjectWithDetails() throws Exception {
        createCollectionInUnit(new AclModelBuilder("Collection with role")
                .addCanViewMetadata(AUTHENTICATED_PRINC)
                .model);

        treeIndexer.indexAll(baseAddress);

        // Request to grant staff permission
        PatronAccessDetails accessDetails = new PatronAccessDetails();

        MvcResult mvcResult = mvc.perform(put("/edit/acl/patron/" + collObj.getPid().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(makeRequestBody(accessDetails)))
                .andExpect(status().isOk())
            .andReturn();

        assertResponseSuccess(mvcResult);

        RepositoryObject target = repositoryObjectLoader.getRepositoryObject(collObj.getPid());
        assertNoRoles(target);
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
                .addUnitOwner(USER_URI)
                .model);
    }

    private void assertHasAssignment(String princ, UserRole role, RepositoryObject obj) {
        Resource resc = obj.getResource();
        assertTrue("Expected role " + role.name() + " was not assigned for " + princ,
                resc.hasProperty(role.getProperty(), princ));
    }

    private Date getYearsInTheFuture(int numYears) {
        Date dt = new Date();

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTime(dt);
        c.add(Calendar.DATE, 365 * numYears);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private void assertHasEmbargo(Date expectedEmbargo, RepositoryObject obj) {
        Resource resc = obj.getResource();
        Statement embargoStmt = resc.getProperty(CdrAcl.embargoUntil);
        assertNotNull("Embargo was expected by not found", embargoStmt);
        Date assigned = ((XSDDateTime) embargoStmt.getLiteral().getValue()).asCalendar().getTime();
        assertEquals("Embargo did not match expected value",
                expectedEmbargo, assigned);
    }

    private void assertNoRoles(RepositoryObject obj) {
        Resource resc = obj.getResource();
        StmtIterator it = resc.listProperties();
        while (it.hasNext()) {
            Statement stmt = it.next();
            UserRole role = UserRole.getRoleByProperty(stmt.getPredicate().getURI());
            if (role != null) {
                fail("No roles should be assigned, but " + role + " for " + stmt.getString() + " was present");
            }
        }
    }

    private void assertResponseWithoutChanges(MvcResult mvcResult) throws Exception {
        Map<String, Object> resp = getMapFromResponse(mvcResult);
        assertEquals("No changes made", resp.get("status"));
        assertEquals("editPatronAccess", resp.get("action"));
    }

    private void assertResponseSuccess(MvcResult mvcResult) throws Exception {
        Map<String, Object> resp = getMapFromResponse(mvcResult);
        assertTrue("Missing job id", resp.containsKey("job"));
        assertEquals("editPatronAccess", resp.get("action"));
    }
}

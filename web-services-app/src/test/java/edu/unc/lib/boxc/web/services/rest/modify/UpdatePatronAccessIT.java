package edu.unc.lib.boxc.web.services.rest.modify;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.USER_NAMESPACE;
import static edu.unc.lib.boxc.auth.api.UserRole.canManage;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewMetadata;
import static edu.unc.lib.boxc.auth.api.UserRole.canViewOriginals;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.http.MediaType;

import edu.unc.lib.boxc.web.services.rest.MvcTestHelpers;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.RoleAssignment;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.fcrepo.test.AclModelBuilder;
import edu.unc.lib.boxc.operations.impl.acl.PatronAccessAssignmentService.PatronAccessAssignmentRequest;
import edu.unc.lib.boxc.web.services.rest.modify.UpdatePatronAccessController.BulkPatronAccessDetails;
import edu.unc.lib.boxc.operations.impl.acl.PatronAccessDetails;
import edu.unc.lib.boxc.operations.impl.acl.PatronAccessOperationSender;

/**
 * @author bbpennel
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/spring-test/acl-service-context.xml"),
    @ContextConfiguration("/update-patron-it-servlet.xml")
})
public class UpdatePatronAccessIT extends AbstractAPIIT {
    private static final String USER_NAME = "adminuser";
    private static final String USER_URI = USER_NAMESPACE + USER_NAME;
    private static final String USER_GROUPS = "edu:lib:admin_grp";

    private AdminUnit adminUnit;
    private CollectionObject collObj;
    private AutoCloseable closeable;
    @Autowired
    private PatronAccessOperationSender patronAccessOperationSender;
    @Captor
    private ArgumentCaptor<PatronAccessAssignmentRequest> requestCaptor;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);
        reset(patronAccessOperationSender);
        AccessGroupSet testPrincipals = new AccessGroupSetImpl(USER_GROUPS);

        GroupsThreadStore.storeUsername(USER_NAME);
        GroupsThreadStore.storeGroups(testPrincipals);
    }

    @AfterEach
    public void teardown() throws Exception {
        closeable.close();
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
                .content(MvcTestHelpers.makeRequestBody(accessDetails)))
                .andExpect(status().isForbidden())
            .andReturn();

        verify(patronAccessOperationSender, never()).sendUpdateRequest(any());
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
                .content(MvcTestHelpers.makeRequestBody(accessDetails)))
                .andExpect(status().isNotFound())
            .andReturn();

        verify(patronAccessOperationSender, never()).sendUpdateRequest(any());
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
                .content(MvcTestHelpers.makeRequestBody(accessDetails)))
                .andExpect(status().isBadRequest())
            .andReturn();

        verify(patronAccessOperationSender, never()).sendUpdateRequest(any());
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
                .content(MvcTestHelpers.makeRequestBody(accessDetails)))
                .andExpect(status().isBadRequest())
            .andReturn();

        verify(patronAccessOperationSender, never()).sendUpdateRequest(any());
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

        verify(patronAccessOperationSender, never()).sendUpdateRequest(any());
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
                .content(MvcTestHelpers.makeRequestBody(accessDetails)))
                .andExpect(status().isOk())
            .andReturn();

        assertResponseSuccess(mvcResult);

        verify(patronAccessOperationSender).sendUpdateRequest(any());
    }

    @Test
    public void invalidPid() throws Exception {
        PatronAccessDetails accessDetails = new PatronAccessDetails();
        List<RoleAssignment> assignments = asList(
                new RoleAssignment(PUBLIC_PRINC, canViewOriginals));
        accessDetails.setRoles(assignments);

        mvc.perform(put("/edit/acl/patron/definitelynotapid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(accessDetails)))
                .andExpect(status().isForbidden())
            .andReturn();

        verify(patronAccessOperationSender, never()).sendUpdateRequest(any());
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
                .content(MvcTestHelpers.makeRequestBody(accessDetails)))
                .andExpect(status().isOk())
            .andReturn();

        assertResponseSuccess(mvcResult);

        verify(patronAccessOperationSender).sendUpdateRequest(any());
    }

    @Test
    public void bulkUpdateOneObject() throws Exception {
        createCollectionInUnit(null);

        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();
        List<RoleAssignment> assignments = asList(
                new RoleAssignment(PUBLIC_PRINC, canViewMetadata),
                new RoleAssignment(AUTHENTICATED_PRINC, canViewOriginals));
        accessDetails.setRoles(assignments);
        BulkPatronAccessDetails bulkDetails = new BulkPatronAccessDetails();
        bulkDetails.setAccessDetails(accessDetails);
        bulkDetails.setIds(Arrays.asList(collObj.getPid().getId()));

        MvcResult mvcResult = mvc.perform(put("/edit/acl/patron")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(bulkDetails)))
                .andExpect(status().isOk())
            .andReturn();

        assertResponseSuccess(mvcResult);

        verify(patronAccessOperationSender).sendUpdateRequest(any());
    }

    @Test
    public void bulkUpdateNoIds() throws Exception {
        createCollectionInUnit(null);

        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();
        List<RoleAssignment> assignments = asList(
                new RoleAssignment(PUBLIC_PRINC, canViewMetadata),
                new RoleAssignment(AUTHENTICATED_PRINC, canViewOriginals));
        accessDetails.setRoles(assignments);
        BulkPatronAccessDetails bulkDetails = new BulkPatronAccessDetails();
        bulkDetails.setAccessDetails(accessDetails);

        mvc.perform(put("/edit/acl/patron")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(bulkDetails)))
                .andExpect(status().isBadRequest())
            .andReturn();

        verify(patronAccessOperationSender, never()).sendUpdateRequest(any());
    }

    @Test
    public void bulkUpdateNoDetails() throws Exception {
        createCollectionInUnit(null);

        treeIndexer.indexAll(baseAddress);

        BulkPatronAccessDetails bulkDetails = new BulkPatronAccessDetails();
        bulkDetails.setIds(Arrays.asList(collObj.getPid().getId()));

        mvc.perform(put("/edit/acl/patron")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(bulkDetails)))
                .andExpect(status().isBadRequest())
            .andReturn();

        verify(patronAccessOperationSender, never()).sendUpdateRequest(any());
    }

    @Test
    public void bulkUpdateMultiple() throws Exception {
        createCollectionInUnit(null);
        FolderObject folder1 = repositoryObjectFactory.createFolderObject(null);
        FolderObject folder2 = repositoryObjectFactory.createFolderObject(null);
        collObj.addMember(folder1);
        collObj.addMember(folder2);

        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();
        List<RoleAssignment> assignments = asList(
                new RoleAssignment(PUBLIC_PRINC, canViewMetadata),
                new RoleAssignment(AUTHENTICATED_PRINC, canViewOriginals));
        accessDetails.setRoles(assignments);
        BulkPatronAccessDetails bulkDetails = new BulkPatronAccessDetails();
        bulkDetails.setAccessDetails(accessDetails);
        bulkDetails.setIds(Arrays.asList(collObj.getPid().getId(),
                folder1.getPid().getId(), folder2.getPid().getId()));

        MvcResult mvcResult = mvc.perform(put("/edit/acl/patron")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(bulkDetails)))
                .andExpect(status().isOk())
            .andReturn();

        assertResponseSuccess(mvcResult);

        verify(patronAccessOperationSender, times(3)).sendUpdateRequest(any());
    }

    @Test
    public void bulkUpdateInsufficientPermissions() throws Exception {
        // Construct unit without any staff permissions granted
        createCollectionInUnit(null, null);

        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();
        List<RoleAssignment> assignments = asList(
                new RoleAssignment(PUBLIC_PRINC, canViewOriginals));
        accessDetails.setRoles(assignments);
        BulkPatronAccessDetails bulkDetails = new BulkPatronAccessDetails();
        bulkDetails.setIds(Arrays.asList(collObj.getPid().getId()));
        bulkDetails.setAccessDetails(accessDetails);

        mvc.perform(put("/edit/acl/patron")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(bulkDetails)))
                .andExpect(status().isForbidden())
            .andReturn();

        verify(patronAccessOperationSender, never()).sendUpdateRequest(any());
    }

    @Test
    public void bulkUpdateWithRolesAndEmbargo() throws Exception {
        createCollectionInUnit(null);

        treeIndexer.indexAll(baseAddress);

        Date embargoUntil = getYearsInTheFuture(1);
        PatronAccessDetails accessDetails = new PatronAccessDetails();
        List<RoleAssignment> assignments = asList(new RoleAssignment(AUTHENTICATED_PRINC, canViewOriginals));
        accessDetails.setRoles(assignments);
        accessDetails.setEmbargo(embargoUntil);
        BulkPatronAccessDetails bulkDetails = new BulkPatronAccessDetails();
        bulkDetails.setAccessDetails(accessDetails);
        bulkDetails.setIds(Arrays.asList(collObj.getPid().getId()));

        MvcResult mvcResult = mvc.perform(put("/edit/acl/patron")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(bulkDetails)))
                .andExpect(status().isOk())
            .andReturn();

        assertResponseSuccess(mvcResult);

        verify(patronAccessOperationSender).sendUpdateRequest(requestCaptor.capture());
        PatronAccessAssignmentRequest sentRequest = requestCaptor.getValue();
        assertEquals(embargoUntil, sentRequest.getAccessDetails().getEmbargo());
        assertEquals(1, sentRequest.getAccessDetails().getRoles().size());
        assertFalse(sentRequest.isFolderCreation());
        assertFalse(sentRequest.isSkipEmbargo());
    }

    @Test
    public void bulkUpdateSkipEmbargo() throws Exception {
        createCollectionInUnit(null);

        treeIndexer.indexAll(baseAddress);

        PatronAccessDetails accessDetails = new PatronAccessDetails();
        List<RoleAssignment> assignments = asList(new RoleAssignment(AUTHENTICATED_PRINC, canViewOriginals));
        accessDetails.setRoles(assignments);
        BulkPatronAccessDetails bulkDetails = new BulkPatronAccessDetails();
        bulkDetails.setAccessDetails(accessDetails);
        bulkDetails.setIds(Arrays.asList(collObj.getPid().getId()));
        bulkDetails.setSkipEmbargo(true);

        MvcResult mvcResult = mvc.perform(put("/edit/acl/patron")
                .contentType(MediaType.APPLICATION_JSON)
                .content(MvcTestHelpers.makeRequestBody(bulkDetails)))
                .andExpect(status().isOk())
            .andReturn();

        assertResponseSuccess(mvcResult);

        verify(patronAccessOperationSender).sendUpdateRequest(requestCaptor.capture());
        PatronAccessAssignmentRequest sentRequest = requestCaptor.getValue();
        assertNull(sentRequest.getAccessDetails().getEmbargo());
        assertEquals(1, sentRequest.getAccessDetails().getRoles().size());
        assertFalse(sentRequest.isFolderCreation());
        assertTrue(sentRequest.isSkipEmbargo());
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

    private void assertResponseSuccess(MvcResult mvcResult) throws Exception {
        Map<String, Object> resp = MvcTestHelpers.getMapFromResponse(mvcResult);
        assertTrue(((String) resp.get("status")).contains("Submitted patron access update"));
        assertEquals("editPatronAccess", resp.get("action"));
    }
}

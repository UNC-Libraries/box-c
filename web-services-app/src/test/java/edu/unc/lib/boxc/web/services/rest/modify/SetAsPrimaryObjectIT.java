package edu.unc.lib.boxc.web.services.rest.modify;

import static edu.unc.lib.boxc.auth.api.Permission.editResourceType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;

/**
 *
 * @author harring
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/set-as-primary-object-it-servlet.xml")
})
public class SetAsPrimaryObjectIT extends AbstractAPIIT {

    private WorkObject parent;
    private PID parentPid;
    private FileObject fileObj;
    private PID fileObjPid;

    @Test
    public void testSetPrimaryObject() throws UnsupportedOperationException, Exception {
        makePidsAndObjects();

        addFileObjAsMember();

        assertPrimaryObjectNotSet(parent);

        MvcResult result = mvc.perform(put("/edit/setAsPrimaryObject/" + fileObjPid.getUUID()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        treeIndexer.indexAll(baseAddress);

        assertPrimaryObjectSet(parent, fileObj);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(fileObjPid.getUUID(), respMap.get("pid"));
        assertEquals("setAsPrimaryObject", respMap.get("action"));
    }

    @Test
    public void testAuthorizationFailure() throws Exception {
        makePidsAndObjects();

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(fileObjPid), any(AccessGroupSetImpl.class), eq(editResourceType));

        addFileObjAsMember();

        MvcResult result = mvc.perform(put("/edit/setAsPrimaryObject/" + fileObjPid.getUUID()))
            .andExpect(status().isForbidden())
            .andReturn();

        treeIndexer.indexAll(baseAddress);

        assertPrimaryObjectNotSet(parent);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(fileObjPid.getUUID(), respMap.get("pid"));
        assertEquals("setAsPrimaryObject", respMap.get("action"));
        assertTrue(respMap.containsKey("error"));
    }

    @Test
    public void testAddFolderAsPrimaryObject() throws UnsupportedOperationException, Exception {
        makePidsAndObjects();
        PID folderObjPid = makePid();

        repositoryObjectFactory.createFolderObject(folderObjPid, null);

        MvcResult result = mvc.perform(put("/edit/setAsPrimaryObject/" + folderObjPid.getUUID()))
                .andExpect(status().isInternalServerError())
                .andReturn();

        treeIndexer.indexAll(baseAddress);

        assertPrimaryObjectNotSet(parent);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(folderObjPid.getUUID(), respMap.get("pid"));
        assertEquals("setAsPrimaryObject", respMap.get("action"));
        assertTrue(respMap.containsKey("error"));
    }

    @Test
    public void testClearPrimaryObjectViaFile() throws UnsupportedOperationException, Exception {
        makePidsAndObjects();
        testClearPrimaryObject(fileObj);
    }

    @Test
    public void testClearPrimaryObjectViaWork() throws UnsupportedOperationException, Exception {
        makePidsAndObjects();
        testClearPrimaryObject(parent);
    }

    private void testClearPrimaryObject(RepositoryObject target) throws Exception {
        addFileObjAsMember();

        parent.setPrimaryObject(fileObj.getPid());
        assertPrimaryObjectSet(parent, fileObj);

        MvcResult result = mvc.perform(put("/edit/clearPrimaryObject/" + target.getPid().getUUID()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        treeIndexer.indexAll(baseAddress);

        assertPrimaryObjectNotSet(parent);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(target.getPid().getUUID(), respMap.get("pid"));
        assertEquals("clearPrimaryObject", respMap.get("action"));
    }

    @Test
    public void testClearPrimaryObjectAuthorizationFailure() throws Exception {
        makePidsAndObjects();
        addFileObjAsMember();
        parent.setPrimaryObject(fileObj.getPid());

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(parentPid), any(AccessGroupSetImpl.class), eq(editResourceType));

        MvcResult result = mvc.perform(put("/edit/clearPrimaryObject/" + fileObjPid.getUUID()))
            .andExpect(status().isForbidden())
            .andReturn();

        treeIndexer.indexAll(baseAddress);

        assertPrimaryObjectSet(parent, fileObj);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(fileObjPid.getUUID(), respMap.get("pid"));
        assertEquals("clearPrimaryObject", respMap.get("action"));
        assertTrue(respMap.containsKey("error"));
    }

    private void assertPrimaryObjectSet(WorkObject parent, FileObject fileObj) {
        parent.shouldRefresh();
        assertNotNull(parent.getPrimaryObject());
        assertEquals(parent.getPrimaryObject().getPid(), fileObj.getPid());
    }

    private void assertPrimaryObjectNotSet(WorkObject parent) {
        parent.shouldRefresh();
        assertNull(parent.getPrimaryObject());
    }

    private void addFileObjAsMember() {
        parent.addMember(fileObj);
    }

    private void makePidsAndObjects() {
        fileObjPid = makePid();
        fileObj = repositoryObjectFactory.createFileObject(fileObjPid, null);
        parentPid = makePid();
        parent = repositoryObjectFactory.createWorkObject(parentPid, null);
    }

}

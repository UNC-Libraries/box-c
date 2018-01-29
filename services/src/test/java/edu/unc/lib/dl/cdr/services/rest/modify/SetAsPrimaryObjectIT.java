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

import static edu.unc.lib.dl.acl.util.Permission.editResourceType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.PcdmModels;

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

    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;
    @Autowired
    private Model queryModel;

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
                .assertHasAccess(anyString(), eq(fileObjPid), any(AccessGroupSet.class), eq(editResourceType));

        addFileObjAsMember();

        MvcResult result = mvc.perform(put("/edit/setAsPrimaryObject/" + fileObjPid.getUUID()))
            .andExpect(status().isForbidden())
            .andReturn();

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

            assertPrimaryObjectNotSet(parent);

            // Verify response from api
            Map<String, Object> respMap = getMapFromResponse(result);
            assertEquals(folderObjPid.getUUID(), respMap.get("pid"));
            assertEquals("setAsPrimaryObject", respMap.get("action"));
            assertTrue(respMap.containsKey("error"));
    }

    private void assertPrimaryObjectSet(WorkObject parent, FileObject fileObj) {
        assertNotNull(parent.getPrimaryObject());
        assertEquals(parent.getPrimaryObject().getPid(), fileObj.getPid());
    }

    private void assertPrimaryObjectNotSet(WorkObject parent) {
        assertNull(parent.getPrimaryObject());
    }

    private void addFileObjAsMember() {
        queryModel.getResource(parentPid.getRepositoryPath())
                .addProperty(PcdmModels.hasMember, fileObj.getResource());
        parent.addMember(fileObj);
    }

    private void makePidsAndObjects() {
        fileObjPid = makePid();
        fileObj = repositoryObjectFactory.createFileObject(fileObjPid, null);
        parentPid = makePid();
        parent = repositoryObjectFactory.createWorkObject(parentPid, null);
    }

}

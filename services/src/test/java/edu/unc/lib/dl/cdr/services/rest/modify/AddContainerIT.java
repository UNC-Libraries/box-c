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

import static edu.unc.lib.dl.acl.util.Permission.ingest;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fcrepo4.AdminUnit;
import edu.unc.lib.dl.fcrepo4.CollectionObject;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
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
    @ContextConfiguration("/add-container-it-servlet.xml")
})
public class AddContainerIT extends AbstractAPIIT {

    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;

    @Test
    public void testAddCollectionToAdminUnit() throws UnsupportedOperationException, Exception {
        PID parentPid = makePid();

        AdminUnit parent = repositoryObjectFactory.createAdminUnit(parentPid, null);

        assertChildContainerNotAdded(parent);

        String label = "collection_label";
        MvcResult result = mvc.perform(post("/edit/create/collection/" + parentPid.getUUID())
                .param("label", label))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertChildContainerAdded(parent, label);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(parentPid.getUUID(), respMap.get("pid"));
        assertEquals("create", respMap.get("action"));
    }

    @Test
    public void testAddAdminUnitToCollection() throws UnsupportedOperationException, Exception {
        PID parentPid = makePid();

        CollectionObject parent = repositoryObjectFactory.createCollectionObject(parentPid, null);

        assertChildContainerNotAdded(parent);

        String label = "admin_unit";
        MvcResult result = mvc.perform(post("/edit/create/adminUnit/" + parentPid.getUUID())
                .param("label", label))
            .andExpect(status().isInternalServerError())
            .andReturn();

        assertChildContainerNotAdded(parent);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(parentPid.getUUID(), respMap.get("pid"));
        assertEquals("create", respMap.get("action"));
        assertTrue(respMap.containsKey("error"));
    }

    @Test
    public void testAuthorizationFailure() throws Exception {
        PID pid = makePid();
        FolderObject folder = repositoryObjectFactory.createFolderObject(pid, null);

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSet.class), eq(ingest));

        String label = "folder";
        MvcResult result = mvc.perform(post("/edit/create/folder/" + pid.getUUID())
                .param("label", label))
            .andReturn();

        assertChildContainerNotAdded(folder);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("create", respMap.get("action"));
        assertTrue(respMap.containsKey("error"));
    }

    private void assertChildContainerAdded(ContentContainerObject parent, String label) {
        // Refresh the model
        parent = repositoryObjectLoader.getAdminUnit(parent.getPid());
        List<ContentObject> members = parent.getMembers();
        if (members.size() != 0) {
            assertTrue(members.get(0) instanceof ContentContainerObject);
        } else {
            fail("No child container was added to parent");
        }
        ContentContainerObject childContainer = (ContentContainerObject) members.get(0);
        Model childModel = childContainer.getModel();
        assertTrue(childModel.contains(childModel.getResource(childContainer.getUri().toString()), DcElements.title,
                label));
    }

    private void assertChildContainerNotAdded(ContentContainerObject parent) {
        assertTrue(parent.getMembers().size() == 0);
    }

}

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
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
    @ContextConfiguration("/edit-label-it-servlet.xml")
})
public class EditLabelIT extends AbstractAPIIT {

    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;

    @Test
    public void testCreateLabelWhereNoneExists() throws UnsupportedOperationException, Exception {
        PID pid = makePid();

        WorkObject work = repositoryObjectFactory.createWorkObject(pid, null);

        String label = "work_label";
        MvcResult result = mvc.perform(put("/edit/label/" + pid.getUUID())
                .param("label", label))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("editLabel", respMap.get("action"));

        assertEquals("work_label",
                work.getModel().getRequiredProperty(work.getResource(), DcElements.title).getLiteral().toString());
    }

    @Test
    public void testReplaceLabel() throws UnsupportedOperationException, Exception {
        PID pid = makePid();
        String oldLabel = "old_work_label";
        Model workModel = ModelFactory.createDefaultModel();
        workModel.add(workModel.createResource(pid.getRepositoryPath()), DcElements.title,
                oldLabel);
        WorkObject work = repositoryObjectFactory.createWorkObject(pid, workModel);

        String newLabel = "new_work_label";
        MvcResult result = mvc.perform(put("/edit/label/" + pid.getUUID())
                .param("label", newLabel))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("editLabel", respMap.get("action"));

        assertEquals("new_work_label",
                work.getModel().getRequiredProperty(work.getResource(), DcElements.title).getLiteral().toString());
    }

    @Test
    public void testAuthorizationFailure() throws Exception {
        PID pid = makePid();
        FolderObject folder = repositoryObjectFactory.createFolderObject(pid, null);

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSet.class), eq(Permission.editDescription));

        String label = "folder_label";
        MvcResult result = mvc.perform(put("/edit/label/" + pid.getUUID())
                .param("label", label))
                .andExpect(status().isForbidden())
            .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("editLabel", respMap.get("action"));
        assertTrue(respMap.containsKey("error"));
    }

}

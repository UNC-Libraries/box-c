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

import static java.nio.file.Files.createTempFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.fcrepo.model.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.Permission;

/**
 *
 * @author harring
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/edit-filename-it-servlet.xml")
})
public class EditFilenameIT extends AbstractAPIIT {
    private String filename = "file.txt";
    private String mimetype = "text/plain";

    @Test
    public void testCreateLabelWhereNoneExists() throws UnsupportedOperationException, Exception {
        PID pid = makePid();

        Path file = createTempFile("test", "txt");
        FileObject fileObj = repositoryObjectFactory.createFileObject(pid, null);
        fileObj.addOriginalFile(file.toUri(), filename, mimetype, null, null);

        String label = "work_filename";
        MvcResult result = mvc.perform(put("/edit/filename/" + pid.getUUID())
                .param("label", label))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("editLabel", respMap.get("action"));

        assertEquals("work_filename",
                fileObj.getOriginalFile().getResource().getProperty(Ebucore.filename)
                        .getLiteral().toString());
    }

    @Test
    public void testReplaceLabel() throws UnsupportedOperationException, Exception {
        PID pid = makePid();
        Path file = createTempFile("test", "txt");

        FileObject fileObj = repositoryObjectFactory.createFileObject(pid, null);
        fileObj.addOriginalFile(file.toUri(), filename, mimetype, null, null);

        String newLabel = "new_work_filename";
        MvcResult result = mvc.perform(put("/edit/filename/" + pid.getUUID())
                .param("label", newLabel))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("editLabel", respMap.get("action"));

        assertEquals(newLabel,
                fileObj.getOriginalFile().getResource().getProperty(Ebucore.filename)
                        .getLiteral().toString());
    }

    @Test
    public void testWrongObjectType() throws UnsupportedOperationException, Exception {
        PID pid = makePid();

        WorkObject work = repositoryObjectFactory.createWorkObject(pid, null);

        String label = "work_filename";
        mvc.perform(put("/edit/filename/" + pid.getUUID())
                .param("label", label))
                .andExpect(status().is5xxServerError())
                .andReturn();
    }

    @Test
    public void testAuthorizationFailure() throws Exception {
        PID pid = makePid();
        repositoryObjectFactory.createFolderObject(pid, null);

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSet.class), eq(Permission.editDescription));

        String label = "folder_filename";
        MvcResult result = mvc.perform(put("/edit/filename/" + pid.getUUID())
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

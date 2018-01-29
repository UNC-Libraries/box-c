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

import static edu.unc.lib.dl.acl.util.Permission.markForDeletion;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fcrepo4.PremisEventObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.CdrAcl;
import edu.unc.lib.dl.rdf.Premis;

/**
 *
 * @author bbpennel
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/mark-for-deletion-it-servlet.xml")
})
public class MarkForDeletionIT extends AbstractAPIIT {

    @Autowired
    private RepositoryObjectFactory repositoryObjectFactory;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;

    @Test
    public void testMarkSingle() throws Exception {
        PID pid = makePid();

        repositoryObjectFactory.createWorkObject(pid, null);

        MvcResult result = mvc.perform(post("/edit/delete/" + pid.getUUID()))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertIsMarkedForDeletion(pid);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("delete", respMap.get("action"));
    }

    @Test
    public void testRestoreSingle() throws Exception {
        PID pid = makePid();

        // Create work which starts as marked for deletion
        repositoryObjectFactory.createWorkObject(pid, makeModelWithDeletion(pid));

        MvcResult result = mvc.perform(post("/edit/restore/" + pid.getUUID()))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertNotMarkedForDeletion(pid);

        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("restore", respMap.get("action"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMarkMultiple() throws Exception {
        PID pid1 = makePid();
        PID pid2 = makePid();
        PID pid3 = makePid();
        List<String> idList = Arrays.asList(pid1.getUUID(), pid2.getUUID(), pid3.getUUID());

        repositoryObjectFactory.createWorkObject(pid1, null);
        repositoryObjectFactory.createWorkObject(pid2, null);
        repositoryObjectFactory.createWorkObject(pid3, makeModelWithDeletion(pid3));

        MvcResult result = mvc.perform(post("/edit/delete")
                .param("ids", String.join("\n", idList)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertIsMarkedForDeletion(pid1);
        assertIsMarkedForDeletion(pid2);
        // Verify deletion was retained
        assertIsMarkedForDeletion(pid3);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertTrue(idList.containsAll((List<String>) respMap.get("pids")));
        assertEquals("delete", respMap.get("action"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRestoreMultiple() throws Exception {
        PID pid1 = makePid();
        PID pid2 = makePid();
        PID pid3 = makePid();
        List<String> idList = Arrays.asList(pid1.getUUID(), pid2.getUUID(), pid3.getUUID());

        repositoryObjectFactory.createWorkObject(pid1, makeModelWithDeletion(pid1));
        repositoryObjectFactory.createWorkObject(pid2, makeModelWithDeletion(pid2));
        repositoryObjectFactory.createWorkObject(pid3, null);

        MvcResult result = mvc.perform(post("/edit/restore")
                .param("ids", String.join("\n", idList)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertNotMarkedForDeletion(pid1);
        assertNotMarkedForDeletion(pid2);
        // Verify continued to not be marked for deletion
        assertNotMarkedForDeletion(pid3);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertTrue(idList.containsAll((List<String>) respMap.get("pids")));
        assertEquals("restore", respMap.get("action"));
    }

    @Test
    public void testAuthorizationFailure() throws Exception {
        PID pid = makePid();

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSet.class), eq(markForDeletion));

        repositoryObjectFactory.createWorkObject(pid, null);

        MvcResult result = mvc.perform(post("/edit/delete/" + pid.getUUID()))
            .andExpect(status().isForbidden())
            .andReturn();

        assertNotMarkedForDeletion(pid);

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(pid.getUUID(), respMap.get("pid"));
        assertEquals("delete", respMap.get("action"));
        assertTrue(respMap.containsKey("error"));
    }

    private void assertIsMarkedForDeletion(PID pid) {
        RepositoryObject repoObj = repositoryObjectLoader.getRepositoryObject(pid);
        Resource resc = repoObj.getResource();
        assertTrue(resc.getProperty(CdrAcl.markedForDeletion).getBoolean());

        PremisEventObject event = repoObj.getPremisLog().getEvents().get(0);
        assertTrue(event.getResource().hasProperty(Premis.hasEventType, Premis.Deletion));
    }

    private void assertNotMarkedForDeletion(PID pid) {
        RepositoryObject repoObj = repositoryObjectLoader.getRepositoryObject(pid);
        Resource resc = repoObj.getResource();
        assertFalse(resc.hasProperty(CdrAcl.markedForDeletion));
    }

    private Model makeModelWithDeletion(PID pid) {
        Model model = ModelFactory.createDefaultModel();
        model.getResource(pid.getRepositoryPath())
                .addProperty(CdrAcl.markedForDeletion, model.createTypedLiteral(true));
        return model;
    }

}

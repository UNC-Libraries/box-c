package edu.unc.lib.boxc.web.services.rest.modify;

import static edu.unc.lib.boxc.auth.api.Permission.markForDeletion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;
import edu.unc.lib.boxc.model.api.rdf.Premis;

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

    @Test
    public void testMarkSingle() throws Exception {
        PID pid = makePid();

        repositoryObjectFactory.createWorkObject(pid, null);

        MvcResult result = mvc.perform(post("/edit/delete/" + pid.getUUID())
                .param("message", "reason message"))
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
                .param("ids", String.join("\n", idList))
                .param("message", "reason message"))
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
                .assertHasAccess(anyString(), eq(pid), any(AccessGroupSetImpl.class), eq(markForDeletion));

        repositoryObjectFactory.createWorkObject(pid, null);

        MvcResult result = mvc.perform(post("/edit/delete/" + pid.getUUID())
                .param("message", "reason message"))
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

        Model logModel = repoObj.getPremisLog().getEventsModel();
        assertTrue(logModel.contains(null, RDF.type, Premis.Deaccession));
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

package edu.unc.lib.boxc.web.services.rest.modify;

import static edu.unc.lib.boxc.auth.api.Permission.reindex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 *
 * @author harring
 *
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/indexing-it-servlet.xml")
})
public class IndexingIT extends AbstractAPIIT {

    @Autowired
    private JmsTemplate mockJmsTemplate;

    @BeforeEach
    public void setup() {
        reset(mockJmsTemplate);
    }

    @Test
    public void testAuthorizationFailure() throws Exception {
        PID objPid = makePid();
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(objPid), any(AccessGroupSetImpl.class), eq(reindex));

        MvcResult result = mvc.perform(post("/edit/solr/update/" + objPid.getUUID()))
            .andExpect(status().isForbidden())
            .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(objPid.getUUID(), respMap.get("pid"));
        assertEquals("updateSolr", respMap.get("action"));
        assertTrue(respMap.containsKey("error"));

        verify(mockJmsTemplate, never()).send(any(MessageCreator.class));
    }

    @Test
    public void testUpdateObject() throws Exception {
        PID objPid = makePid();
        MvcResult result = mvc.perform(post("/edit/solr/update/" + objPid.getUUID()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(objPid.getUUID(), respMap.get("pid"));
        assertEquals("updateSolr", respMap.get("action"));
        assertFalse(respMap.containsKey("error"));

        verify(mockJmsTemplate).send(any(MessageCreator.class));
    }

    @Test
    public void testInplaceReindex() throws Exception {
        PID objPid = makePid();
        MvcResult result = mvc.perform(post("/edit/solr/reindex/" + objPid.getUUID(), true))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(objPid.getUUID(), respMap.get("pid"));
        assertEquals("reindexSolr", respMap.get("action"));
        assertFalse(respMap.containsKey("error"));

        verify(mockJmsTemplate).send(any(MessageCreator.class));
    }

    @Test
    public void testCleanReindex() throws Exception {
        PID parentPid = makePid();

        MvcResult result = mvc.perform(post("/edit/solr/reindex/" + parentPid.getUUID(), false))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(parentPid.getUUID(), respMap.get("pid"));
        assertEquals("reindexSolr", respMap.get("action"));
        assertFalse(respMap.containsKey("error"));

        verify(mockJmsTemplate).send(any(MessageCreator.class));
    }

    @Test
    public void testReindexTriples() throws Exception {
        PID parentPid = makePid();

        MvcResult result = mvc.perform(post("/edit/triples/reindex/" + parentPid.getUUID(), false))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(parentPid.getUUID(), respMap.get("pid"));
        assertEquals("reindexTriples", respMap.get("action"));
        assertFalse(respMap.containsKey("error"));

        verify(mockJmsTemplate).send(any(MessageCreator.class));
    }

    @Test
    public void testReindexTriplesAuthorizationFailure() throws Exception {
        PID parentPid = makePid();
        doThrow(new AccessRestrictionException()).when(aclService)
            .assertHasAccess(anyString(), eq(parentPid), any(AccessGroupSetImpl.class), eq(reindex));

        MvcResult result = mvc.perform(post("/edit/triples/reindex/" + parentPid.getUUID(), false))
                .andExpect(status().isForbidden())
                .andReturn();

        // Verify response from api
        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(parentPid.getUUID(), respMap.get("pid"));
        assertEquals("reindexTriples", respMap.get("action"));
        assertTrue(respMap.containsKey("error"));

        verify(mockJmsTemplate, never()).send(any(MessageCreator.class));
    }
}

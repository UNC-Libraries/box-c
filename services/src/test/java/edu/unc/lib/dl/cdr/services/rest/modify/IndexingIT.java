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

import static edu.unc.lib.dl.acl.util.Permission.reindex;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.fedora.PID;

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

    @Before
    public void setup() {
        reset(mockJmsTemplate);
    }

    @Test
    public void testAuthorizationFailure() throws Exception {
        PID objPid = makePid();
        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(objPid), any(AccessGroupSet.class), eq(reindex));

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
            .assertHasAccess(anyString(), eq(parentPid), any(AccessGroupSet.class), eq(reindex));

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

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
package edu.unc.lib.dl.cdr.services.rest;

import static edu.unc.lib.dl.cdr.services.rest.DepositPipelineController.ACTION_KEY;
import static edu.unc.lib.dl.cdr.services.rest.DepositPipelineController.ERROR_KEY;
import static edu.unc.lib.dl.cdr.services.rest.DepositPipelineController.STATE_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.cdr.services.rest.modify.AbstractAPIIT;
import edu.unc.lib.dl.util.DepositPipelineStatusFactory;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositPipelineState;

/**
 * @author bbpennel
 */
@ContextHierarchy({
    @ContextConfiguration("/spring-test/redis-server-context.xml"),
    @ContextConfiguration("/deposit-pipeline-it-servlet.xml")
})
public class DepositPipelineControllerIT extends AbstractAPIIT {

    @Autowired
    private DepositPipelineStatusFactory pipelineStatusFactory;

    @Before
    public void setup() {
        pipelineStatusFactory.clearPipelineActionRequest();
        pipelineStatusFactory.setPipelineState(null);
    }

    @Test
    public void getState_ValidState() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.active);

        MvcResult result = mvc.perform(get("/edit/depositPipeline"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals(DepositPipelineState.active.name(), respMap.get(STATE_KEY));
    }

    @Test
    public void getState_NoState() throws Exception {
        MvcResult result = mvc.perform(get("/edit/depositPipeline"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals("unknown", respMap.get(STATE_KEY));
    }

    @Test
    public void getState_NoPermission() throws Exception {
        GroupsThreadStore.storeGroups(new AccessGroupSet("authenticated"));

        MvcResult result = mvc.perform(get("/edit/depositPipeline"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals("Unauthorized", respMap.get(ERROR_KEY));
    }

    @Test
    public void requestAction_ValidQuiet() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.active);

        MvcResult result = mvc.perform(post("/edit/depositPipeline/quiet"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals("quiet", respMap.get(ACTION_KEY));
    }

    @Test
    public void requestAction_ValidUnquiet() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.quieted);

        MvcResult result = mvc.perform(post("/edit/depositPipeline/unquiet"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals("unquiet", respMap.get(ACTION_KEY));
    }

    @Test
    public void requestAction_ValidStop() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.quieted);

        MvcResult result = mvc.perform(post("/edit/depositPipeline/stop"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals("stop", respMap.get(ACTION_KEY));
    }

    @Test
    public void requestAction_NoPermission() throws Exception {
        GroupsThreadStore.storeGroups(new AccessGroupSet("authenticated"));

        MvcResult result = mvc.perform(post("/edit/depositPipeline/quiet"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertEquals("Unauthorized", respMap.get(ERROR_KEY));
    }

    @Test
    public void requestAction_InvalidAction() throws Exception {
        MvcResult result = mvc.perform(post("/edit/depositPipeline/plumb"))
                .andExpect(status().isBadRequest())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertTrue(((String) respMap.get(ERROR_KEY)).contains("Invalid action specified"));
    }

    @Test
    public void requestAction_QuietInvalidState() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.quieted);

        MvcResult result = mvc.perform(post("/edit/depositPipeline/quiet"))
                .andExpect(status().isConflict())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertTrue(((String) respMap.get(ERROR_KEY)).contains("Cannot perform quiet, the pipeline must be 'active'"));
    }

    @Test
    public void requestAction_UnquietInvalidState() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.active);

        MvcResult result = mvc.perform(post("/edit/depositPipeline/unquiet"))
                .andExpect(status().isConflict())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertTrue(((String) respMap.get(ERROR_KEY))
                .contains("Cannot perform unquiet, the pipeline must be 'quieted'"));
    }

    @Test
    public void requestAction_StoppedState() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.stopped);

        MvcResult result = mvc.perform(post("/edit/depositPipeline/quiet"))
                .andExpect(status().isConflict())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertTrue(((String) respMap.get(ERROR_KEY))
                .contains("Cannot perform actions while in the 'stopped' state"));
    }

    @Test
    public void requestAction_ShutdownState() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.shutdown);

        MvcResult result = mvc.perform(post("/edit/depositPipeline/unquiet"))
                .andExpect(status().isConflict())
                .andReturn();

        Map<String, Object> respMap = getMapFromResponse(result);
        assertTrue(((String) respMap.get(ERROR_KEY))
                .contains("Cannot perform actions while in the 'shutdown' state"));
    }
}

package edu.unc.lib.boxc.web.services.rest;

import static edu.unc.lib.boxc.web.services.rest.DepositPipelineController.ACTION_KEY;
import static edu.unc.lib.boxc.web.services.rest.DepositPipelineController.ERROR_KEY;
import static edu.unc.lib.boxc.web.services.rest.DepositPipelineController.STATE_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineState;
import edu.unc.lib.boxc.deposit.impl.model.DepositPipelineStatusFactory;
import edu.unc.lib.boxc.web.services.rest.modify.AbstractAPIIT;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import redis.clients.jedis.JedisPool;

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
    @Autowired
    private JedisPool jedisPool;

    @BeforeEach
    public void setup() {
        pipelineStatusFactory.clearPipelineActionRequest();
        pipelineStatusFactory.setPipelineState(null);
    }

    @AfterEach
    public void teardownLocal() {
        jedisPool.getResource().flushAll();
    }

    @Test
    public void getState_ValidState() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.active);

        MvcResult result = mvc.perform(get("/edit/depositPipeline"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals(DepositPipelineState.active.name(), respMap.get(STATE_KEY));
    }

    @Test
    public void getState_NoState() throws Exception {
        MvcResult result = mvc.perform(get("/edit/depositPipeline"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("unknown", respMap.get(STATE_KEY));
    }

    @Test
    public void getState_NoPermission() throws Exception {
        GroupsThreadStore.storeGroups(new AccessGroupSetImpl("authenticated"));

        MvcResult result = mvc.perform(get("/edit/depositPipeline"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("Unauthorized", respMap.get(ERROR_KEY));
    }

    @Test
    public void requestAction_ValidQuiet() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.active);

        MvcResult result = mvc.perform(post("/edit/depositPipeline/quiet"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("quiet", respMap.get(ACTION_KEY));
    }

    @Test
    public void requestAction_ValidUnquiet() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.quieted);

        MvcResult result = mvc.perform(post("/edit/depositPipeline/unquiet"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("unquiet", respMap.get(ACTION_KEY));
    }

    @Test
    public void requestAction_ValidStop() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.quieted);

        MvcResult result = mvc.perform(post("/edit/depositPipeline/stop"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("stop", respMap.get(ACTION_KEY));
    }

    @Test
    public void requestAction_NoPermission() throws Exception {
        GroupsThreadStore.storeGroups(new AccessGroupSetImpl("authenticated"));

        MvcResult result = mvc.perform(post("/edit/depositPipeline/quiet"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertEquals("Unauthorized", respMap.get(ERROR_KEY));
    }

    @Test
    public void requestAction_InvalidAction() throws Exception {
        MvcResult result = mvc.perform(post("/edit/depositPipeline/plumb"))
                .andExpect(status().isBadRequest())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertTrue(((String) respMap.get(ERROR_KEY)).contains("Invalid action specified"));
    }

    @Test
    public void requestAction_QuietInvalidState() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.quieted);

        MvcResult result = mvc.perform(post("/edit/depositPipeline/quiet"))
                .andExpect(status().isConflict())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertTrue(((String) respMap.get(ERROR_KEY)).contains("Cannot perform quiet, the pipeline must be 'active'"));
    }

    @Test
    public void requestAction_UnquietInvalidState() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.active);

        MvcResult result = mvc.perform(post("/edit/depositPipeline/unquiet"))
                .andExpect(status().isConflict())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertTrue(((String) respMap.get(ERROR_KEY))
                .contains("Cannot perform unquiet, the pipeline must be 'quieted'"));
    }

    @Test
    public void requestAction_StoppedState() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.stopped);

        MvcResult result = mvc.perform(post("/edit/depositPipeline/quiet"))
                .andExpect(status().isConflict())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertTrue(((String) respMap.get(ERROR_KEY))
                .contains("Cannot perform actions while in the 'stopped' state"));
    }

    @Test
    public void requestAction_ShutdownState() throws Exception {
        pipelineStatusFactory.setPipelineState(DepositPipelineState.shutdown);

        MvcResult result = mvc.perform(post("/edit/depositPipeline/unquiet"))
                .andExpect(status().isConflict())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);
        assertTrue(((String) respMap.get(ERROR_KEY))
                .contains("Cannot perform actions while in the 'shutdown' state"));
    }
}

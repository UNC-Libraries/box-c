package edu.unc.lib.boxc.web.services.rest;

import static edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DEPOSIT_TO_JOBS_PREFIX;
import static edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.JOB_STATUS_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.deposit.api.DepositConstants;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineState;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.JobField;
import edu.unc.lib.boxc.web.services.rest.modify.AbstractAPIIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.web.servlet.MvcResult;


import edu.unc.lib.boxc.deposit.impl.model.DepositPipelineStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.JobStatusFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@ContextHierarchy({
        @ContextConfiguration("/spring-test/redis-server-context.xml"),
        @ContextConfiguration("/deposit-it-servlet.xml")
})
public class DepositControllerIT extends AbstractAPIIT {
    private static final String TEST_UUID = "test-uuid-123";
    private static final String TEST_UUID_2 = "test-uuid-456";
    private static final String TEST_UUID_3 = "test-uuid-789";
    private static final String TEST_JOB_UUID = "test-job-123";
    private static final String TEST_USERNAME = "testuser";
    private static final String OTHER_USERNAME = "otheruser";

    @TempDir
    public Path tmpFolder;

    @Autowired
    private DepositStatusFactory depositStatusFactory;

    @Autowired
    private JobStatusFactory jobStatusFactory;

    @Autowired
    private DepositPipelineStatusFactory pipelineStatusFactory;

    @Autowired
    private JedisPool jedisPool;

    @BeforeEach
    public void setup() {
        flushRedis();

        pipelineStatusFactory.setPipelineState(DepositPipelineState.active);

        GroupsThreadStore.storeUsername(TEST_USERNAME);
        GroupsThreadStore.storeGroups(new AccessGroupSetImpl("authenticated"));
    }

    @AfterEach
    public void teardownLocal() {
        flushRedis();
    }

    @Test
    public void getInfoReturnsDepositSummary() throws Exception {
        saveDeposit(TEST_UUID, DepositState.queued, TEST_USERNAME);
        saveDeposit(TEST_UUID_2, DepositState.running, TEST_USERNAME);
        saveDeposit(TEST_UUID_3, DepositState.failed, OTHER_USERNAME);

        MvcResult result = mvc.perform(get("/edit/deposit"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);

        assertEquals(DepositPipelineState.active.name(), respMap.get("state"));
        assertEquals("DEPOSIT", respMap.get("id"));

        assertEquals(1, ((Number) respMap.get("queuedJobs")).intValue());
        assertEquals(1, ((Number) respMap.get("activeJobs")).intValue());
        assertEquals(0, ((Number) respMap.get("pausedJobs")).intValue());
        assertEquals(1, ((Number) respMap.get("failedJobs")).intValue());
        assertEquals(0, ((Number) respMap.get("finishedJobs")).intValue());

        assertTrue(respMap.containsKey("workers"));
        assertTrue(respMap.containsKey("uris"));
        assertTrue(respMap.containsKey("isAdmin"));

        @SuppressWarnings("unchecked")
        Map<String, Object> uris = (Map<String, Object>) respMap.get("uris");

        assertEquals("/api/edit/deposit/queued", uris.get(DepositState.queued.name()));
        assertEquals("/api/edit/deposit/running", uris.get(DepositState.running.name()));
        assertEquals("/api/edit/deposit/failed", uris.get(DepositState.failed.name()));
        assertEquals("/api/edit/deposit/finished", uris.get(DepositState.finished.name()));
        assertEquals("/api/edit/deposit/paused", uris.get(DepositState.paused.name()));
        assertEquals("/api/edit/deposit/cancelled", uris.get(DepositState.cancelled.name()));
        assertEquals("/api/edit/deposit/unregistered", uris.get(DepositState.unregistered.name()));
    }

    @Test
    public void getInfoNoPipelineStateReturnsUnknown() throws Exception {
        pipelineStatusFactory.setPipelineState(null);

        MvcResult result = mvc.perform(get("/edit/deposit"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);

        assertEquals("unknown", respMap.get("state"));
    }

    @Test
    public void getInfoReturnsWorkerStateSummary() throws Exception {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.sadd("resque:workers", "idle-worker");
            jedis.sadd("resque:workers", "active-worker");
            jedis.sadd("resque:workers", "paused-worker");

            jedis.set("resque:worker:active-worker", "{\"paused\":false}");
            jedis.set("resque:worker:paused-worker", "{\"paused\":true}");
        }

        MvcResult result = mvc.perform(get("/edit/deposit"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);

        assertEquals("1 active, 1 idle, 1 paused", respMap.get("workers"));
    }

    @Test
    public void getByUuidReturnsDepositDetails() throws Exception {
        saveDeposit(TEST_UUID, DepositState.queued, TEST_USERNAME);

        MvcResult result = mvc.perform(get("/edit/deposit/{uuid}", TEST_UUID))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);

        assertEquals(TEST_UUID, respMap.get(DepositField.uuid.name()));
        assertEquals(DepositState.queued.name(), respMap.get(DepositField.state.name()));
        assertEquals(TEST_USERNAME, respMap.get(DepositField.depositorName.name()));
        assertEquals("/api/status/deposit/" + TEST_UUID + "/jobs", respMap.get("jobsURI"));
        assertEquals("/api/status/deposit/" + TEST_UUID + "/eventsXML", respMap.get("eventsURI"));

        assertTrue(respMap.containsKey("jobs"));
        assertTrue(respMap.containsKey("isAdmin"));
    }

    @Test
    public void getByUuidRunningDepositIncludesCurrentJobUuid() throws Exception {
        saveDeposit(TEST_UUID, DepositState.running, TEST_USERNAME);
        jobStatusFactory.started(TEST_JOB_UUID, TEST_UUID, DepositControllerIT.class);

        MvcResult result = mvc.perform(get("/edit/deposit/{uuid}", TEST_UUID))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);

        assertEquals(TEST_UUID, respMap.get(DepositField.uuid.name()));
        assertEquals(DepositState.running.name(), respMap.get(DepositField.state.name()));
        assertEquals(TEST_JOB_UUID, respMap.get("currentJobUUID"));

        @SuppressWarnings("unchecked")
        Map<String, Object> jobs = (Map<String, Object>) respMap.get("jobs");

        assertTrue(jobs.containsKey(TEST_JOB_UUID));
    }

    @Test
    public void getByStateReturnsOnlyDepositsInRequestedState() throws Exception {
        saveDeposit(TEST_UUID, DepositState.queued, TEST_USERNAME);
        saveDeposit(TEST_UUID_2, DepositState.running, TEST_USERNAME);
        saveDeposit(TEST_UUID_3, DepositState.queued, OTHER_USERNAME);

        MvcResult result = mvc.perform(get("/edit/deposit/queued"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);

        assertTrue(respMap.containsKey(TEST_UUID));
        assertTrue(respMap.containsKey(TEST_UUID_3));
        assertFalse(respMap.containsKey(TEST_UUID_2));
    }

    @Test
    public void getByStateRunningDepositIncludesCurrentJob() throws Exception {
        saveDeposit(TEST_UUID, DepositState.running, TEST_USERNAME);
        jobStatusFactory.started(TEST_JOB_UUID, TEST_UUID, DepositControllerIT.class);

        MvcResult result = mvc.perform(get("/edit/deposit/running"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);

        assertTrue(respMap.containsKey(TEST_UUID));

        @SuppressWarnings("unchecked")
        Map<String, Object> depositMap = (Map<String, Object>) respMap.get(TEST_UUID);

        assertEquals(TEST_UUID, depositMap.get(DepositField.uuid.name()));
        assertEquals(DepositState.running.name(), depositMap.get(DepositField.state.name()));
        assertTrue(depositMap.containsKey("currentJob"));

        @SuppressWarnings("unchecked")
        Map<String, Object> currentJob = (Map<String, Object>) depositMap.get("currentJob");

        assertNotNull(currentJob);
        assertEquals(TEST_JOB_UUID, currentJob.get(JobField.uuid.name()));
        assertEquals("working", currentJob.get(JobField.status.name()));
        assertEquals(DepositControllerIT.class.getName(), currentJob.get(JobField.name.name()));
    }

    @Test
    public void updatePauseActionSuccess() throws Exception {
        saveDeposit(TEST_UUID, DepositState.queued, TEST_USERNAME);

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "pause"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void updatePauseActionForbiddenWrongUser() throws Exception {
        saveDeposit(TEST_UUID, DepositState.queued, OTHER_USERNAME);

        GroupsThreadStore.storeUsername(TEST_USERNAME);
        GroupsThreadStore.storeGroups(new AccessGroupSetImpl("authenticated"));

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "pause"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void updatePauseActionFinishedDepositReturnsBadRequest() throws Exception {
        saveDeposit(TEST_UUID, DepositState.finished, TEST_USERNAME);

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "pause"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updatePauseActionFailedDepositReturnsBadRequest() throws Exception {
        saveDeposit(TEST_UUID, DepositState.failed, TEST_USERNAME);

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "pause"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateResumeActionPausedDepositSuccess() throws Exception {
        saveDeposit(TEST_UUID, DepositState.paused, TEST_USERNAME);

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "resume"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void updateResumeActionFailedDepositSuccess() throws Exception {
        saveDeposit(TEST_UUID, DepositState.failed, TEST_USERNAME);

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "resume"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void updateResumeActionRunningDepositReturnsBadRequest() throws Exception {
        saveDeposit(TEST_UUID, DepositState.running, TEST_USERNAME);

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "resume"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateCancelActionSuccess() throws Exception {
        saveDeposit(TEST_UUID, DepositState.queued, TEST_USERNAME);

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "cancel"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void updateCancelActionFinishedDepositReturnsBadRequest() throws Exception {
        saveDeposit(TEST_UUID, DepositState.finished, TEST_USERNAME);

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "cancel"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateDestroyActionCancelledDepositSuccess() throws Exception {
        saveDeposit(TEST_UUID, DepositState.cancelled, TEST_USERNAME);

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "destroy"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void updateDestroyActionFinishedDepositSuccess() throws Exception {
        saveDeposit(TEST_UUID, DepositState.finished, TEST_USERNAME);

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "destroy"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void updateDestroyActionRunningDepositReturnsBadRequest() throws Exception {
        saveDeposit(TEST_UUID, DepositState.running, TEST_USERNAME);

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "destroy"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateInvalidActionReturnsBadRequest() throws Exception {
        saveDeposit(TEST_UUID, DepositState.queued, TEST_USERNAME);

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateMissingActionParamReturnsBadRequest() throws Exception {
        saveDeposit(TEST_UUID, DepositState.queued, TEST_USERNAME);

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void updateActionNoUsernameReturnsForbidden() throws Exception {
        saveDeposit(TEST_UUID, DepositState.queued, OTHER_USERNAME);

        GroupsThreadStore.storeUsername(null);
        GroupsThreadStore.storeGroups(new AccessGroupSetImpl("authenticated"));

        mvc.perform(post("/edit/deposit/{uuid}", TEST_UUID)
                        .param("action", "pause"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getByStateFinishedReturnsFinishedDeposits() throws Exception {
        saveDeposit(TEST_UUID, DepositState.finished, TEST_USERNAME);
        saveDeposit(TEST_UUID_2, DepositState.queued, TEST_USERNAME);

        MvcResult result = mvc.perform(get("/edit/deposit/finished"))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);

        assertTrue(respMap.containsKey(TEST_UUID));
        assertFalse(respMap.containsKey(TEST_UUID_2));
    }

    @Test
    public void getEventsMissingEventsFileReturnsEmptyResponse() throws Exception {
        Path bagDir = tmpFolder.resolve("bag-without-events");
        Files.createDirectories(bagDir);

        saveDepositWithDirectory(TEST_UUID, DepositState.finished, TEST_USERNAME, bagDir.toString());

        MvcResult result = mvc.perform(get("/edit/deposit/{uuid}/events", TEST_UUID))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("", result.getResponse().getContentAsString());
    }

    @Test
    public void getJobsReturnsJobMapFromRedis() throws Exception {
        saveDeposit(TEST_UUID, DepositState.running, TEST_USERNAME);

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.sadd(DEPOSIT_TO_JOBS_PREFIX + TEST_UUID, TEST_JOB_UUID);

            Map<String, String> job = new HashMap<>();
            job.put(JobField.uuid.name(), TEST_JOB_UUID);
            job.put(JobField.name.name(), DepositControllerIT.class.getName());
            job.put(JobField.status.name(), "working");
            job.put(JobField.num.name(), "3");
            job.put(JobField.total.name(), "10");
            job.put(JobField.starttime.name(), "123456789");

            jedis.hmset(JOB_STATUS_PREFIX + TEST_JOB_UUID, job);
        }

        MvcResult result = mvc.perform(get("/edit/deposit/{uuid}/jobs", TEST_UUID))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> respMap = MvcTestHelpers.getMapFromResponse(result);

        assertTrue(respMap.containsKey(TEST_JOB_UUID));

        @SuppressWarnings("unchecked")
        Map<String, Object> jobMap = (Map<String, Object>) respMap.get(TEST_JOB_UUID);

        assertEquals(TEST_JOB_UUID, jobMap.get(JobField.uuid.name()));
        assertEquals(DepositControllerIT.class.getName(), jobMap.get(JobField.name.name()));
        assertEquals("working", jobMap.get(JobField.status.name()));
        assertEquals("3", jobMap.get(JobField.num.name()));
        assertEquals("10", jobMap.get(JobField.total.name()));
        assertEquals("123456789", jobMap.get(JobField.starttime.name()));
    }

    @Test
    public void getEventsMissingBagDirectoryReturnsEmptyResponse() throws Exception {
        saveDepositWithDirectory(TEST_UUID, DepositState.finished, TEST_USERNAME,
                tmpFolder.resolve("missing-bag").toString());

        MvcResult result = mvc.perform(get("/edit/deposit/{uuid}/events", TEST_UUID))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals("", result.getResponse().getContentAsString());
    }

    @Test
    public void getEventsReturnsEventsXml() throws Exception {
        Path bagDir = tmpFolder.resolve("bag");
        Files.createDirectories(bagDir);

        String eventsXml = "<event xmlns=\"http://www.loc.gov/premis/v3\">"
                + "<eventIdentifier>"
                + "<eventIdentifierType>UUID</eventIdentifierType>"
                + "<eventIdentifierValue>event-123</eventIdentifierValue>"
                + "</eventIdentifier>"
                + "<eventType>ingestion</eventType>"
                + "</event>";

        Files.write(
                bagDir.resolve(DepositConstants.EVENTS_FILE),
                eventsXml.getBytes(StandardCharsets.UTF_8));

        saveDepositWithDirectory(TEST_UUID, DepositState.finished, TEST_USERNAME, bagDir.toString());

        MvcResult result = mvc.perform(get("/edit/deposit/{uuid}/events", TEST_UUID))
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        assertTrue(response.contains("events"));
        assertTrue(response.contains("eventIdentifier"));
        assertTrue(response.contains("eventIdentifierValue"));
        assertTrue(response.contains("event-123"));
        assertTrue(response.contains("ingestion"));
    }

    private void flushRedis() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushAll();
        }
    }

    private void saveDeposit(String uuid, DepositState state, String depositorName) {
        Map<String, String> deposit = new HashMap<>();
        deposit.put(DepositField.uuid.name(), uuid);
        deposit.put(DepositField.state.name(), state.name());
        deposit.put(DepositField.depositorName.name(), depositorName);
        deposit.put(DepositField.directory.name(), "/tmp/" + uuid);

        depositStatusFactory.save(uuid, deposit);
    }

    private void saveDepositWithDirectory(String uuid, DepositState state, String depositorName, String directory) {
        Map<String, String> deposit = new HashMap<>();
        deposit.put(DepositField.uuid.name(), uuid);
        deposit.put(DepositField.state.name(), state.name());
        deposit.put(DepositField.depositorName.name(), depositorName);
        deposit.put(DepositField.directory.name(), directory);

        depositStatusFactory.save(uuid, deposit);
    }
}
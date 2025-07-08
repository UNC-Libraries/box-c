package edu.unc.lib.boxc.web.services.rest;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.boxc.common.xml.SecureXMLFactory.createXMLInputFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.mutable.MutableInt;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.StAXStreamBuilder;
import org.jdom2.input.stax.DefaultStAXFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.deposit.api.DepositConstants;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositAction;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositPipelineState;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.model.DepositPipelineStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.DepositStatusFactory;
import edu.unc.lib.boxc.deposit.impl.model.JobStatusFactory;
import edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @author Gregory Jansen
 *
 */
@Controller
@RequestMapping(value = { "/edit/deposit*", "/edit/deposit" })
public class DepositController {
    private static final Logger LOG = LoggerFactory
            .getLogger(DepositController.class);
    public static final String BASE_PATH = "/api/edit/deposit/";

    @Resource
    protected JedisPool jedisPool;

    @Resource
    private DepositStatusFactory depositStatusFactory;

    @Resource
    private JobStatusFactory jobStatusFactory;

    @Resource
    private DepositPipelineStatusFactory pipelineStatusFactory;

    @Resource
    private File batchIngestFolder;

    @Autowired
    private GlobalPermissionEvaluator globalPermissionEvaluator;

    @PostConstruct
    public void init() {
    }

    @RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
    public @ResponseBody
    Map<String, ? extends Object> getInfo() {
        LOG.debug("getInfo() called");
        Map<String, Object> result = new HashMap<>();

        DepositPipelineState pipelineState = pipelineStatusFactory.getPipelineState();
        String pipeline = pipelineState == null ? "unknown" : pipelineState.toString();
        Map<String, MutableInt> countDepositStates = countDepositStates();
        result.put("state", pipeline);
        result.put("workers", determineWorkerState());
        result.put("activeJobs", countDepositStates.get(DepositState.running.name()).getValue());
        result.put("queuedJobs", countDepositStates.get(DepositState.queued.name()).getValue());
        result.put("pausedJobs", countDepositStates.get(DepositState.paused.name()).getValue());
        result.put("failedJobs", countDepositStates.get(DepositState.failed.name()).getValue());
        result.put("finishedJobs", countDepositStates.get(DepositState.finished.name()).getValue());
        result.put("id", "DEPOSIT");
        LOG.debug("getInfo() added counts: {}", result);

        Map<String, Object> uris = new HashMap<>();
        result.put("uris", uris);

        for (DepositState s : DepositState.values()) {
            uris.put(s.name(), BASE_PATH + s.name());
        }
        LOG.debug("getInfo() has: {}", result);

        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        result.put("isAdmin", globalPermissionEvaluator.hasGlobalPermission(principals,
                Permission.createAdminUnit));

        return result;
    }

    private String determineWorkerState() {
        Map<String, MutableInt> counts = countWorkerStates();
        int active = counts.get("active").getValue();
        int idle = counts.get("idle").getValue();
        int paused = counts.get("paused").getValue();

        LOG.debug("Work states: Active {}, Idle {}, Paused {}", active, idle, paused);

        String result = "";
        if (active > 0) {
            result = active + " active";
        }
        if (idle > 0) {
            if (result.length() > 0) {
                result += ", ";
            }
            result += idle + " idle";
        }
        if (paused > 0) {
            if (result.length() > 0) {
                result += ", ";
            }
            result += paused + " paused";
        }

        return result;
    }

    public @ResponseBody
    Map<String, MutableInt> countDepositStates() {
        Map<String, MutableInt> result = new HashMap<>();
        result.put(DepositState.cancelled.name(), new MutableInt());
        result.put(DepositState.failed.name(), new MutableInt());
        result.put(DepositState.finished.name(), new MutableInt());
        result.put(DepositState.paused.name(), new MutableInt());
        result.put(DepositState.queued.name(), new MutableInt());
        result.put(DepositState.running.name(), new MutableInt());
        result.put(DepositState.unregistered.name(), new MutableInt());
        LOG.debug("count deposit states");
        Map<String, Map<String, String>> deposits = fetchDepositMap();
        for (Map<String, String> deposit : deposits.values()) {
            String state = deposit.get(DepositField.state.name());
                MutableInt it = result.get(state);
                if (it != null) {
                    it.increment();
                }
        }
        return result;
    }

    private Map<String, MutableInt> countWorkerStates() {
        try (Jedis jedis = getJedisPool().getResource()) {
            Map<String, MutableInt> result = new HashMap<>();
            result.put("idle", new MutableInt());
            result.put("paused", new MutableInt());
            result.put("active", new MutableInt());
            final Set<String> workerNames = jedis.smembers("resque:workers");
            for (final String workerName : workerNames) {
                final String statusPayload = jedis.get("resque:worker:" + workerName);
                if (statusPayload == null) { // no payload key for workers that just started
                    result.get("idle").increment();
                } else {
                    try {
                        JsonNode w = new ObjectMapper().readTree(statusPayload.getBytes());
                        if (w.get("paused").asBoolean()) {
                            result.get("paused").increment();
                        } else {
                            result.get("active").increment();
                        }
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return result;
        }
    }

    private Map<String, Map<String, String>> fetchDepositMap() {
        Map<String, Map<String, String>> result = new HashMap<>();
        for (Map<String, String> deposit : this.depositStatusFactory.getAll()) {
            String uuid = deposit.get(DepositField.uuid.name());
            result.put(uuid, deposit);
        }
        return result;
    }

    public Map<String, Object> getByState(DepositState state) {
        LOG.debug("get by state: {}", state.name());
        Map<String, Object> result = new HashMap<>();
        Map<String, Map<String, String>> deposits = fetchDepositMap();

        boolean isRunning = state.equals(DepositState.running);

        for (Map<String, String> deposit : deposits.values()) {
            if (state.name().equals(deposit.get(DepositField.state.name()))) {
                Map<String, Object> depositResult = new HashMap<>();
                String uuid = deposit.get(DepositField.uuid.name());

                for (Entry<String, String> field : deposit.entrySet()) {
                    depositResult.put(field.getKey(), field.getValue());
                }

                if (isRunning) {
                    String jobUUID = jobStatusFactory.getWorkingJob(uuid);
                    if (jobUUID != null) {
                        Map<String, String> jobStatus = jobStatusFactory.get(jobUUID);

                        depositResult.put("currentJob", jobStatus);
                    }
                }

                result.put(uuid, depositResult);
            }
        }
        return result;
    }

    private Map<String, Object> getDetails(String depositUUID) {
        Map<String, String> status = depositStatusFactory.get(depositUUID);
        Map<String, Object> result = new HashMap<>();

        for (Entry<String, String> field : status.entrySet()) {
            result.put(field.getKey(), field.getValue());
        }

        Map<String, Map<String, String>> jobStatuses = jobStatusFactory.getAllJobs(depositUUID);
        result.put("jobs", jobStatuses);

        String state = status.get(DepositField.state.name());
        if (DepositState.running.name().equals(state)) {
            String jobUUID = jobStatusFactory.getWorkingJob(depositUUID);
            result.put("currentJobUUID", jobUUID);
        }

        result.put("jobsURI", "/api/status/deposit/" + depositUUID + "/jobs");
        result.put("eventsURI", "/api/status/deposit/" + depositUUID + "/eventsXML");

        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        result.put("isAdmin", globalPermissionEvaluator.hasGlobalPermission(principals,
                Permission.createAdminUnit));

        return result;
    }

    @RequestMapping(value = { "{stateOrUUID}", "/{stateOrUUID}" }, method = RequestMethod.GET)
    public @ResponseBody
    Map<String, Object> get(@PathVariable String stateOrUUID) {
        DepositState depositState = null;
        try {
            depositState = DepositState.valueOf(stateOrUUID);
        } catch (IllegalArgumentException ignore) {
        }

        // Request was for a state, get all jobs in that state
        if (depositState != null) {
            return getByState(depositState);
        } else {
            // Request was for a specific job
            return getDetails(stateOrUUID);
        }
    }

    /**
     * Aborts the deposit, reversing any ingests and scheduling a cleanup job.
     *
     * @param uuid
     */
    @RequestMapping(value = { "{uuid}", "/{uuid}" }, method = RequestMethod.DELETE)
    public void destroy(@PathVariable String uuid) {
        // verify deposit is registered and not yet cleaned up
        // set deposit status to canceling
    }

    /**
     * Request to pause, resume, cancel or destroy a deposit. The deposit cancel action will stop the deposit, purge any
     * ingested objects and schedule deposit destroy in the future. The deposit pause action halts work on a
     * deposit such that it can be resumed later. The deposit destroy action cleans up the submitted deposit package,
     * leaving staged files alone.
     *
     * @param uuid
     *           the unique identifier of the deposit
     * @param action
     *           the action to take on the deposit (pause, resume, cancel, destroy)
     */
    @RequestMapping(value = { "{uuid}", "/{uuid}" }, method = RequestMethod.POST)
    public void update(@PathVariable String uuid, @RequestParam(required = true) String action,
            HttpServletResponse response) {
        DepositAction actionRequested = DepositAction.valueOf(action);
        if (actionRequested == null) {
            throw new IllegalArgumentException(
                    "The deposit action is not recognized: " + action);
        }
        // permission check, admin group or depositor required
        String username = GroupsThreadStore.getUsername();
        Map<String, String> status = depositStatusFactory.get(uuid);
        AccessGroupSet principals = getAgentPrincipals().getPrincipals();

        if (!globalPermissionEvaluator.hasGlobalPermission(principals, Permission.ingest)) {
            if (username == null || (!username.equals(status.get(DepositField.depositorName.name())) &&
                    !globalPermissionEvaluator.hasGlobalPermission(principals, Permission.createAdminUnit))) {
                response.setStatus(403);
                return;
            }
        }
        String state = status.get(DepositField.state.name());
        switch (actionRequested) {
            case pause:
                if (DepositState.finished.name().equals(state)) {
                    throw new IllegalArgumentException("That deposit has already finished");
                } else if (DepositState.failed.name().equals(state)) {
                    throw new IllegalArgumentException("That deposit has already failed");
                } else {
                    depositStatusFactory.requestAction(uuid, DepositAction.pause);
                    response.setStatus(204);
                }
                break;
            case resume:
                if (!DepositState.paused.name().equals(state) && !DepositState.failed.name().equals(state)) {
                    throw new IllegalArgumentException("The deposit must be paused or failed before you can resume");
                } else {
                    depositStatusFactory.requestAction(uuid, DepositAction.resume);
                    response.setStatus(204);
                }
                break;
            case cancel:
                if (DepositState.finished.name().equals(state)) {
                    throw new IllegalArgumentException("That deposit has already finished");
                } else {
                    depositStatusFactory.requestAction(uuid, DepositAction.cancel);
                    response.setStatus(204);
                }
                break;
            case destroy:
                if (DepositState.cancelled.name().equals(state) || DepositState.finished.name().equals(state)) {
                    depositStatusFactory.requestAction(uuid, DepositAction.destroy);
                    response.setStatus(204);
                } else {
                    throw new IllegalArgumentException(
                            "The deposit must be finished or cancelled before it is destroyed");
                }
                break;
            default:
                throw new IllegalArgumentException("The requested deposit action is not implemented: " + action);
        }
    }

    @RequestMapping(value = { "{uuid}/jobs", "/{uuid}/jobs" }, method = RequestMethod.GET)
    public @ResponseBody
    Map<String, Map<String, String>> getJobs(@PathVariable String uuid) {
        LOG.debug("getJobs( {} )", uuid);
        try (Jedis jedis = getJedisPool().getResource()) {
            Map<String, Map<String, String>> jobs = new HashMap<>();
            Set<String> jobUUIDs = jedis
                    .smembers(RedisWorkerConstants.DEPOSIT_TO_JOBS_PREFIX + uuid);
            for (String jobUUID : jobUUIDs) {
                Map<String, String> info = jedis
                        .hgetAll(RedisWorkerConstants.JOB_STATUS_PREFIX + jobUUID);
                jobs.put(jobUUID, info);
            }
            return jobs;
        }

    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    @RequestMapping(value = { "{uuid}/events" }, method = RequestMethod.GET)
    public @ResponseBody
    Document getEvents(@PathVariable String uuid) throws Exception {
        LOG.debug("getEvents( {} )", uuid);
        String bagDirectory;
        try (Jedis jedis = getJedisPool().getResource()) {
            bagDirectory = jedis.hget(
                    RedisWorkerConstants.DEPOSIT_STATUS_PREFIX + uuid,
                    RedisWorkerConstants.DepositField.directory.name());
        }
        File bagFile = new File(bagDirectory);
        if (!bagFile.exists()) {
            return null;
        }
        File eventsFile = new File(bagDirectory, DepositConstants.EVENTS_FILE);
        if (!eventsFile.exists()) {
            return null;
        }
        Element events = new Element("events", JDOMNamespaceUtil.PREMIS_V2_NS);
        Document result = new Document(events);
        XMLInputFactory factory = createXMLInputFactory();
        try (FileInputStream fis = new FileInputStream(eventsFile)) {
            XMLStreamReader reader = factory.createXMLStreamReader(fis);
            StAXStreamBuilder builder = new StAXStreamBuilder();
            List<Content> list = builder.buildFragments(reader,
                    new DefaultStAXFilter());
            events.addContent(list);
            return result;
        }
    }

}

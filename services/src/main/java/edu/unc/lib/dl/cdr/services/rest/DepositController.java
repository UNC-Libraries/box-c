/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.cdr.services.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom2.Content;
import org.jdom2.input.StAXStreamBuilder;
import org.jdom2.input.stax.DefaultStAXFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.RedisWorkerConstants;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositAction;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * @author Gregory Jansen
 * 
 */
@Controller
@RequestMapping(value = { "/status/deposit*", "/status/deposit" })
public class DepositController {
	private static final Logger LOG = LoggerFactory
			.getLogger(DepositController.class);
	public static final String BASE_PATH = "/api/status/deposit/";

	@Resource
	protected JedisPool jedisPool;

	@Resource
	private DepositStatusFactory depositStatusFactory;

	@Resource
	private File batchIngestFolder;
	
	class MutableInt {
		  int value = 0;
		  public void increment () { ++value;      }
		  public int  get ()       { return value; }
	}
	
	@PostConstruct
	public void init() {
	}

	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getInfo() {
		LOG.debug("getInfo() called");
		Map<String, Object> result = new HashMap<String, Object>();
		//Map<String, Map<String, String>> deposits = fetchDepositMap();
		
		Map<String, MutableInt> counts = countWorkerStates();
		Map<String, MutableInt> countDepositStates = countDepositStates();
		int active = counts.get("active").get();
		result.put("active", (active > 0) ? true : false);
		result.put("idle", (counts.get("idle").get()) > 0 ? true : false);
		//result.put("activeJobs", counts.get("active").get());
		result.put("activeJobs", countDepositStates.get(DepositState.running.name()).get());
		result.put("queuedJobs", countDepositStates.get(DepositState.queued.name()).get());
		result.put("pausedJobs", countDepositStates.get(DepositState.paused.name()).get());
		result.put("failedJobs", countDepositStates.get(DepositState.failed.name()).get());
		result.put("finishedJobs", countDepositStates.get(DepositState.finished.name()).get());
		result.put("id", "DEPOSIT");
		LOG.debug("getInfo() added counts: {}", result);

		Map<String, Object> uris = new HashMap<String, Object>();
		result.put("uris", uris);

		for(DepositState s : DepositState.values()) {
			uris.put(s.name(), BASE_PATH + s.name());
		}
		LOG.debug("getInfo() has: {}", result);

		return result;
	}
	
	public @ResponseBody
	Map<String, MutableInt> countDepositStates() {
		Map<String, MutableInt> result = new HashMap<String, MutableInt>();
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
				String state = (String) deposit.get(DepositField.state.name());
				MutableInt it = result.get(state);
				if(it != null) it.increment();
		}
		return result;
	}
	
	private Map<String, MutableInt> countWorkerStates() {
		Jedis jedis = getJedisPool().getResource();
		Map<String, MutableInt> result = new HashMap<String, MutableInt>();
		result.put("idle", new MutableInt());
		result.put("paused", new MutableInt());
		result.put("active", new MutableInt());
        final Set<String> workerNames = jedis.smembers("resque:workers");
        for (final String workerName : workerNames) {
        	final String statusPayload = jedis.get("resque:worker:"+workerName);
        	if(statusPayload == null) { // no payload key for works that just started
        		result.get("idle").increment();
        	} else {
        		try {
					JsonNode w = new ObjectMapper().readTree(statusPayload.getBytes());
					if(w.get("paused").asBoolean()) {
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

	private Map<String, Map<String, String>> fetchDepositMap() {
		Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
		for (Map<String, String> deposit : this.depositStatusFactory.getAll()) {
			String uuid = deposit.get(DepositField.uuid.name());
			result.put(uuid, deposit);
		}
		return result;
	}
	
	public @ResponseBody
	Map<String, Object> getByState(DepositState state) {
		LOG.debug("get by state: {}", state.name());
		Map<String, Object> result = new HashMap<String, Object>();
		Map<String, Map<String, String>> deposits = fetchDepositMap();
		for (Map<String, String> deposit : deposits.values()) {
			if (state.equals(deposit.get(DepositField.state.name()))) {
				String uuid = (String) deposit.get(DepositField.uuid.name());
				result.put(uuid, deposit);
			}
		}
		return result;
	}

	@RequestMapping(value = { "{uuid}", "/{uuid}" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, Object> get(@PathVariable String uuid) {
		DepositState state = null;
		try {
			state = DepositState.valueOf(uuid);
		} catch(IllegalArgumentException ignore) {}
		if (state != null) {
			return getByState(state);
		} else {
			Map<String, Object> result = new HashMap<String, Object>();
			Jedis jedis = getJedisPool().getResource();
			Map<String, String> status = jedis.hgetAll(uuid);
			getJedisPool().returnResource(jedis);
			for(Entry<String, String> field : status.entrySet()) {
				result.put(field.getKey(), field.getValue());
			}			
			result.put("jobsURI", "/api/status/deposit/" + uuid + "/jobs");
			result.put("eventsURI", "/api/status/deposit/" + uuid + "/eventsXML");
			
			return result;
		}
	}

	/**
	 * Aborts the deposit, reversing any ingests and scheduling a cleanup job.
	 * 
	 * @param depositUUID
	 */
	@RequestMapping(value = { "{uuid}", "/{uuid}" }, method = RequestMethod.DELETE)
	public void destroy(@PathVariable String uuid) {
		// verify deposit is registered and not yet cleaned up
		// set deposit status to canceling
	}

	/**
	 * Request to pause, resume, cancel or destroy a deposit. The deposit cancel
	 * action will stop the deposit, purge any ingested objects and schedule
	 * deposit destroy in the future. The deposit pause action halts work on a
	 * deposit such that it can be resumed later. The deposit destroy action
	 * cleans up the submitted deposit package, leaving staged files alone.
	 * 
	 * @param depositUUID
	 *            the unique identifier of the deposit
	 * @param action
	 *            the action to take on the deposit (pause, resume, cancel,
	 *            destroy)
	 */
	@RequestMapping(value = { "{uuid}", "/{uuid}" }, method = RequestMethod.POST)
	public void update(@PathVariable String uuid,
			@RequestParam(required = true) String action) {
		DepositAction actionRequested = DepositAction.valueOf(action);
		if (actionRequested == null) {
			throw new IllegalArgumentException(
					"The deposit action is not recognized: " + action);
		}
		Map<String, String> status = depositStatusFactory.get(uuid);
		String state = status.get(DepositField.state.name());
		switch (actionRequested) {
		case pause:
			if (DepositState.finished.name().equals(state)) {
				throw new IllegalArgumentException(
						"That deposit has already finished");
			} else if (DepositState.failed.name().equals(state)) {
				throw new IllegalArgumentException(
						"That deposit has already failed");
			} else {
				depositStatusFactory.requestAction(uuid, DepositAction.pause);
			}
			break;
		case resume:
			if (!DepositState.paused.name().equals(state)) {
				throw new IllegalArgumentException(
						"The deposit must be paused before you can resume");
			} else {
				depositStatusFactory.requestAction(uuid, DepositAction.resume);
			}
			break;
		case cancel:
			if (DepositState.finished.name().equals(state)) {
				throw new IllegalArgumentException(
						"That deposit has already finished");
			} else {
				depositStatusFactory.requestAction(uuid, DepositAction.cancel);
			}
			break;
		case destroy:
			if (DepositState.cancelled.name().equals(state)
					|| DepositState.finished.name().equals(state)) {
				depositStatusFactory.requestAction(uuid, DepositAction.destroy);
			} else {
				throw new IllegalArgumentException(
						"The deposit must be finished or cancelled before it is destroyed");
			}
			break;
		default:
			throw new IllegalArgumentException(
					"The requested deposit action is not implemented: "
							+ action);
		}
	}

	@RequestMapping(value = { "{uuid}/jobs", "/{uuid}/jobs" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, Map<String, String>> getJobs(@PathVariable String uuid) {
		LOG.debug("getJobs( {} )", uuid);
		Jedis jedis = getJedisPool().getResource();
		Map<String, Map<String, String>> jobs = new HashMap<String, Map<String, String>>();
		Set<String> jobUUIDs = jedis
				.smembers(RedisWorkerConstants.DEPOSIT_TO_JOBS_PREFIX + uuid);
		for (String jobUUID : jobUUIDs) {
			Map<String, String> info = jedis
					.hgetAll(RedisWorkerConstants.JOB_STATUS_PREFIX + jobUUID);
			jobs.put(jobUUID, info);
		}
		getJedisPool().returnResource(jedis);
		return jobs;
	}

	public JedisPool getJedisPool() {
		return jedisPool;
	}

	@RequestMapping(value = { "{uuid}/events", "/{uuid}/events" }, method = RequestMethod.GET)
	public @ResponseBody
	Document getEvents(@PathVariable String uuid) throws Exception {
		LOG.debug("getEvents( {} )", uuid);
		Jedis jedis = getJedisPool().getResource();
		String bagDirectory = jedis.hget(
				RedisWorkerConstants.DEPOSIT_STATUS_PREFIX + uuid,
				RedisWorkerConstants.DepositField.directory.name());
		getJedisPool().returnResource(jedis);
		File bagFile = new File(bagDirectory);
		if (!bagFile.exists())
			return null;
		File eventsFile = new File(bagDirectory, DepositConstants.EVENTS_FILE);
		if (!eventsFile.exists())
			return null;
		Element events = new Element("events", JDOMNamespaceUtil.PREMIS_V2_NS);
		Document result = new Document(events);
		XMLInputFactory factory = XMLInputFactory.newInstance();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(eventsFile);
			XMLStreamReader reader = factory.createXMLStreamReader(fis);
			StAXStreamBuilder builder = new StAXStreamBuilder();
			List<Content> list = builder.buildFragments(reader,
					new DefaultStAXFilter());
			events.addContent(list);
		} finally {
			IOUtils.closeQuietly(fis);
		}
		return result;
	}

}

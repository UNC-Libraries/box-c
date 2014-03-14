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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.RedisWorkerConstants;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * @author Gregory Jansen
 * 
 */
@Controller
@RequestMapping(value = { "/deposit*", "/deposit" })
public class DepositController {
	private static final Logger LOG = LoggerFactory.getLogger(DepositController.class);

	@Resource
	protected Jedis jedis;
	
	@Resource
	private File batchIngestFolder;

	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getAll() {
		Map<String, Object> result = new HashMap<String, Object>();
		LOG.debug("getAll()");
		Map<String, Map<String, String>> deposits = new HashMap<String, Map<String, String>>();
		result.put("deposits", deposits);
		Set<String> depositUUIDs = this.jedis.smembers(RedisWorkerConstants.DEPOSIT_SET);
		for(String depositUUID : depositUUIDs) {
			deposits.put(depositUUID, get(depositUUID));
		}
		return result;
	}
	
	public List<String> getUnregistered() {
		List<String> result = null;
		return result;
	}

	/**
	 * Registers a new deposit folder or file in Jedis.
	 * @param uuid
	 */
	@RequestMapping(value = { "{uuid}", "/{uuid}" }, method = RequestMethod.PUT)
	public void register(@PathVariable String uuid) {
		
	}
	
	@RequestMapping(value = { "{uuid}", "/{uuid}" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, String> get(@PathVariable String uuid) {
		Map<String, String> info = this.jedis.hgetAll(uuid);
		info.put("jobsURI", "/api/status/deposit/"+uuid+"/jobs");
		info.put("eventsURI", "/api/status/deposit/"+uuid+"/eventsXML");
		return info;
	}
	
	/**
	 * Aborts the deposit, reversing any ingests and scheduling a cleanup job.
	 * @param depositUUID
	 */
	@RequestMapping(value = { "{uuid}", "/{uuid}" }, method = RequestMethod.DELETE)
	public void cancel(@PathVariable String uuid) {
		// verify deposit is registered and not yet cleaned up
		// set deposit status to cancelled
	}
	
//	/**
//	 * Asks repository to pause work on this deposit until further notice.
//	 * @param depositUUID
//	 */
//	public void pause(String depositUUID) {
//		
//	}
//	
//	/**
//	 * Asks repository to resume work on this deposit (after finishing other deposits).
//	 * @param depositUUID
//	 */
//	public void resume(String depositUUID) {
//		
//	}
//	
//	/**
//	 * Requests clean up of the deposit package and optionally any staged files.
//	 * @param depositUUID
//	 * @param deleteExtraStagedFiles if true, attempt to delete ingested staged files
//	 */
//	public void cleanup(String depositUUID, boolean deleteExtraStagedFiles) {
//		
//	}
	
	@RequestMapping(value = { "{uuid}/jobs", "/{uuid}/jobs" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getJobs(@PathVariable String uuid) {
		Map<String, Object> result = new HashMap<String, Object>();
		LOG.debug("getJobs( {} )", uuid);		
		Map<String, Map<String, String>> jobs = new HashMap<String, Map<String, String>>();
		result.put("jobs", jobs);
		Set<String> jobUUIDs = this.jedis.smembers(RedisWorkerConstants.DEPOSIT_TO_JOBS_PREFIX+uuid);
		for(String jobUUID : jobUUIDs) {
			Map<String, String> info = this.jedis.hgetAll(RedisWorkerConstants.JOB_STATUS_PREFIX+jobUUID);
			jobs.put(jobUUID, info);
		}
		return result;
	}
	
	@RequestMapping(value = { "{uuid}/events", "/{uuid}/events" }, method = RequestMethod.GET)
	public @ResponseBody()
	Document getEvents(@PathVariable String uuid) throws Exception {
		LOG.debug("getEvents( {} )", uuid);
		String bagDirectory = this.jedis.hget(
				RedisWorkerConstants.DEPOSIT_STATUS_PREFIX+uuid, 
				RedisWorkerConstants.DepositField.directory.name());
		File bagFile = new File(bagDirectory);
		if(!bagFile.exists()) return null;
		File eventsFile = new File(bagDirectory, DepositConstants.EVENTS_FILE);
		if(!eventsFile.exists()) return null;
		Element events = new Element("events", JDOMNamespaceUtil.PREMIS_V2_NS);
		Document result = new Document(events);
		XMLInputFactory factory = XMLInputFactory.newInstance();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(eventsFile);
			XMLStreamReader reader = factory.createXMLStreamReader(fis);
			StAXStreamBuilder builder = new StAXStreamBuilder();
			List<Content> list = builder.buildFragments(reader, new DefaultStAXFilter());
			events.addContent(list);
		} finally {
			IOUtils.closeQuietly(fis);
		}
		return result;
	}

}

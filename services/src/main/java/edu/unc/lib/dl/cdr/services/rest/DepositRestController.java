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
import org.springframework.web.bind.annotation.ResponseBody;

import redis.clients.jedis.Jedis;
import edu.unc.lib.dl.util.BagConstants;
import edu.unc.lib.dl.util.RedisWorkerConstants;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * @author Gregory Jansen
 * 
 */
@Controller
@RequestMapping(value = { "/status/deposit*", "/status/deposit" })
public class DepositRestController {
	private static final Logger LOG = LoggerFactory.getLogger(DepositRestController.class);

	@Resource
	protected Jedis jedis;

	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getDeposits() {
		Map<String, Object> result = new HashMap<String, Object>();
		LOG.debug("getDeposits()");		
		Map<String, Map<String, String>> deposits = new HashMap<String, Map<String, String>>();
		result.put("deposits", deposits);
		Set<String> depositUUIDs = this.jedis.smembers(RedisWorkerConstants.DEPOSIT_SET);
		for(String depositUUID : depositUUIDs) {
			Map<String, String> info = this.jedis.hgetAll(depositUUID);
			info.put("jobsURI", "/api/status/deposit/"+depositUUID+"/jobs");
			info.put("eventsURI", "/api/status/deposit/"+depositUUID+"/eventsXML");
			deposits.put(depositUUID, info);
		}
		return result;
	}
	
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
	
	@RequestMapping(value = { "{uuid}/eventsXML", "/{uuid}/eventsXML" }, method = RequestMethod.GET)
	public @ResponseBody()
	Document getEvents(@PathVariable String uuid) throws Exception {
		LOG.debug("getEvents( {} )", uuid);
		String bagDirectory = this.jedis.hget(
				RedisWorkerConstants.DEPOSIT_STATUS_PREFIX+uuid, 
				RedisWorkerConstants.DepositField.bagDirectory.name());
		File bagFile = new File(bagDirectory);
		if(!bagFile.exists()) return null;
		File eventsFile = new File(bagDirectory, BagConstants.EVENTS_FILE);
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

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

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.model.CDRMessageContent;
import edu.unc.lib.dl.cdr.services.model.FailedObjectHashMap;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.processing.EnhancementConductor;
import edu.unc.lib.dl.cdr.services.processing.EnhancementConductor.PerformServicesRunnable;

/**
 * 
 * @author bbpennel
 *
 */
@Controller
@RequestMapping(value={"/enhancement*", "/enhancement"})
public class EnhancementConductorRestController extends AbstractServiceConductorRestController {
	private static final Logger LOG = LoggerFactory.getLogger(EnhancementConductorRestController.class);
	public static final String BASE_PATH = "/rest/enhancement/";
	public static final String QUEUED_PATH = "queued";
	public static final String BLOCKED_PATH = "blocked";
	public static final String ACTIVE_PATH = "active";
	public static final String FAILED_PATH = "failed";
	
	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss.SSS");
	
	@Resource
	private EnhancementConductor enhancementConductor;
	
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getInfo() {
		Map<String, Object> result = new HashMap<String, Object>();
		addServiceConductorInfo(result, this.enhancementConductor);
		result.put("pendingJobs", this.enhancementConductor.getQueueSize());
		result.put("queuedJobs", this.enhancementConductor.getPidQueue().size());
		result.put("blockedJobs", this.enhancementConductor.getCollisionList().size());
		result.put("failedJobs", this.enhancementConductor.getFailedPids().size());
		result.put("actievJobs", this.enhancementConductor.getExecutor().getRunningNow().size());
		
		Map<String, Object> uris = new HashMap<String, Object>();
		result.put("uris", uris);
		
		uris.put("queuedJobs", contextUrl + BASE_PATH + QUEUED_PATH);
		uris.put("blockedJobs", contextUrl + BASE_PATH + BLOCKED_PATH);
		uris.put("activeJobs", contextUrl + BASE_PATH + ACTIVE_PATH);
		uris.put("failedJobs", contextUrl + BASE_PATH + FAILED_PATH);
		
		return result;
	}
	
	@RequestMapping(value = QUEUED_PATH, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getQueuedInfo( 
			@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		//Duplicate pid queue so that we can iterate over it.
		List<PIDMessage> queued = new ArrayList<PIDMessage>(this.enhancementConductor.getPidQueue());
		addPIDMessageListInfo(result, queued, begin, end, QUEUED_PATH);
		
		return result;
	}
	
	@RequestMapping(value = BLOCKED_PATH, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getBlockedInfo( 
			@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		
		Map<String, Object> result = new HashMap<String, Object>();
		addPIDMessageListInfo(result, this.enhancementConductor.getCollisionList(), begin, end, BLOCKED_PATH);
		return result;
	}
	
	@RequestMapping(value = ACTIVE_PATH, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getActiveInfo( 
			@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		
		Set<PerformServicesRunnable> currentlyRunning = this.enhancementConductor.getExecutor().getRunningNow();
		List<PIDMessage> messages = new ArrayList<PIDMessage>();
		for (PerformServicesRunnable task: currentlyRunning){
			messages.add(task.getPidMessage());
		}
		Map<String, Object> result = new HashMap<String, Object>();
		addPIDMessageListInfo(result, messages, begin, end, ACTIVE_PATH);
		return result;
	}
	
	private void addPIDMessageListInfo(Map<String, Object> result, List<PIDMessage> messages, Integer begin, Integer end, String queuePath){
		result.put("count", messages.size());
		
		if(begin == null) {
			begin = 0;
		}
		if(end == null) {
			end = messages.size();
		}
		result.put("begin", begin);
		result.put("end", end);
		if (begin != null && end != null) {
			messages = messages.subList(begin, end);
		}
		
		List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
		result.put("jobs", jobs);
		for (PIDMessage message: messages){
			if (message != null){
				Map<String, Object> job = getJobBriefInfo(message, queuePath);
				jobs.add(job);
			}
		}
	}
	
	/**
	 * Returns a view of the failed job list.
	 * Failed list does not retain order, so paging is not possible.
	 * @return
	 */
	@RequestMapping(value = FAILED_PATH, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getFailedInfo() {
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
		result.put("jobs", jobs);
		
		FailedObjectHashMap failedList = this.enhancementConductor.getFailedPids();
		Iterator<Entry<String,Set<String>>> iterator = failedList.entrySet().iterator();
		
		result.put("count", failedList.size());
		
		while (iterator.hasNext()){
			Entry<String,Set<String>> entry = iterator.next();
			Map<String, Object> job = new HashMap<String,Object>();
			job.put("id", entry.getKey());
			job.put("services", entry.getValue());
			jobs.add(job);
		}
		
		return result;
	}
	
	/**
	 * Returns the full details for a job selected by id, which is the hash code of the message object
	 * until another identifier is added.
	 * @param id
	 * @return
	 */
	@RequestMapping(value = { QUEUED_PATH + "/job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getQueuedJobInfo(@PathVariable("id") String id){
		return getJobFullInfo(lookupJobInfo(id, new ArrayList<PIDMessage>(this.enhancementConductor.getPidQueue())), QUEUED_PATH);
	}
	
	@RequestMapping(value = { BLOCKED_PATH + "/job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getBlockedJobInfo(@PathVariable("id") String id){
		return getJobFullInfo(lookupJobInfo(id, this.enhancementConductor.getCollisionList()), BLOCKED_PATH);
	}
	
	@RequestMapping(value = { ACTIVE_PATH + "/job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getActiveJobInfo(@PathVariable("id") String id){
		Set<PerformServicesRunnable> currentlyRunning = this.enhancementConductor.getExecutor().getRunningNow();
		List<PIDMessage> messages = new ArrayList<PIDMessage>();
		for (PerformServicesRunnable task: currentlyRunning){
			messages.add(task.getPidMessage());
		}
		return getJobFullInfo(lookupJobInfo(id, messages), ACTIVE_PATH);
	}
	
	/**
	 * Finds a PIDMessage in the provided list which matches the given message id.
	 * @param id
	 * @param messages
	 * @return
	 */
	private PIDMessage lookupJobInfo(String id, List<PIDMessage> messages){
		for (PIDMessage message: messages){
			if (message.getMessageID().equals(id)){
				return message;
			}
		}
		return null;
	}
	
	/**
	 * Transforms a PIDMessage object into a map of properties for reporting purposes.
	 * @return
	 */
	private Map<String, Object> getJobBriefInfo(PIDMessage message, String queuePath){
		Map<String, Object> job = new HashMap<String, Object>();
		
		job.put("id", message.getMessageID());
		job.put("targetPID", message.getPIDString());
		addJobPropertyIfNotEmpty("depositID", message.getDepositID(), job);
		addJobPropertyIfNotEmpty("dataStream", message.getDatastream(), job);
		addJobPropertyIfNotEmpty("relation", message.getRelation(), job);
		addJobPropertyIfNotEmpty("action", message.getQualifiedAction(), job);

		CDRMessageContent cdrMessage = message.getCDRMessageContent(); 
		if (cdrMessage != null){
			addJobPropertyIfNotEmpty("mode", cdrMessage.getMode(), job);
			addJobPropertyIfNotEmpty("targetParent", cdrMessage.getParent(), job);
		}
		
		addJobPropertyIfNotEmpty("generatedTimestamp", message.getTimestamp(), job);
		Date timeCreated = new Date(message.getTimeCreated());
		job.put("queuedTimestamp", format.format(timeCreated));
		
		Map<String, Object> uris = new HashMap<String, Object>();
		job.put("uris", uris);
		uris.put("jobInfo", contextUrl + BASE_PATH + queuePath + "/job/" + message.getMessageID());

		return job;
	}
	
	/**
	 * Transforms a PIDMessage object into a map of properties for reporting purposes.
	 * @return
	 */
	private Map<String, Object> getJobFullInfo(PIDMessage message, String queuedPath){
		Map<String, Object> job = new HashMap<String, Object>();
		
		job.put("id", message.getMessageID());
		job.put("targetPID", message.getPIDString());
		addJobPropertyIfNotEmpty("depositID", message.getDepositID(), job);
		addJobPropertyIfNotEmpty("dataStream", message.getDatastream(), job);
		addJobPropertyIfNotEmpty("relation", message.getRelation(), job);
		addJobPropertyIfNotEmpty("action", message.getQualifiedAction(), job);

		CDRMessageContent cdrMessage = message.getCDRMessageContent(); 
		if (cdrMessage != null){
			addJobPropertyIfNotEmpty("mode", cdrMessage.getMode(), job);
			addJobPropertyIfNotEmpty("targetParent", cdrMessage.getParent(), job);
			addJobPropertyIfNotEmpty("oldParents", cdrMessage.getOldParents(), job);
			addJobPropertyIfNotEmpty("reordered", cdrMessage.getReordered(), job);
			addJobPropertyIfNotEmpty("subjects", cdrMessage.getSubjects(), job);
		}
		
		addJobPropertyIfNotEmpty("generatedTimestamp", message.getTimestamp(), job);
		Date timeCreated = new Date(message.getTimeCreated());
		job.put("queuedTimestamp", format.format(timeCreated));
		
		if (message.getFilteredServices() != null){
			List<String> filteredServices = new ArrayList<String>();
			for (ObjectEnhancementService service: message.getFilteredServices()){
				filteredServices.add(service.getClass().getName());
			}
			job.put("filteredServices", filteredServices);
		}
		
		if (message.getMessage() != null){
			Map<String, Object> uris = new HashMap<String, Object>();
			job.put("uris", uris);
			uris.put("xml", contextUrl + BASE_PATH + queuedPath + "/job/" + message.getMessageID() + "/xml");
		}

		return job;
	}
	
	@RequestMapping(value = { QUEUED_PATH + "/job/{id}/xml" }, method = RequestMethod.GET)
	public void getQueuedJobXML(HttpServletResponse response, @PathVariable("id") String id) throws IOException{
		response.setContentType("application/xml");
		PrintWriter pr = response.getWriter();
		pr.write(getJobXML(id, new ArrayList<PIDMessage>(this.enhancementConductor.getPidQueue())));
	}
	
	@RequestMapping(value = { BLOCKED_PATH + "/job/{id}/xml" }, method = RequestMethod.GET)
	public void getBlockedJobXML(HttpServletResponse response, @PathVariable("id") String id) throws IOException{
		response.setContentType("application/xml");
		PrintWriter pr = response.getWriter();
		pr.write(getJobXML(id, new ArrayList<PIDMessage>(this.enhancementConductor.getCollisionList())));
	}
	
	@RequestMapping(value = { ACTIVE_PATH + "/job/{id}/xml" }, method = RequestMethod.GET)
	public void getActiveJobXML(HttpServletResponse response, @PathVariable("id") String id) throws IOException{
		response.setContentType("application/xml");
		PrintWriter pr = response.getWriter();
		Set<PerformServicesRunnable> currentlyRunning = this.enhancementConductor.getExecutor().getRunningNow();
		List<PIDMessage> messages = new ArrayList<PIDMessage>();
		for (PerformServicesRunnable task: currentlyRunning){
			messages.add(task.getPidMessage());
		}
		pr.write(getJobXML(id, messages));
	}
	
	private String getJobXML(String id, List<PIDMessage> messages){
		PIDMessage message = lookupJobInfo(id, messages);
		if (message != null && message.getMessage() != null){
			XMLOutputter outputter = new XMLOutputter();
			try {
				return outputter.outputString(message.getMessage());
			} catch (Exception e) {
				LOG.error("Error while generating xml output for " + id, e);
			}
		}
		return null;
	}
	
	private void addJobPropertyIfNotEmpty(String propertyName, Object propertyValue, Map<String, Object> job){
		if (propertyValue != null || !"".equals(propertyValue)){
			job.put(propertyName, propertyValue);
		}
	}

	public void setEnhancementConductor(EnhancementConductor enhancementConductor) {
		this.enhancementConductor = enhancementConductor;
	}
}

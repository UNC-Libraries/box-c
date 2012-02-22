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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.cdr.services.processing.SolrUpdateConductor;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRunnable;
import edu.unc.lib.dl.message.ActionMessage;

@Controller
@RequestMapping(value={"/indexing*", "/indexing"})
public class SolrUpdateConductorRestController extends AbstractServiceConductorRestController {
	public static final String BASE_PATH = "/rest/indexing/";
	public static final String QUEUED_PATH = "queued";
	public static final String BLOCKED_PATH = "blocked";
	public static final String ACTIVE_PATH = "active";
	
	@Resource
	private SolrUpdateConductor solrUpdateConductor;
	
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getInfo() {
		Map<String, Object> result = new HashMap<String, Object>();
		addServiceConductorInfo(result, this.solrUpdateConductor);
		result.put("pendingJobs", this.solrUpdateConductor.getQueueSize());
		result.put("queuedJobs", this.solrUpdateConductor.getPidQueue().size());
		result.put("blockedJobs", this.solrUpdateConductor.getCollisionList().size());
		result.put("activeJobs", this.solrUpdateConductor.getThreadPoolExecutor().getRunningNow().size());
		
		Map<String, Object> uris = new HashMap<String, Object>();
		result.put("uris", uris);
		
		uris.put("queuedJobs", contextUrl + BASE_PATH + QUEUED_PATH);
		uris.put("blockedJobs", contextUrl + BASE_PATH + BLOCKED_PATH);
		uris.put("activeJobs", contextUrl + BASE_PATH + ACTIVE_PATH);
		
		return result;
	}
	
	@RequestMapping(value = QUEUED_PATH, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getQueuedInfo( 
			@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		//Duplicate pid queue so that we can iterate over it.
		List<ActionMessage> messageList = new ArrayList<ActionMessage>(this.solrUpdateConductor.getPidQueue());
		addMessageListInfo(result, messageList, begin, end, QUEUED_PATH);
		
		return result;
	}
	
	@RequestMapping(value = BLOCKED_PATH, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getBlockedInfo( 
			@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		//Duplicate pid queue so that we can iterate over it.
		List<ActionMessage> messageList = new ArrayList<ActionMessage>(this.solrUpdateConductor.getCollisionList());
		addMessageListInfo(result, messageList, begin, end, BLOCKED_PATH);
		
		return result;
	}
	
	@RequestMapping(value = ACTIVE_PATH, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getActiveInfo( 
			@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		//Duplicate pid queue so that we can iterate over it.
		List<ActionMessage> messageList = new ArrayList<ActionMessage>();
		Set<SolrUpdateRunnable> currentlyRunning = this.solrUpdateConductor.getThreadPoolExecutor().getRunningNow();
		for (SolrUpdateRunnable runningTask : currentlyRunning) {
			messageList.add(runningTask.getUpdateRequest());
		}
		addMessageListInfo(result, messageList, begin, end, ACTIVE_PATH);
		
		return result;
	}
	
	@RequestMapping(value = { QUEUED_PATH + "/job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getQueuedJobInfo(@PathVariable("id") String id){
		return getJobFullInfo(lookupJobInfo(id, new ArrayList<ActionMessage>(this.solrUpdateConductor.getPidQueue())), QUEUED_PATH);
	}
	
	@RequestMapping(value = { BLOCKED_PATH + "/job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getBlockedJobInfo(@PathVariable("id") String id){
		return getJobFullInfo(lookupJobInfo(id, new ArrayList<ActionMessage>(this.solrUpdateConductor.getPidQueue())), BLOCKED_PATH);
	}
	
	@RequestMapping(value = { ACTIVE_PATH + "/job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getActiveJobInfo(@PathVariable("id") String id){
		return getJobFullInfo(lookupJobInfo(id, new ArrayList<ActionMessage>(this.solrUpdateConductor.getPidQueue())), ACTIVE_PATH);
	}
	
	/**
	 * Transforms a PIDMessage object into a map of properties for reporting purposes.
	 * @return
	 */
	@Override
	protected Map<String,Object> getJobBriefInfo(ActionMessage actionMessage, String queuePath){
		SolrUpdateRequest message = (SolrUpdateRequest)actionMessage;
		Map<String, Object> job = new HashMap<String, Object>();
		
		job.put("id", message.getMessageID());
		job.put("targetPID", message.getTargetID());
		job.put("action", message.getQualifiedAction());
		if (message.getLinkedRequest() != null){
			job.put("linkedID", message.getLinkedRequest().getMessageID());
		} else {
			job.put("linkedID", null);
		}
		
		Map<String, Object> uris = new HashMap<String, Object>();
		job.put("uris", uris);
		uris.put("jobInfo", contextUrl + BASE_PATH + queuePath + "/job/" + message.getMessageID());

		return job;
	}
	
	protected Map<String,Object> getJobFullInfo(ActionMessage actionMessage, String queuePath){
		SolrUpdateRequest message = (SolrUpdateRequest)actionMessage;
		Map<String, Object> job = new HashMap<String, Object>();
		
		job.put("id", message.getMessageID());
		job.put("targetPID", message.getTargetID());
		job.put("action", message.getQualifiedAction());
		Date timeCreated = new Date(message.getTimeCreated());
		job.put("queuedTimestamp", formatISO8601.format(timeCreated));
		job.put("class", message.getClass().getName());
		job.put("waitingOnLinked", message.isBlocked());
		
		Map<String, Object> uris = new HashMap<String, Object>();
		if (message.getLinkedRequest() != null){
			job.put("linkedID", message.getLinkedRequest().getMessageID());
			uris.put("linkedJob", contextUrl + BASE_PATH + queuePath + "/job/" + message.getMessageID());
		} else {
			job.put("linkedID", null);
		}
		uris.put("targetInfo", contextUrl + ItemInfoRestController.BASE_PATH + message.getTargetID());
		job.put("uris", uris);

		return job;
	}
}

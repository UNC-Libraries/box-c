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

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.cdr.services.processing.SolrUpdateConductor;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.UpdateNodeRequest;
import edu.unc.lib.dl.message.ActionMessage;

@Controller
@RequestMapping(value={"/status/indexing*", "/status/indexing"})
public class SolrUpdateConductorRestController extends AbstractServiceConductorRestController {
	private static final Logger log = LoggerFactory.getLogger(SolrUpdateConductorRestController.class);
	public static final String BASE_PATH = "/api/status/indexing/";
	public static final String QUEUED_PATH = "queued";
	public static final String BLOCKED_PATH = "blocked";
	public static final String ACTIVE_PATH = "active";
	public static final String FAILED_PATH = "failed";
	public static final String FINISHED_PATH = "finished";
	public static final String CHILDREN_JOBS_PATH = "jobs";
	
	@Resource
	private SolrUpdateConductor solrUpdateConductor;
	
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getInfo() {
		Map<String, Object> result = new HashMap<String, Object>();
		addServiceConductorInfo(result, this.solrUpdateConductor);
		result.put("pendingJobs", this.solrUpdateConductor.getQueueSize());
		result.put("queuedJobs", this.solrUpdateConductor.getPidQueue().size());
		result.put("blockedJobs", this.solrUpdateConductor.getCollisionList().size());
		result.put("failedJobs", this.solrUpdateConductor.getFailedMessages().size());
		result.put("finishedJobs", this.solrUpdateConductor.getFinishedMessages().size());
		result.put("activeJobs", this.solrUpdateConductor.getActiveMessages().size());
		
		Map<String, Object> uris = new HashMap<String, Object>();
		result.put("uris", uris);
		
		uris.put("queuedJobs", BASE_PATH + QUEUED_PATH);
		uris.put("blockedJobs", BASE_PATH + BLOCKED_PATH);
		uris.put("activeJobs", BASE_PATH + ACTIVE_PATH);
		uris.put("failedJobs", BASE_PATH + FAILED_PATH);
		uris.put("finishedJobs", BASE_PATH + FINISHED_PATH);
		uris.put("childrenJobs", BASE_PATH + CHILDREN_JOBS_PATH);
		
		return result;
	}
	
	@RequestMapping(value = {CHILDREN_JOBS_PATH + "/{id}"}, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getChildrenJobsInfo(
			@PathVariable("id") String id,
			@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		
		log.debug("Getting children jobs for " + id);
		for (UpdateNodeRequest message: solrUpdateConductor.getActiveMessages())
			if (message.getMessageID().equals(id))
				return getJobFullInfo(message, ACTIVE_PATH);
		for (UpdateNodeRequest message: solrUpdateConductor.getCollisionList())
			if (message.getMessageID().equals(id))
				return getJobFullInfo(message, BLOCKED_PATH);
		for (UpdateNodeRequest message: solrUpdateConductor.getPidQueue())
			if (message.getMessageID().equals(id))
				return getJobFullInfo(message, QUEUED_PATH);
		for (UpdateNodeRequest message: solrUpdateConductor.getFailedMessages())
			if (message.getMessageID().equals(id))
				return getJobFullInfo(message, FAILED_PATH);
		for (UpdateNodeRequest message: solrUpdateConductor.getFinishedMessages())
			if (message.getMessageID().equals(id))
				return getJobFullInfo(message, FINISHED_PATH);
		
		return null;
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
		List<ActionMessage> messageList = new ArrayList<ActionMessage>(this.solrUpdateConductor.getActiveMessages());
		addMessageListInfo(result, messageList, begin, end, ACTIVE_PATH);
		
		return result;
	}
	
	@RequestMapping(value = FAILED_PATH, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getFailedInfo( 
			@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		Map<String, Object> result = new HashMap<String, Object>();
		
		//Duplicate pid queue so that we can iterate over it.
		List<ActionMessage> messageList = new ArrayList<ActionMessage>(this.solrUpdateConductor.getFailedMessages());
		addMessageListInfo(result, messageList, begin, end, FAILED_PATH);
		
		return result;
	}
	
	@RequestMapping(value = FINISHED_PATH, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getFinishedInfo( 
			@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		Map<String, Object> result = new HashMap<String, Object>();
		
		//Duplicate pid queue so that we can iterate over it.
		List<ActionMessage> messageList = new ArrayList<ActionMessage>(this.solrUpdateConductor.getFinishedMessages());
		addMessageListInfo(result, messageList, begin, end, FINISHED_PATH);
		
		return result;
	}
	
	@RequestMapping(value = { QUEUED_PATH + "/job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getQueuedJobInfo(@PathVariable("id") String id){
		return getJobFullInfo(lookupJobInfo(id, new ArrayList<ActionMessage>(this.solrUpdateConductor.getPidQueue())), QUEUED_PATH);
	}
	
	@RequestMapping(value = { BLOCKED_PATH + "/job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getBlockedJobInfo(@PathVariable("id") String id){
		return getJobFullInfo(lookupJobInfo(id, new ArrayList<ActionMessage>(this.solrUpdateConductor.getCollisionList())), BLOCKED_PATH);
	}
	
	@RequestMapping(value = { ACTIVE_PATH + "/job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getActiveJobInfo(@PathVariable("id") String id){
		return getJobFullInfo(lookupJobInfo(id, new ArrayList<ActionMessage>(this.solrUpdateConductor.getActiveMessages())), ACTIVE_PATH);
	}
	
	@RequestMapping(value = { FAILED_PATH + "/job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getFailedJobInfo(@PathVariable("id") String id){
		return getJobFullInfo(lookupJobInfo(id, new ArrayList<ActionMessage>(this.solrUpdateConductor.getFailedMessages())), FAILED_PATH);
	}
	
	@RequestMapping(value = { FINISHED_PATH + "/job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getFinishedJobInfo(@PathVariable("id") String id){
		return getJobFullInfo(lookupJobInfo(id, new ArrayList<ActionMessage>(this.solrUpdateConductor.getFinishedMessages())), FINISHED_PATH);
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
		job.put("targetLabel", message.getTargetLabel());
		job.put("status", message.getStatus());
		
		if (message.getUpdateAction() != null) {
			Map<String, Object> actionInfo = new HashMap<String, Object>();
			actionInfo.put("uri", message.getUpdateAction().getURI());
			actionInfo.put("name", message.getUpdateAction().getName());
			actionInfo.put("label", message.getUpdateAction().getLabel());
			job.put("action", actionInfo);
		}
		
		job.put("childrenProcessed", message.getChildrenProcessed());
		job.put("childrenPending", message.getChildrenPending());
		
		Map<String, Object> uris = new HashMap<String, Object>();
		job.put("uris", uris);
		uris.put("jobInfo", BASE_PATH + queuePath + "/job/" + message.getMessageID());

		return job;
	}
	
	protected Map<String,Object> getJobFullInfo(ActionMessage actionMessage, String queuePath){
		SolrUpdateRequest message = (SolrUpdateRequest)actionMessage;
		Map<String, Object> job = new HashMap<String, Object>();
		
		job.put("id", message.getMessageID());
		job.put("targetPID", message.getTargetID());
		job.put("targetLabel", message.getTargetLabel());
		job.put("status", message.getStatus());
		if (message.getUpdateAction() != null) {
			Map<String, Object> actionInfo = new HashMap<String, Object>();
			actionInfo.put("uri", message.getUpdateAction().getURI());
			actionInfo.put("name", message.getUpdateAction().getName());
			actionInfo.put("label", message.getUpdateAction().getLabel());
			actionInfo.put("description", message.getUpdateAction().getDescription());
			job.put("action", actionInfo);
		}
		Date timeCreated = new Date(message.getTimeCreated());
		job.put("queuedTimestamp", formatISO8601.format(timeCreated));
		job.put("timeStarted", formatISO8601.format(new Date(message.getTimeStarted())));
		job.put("duration", message.getActiveDuration());
		job.put("class", message.getClass().getName());
		
		job.put("childrenProcessed", message.getChildrenProcessed());
		job.put("childrenPending", message.getChildrenPending());
		
		Map<String, Object> uris = new HashMap<String, Object>();
		uris.put("targetInfo", ItemInfoRestController.BASE_PATH + message.getTargetID());
		job.put("uris", uris);

		return job;
	}
}

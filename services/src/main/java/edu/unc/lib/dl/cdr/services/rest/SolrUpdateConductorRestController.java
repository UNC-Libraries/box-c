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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
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
import edu.unc.lib.dl.data.ingest.solr.ProcessingStatus;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.UpdateNodeRequest;
import edu.unc.lib.dl.message.ActionMessage;

@Controller
@RequestMapping(value={"/indexing*", "/indexing"})
public class SolrUpdateConductorRestController extends AbstractServiceConductorRestController {
	private static final Logger log = LoggerFactory.getLogger(SolrUpdateConductorRestController.class);
	public static final String BASE_PATH = "/rest/indexing/";
	public static final String QUEUED_PATH = "queued";
	public static final String BLOCKED_PATH = "blocked";
	public static final String ACTIVE_PATH = "active";
	public static final String FAILED_PATH = "failed";
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
		uris.put("childrenJobs", BASE_PATH + CHILDREN_JOBS_PATH);
		
		return result;
	}
	
	@RequestMapping(value = {CHILDREN_JOBS_PATH}, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getChildrenJobsInfo(
			@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		log.debug("Getting children jobs for root");
		return getChildrenJobsInfo(solrUpdateConductor.getRoot(), begin, end);
	}
	
	
	@RequestMapping(value = {CHILDREN_JOBS_PATH + "/{id}"}, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getChildrenJobsInfo(
			@PathVariable("id") String id,
			@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		
		log.debug("Getting children jobs for " + id);
		UpdateNodeRequest root = solrUpdateConductor.getRoot().getChild(id);
		return getChildrenJobsInfo(root, begin, end);
	}
	
	/**
	 * The dickensian view of indexing.
	 * 
	 * @param root
	 * @param begin
	 * @param end
	 * @return
	 */
	public Map<String, ? extends Object> getChildrenJobsInfo(UpdateNodeRequest root, Integer begin, Integer end) {
		if (root == null)
			return null;
		
		Map<String, Object> result = new HashMap<String, Object>();
		if (root != solrUpdateConductor.getRoot()) {
			Map<String,Object> parentInfo = getJobBriefInfo(root, "");
			this.getChildrenJobInfo(parentInfo, root);
			result.put("parent", parentInfo);
		}
		
		List<Object> jobs = new ArrayList<Object>();
		result.put("jobs", jobs);
		
		if (root.getChildren() == null || root.getChildren().size() == 0){
			return result;
		}
		
		int[] statusCount = new int[ProcessingStatus.values().length];
		List<ActionMessage> topLevelMessages = new ArrayList<ActionMessage>(root.getChildren());
		
		// Sort messages by status, backwards
		Collections.sort(topLevelMessages, new Comparator<ActionMessage>() {
			@Override
			public int compare(ActionMessage o2, ActionMessage o1) {
				if (o1 == o2)
					return 0;
				if (o1 == null)
					return 1;
				if (o2 == null)
					return -1;
				UpdateNodeRequest r1 = (UpdateNodeRequest)o1;
				UpdateNodeRequest r2 = (UpdateNodeRequest)o2;
				int compare = r2.getStatus().compareTo(r1.getStatus());
				// Tie-break with timestamp
				if (compare == 0) {
					return (int) (o1.getTimeCreated() - o2.getTimeCreated());
				}
				return compare;
			}
		});
		
		if (begin == null)
			begin = 0;
		if (end == null)
			end = topLevelMessages.size();
		// Cut down result set to the current page
		Iterator<ActionMessage> messageIt = topLevelMessages.iterator();
		while (messageIt.hasNext()) {
			SolrUpdateRequest next = (SolrUpdateRequest)messageIt.next();
			if (next.getStatus() != null) {
				if (statusCount[next.getStatus().ordinal()] < begin || statusCount[next.getStatus().ordinal()] >= end){
					messageIt.remove();
				}
				statusCount[next.getStatus().ordinal()]++;
			}
		}
		
		populateLabels(topLevelMessages);
		
		for (ActionMessage message: topLevelMessages) {
			Map<String,Object> jobInfo = getJobBriefInfo(message, "");
			jobs.add(jobInfo);
			
			UpdateNodeRequest messageNode = (UpdateNodeRequest)message;
			this.getChildrenJobInfo(jobInfo, messageNode);
			
		}
		
		return result;
	}
	
	@RequestMapping(value = {CHILDREN_JOBS_PATH + "/job/{id}"}, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getChildrenJobsFullInfo(
			@PathVariable("id") String id) {
		UpdateNodeRequest message = solrUpdateConductor.getRoot().getChild(id);
		if (message == null)
			return null;
		
		Map<String,Object> jobInfo = getJobFullInfo(message, "");
		this.getChildrenJobInfo(jobInfo, message);
		
		Map<ProcessingStatus, Integer> counts = message.countChildrenByStatus();
		jobInfo.put("childrenCounts", counts);
		
		List<UpdateNodeRequest> ancestors = message.getAncestors();
		populateLabels(ancestors);
		List<Object> ancestorList = new ArrayList<Object>(ancestors.size());
		jobInfo.put("ancestors", ancestorList);
		
		//Iterate over ancestors in reverse to get them in the correct order
		ListIterator<UpdateNodeRequest> iterator = ancestors.listIterator(ancestors.size());
		while (iterator.hasPrevious()) {
			UpdateNodeRequest ancestor = iterator.previous();
			Map<String,Object> ancestorInfo = new HashMap<String,Object>();
			ancestorInfo.put("label", ancestor.getTargetLabel());
			ancestorInfo.put("pid", ancestor.getTargetID());
			ancestorList.add(ancestorInfo);
		}
		
		return jobInfo;
	}
	
	private void getChildrenJobInfo(Map<String, Object> jobInfo, UpdateNodeRequest message) {
		boolean active = false;
		for (UpdateNodeRequest activeMessage: solrUpdateConductor.getActiveMessages()) {
			if (activeMessage == message || activeMessage.hasAncestor(message.getMessageID())) {
				active = true;
				break;
			}
		}
		jobInfo.put("jobActive", active);
		
		
		jobInfo.put("childrenPending", message.getChildrenPending());
		jobInfo.put("childrenProcessed", message.getChildrenProcessed());
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
		
		if (message.getLinkedRequest() != null){
			job.put("linkedID", message.getLinkedRequest().getMessageID());
		} else {
			job.put("linkedID", null);
		}
		if (message.getParent() != null) {
			Map<String, Object> parentInfo = new HashMap<String, Object>();
			parentInfo.put("id", message.getParent().getMessageID());
			parentInfo.put("label", message.getParent().getTargetLabel());
			job.put("parent", parentInfo);
		}
		
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
		job.put("class", message.getClass().getName());
		job.put("waitingOnLinked", message.isBlocked());
		
		Map<String, Object> uris = new HashMap<String, Object>();
		if (message.getLinkedRequest() != null){
			job.put("linkedID", message.getLinkedRequest().getMessageID());
			uris.put("linkedJob", BASE_PATH + queuePath + "/job/" + message.getMessageID());
		} else {
			job.put("linkedID", null);
		}
		if (message.getParent() != null) {
			Map<String, Object> parentInfo = new HashMap<String, Object>();
			parentInfo.put("id", message.getParent().getMessageID());
			parentInfo.put("label", message.getParent().getTargetLabel());
			job.put("parent", parentInfo);
		}
		
		uris.put("targetInfo", ItemInfoRestController.BASE_PATH + message.getTargetID());
		job.put("uris", uris);

		return job;
	}
}

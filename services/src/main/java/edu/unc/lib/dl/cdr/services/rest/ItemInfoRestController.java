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

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.collections.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.processing.EnhancementConductor;
import edu.unc.lib.dl.cdr.services.processing.SolrUpdateConductor;
import edu.unc.lib.dl.cdr.services.processing.EnhancementConductor.PerformServicesRunnable;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRunnable;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.message.ActionMessage;
import edu.unc.lib.dl.security.access.AccessGroupConstants;
import edu.unc.lib.dl.security.access.AccessGroupSet;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.util.TripleStoreQueryService.PathInfo;

@Controller
@RequestMapping(value={"/item*", "/item"})
public class ItemInfoRestController extends AbstractServiceConductorRestController {
	private static final Logger LOG = LoggerFactory.getLogger(ItemInfoRestController.class);

	public static final String BASE_PATH = "/rest/item/";
	public static final String SERVICE_STATUS_PATH = "serviceStatus"; 
	
	@Resource
	private TripleStoreQueryService tripleStoreQueryService;
	@Resource
	private EnhancementConductor enhancementConductor;
	@Resource
	private SolrUpdateConductor solrUpdateConductor;

	@RequestMapping(value = {"", "/"}, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getItemRoot(@PathVariable("id") String id) {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("hint", "correct path is " + contextUrl + BASE_PATH + "<pid>");
		return result;
	}
	
	@RequestMapping(value = "{id}", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getItemInfo(@PathVariable("id") String id) {
		PID pid = new PID(id);
		
		List<URI> contentModels = tripleStoreQueryService.lookupContentModels(pid);
		//If the item doesn't return any content models, it probably doesn't exist
		if (contentModels == null || contentModels.size() == 0)
			return null;
		
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("pid", id);
		result.put("contentModels", contentModels);
		
		List<String> pidPath = new ArrayList<String>();
		List<PathInfo> path = tripleStoreQueryService.lookupRepositoryPathInfo(pid);
		if (path.size() > 1) {
			//Path size needs to be greater than one, because path always begins with REPOSITORY
			for (PathInfo i : path) {
				pidPath.add(i.getPid().getPid());
			}
			PathInfo mainInfo = path.get(path.size()-1);
			result.put("label", mainInfo.getLabel());
			result.put("slug", mainInfo.getSlug());
			result.put("path", mainInfo.getPath());
			result.put("orphaned", false);
		} else {
			result.put("path", "");
			result.put("orphaned", true);
		}
		result.put("ancestors", pidPath);
		
		Map<String, Object> uris = new HashMap<String, Object>();
		result.put("uris", uris);
		
		uris.put("serviceStatus", contextUrl + BASE_PATH + id + "/" + SERVICE_STATUS_PATH + "/enhancement");
		uris.put("serviceStatus", contextUrl + BASE_PATH + id + "/" + SERVICE_STATUS_PATH + "/indexing");
		// Determine if this item is a container, add link to its children
		for (URI contentModel: contentModels){
			if (ContentModelHelper.Model.CONTAINER.getURI().equals(contentModel)){
				uris.put("serviceStatus", contextUrl + BASE_PATH + id + "/children");
				break;
			}
		}
		
		return result;
	}
	
	@RequestMapping(value = "{id}/children", method = RequestMethod.GET)
	public @ResponseBody
	List<? extends Object> getContainerChildren(@PathVariable("id") String id) {
		PID pid = new PID(id);
		List<Object> result = new ArrayList<Object>();
		
		List<PathInfo> children = tripleStoreQueryService.fetchChildPathInfo(pid);
		for (PathInfo child: children){
			Map<String, Object> childInfo = new HashMap<String, Object>();
			childInfo.put("pid", child.getPid().getPid());
			childInfo.put("label", child.getLabel());
			childInfo.put("slug", child.getSlug());
			result.add(childInfo);
		}
		
		return result;
	}
	
	@RequestMapping(value = "{id}/" + SERVICE_STATUS_PATH, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getItemStatus(@PathVariable("id") String id) {
		Map<String, Object> result = new HashMap<String, Object>();

		Map<String, Object> uris = new HashMap<String, Object>();
		result.put("uris", uris);
		
		uris.put("serviceStatus", contextUrl + BASE_PATH + id + "/" + SERVICE_STATUS_PATH + "/enhancement");
		uris.put("serviceStatus", contextUrl + BASE_PATH + id + "/" + SERVICE_STATUS_PATH + "/indexing");
		
		return result;
	}
	
	@RequestMapping(value = "/item/{id}/" + SERVICE_STATUS_PATH + "/enhancement", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getItemEnhancementStatus(@PathVariable("id") String id) {
		return getEnhancementProperties(id);
	}
	
	@RequestMapping(value = "/item/{id}/serviceStatus/indexing", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getItemIndexingStatus(@PathVariable("id") String id) {
		return getIndexingProperties(id);
	}
	
	private Map<String, Object> getEnhancementProperties(String id){
		Map<String, Object> result = new HashMap<String, Object>();
		
		List<Object> pendingTasks = new ArrayList<Object>();
		result.put("enhancementTasks", pendingTasks);

		Map<String, Object> task = null;

		List<ActionMessage> matchingMessages = new ArrayList<ActionMessage>();

		// Add in matching active messages
		Set<PerformServicesRunnable> currentlyRunning = this.enhancementConductor.getExecutor().getRunningNow();
		for (PerformServicesRunnable runningTask : currentlyRunning) {
			if (runningTask.getMessage() != null && runningTask.getMessage().getTargetID().equals(id)) {
				task = new HashMap<String, Object>();
				task.put("jobInfo", contextUrl + EnhancementConductorRestController.BASE_PATH
						+ EnhancementConductorRestController.ACTIVE_PATH + "/job/"
						+ runningTask.getMessage().getMessageID());
				task.put("action", runningTask.getMessage().getQualifiedAction());
				pendingTasks.add(task);
				// Store the matching active messages for later
				matchingMessages.add(runningTask.getMessage());
			}
		}
		
		List<ActionMessage> enhancementList = null;
		
		//Add matching blocked messages
		enhancementList = new ArrayList<ActionMessage>(enhancementConductor.getCollisionList());
		addMessageList(id, enhancementList, contextUrl + EnhancementConductorRestController.BASE_PATH
				+ EnhancementConductorRestController.BLOCKED_PATH, matchingMessages, pendingTasks);
		
		//Add matching queued messages;
		enhancementList = new ArrayList<ActionMessage>(enhancementConductor.getPidQueue());
		addMessageList(id, enhancementList, contextUrl + EnhancementConductorRestController.BASE_PATH
				+ EnhancementConductorRestController.QUEUED_PATH, matchingMessages, pendingTasks);
		
		//Build a set of all the services that have failed for this item
		Set<String> queuedServices = new HashSet<String>();
		for (ActionMessage message : matchingMessages) {
			PIDMessage pidMessage = (PIDMessage)message;
			for (ObjectEnhancementService service : pidMessage.getFilteredServices()) {
				queuedServices.add(service.getClass().getName());
			}
		}
		result.put("queuedEnhancements", queuedServices);
		
		Set<String> failed = enhancementConductor.getFailedPids().get(id);
		if (failed == null){
			result.put("failedEnhancements", ListUtils.EMPTY_LIST);
		} else {
			result.put("failedEnhancements", failed);
		}
		
		List<String> applicableServices = new ArrayList<String>();
		PIDMessage dummyMessage = new PIDMessage(id, null, null);
		for (ObjectEnhancementService service: enhancementConductor.getServices()){
			try {
				if (service.isApplicable(dummyMessage)){
					applicableServices.add(service.getClass().getName());
				}
			} catch (EnhancementException e) {
				LOG.error("Error while checking isApplicable for " + service.getClass().getName() + " on " + id, e);
			}
		}
		result.put("applicableServices", applicableServices);
		
		return result;
	}
	
	private Map<String, Object> getIndexingProperties(String id){
		Map<String, Object> result = new HashMap<String, Object>();
		
		List<ActionMessage> enhancementList = null;
		
		//Add Solr properties
		List<Object> pendingTasks = new ArrayList<Object>();
		result.put("indexingQueue", pendingTasks);
		
		Map<String, Object> task = null;
		// Add in matching active messages
		Set<SolrUpdateRunnable> currentlyRunning = this.solrUpdateConductor.getThreadPoolExecutor().getRunningNow();
		for (SolrUpdateRunnable runningTask : currentlyRunning) {
			if (runningTask.getUpdateRequest() != null && runningTask.getUpdateRequest().getTargetID().equals(id)) {
				task = new HashMap<String, Object>();
				task.put("jobInfo", contextUrl + SolrUpdateConductorRestController.BASE_PATH
						+ SolrUpdateConductorRestController.ACTIVE_PATH + "/job/"
						+ runningTask.getUpdateRequest().getMessageID());
				task.put("action", runningTask.getUpdateRequest().getQualifiedAction());
				pendingTasks.add(task);
			}
		}
		
		// Get solr messages
		enhancementList = new ArrayList<ActionMessage>(solrUpdateConductor.getCollisionList());
		addMessageList(id, enhancementList, contextUrl + SolrUpdateConductorRestController.BASE_PATH
				+ SolrUpdateConductorRestController.BLOCKED_PATH, null, pendingTasks);
		
		enhancementList = new ArrayList<ActionMessage>(solrUpdateConductor.getPidQueue());
		addMessageList(id, enhancementList, contextUrl + SolrUpdateConductorRestController.BASE_PATH
				+ SolrUpdateConductorRestController.QUEUED_PATH, null, pendingTasks);

		AccessGroupSet accessGroups = new AccessGroupSet();
		accessGroups.add(AccessGroupConstants.ADMIN_GROUP);
		Date lastIndexed = solrUpdateConductor.getSolrSearchService().getTimestamp(id, accessGroups);
		if (lastIndexed == null){
			result.put("indexed", false);
			result.put("lastIndexed", "");
		} else {
			result.put("indexed", true);
			result.put("lastIndexed", formatISO8601.format(lastIndexed));
		}
		
		
		return result;
	}
	
	private void addMessageList(String id, List<ActionMessage> actionList, String basePath,
			List<ActionMessage> matchingMessages, List<Object> pendingTasks) {
		Map<String, Object> task = null;
		for (ActionMessage message : actionList) {
			if (message.getTargetID().equals(id)) {
				task = new HashMap<String, Object>();
				task.put("url", basePath + "/job/" + message.getMessageID());
				task.put("action", message.getQualifiedAction());
				pendingTasks.add(task);
				if (matchingMessages != null)
					matchingMessages.add(message);
			}
		}
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	public EnhancementConductor getEnhancementConductor() {
		return enhancementConductor;
	}

	public void setEnhancementConductor(EnhancementConductor enhancementConductor) {
		this.enhancementConductor = enhancementConductor;
	}

	public SolrUpdateConductor getSolrUpdateConductor() {
		return solrUpdateConductor;
	}

	public void setSolrUpdateConductor(SolrUpdateConductor solrUpdateConductor) {
		this.solrUpdateConductor = solrUpdateConductor;
	}
}

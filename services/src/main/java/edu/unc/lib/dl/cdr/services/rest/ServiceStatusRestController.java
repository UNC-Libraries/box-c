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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.processing.EnhancementConductor;
import edu.unc.lib.dl.cdr.services.processing.SolrUpdateConductor;
import edu.unc.lib.dl.cdr.services.processing.EnhancementConductor.PerformServicesRunnable;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.util.TripleStoreQueryService.PathInfo;

@Controller
public class ServiceStatusRestController extends AbstractServiceConductorRestController {

	@Resource
	private TripleStoreQueryService tripleStoreQueryService;
	@Resource
	private EnhancementConductor enhancementConductor;
	@Resource
	private SolrUpdateConductor solrUpdateConductor;

	@RequestMapping(value = "/item/{id}/serviceStatus", method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getItemStatus(@PathVariable("id") String id) {
		PID pid = new PID(id);

		Map<String, Object> result = new HashMap<String, Object>();

		result.put("pid", id);
		// result.put("depositID", "");

		List<String> pidPath = new ArrayList<String>();
		List<PathInfo> path = tripleStoreQueryService.lookupRepositoryPathInfo(pid);
		if (path.size() > 0) {
			for (PathInfo i : path) {
				pidPath.add(i.getPid().getPid());
			}
			result.put("ancestors", pidPath);
			result.put("path", path.get(path.size()).getPath());
			result.put("orphaned", false);
		} else {
			result.put("orphaned", true);
		}

		List<Object> pendingTasks = new ArrayList<Object>();
		result.put("enhancementQueue", pendingTasks);

		Map<String, Object> task = null;

		Set<PerformServicesRunnable> currentlyRunning = this.enhancementConductor.getExecutor().getRunningNow();
		
		//Add in matching active messages
		List<PIDMessage> matchingMessages = new ArrayList<PIDMessage>();
		for (PerformServicesRunnable runningTask : currentlyRunning) {
			if (runningTask.getPidMessage().getPIDString().equals(id)) {
				task = new HashMap<String, Object>();
				task.put("jobInfo", contextUrl + EnhancementConductorRestController.BASE_PATH
						+ EnhancementConductorRestController.ACTIVE_PATH + "/job/"
						+ runningTask.getPidMessage().getMessageID());
				task.put("action", runningTask.getPidMessage().getQualifiedAction());
				pendingTasks.add(task);
				//Store the matching active messages for later
				matchingMessages.add(runningTask.getPidMessage());
			}
		}

		//Add matching blocked messages
		for (PIDMessage enhancementMessage : enhancementConductor.getCollisionList()) {
			if (enhancementMessage.getPIDString().equals(id)) {
				task = new HashMap<String, Object>();
				task.put("jobInfo", contextUrl + EnhancementConductorRestController.BASE_PATH
						+ EnhancementConductorRestController.BLOCKED_PATH + "/job/" + enhancementMessage.getMessageID());
				task.put("action", enhancementMessage.getQualifiedAction());
				pendingTasks.add(task);
				matchingMessages.add(enhancementMessage);
			}
		}

		//Add matching queued messages
		List<PIDMessage> enhancementList = new ArrayList<PIDMessage>(enhancementConductor.getPidQueue());
		for (PIDMessage enhancementMessage : enhancementList) {
			if (enhancementMessage.getPIDString().equals(id)) {
				task = new HashMap<String, Object>();
				task.put("url", contextUrl + EnhancementConductorRestController.BASE_PATH
						+ EnhancementConductorRestController.QUEUED_PATH + "/job/" + enhancementMessage.getMessageID());
				task.put("action", enhancementMessage.getQualifiedAction());
				pendingTasks.add(task);
				matchingMessages.add(enhancementMessage);
			}
		}
		
		Set<String> queuedServices = new HashSet<String>();
		for (PIDMessage message: matchingMessages){
			for (ObjectEnhancementService service: message.getFilteredServices()){
				queuedServices.add(service.getClass().getName());
			}
		}
		result.put("queuedServices", queuedServices);
		
		//Get solr messages
		solrUpdateConductor.getCollisionList();
		solrUpdateConductor.getPidQueue();

		return result;
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

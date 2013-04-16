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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import org.jdom.output.Format;
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
import edu.unc.lib.dl.cdr.services.model.AbstractXMLEventMessage;
import edu.unc.lib.dl.cdr.services.model.CDREventMessage;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.model.FailedEnhancementMap;
import edu.unc.lib.dl.cdr.services.model.FailedEnhancementMap.FailedEnhancementEntry;
import edu.unc.lib.dl.cdr.services.model.FedoraEventMessage;
import edu.unc.lib.dl.cdr.services.processing.EnhancementConductor;
import edu.unc.lib.dl.message.ActionMessage;

/**
 * 
 * @author bbpennel
 * 
 */
@Controller
@RequestMapping(value = { "/enhancement*", "/enhancement" })
public class EnhancementConductorRestController extends AbstractServiceConductorRestController {
	private static final Logger LOG = LoggerFactory.getLogger(EnhancementConductorRestController.class);
	public static final String BASE_PATH = "/rest/enhancement/";
	public static final String QUEUED_PATH = "queued";
	public static final String BLOCKED_PATH = "blocked";
	public static final String ACTIVE_PATH = "active";
	public static final String FAILED_PATH = "failed";
	public static final String FINISHED_PATH = "finished";

	@Resource
	private EnhancementConductor enhancementConductor;

	private Map<String, String> serviceNameLookup;

	@PostConstruct
	public void init() {
		serviceNameLookup = new HashMap<String, String>();
		for (ObjectEnhancementService service : enhancementConductor.getServices()) {
			serviceNameLookup.put(service.getClass().getName(), service.getName());
		}
	}

	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getInfo() {
		Map<String, Object> result = new HashMap<String, Object>();
		addServiceConductorInfo(result, this.enhancementConductor);
		result.put("pendingJobs", this.enhancementConductor.getQueueSize());
		result.put("queuedJobs", this.enhancementConductor.getPidQueue().size());
		result.put("blockedJobs", this.enhancementConductor.getCollisionList().size());
		result.put("failedJobs", this.enhancementConductor.getFailedPids().size());
		result.put("activeJobs", this.enhancementConductor.getActiveMessages().size());
		result.put("finishedJobs", this.enhancementConductor.getFinishedMessages().size());

		Map<String, Object> uris = new HashMap<String, Object>();
		result.put("uris", uris);

		uris.put("queuedJobs", BASE_PATH + QUEUED_PATH);
		uris.put("blockedJobs", BASE_PATH + BLOCKED_PATH);
		uris.put("activeJobs", BASE_PATH + ACTIVE_PATH);
		uris.put("failedJobs", BASE_PATH + FAILED_PATH);
		uris.put("finishedJobs", BASE_PATH + FINISHED_PATH);

		return result;
	}

	@RequestMapping(value = QUEUED_PATH, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getQueuedInfo(@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {

		Map<String, Object> result = new HashMap<String, Object>();

		// Duplicate pid queue so that we can iterate over it.
		List<ActionMessage> queued = new ArrayList<ActionMessage>(this.enhancementConductor.getPidQueue());
		addMessageListInfo(result, queued, begin, end, QUEUED_PATH);

		return result;
	}

	@RequestMapping(value = BLOCKED_PATH, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getBlockedInfo(@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {

		Map<String, Object> result = new HashMap<String, Object>();
		List<ActionMessage> messageList = new ArrayList<ActionMessage>(this.enhancementConductor.getCollisionList());
		addMessageListInfo(result, messageList, begin, end, BLOCKED_PATH);
		return result;
	}

	@RequestMapping(value = ACTIVE_PATH, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getActiveInfo(@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {

		Map<String, Object> result = new HashMap<String, Object>();
		List<ActionMessage> messageList = new ArrayList<ActionMessage>(this.enhancementConductor.getActiveMessages());
		addMessageListInfo(result, messageList, begin, end, ACTIVE_PATH);
		return result;
	}

	@RequestMapping(value = FINISHED_PATH, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getFinishedInfo(@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {

		Map<String, Object> result = new HashMap<String, Object>();
		List<ActionMessage> messageList = new ArrayList<ActionMessage>(this.enhancementConductor.getFinishedMessages());
		addMessageListInfo(result, messageList, begin, end, FINISHED_PATH);
		return result;
	}

	/**
	 * Returns a view of the failed job list.
	 * 
	 * @return
	 */
	@RequestMapping(value = FAILED_PATH, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getFailedInfo(@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		if (begin == null)
			begin = 0;
		
		Map<String, Object> result = new HashMap<String, Object>();

		List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
		result.put("jobs", jobs);

		FailedEnhancementMap failedList = this.enhancementConductor.getFailedPids();
		result.put("count", failedList.size());
		
		Iterator<String> pidIt = failedList.getPidToService().keySet().iterator();
		int cnt = 0;
		//LOG.debug("Failed PIDS: " + failedList.getPidToService().keySet());
		LOG.debug("Returning results " + begin + " to " + end);
		while (pidIt.hasNext()) {
			String pid = pidIt.next();
			if (end != null && cnt > end)
				break;
			if (cnt++ < begin)
				continue;
			//LOG.debug("Picking up " + pid);
			
			Map<String, Object> failedEntry = new HashMap<String, Object>();
			jobs.add(failedEntry);
			failedEntry.put("targetPID", pid);
			
			Map<String, Object> failedServices = new HashMap<String, Object>();
			failedEntry.put("failedServices", failedServices);
			
			// Get the list of failed enhancements for this pid
			List<FailedEnhancementEntry> entryList = failedList.get(pid);
			// Store a representative label from the first message 
			if (entryList.size() > 0) {
				this.populateLabel(entryList.get(0));
				failedEntry.put("targetLabel", entryList.get(0).getTargetLabel());
			} else {
				continue;
			}
			
			Map<String, Object> uris = new HashMap<String, Object>();
			failedEntry.put("uris", uris);
			uris.put("jobInfo", BASE_PATH + FAILED_PATH + "/job/" + pid);
			
			for (FailedEnhancementEntry entry: entryList) {
				failedServices.put(entry.serviceName, this.serviceNameLookup.get(entry.serviceName));
			}
			
		}

		return result;
	}

	@RequestMapping(value = { FAILED_PATH + "/job/{pid}" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getFailedMessageInfo(@PathVariable("pid") String pid) {
		if (pid == null || pid.length() == 0)
			return null;
		
		FailedEnhancementMap failedList = this.enhancementConductor.getFailedPids();
		List<FailedEnhancementEntry> entryList = failedList.get(pid);
		if (entryList.size() == 0)
			return null;
		
		Map<String, Object> failedEntry = new HashMap<String, Object>();
		failedEntry.put("type", "failed");
		this.populateLabel(entryList.get(0));
		failedEntry.put("targetLabel", entryList.get(0).getTargetLabel());
		failedEntry.put("targetPID", pid);
		
		Map<String, Object> failedServices = new HashMap<String, Object>();
		failedEntry.put("failedServices", failedServices);
		for (FailedEnhancementEntry entry: entryList) {
			Map<String, Object> failedService = getJobFullInfo(entry.message, FAILED_PATH);
			failedServices.put(entry.serviceName, failedService);
			failedService.put("stackTrace", entry.stackTrace);
			failedService.put("timeFailed", entry.timeFailed);
		}
		
		return failedEntry;
	}

	/**
	 * Returns the full details for a job selected by id, which is the hash code of the message object until another
	 * identifier is added.
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping(value = { QUEUED_PATH + "/job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getQueuedJobInfo(@PathVariable("id") String id) {
		return getJobFullInfo(lookupJobInfo(id, new ArrayList<ActionMessage>(this.enhancementConductor.getPidQueue())),
				QUEUED_PATH);
	}

	@RequestMapping(value = { BLOCKED_PATH + "/job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getBlockedJobInfo(@PathVariable("id") String id) {
		return getJobFullInfo(
				lookupJobInfo(id, new ArrayList<ActionMessage>(this.enhancementConductor.getCollisionList())), BLOCKED_PATH);
	}

	@RequestMapping(value = { ACTIVE_PATH + "/job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getActiveJobInfo(@PathVariable("id") String id) {
		return getJobFullInfo(
				lookupJobInfo(id, new ArrayList<ActionMessage>(this.enhancementConductor.getActiveMessages())), ACTIVE_PATH);
	}

	@RequestMapping(value = { FINISHED_PATH + "/job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getFinishedJobInfo(@PathVariable("id") String id) {
		return getJobFullInfo(
				lookupJobInfo(id, new ArrayList<ActionMessage>(this.enhancementConductor.getFinishedMessages())),
				FINISHED_PATH);
	}

	/**
	 * Seeks out the job info for the given message id in each of the enhancement lists. If a messageType is provided,
	 * then it will start seeking in the specified list and work its way down the stack if not found there.
	 * 
	 * @param id
	 * @param messageType
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value = { "job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getJobInfo(@PathVariable("id") String id, @RequestParam("type") String messageType) {
		String[] typeStack = { "queued", "blocked", "active", "finished", "failed" };

		LOG.debug("Retrieving message for enhancement " + id + " starting with type " + messageType);

		boolean reachedMessageType = messageType == null;

		for (String type : typeStack) {
			if (!reachedMessageType && type.equals(messageType))
				reachedMessageType = true;
			if (reachedMessageType) {
				Map<String, ?> result = null;
				if ("queued".equals(type)) {
					result = getQueuedJobInfo(id);
				} else if ("blocked".equals(type)) {
					result = getBlockedJobInfo(id);
				} else if ("active".equals(type)) {
					result = getActiveJobInfo(id);
				} else if ("finished".equals(type)) {
					result = getFinishedJobInfo(id);
				} else if ("failed".equals(type)) {
					result = getFailedMessageInfo(id);
				}

				if (result != null) {
					((Map<String, Object>) result).put("type", type);
					return result;
				}
			}
		}
		return null;
	}

	/**
	 * Transforms a PIDMessage object into a map of properties for reporting purposes.
	 * 
	 * @return
	 */
	@Override
	protected Map<String, Object> getJobBriefInfo(ActionMessage actionMessage, String queuePath) {
		if (actionMessage == null)
			return null;
		EnhancementMessage message = (EnhancementMessage) actionMessage;
		Map<String, Object> job = new HashMap<String, Object>();

		job.put("id", message.getMessageID());
		job.put("targetPID", message.getTargetID());
		job.put("targetLabel", message.getTargetLabel());
		addJobPropertyIfNotEmpty("depositID", message.getDepositID(), job);
		addJobPropertyIfNotEmpty("action", message.getQualifiedAction(), job);
		addJobPropertyIfNotEmpty("serviceName", message.getServiceName(), job);
		addJobPropertyIfNotEmpty("activeService", message.getActiveService(), job);

		if (message instanceof FedoraEventMessage) {
			FedoraEventMessage fMessage = (FedoraEventMessage) message;
			addJobPropertyIfNotEmpty("dataStream", fMessage.getDatastream(), job);
			addJobPropertyIfNotEmpty("relation", fMessage.getRelationPredicate(), job);
			addJobPropertyIfNotEmpty("generatedTimestamp", fMessage.getEventTimestamp(), job);
		} else if (message instanceof CDREventMessage) {
			CDREventMessage cdrMessage = (CDREventMessage) message;
			addJobPropertyIfNotEmpty("mode", cdrMessage.getMode(), job);
			addJobPropertyIfNotEmpty("targetParent", cdrMessage.getParent(), job);
		}

		job.put("queuedTimestamp", formatISO8601.format(message.getTimeCreated()));
		if (message.getTimeFinished() > 0) {
			job.put("finishedTimestamp", formatISO8601.format(message.getTimeFinished()));
		}

		if (message.getFilteredServices() != null) {
			Map<String, String> filteredServices = new HashMap<String, String>();
			for (String filteredService : message.getFilteredServices()) {
				filteredServices.put(filteredService, this.serviceNameLookup.get(filteredService));
			}
			job.put("filteredServices", filteredServices);
		}

		Map<String, Object> uris = new HashMap<String, Object>();
		job.put("uris", uris);
		uris.put("jobInfo", BASE_PATH + queuePath + "/job/" + message.getMessageID());

		return job;
	}

	/**
	 * Transforms a PIDMessage object into a map of properties for reporting purposes.
	 * 
	 * @return
	 */
	private Map<String, Object> getJobFullInfo(ActionMessage actionMessage, String queuedPath) {
		if (actionMessage == null)
			return null;
		EnhancementMessage message = (EnhancementMessage) actionMessage;
		Map<String, Object> job = new HashMap<String, Object>();

		job.put("id", message.getMessageID());
		job.put("targetPID", message.getTargetID());
		job.put("targetLabel", message.getTargetLabel());
		addJobPropertyIfNotEmpty("depositID", message.getDepositID(), job);
		addJobPropertyIfNotEmpty("action", message.getQualifiedAction(), job);
		addJobPropertyIfNotEmpty("serviceName", message.getServiceName(), job);
		addJobPropertyIfNotEmpty("activeService", message.getActiveService(), job);

		if (message instanceof FedoraEventMessage) {
			FedoraEventMessage fMessage = (FedoraEventMessage) message;
			addJobPropertyIfNotEmpty("dataStream", fMessage.getDatastream(), job);
			addJobPropertyIfNotEmpty("relationPredicate", fMessage.getRelationPredicate(), job);
			addJobPropertyIfNotEmpty("relationObject", fMessage.getRelationObject(), job);
			addJobPropertyIfNotEmpty("generatedTimestamp", fMessage.getEventTimestamp(), job);
		} else if (message instanceof CDREventMessage) {
			CDREventMessage cdrMessage = (CDREventMessage) message;
			addJobPropertyIfNotEmpty("mode", cdrMessage.getMode(), job);
			addJobPropertyIfNotEmpty("targetParent", cdrMessage.getParent(), job);
			addJobPropertyIfNotEmpty("oldParents", cdrMessage.getOldParents(), job);
			addJobPropertyIfNotEmpty("reordered", cdrMessage.getReordered(), job);
			addJobPropertyIfNotEmpty("subjects", cdrMessage.getSubjects(), job);
			addJobPropertyIfNotEmpty("generatedTimestamp", cdrMessage.getEventTimestamp(), job);
		}
		job.put("queuedTimestamp", formatISO8601.format(message.getTimeCreated()));
		if (message.getTimeFinished() > 0) {
			job.put("finishedTimestamp", formatISO8601.format(message.getTimeFinished()));
		}

		if (message.getFilteredServices() != null) {
			Map<String, String> filteredServices = new HashMap<String, String>();
			for (String filteredService : message.getFilteredServices()) {
				filteredServices.put(filteredService, this.serviceNameLookup.get(filteredService));
			}
			job.put("filteredServices", filteredServices);
		}

		Map<String, Object> uris = new HashMap<String, Object>();
		job.put("uris", uris);
		if (message instanceof AbstractXMLEventMessage && ((AbstractXMLEventMessage) message).getMessageBody() != null) {
			uris.put("xml", BASE_PATH + queuedPath + "/job/" + message.getMessageID() + "/xml");
		}
		uris.put("targetInfo", ItemInfoRestController.BASE_PATH + message.getTargetID());

		return job;
	}

	@RequestMapping(value = { QUEUED_PATH + "/job/{id}/xml" }, method = RequestMethod.GET)
	public void getQueuedJobXML(HttpServletResponse response, @PathVariable("id") String id) throws IOException {
		response.setContentType("application/xml");
		PrintWriter pr = response.getWriter();
		pr.write(getJobXML(id, new ArrayList<ActionMessage>(this.enhancementConductor.getPidQueue())));
	}

	@RequestMapping(value = { BLOCKED_PATH + "/job/{id}/xml" }, method = RequestMethod.GET)
	public void getBlockedJobXML(HttpServletResponse response, @PathVariable("id") String id) throws IOException {
		response.setContentType("application/xml");
		PrintWriter pr = response.getWriter();
		pr.write(getJobXML(id, new ArrayList<ActionMessage>(this.enhancementConductor.getCollisionList())));
	}

	@RequestMapping(value = { ACTIVE_PATH + "/job/{id}/xml" }, method = RequestMethod.GET)
	public void getActiveJobXML(HttpServletResponse response, @PathVariable("id") String id) throws IOException {
		response.setContentType("application/xml");
		PrintWriter pr = response.getWriter();
		pr.write(getJobXML(id, new ArrayList<ActionMessage>(this.enhancementConductor.getActiveMessages())));
	}

	@RequestMapping(value = { FINISHED_PATH + "/job/{id}/xml" }, method = RequestMethod.GET)
	public void getFinishedJobXML(HttpServletResponse response, @PathVariable("id") String id) throws IOException {
		response.setContentType("application/xml");
		PrintWriter pr = response.getWriter();
		pr.write(getJobXML(id, new ArrayList<ActionMessage>(this.enhancementConductor.getFinishedMessages())));
	}
	
	@RequestMapping(value = { FAILED_PATH + "/job/{id}/{service}/xml" }, method = RequestMethod.GET)
	public void getFailedJobXML(HttpServletResponse response, @PathVariable("id") String id, @PathVariable("service") String service) throws IOException {
		response.setContentType("application/xml");
		PrintWriter pr = response.getWriter();
		FailedEnhancementEntry entry = this.enhancementConductor.getFailedPids().get(id, service);
		if (entry == null || entry.message == null)
			return;
		pr.write(getJobXML(entry.message));
	}

	private String getJobXML(String id, List<ActionMessage> messages) {
		ActionMessage aMessage = lookupJobInfo(id, messages);
		
		return getJobXML(aMessage);
	}
	
	private String getJobXML(ActionMessage aMessage) {
		if (!(aMessage instanceof AbstractXMLEventMessage))
			return null;
		AbstractXMLEventMessage message = (AbstractXMLEventMessage) aMessage;
		if (message != null && message.getMessageBody() != null) {
			XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
			try {
				return outputter.outputString(message.getMessageBody());
			} catch (Exception e) {
				LOG.error("Error while generating xml output for " + aMessage.getMessageID(), e);
			}
		}
		return null;
	}

	private void addJobPropertyIfNotEmpty(String propertyName, Object propertyValue, Map<String, Object> job) {
		if (propertyValue != null || !"".equals(propertyValue)) {
			job.put(propertyName, propertyValue);
		}
	}

	public void setEnhancementConductor(EnhancementConductor enhancementConductor) {
		this.enhancementConductor = enhancementConductor;
	}
}

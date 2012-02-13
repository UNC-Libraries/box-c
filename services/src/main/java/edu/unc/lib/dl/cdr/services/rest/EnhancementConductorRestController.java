package edu.unc.lib.dl.cdr.services.rest;

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
	private final String BASE_PATH = "/rest/enhancement/";
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
		
		Map<String, Object> uris = new HashMap<String, Object>();
		result.put("uris", uris);
		
		uris.put("queuedJobs", contextUrl + BASE_PATH + "queued");
		uris.put("blockedJobs", contextUrl + BASE_PATH + "blocked");
		uris.put("activeJobs", contextUrl + BASE_PATH + "active");
		uris.put("failedJobs", contextUrl + BASE_PATH + "failed");
		
		return result;
	}
	
	@RequestMapping(value = "queued", method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getQueuedInfo( 
			@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		//Duplicate pid queue so that we can iterate over it.
		List<PIDMessage> queued = new ArrayList<PIDMessage>(this.enhancementConductor.getPidQueue());
		addPIDMessageListInfo(result, queued, begin, end);
		
		return result;
	}
	
	@RequestMapping(value = "blocked", method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getBlockedInfo( 
			@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		
		Map<String, Object> result = new HashMap<String, Object>();
		addPIDMessageListInfo(result, this.enhancementConductor.getCollisionList(), begin, end);
		return result;
	}
	
	@RequestMapping(value = "active", method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getActiveInfo( 
			@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		
		Set<PerformServicesRunnable> currentlyRunning = this.enhancementConductor.getExecutor().getRunningNow();
		List<PIDMessage> messages = new ArrayList<PIDMessage>();
		for (PerformServicesRunnable task: currentlyRunning){
			messages.add(task.getPidMessage());
		}
		Map<String, Object> result = new HashMap<String, Object>();
		addPIDMessageListInfo(result, messages, begin, end);
		return result;
	}
	
	private void addPIDMessageListInfo(Map<String, Object> result, List<PIDMessage> messages, Integer begin, Integer end){
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
				Map<String, Object> job = getJobBriefInfo(message);
				jobs.add(job);
			}
		}
	}
	
	/**
	 * Returns a view of the failed job list.
	 * Failed list does not retain order, so paging is not possible.
	 * @return
	 */
	@RequestMapping(value = "failed", method = RequestMethod.GET)
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
	@RequestMapping(value = { "queued/job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getQueuedJobInfo(@PathVariable("id") String id){
		return lookupJobInfo(id, new ArrayList<PIDMessage>(this.enhancementConductor.getPidQueue()));
	}
	
	@RequestMapping(value = { "blocked/job/{id}" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getBlockedJobInfo(@PathVariable("id") String id){
		return lookupJobInfo(id, this.enhancementConductor.getCollisionList());
	}
	
	
	
	/**
	 * Finds a PIDMessage in the provided list which matches the given message id.
	 * @param id
	 * @param messages
	 * @return
	 */
	private Map<String, ? extends Object> lookupJobInfo(String id, List<PIDMessage> messages){
		long messageHash = Long.parseLong(id);
		
		for (PIDMessage message: messages){
			if (message.getMessageID() == messageHash){
				return getJobFullInfo(message);
			}
		}
		return null;
	}
	
	/**
	 * Transforms a PIDMessage object into a map of properties for reporting purposes.
	 * @return
	 */
	private Map<String, Object> getJobBriefInfo(PIDMessage message){
		Map<String, Object> job = new HashMap<String, Object>();
		
		job.put("id", message.getMessageID());
		job.put("targetPID", message.getPIDString());
		addJobPropertyIfNotEmpty("depositID", message.getDepositID(), job);
		addJobPropertyIfNotEmpty("dataStream", message.getDatastream(), job);
		addJobPropertyIfNotEmpty("relation", message.getRelation(), job);
		addJobPropertyIfNotEmpty("action", message.getAction(), job);

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
		uris.put("jobInfo", contextUrl + BASE_PATH + "queued/job/" + message.getMessageID());

		return job;
	}
	
	/**
	 * Transforms a PIDMessage object into a map of properties for reporting purposes.
	 * @return
	 */
	private Map<String, Object> getJobFullInfo(PIDMessage message){
		Map<String, Object> job = new HashMap<String, Object>();
		
		job.put("id", message.getMessageID());
		job.put("targetPID", message.getPIDString());
		addJobPropertyIfNotEmpty("depositID", message.getDepositID(), job);
		addJobPropertyIfNotEmpty("dataStream", message.getDatastream(), job);
		addJobPropertyIfNotEmpty("relation", message.getRelation(), job);
		addJobPropertyIfNotEmpty("action", message.getAction(), job);

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

		return job;
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

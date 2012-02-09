package edu.unc.lib.dl.cdr.services.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.processing.EnhancementConductor;

/**
 * 
 * @author bbpennel
 *
 */
@Controller
@RequestMapping(value={"/enhancement*", "/enhancement"})
public class EnhancementConductorRestController extends AbstractServiceConductorRestController {
	private final String BASE_PATH = "/rest/enhancement/";
	
	@Resource
	private EnhancementConductor enhancementConductor;
	
	@RequestMapping(value = { "", "/" }, method = RequestMethod.GET)
	public @ResponseBody Map<String, ? extends Object> getInfo() {
		Map<String, Object> result = new HashMap<String, Object>();
		addServiceConductorInfo(result, this.enhancementConductor);
		result.put("queuedJobs", this.enhancementConductor.getQueueSize());
		result.put("failedJobs", this.enhancementConductor.getFailedPids().size());
		
		Map<String, Object> uris = new HashMap<String, Object>();
		result.put("uris", uris);
		
		uris.put("queuedJobs", contextUrl + BASE_PATH + "/queued");
		uris.put("activeJobs", contextUrl + BASE_PATH + "/active");
		uris.put("failedJobs", contextUrl + BASE_PATH + "/failed");
		
		return result;
	}
	
	@RequestMapping(value = { "queued" }, method = RequestMethod.GET)
	public @ResponseBody
	Map<String, ? extends Object> getQueued(@RequestParam(value = "begin", required = false) Integer begin,
			@RequestParam(value = "end", required = false) Integer end) {
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		List<Map<String, Object>> jobs = new ArrayList<Map<String, Object>>();
		result.put("jobs", jobs);
		//List<PIDMessage> queued = this.enhancementConductor.getPidQueue();
		return result;
	}

	public EnhancementConductor getenhancementConductor() {
		return enhancementConductor;
	}

	public void setenhancementConductor(EnhancementConductor enhancementConductor) {
		this.enhancementConductor = enhancementConductor;
	}
}

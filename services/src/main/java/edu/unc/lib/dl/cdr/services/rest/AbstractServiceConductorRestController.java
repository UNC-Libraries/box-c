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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletContext;

import org.springframework.web.context.ServletContextAware;

import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.processing.ServiceConductor;
import edu.unc.lib.dl.message.ActionMessage;

/**
 * @author Gregory Jansen
 *
 */
public class AbstractServiceConductorRestController implements ServletContextAware {

	protected ServletContext servletContext = null;
	protected SimpleDateFormat formatISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'kk:mm:ss.SSS'Z'");

	@Resource(name = "contextUrl")
	protected String contextUrl = null;

	/**
	 *
	 */
	public AbstractServiceConductorRestController() {
		super();
	}

	protected void addServiceConductorInfo(Map<String, Object> result, ServiceConductor c) {
		result.put("active", !c.isPaused());
		result.put("idle", c.isIdle());
		result.put("id", c.getIdentifier());
		result.put("activeJobs", c.getActiveThreadCount());
	}
	
	protected void addMessageListInfo(Map<String, Object> result, List<ActionMessage> messages, Integer begin, Integer end, String queuePath){
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
		for (ActionMessage message: messages){
			if (message != null){
				Map<String, Object> job = getJobBriefInfo(message, queuePath);
				jobs.add(job);
			}
		}
	}
	
	protected Map<String,Object> getJobBriefInfo(ActionMessage message, String queuePath){
		return null;
	}
	
	/**
	 * Finds an ActionMessage in the provided list which matches the given message id.
	 * @param id
	 * @param messages
	 * @return
	 */
	protected ActionMessage lookupJobInfo(String id, List<ActionMessage> messages){
		for (ActionMessage message: messages){
			if (message.getMessageID().equals(id)){
				return message;
			}
		}
		return null;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

}
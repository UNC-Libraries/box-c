/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.unc.lib.dl.cdr.services.processing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.message.ActionMessage;
import edu.unc.lib.dl.util.JMSMessageUtil;

/**
 * Determines which services in the Services stack apply to the current message.  If none do, then return false.
 * 
 * @author bbpennel
 *
 */
public class ServicesQueueMessageFilter implements MessageFilter {
	private static final Logger LOG = LoggerFactory.getLogger(ServicesQueueMessageFilter.class);
	
	private List<ObjectEnhancementService> services;
	
	private EnhancementConductor enhancementConductor;
	
	public void setServices(List<ObjectEnhancementService> services) {
		this.services = Collections.unmodifiableList(services);
	}
	
	public ServicesQueueMessageFilter(){
	}
	
	@Override
	public String getConductor(){
		return EnhancementConductor.identifier;
	}
	
	@Override
	public boolean filter(ActionMessage aMessage) {
		if (aMessage == null || !(aMessage instanceof EnhancementMessage))
			return false;
		EnhancementMessage message = (EnhancementMessage)aMessage;
		String pid = message.getTargetID();
		if (pid == null)
			return false;
		
		//Iterate through the services stack
		List<String> messageServices = new ArrayList<String>(services.size());
		message.setFilteredServices(messageServices);
		
		Set<String> failedServices = enhancementConductor.getFailedPids().getFailedServices(message.getTargetID());
		
		boolean applyServiceStack = JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.equals(message.getQualifiedAction());
		boolean serviceReached = !applyServiceStack || message.getServiceName() == null;
		
		for (ObjectEnhancementService s : services) {
			try {
				//If this is a service stack message with a starting service specified, then scan for it
				if (applyServiceStack && !serviceReached){
					if (s.getClass().getName().equals(message.getServiceName())){
						serviceReached = true;
					}
				}
				 
				if (serviceReached){
					//add services to the message's service list which have not failed previously and pass the prefilter method.
					if (!(failedServices != null && failedServices.contains(s.getClass().getName()))
							&& s.prefilterMessage(message)){
						messageServices.add(s.getClass().getName());
					} else {
						//If the starting service doesn't pass, then skip the rest of the stack 
						if (applyServiceStack && message.getServiceName() != null && messageServices.size() == 0){
							break;
						}
					}
				}
			} catch (EnhancementException e) {
				LOG.error("Error while performing prefilterMessage on " + pid + " for service " + s.getClass().getName(), e);
			}
		}
		
		if (messageServices.size() == 0){
			message.setFilteredServices(null);
			return false;
		}
		
		if (LOG.isDebugEnabled()){
			StringBuilder sb = new StringBuilder();
			sb.append("Service filter for ").append(message.getTargetID()).append(":");
			for (String s: messageServices){
				sb.append(s);
			}
			LOG.debug(sb.toString());
		}
		
		return true;
	}

	public void setenhancementConductor(EnhancementConductor enhancementConductor) {
		this.enhancementConductor = enhancementConductor;
	}
	
	
}
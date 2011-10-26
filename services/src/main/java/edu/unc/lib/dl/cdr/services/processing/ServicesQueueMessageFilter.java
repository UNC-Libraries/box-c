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
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.exception.EnhancementException;
import edu.unc.lib.dl.cdr.services.model.FailedObjectHashMap;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;

/**
 * Determines which services in the Services stack apply to the current message.  If none do, then return false.
 * 
 * @author bbpennel
 *
 */
public class ServicesQueueMessageFilter extends MessageFilter {
	private static final Logger LOG = LoggerFactory.getLogger(ServicesQueueMessageFilter.class);
	
	private List<ObjectEnhancementService> services = new ArrayList<ObjectEnhancementService>();
	
	public void setServices(List<ObjectEnhancementService> services) {
		this.services = services;
	}

	public void setServices(ArrayList<ObjectEnhancementService> services) {
		this.services = services;
	}
	
	public ServicesQueueMessageFilter(){
		MessageFilter.conductor = ServicesConductor.identifier;
	}
	
	@Override
	public boolean filter(PIDMessage message) {
		if (message == null)
			return false;
		String pid = message.getPIDString();
		if (pid == null)
			return false;
		
		//Iterate through the services stack
		List<ObjectEnhancementService> messageServices = new ArrayList<ObjectEnhancementService>(services.size());
		message.setFilteredServices(messageServices);
		for (ObjectEnhancementService s : services) {
			try {
				//Add services which match the message's service name or pass prefilter to this message's service list.
				if ((JMSMessageUtil.ServicesActions.APPLY_SERVICE.equals(message.getAction()) 
						&& s.getClass().getName().equals(message.getServiceName()))
						|| s.prefilterMessage(message)){
					messageServices.add(s);
				}
			} catch (EnhancementException e) {
				LOG.error("Error while performing prefilterMessage on " + pid + " for service " + s.getClass().getName(), e);
			}
		}
		
		if (messageServices.size() == 0){
			message.setFilteredServices(null);
			return false;
		}
		return true;
	}
}
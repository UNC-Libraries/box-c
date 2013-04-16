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

package edu.unc.lib.dl.cdr.services.jmx;

import java.util.List;

import org.jdom.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.model.CDREventMessage;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.cdr.services.model.FedoraEventMessage;
import edu.unc.lib.dl.cdr.services.processing.MessageDirector;
import edu.unc.lib.dl.fedora.ClientUtils;
import edu.unc.lib.dl.util.JMSMessageUtil;

public class MessageJMXService {
	private static final Logger LOG = LoggerFactory.getLogger(MessageJMXService.class);
	
	private MessageDirector messageDirector;
	private List<ObjectEnhancementService> services;
	
	/**
	 * Pass a pid to the director as a message.
	 * @param pid
	 */
	public void submitPidForServices(String pid){
		messageDirector.direct(new EnhancementMessage(pid, JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName()));
	}
	
	/**
	 * Pass a pid plus a service to the director as a message.
	 * @param pid
	 * @param serviceName
	 */
	public void submitPidForService(String pid, String serviceName){
		messageDirector.direct(new EnhancementMessage(pid, JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE.getName(), serviceName));
	}
	
	/**
	 * Directly submit a message as text representing an XML document.
	 * @param messageBody
	 */
	public void submitFedoraMessageBody(String messageBody){
		try {
			Document message = ClientUtils.parseXML(messageBody.getBytes());
			messageDirector.direct(new FedoraEventMessage(message));
		} catch (SAXException e) {
			LOG.error("Failed to parse submitted message body", e);
		}
	}
	
	/**
	 * Directly submit a message as text representing an XML document.
	 * @param messageBody
	 */
	public void submitCDRAdminMessageBody(String messageBody){
		try {
			Document message = ClientUtils.parseXML(messageBody.getBytes());
			messageDirector.direct(new CDREventMessage(message));
		} catch (SAXException e) {
			LOG.error("Failed to parse submitted message body", e);
		}
	}
	
	/**
	 * Returns a list of the class names for services in the stack.
	 * @return
	 */
	public String getServiceNames(){
		StringBuilder sb = new StringBuilder();
		for (ObjectEnhancementService s: this.services){
			sb.append(s.getClass().getName()).append('\n');
		}
		return sb.toString();
	}
	
	/* Getters and setters */

	public MessageDirector getMessageDirector() {
		return messageDirector;
	}

	public void setMessageDirector(MessageDirector messageDirector) {
		this.messageDirector = messageDirector;
	}

	public List<ObjectEnhancementService> getServices() {
		return services;
	}

	public void setServices(List<ObjectEnhancementService> services) {
		this.services = services;
	}
	
	
}

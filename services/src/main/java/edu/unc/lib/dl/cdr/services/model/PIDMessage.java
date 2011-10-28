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
package edu.unc.lib.dl.cdr.services.model;

import java.util.List;

import org.jdom.Document;
import org.jdom.Element;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.util.JMSMessageUtil;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * Object stores the contents of a single Fedora JMS message, with methods for retrieving
 * metadata from the underlying XML.
 * @author bbpennel
 *
 */
public class PIDMessage {
	private Document message;
	private PID pid;
	private String namespace = null;
	private String action = null;
	private String datastream = null;
	private String relation = null;
	private CDRMessageContent cdrMessageContent = null;
	private String serviceName = null;
	private String timestamp = null;
	private long timeCreated = System.currentTimeMillis();
	private List<ObjectEnhancementService> filteredServices = null;
	
	public PIDMessage(){
	}
	
	public PIDMessage(Document message, String namespace){
		this.pid = new PID(JMSMessageUtil.getPid(message));
		this.namespace = namespace;
	}
	
	public PIDMessage(String pid, String namespace, String action){
		this(pid, namespace, action, null);
	}
	
	public PIDMessage(PID pid, String namespace, String action){
		this(pid, namespace, action, null);
	}
	
	public PIDMessage(String pid, String namespace, String action, String service){
		this(new PID(pid), namespace, action, service);
	}
	
	public PIDMessage(PID pid, String namespace, String action, String service){
		this.pid = pid;
		this.namespace = namespace;
		setAction(action);
		this.serviceName = service;
	}
	
	public PIDMessage(String pid, String namespace, String action, String datastream, String relation){
		this.pid = new PID(pid);
		this.datastream = datastream;
		setAction(action);
		this.relation = relation;
	}
	
	public Document getMessage(){
		return message;
	}
	
	public PID getPID(){
		return pid;
	}
	
	public String getPIDString(){
		return pid.getPid();
	}
	
	public String getTimestamp() {
		if (timestamp == null){
			try {
				timestamp = message.getRootElement().getChildTextTrim("updated", JDOMNamespaceUtil.ATOM_NS);
			} catch (NullPointerException e){
				//Message was not set, therefore value is null 
			}
			if (timestamp == null)
				timestamp = "";
		}
		return timestamp;
	}
	
	public void setAction(String action){
		if (action != null && action.length() > 0 && this.namespace != null){
			this.action = this.namespace + "/" + action;
		} else {
			this.action = action;
		}
	}
	
	public String getAction() {
		if (action == null){
			try {
				setAction(JMSMessageUtil.getAction(message));
			} catch (NullPointerException e){
				//Message was not set, therefore value is null 
			}
			if (action == null)
				action = "";
		}
		return action;
	}

	public String getDatastream() {
		if (datastream == null){
			try {
				datastream = JMSMessageUtil.getDatastream(message);
			} catch (NullPointerException e){
				//Message was not set, therefore value is null 
			}
	    	if (datastream == null)
	    		datastream = "";
		}
    	return datastream;
	}
	
	public String getRelation(){
		if (relation == null){
			try {
				relation = JMSMessageUtil.getPredicate(message);
			} catch (NullPointerException e){
				//Message was not set, therefore value is null 
			}
	    	if (relation == null)
	    		relation = "";
		}
    	return relation;
	}
	
	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getServiceName() {
		return serviceName;
	}

	public long getTimeCreated() {
		return timeCreated;
	}

	public void setTimeCreated(long timeCreated) {
		this.timeCreated = timeCreated;
	}

	public List<ObjectEnhancementService> getFilteredServices() {
		return filteredServices;
	}

	public void setFilteredServices(List<ObjectEnhancementService> filteredServices) {
		this.filteredServices = filteredServices;
	}

	public void generateCDRMessageContent(){
		cdrMessageContent = new CDRMessageContent(message);
	}
	
	public CDRMessageContent getCDRMessageContent(){
		return cdrMessageContent;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String toString(){
		return pid.getPid();
	}
	
}

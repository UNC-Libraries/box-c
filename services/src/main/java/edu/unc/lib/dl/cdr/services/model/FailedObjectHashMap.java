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

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.message.ActionMessage;

/**
 * Stores a concurrent map containing a set of pids and the set of services that failed for the
 * given pid.
 * @author bbpennel
 *
 */
public class FailedObjectHashMap extends ConcurrentHashMap<String,FailedEnhancementObject> {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(FailedObjectHashMap.class);
	
	/**
	 * Method takes the output of failedObjectHashMap.toString() and repopulates 
	 * the map from it.  Can handle with out without newlines.
	 * @param dump
	 */
	public synchronized void repopulate(String dump){
		this.clear();
		int offset = 0;
		String[] entries = null;
		if (dump.contains("\n")){
			entries = dump.split("\\n");
			offset = 1;
		} else {
			entries = dump.split("]");
		}
		FailedEnhancementObject failedObject = null;
		
		for (String entry: entries){
			String[] components = entry.split(": ");
			String servicesString = components[1].trim();
			servicesString = servicesString.substring(1, servicesString.length() - offset);
			String[] services = servicesString.split(", ");
			failedObject = new FailedEnhancementObject(new PID(components[0].trim()));
			
			this.put(components[0].trim(), failedObject);
			for (String service: services){
				try {
					failedObject.addFailedService(Class.forName(service));
				} catch (ClassNotFoundException e) {
					LOG.warn("Unable to find class " + service + " while repopulating failed object map.");
				}
			}
		}
	}
	
	/**
	 * Adds a service to the set of failed services for the given pid.
	 * @param pid
	 * @param serviceName
	 */
	public synchronized void add(PID pid, Class<?> service, ActionMessage message){
		FailedEnhancementObject failedObject = this.get(pid.getPid());
		if (failedObject == null){
			failedObject = new FailedEnhancementObject(pid, service, message);
			this.put(pid.getPid(), failedObject);
		} else {
			failedObject.addFailedService(service);
			failedObject.addMessage(message);
		}
	}
	
	public Set<Class<?>> getFailedServices(String pid){
		FailedEnhancementObject failedObject = get(pid);
		if (failedObject == null)
			return null;
		return failedObject.getFailedServices();
	}
	
	public ActionMessage getMessageByMessageID(String messageID){
		for (FailedEnhancementObject failedObject: this.values()){
			if (failedObject.getMessages() != null){
				for (ActionMessage message: failedObject.getMessages()){
					if (message.getMessageID().equals(messageID))
						return message;
				}
			}
		}
		return null;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		for (Entry<String,FailedEnhancementObject> entry: this.entrySet()){
			sb.append(entry.getKey()).append(": ");
			sb.append(entry.getValue().getFailedServices()).append('\n');
		}
		
		return sb.toString();
	}
}

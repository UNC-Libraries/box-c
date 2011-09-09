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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores a concurrent map containing a set of pids and the set of services that failed for the
 * given pid.
 * @author bbpennel
 *
 */
public class FailedObjectHashMap extends ConcurrentHashMap<String,Set<String>> {
	private static final long serialVersionUID = 1L;

	
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
		for (String entry: entries){
			String[] components = entry.split(": ");
			String servicesString = components[1].trim();
			servicesString = servicesString.substring(1, servicesString.length() - offset);
			String[] services = servicesString.split(", ");
			Set<String> failedServices = Collections.synchronizedSet(new HashSet<String>());
			this.put(components[0].trim(), failedServices);
			for (String service: services){
				failedServices.add(service);
			}
		}
	}
	
	/**
	 * Adds a service to the set of failed services for the given pid.
	 * @param pid
	 * @param serviceName
	 */
	public synchronized void add(String pid, String serviceName){
		Set<String> failedServices = this.get(pid);
		if (failedServices == null){
			failedServices = Collections.synchronizedSet(new HashSet<String>());
			this.put(pid, failedServices);
		}
		failedServices.add(serviceName);
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		for (Entry<String,Set<String>> entry: this.entrySet()){
			sb.append(entry.getKey()).append(": ");
			sb.append(entry.getValue()).append('\n');
		}
		
		return sb.toString();
	}
}

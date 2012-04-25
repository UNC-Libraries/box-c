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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.message.ActionMessage;

public class FailedEnhancementObject implements Serializable {
	private static final long serialVersionUID = 1L;
	private Set<String> failedServices;
	private long timestamp = System.currentTimeMillis();
	private PID pid;
	private List<ActionMessage> messages;
	
	public FailedEnhancementObject(PID pid){
		this.pid = pid;
		failedServices = Collections.synchronizedSet(new HashSet<String>());
	}
	
	public FailedEnhancementObject(PID pid, String failedService, ActionMessage message){
		this.pid = pid;
		failedServices = Collections.synchronizedSet(new HashSet<String>());
		addFailedService(failedService);
		messages = new ArrayList<ActionMessage>();
		addMessage(message);
	}
	
	public void addMessage(ActionMessage message){
		if (messages == null){
			messages = new ArrayList<ActionMessage>();
		}
		for (ActionMessage storedMessage: messages){
			if (storedMessage.getMessageID().equals(message.getMessageID()))
				return;
		}
		messages.add(message);
	}
	
	public void addFailedService(String serviceName){
		failedServices.add(serviceName);
	}

	public Set<String> getFailedServices() {
		return failedServices;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public PID getPid() {
		return pid;
	}

	public List<ActionMessage> getMessages() {
		return messages;
	}
}

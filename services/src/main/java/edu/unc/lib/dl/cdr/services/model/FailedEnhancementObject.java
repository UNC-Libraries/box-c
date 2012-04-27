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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
	private List<File> failureLogFiles;

	public FailedEnhancementObject(PID pid) {
		this.pid = pid;
		failedServices = Collections.synchronizedSet(new HashSet<String>());
		messages = new ArrayList<ActionMessage>();
		failureLogFiles = new ArrayList<File>();
	}

	public FailedEnhancementObject(PID pid, String failedService, ActionMessage message, File failureLogFile) {
		this.pid = pid;
		failedServices = Collections.synchronizedSet(new HashSet<String>());
		addFailedService(failedService);
		messages = new ArrayList<ActionMessage>();
		failureLogFiles = new ArrayList<File>();
		addMessage(message, failureLogFile);
	}
	
	/**
	 * Deletes the failure files related to this failed object, if there are any.
	 */
	public void deleteFailureLogFiles(){
		if (failureLogFiles == null)
			return;
		for (File stackTraceFile: failureLogFiles){
			stackTraceFile.delete();
		}
	}

	public void addMessage(ActionMessage message, File failureLogFile) {
		for (ActionMessage storedMessage : messages) {
			if (storedMessage.getMessageID().equals(message.getMessageID()))
				return;
		}
		messages.add(message);
		failureLogFiles.add(failureLogFile);
	}

	public void addFailedService(String serviceName) {
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

	/**
	 * Retrieves the failure log text for the message matching messageID
	 * @param messageID
	 * @return
	 * @throws IOException
	 */
	public String getFailureLog(String messageID) throws IOException {
		if (messageID == null || this.messages == null)
			return null;
		for (int i = 0; i < this.messages.size(); i++) {
			ActionMessage message = this.messages.get(i);
			if (message.getMessageID().equals(messageID))
				return getFailureLog(i);
		}
		return null;
	}

	/**
	 * Retrieves the failure log text for the message at the given index.
	 * @param index
	 * @return
	 * @throws IOException
	 */
	public String getFailureLog(int index) throws IOException {
		if (this.failureLogFiles == null || index >= this.failureLogFiles.size())
			return null;
		
		File stackTraceFile = this.failureLogFiles.get(index);
		if (stackTraceFile == null)
			return null;
		
		// If the failure file doesn't exist, then remove the reference
		if (!stackTraceFile.exists()){
			this.failureLogFiles.set(index, null);
			return null;
		}
		
		BufferedReader reader = new BufferedReader(new FileReader(this.failureLogFiles.get(index)));
		try {
			StringBuilder failureLog = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				failureLog.append(line).append("\n");
			}
			return failureLog.toString();
		} finally {
			reader.close();
		}
	}

	/**
	 * Retrieves the message and failure information for the give message ID
	 * @param messageID
	 * @return
	 * @throws IOException
	 */
	public MessageFailure getMessageFailure(String messageID) throws IOException {
		if (messageID == null || this.messages == null)
			return null;
		for (int i = 0; i < this.messages.size(); i++) {
			ActionMessage message = this.messages.get(i);
			if (messageID != null && messageID.equals(message.getMessageID())) {
				return new MessageFailure(message, getFailureLog(i));
			}
		}
		return null;
	}

	public static class MessageFailure {
		private ActionMessage message;
		private String failureLog;

		public MessageFailure(ActionMessage message, String failureLog) {
			this.message = message;
			this.failureLog = failureLog;
		}

		public ActionMessage getMessage() {
			return message;
		}

		public String getFailureLog() {
			return failureLog;
		}
	}
}

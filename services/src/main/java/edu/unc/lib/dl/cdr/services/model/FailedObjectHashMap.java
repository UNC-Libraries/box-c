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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.message.ActionMessage;

/**
 * Stores a concurrent map containing a set of pids and the set of services that failed for the given pid.
 * 
 * @author bbpennel
 * 
 */
public class FailedObjectHashMap extends ConcurrentHashMap<String, FailedEnhancementObject> {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LoggerFactory.getLogger(FailedObjectHashMap.class);
	private String serializationPath;

	public FailedObjectHashMap() {

	}

	public FailedObjectHashMap(String serializationPath) {
		this.serializationPath = serializationPath;
	}

	/**
	 * Method takes the output of failedObjectHashMap.toString() and repopulates the map from it. Can handle with out
	 * without newlines.
	 * 
	 * @param dump
	 */
	public synchronized void repopulate(String dump) {
		this.clear();
		int offset = 0;
		String[] entries = null;
		if (dump.contains("\n")) {
			entries = dump.split("\\n");
			offset = 1;
		} else {
			entries = dump.split("]");
		}
		FailedEnhancementObject failedObject = null;

		for (String entry : entries) {
			String[] components = entry.split(": ");
			String servicesString = components[1].trim();
			servicesString = servicesString.substring(1, servicesString.length() - offset);
			String[] services = servicesString.split(", ");
			failedObject = new FailedEnhancementObject(new PID(components[0].trim()));

			this.put(components[0].trim(), failedObject);
			for (String service : services) {
				failedObject.addFailedService(service);
			}
		}
	}

	/**
	 * Adds a service to the set of failed services for the given pid.
	 * 
	 * @param pid
	 * @param serviceName
	 */
	public synchronized void add(PID pid, Class<?> service, ActionMessage message) {
		FailedEnhancementObject failedObject = this.get(pid.getPid());
		if (failedObject == null) {
			failedObject = new FailedEnhancementObject(pid, service.getName(), message);
			this.put(pid.getPid(), failedObject);
		} else {
			failedObject.addFailedService(service.getName());
			failedObject.addMessage(message);
		}
	}

	public Set<String> getFailedServices(String pid) {
		FailedEnhancementObject failedObject = get(pid);
		if (failedObject == null)
			return null;
		return failedObject.getFailedServices();
	}

	public ActionMessage getMessageByMessageID(String messageID) {
		for (FailedEnhancementObject failedObject : this.values()) {
			if (failedObject.getMessages() != null) {
				for (ActionMessage message : failedObject.getMessages()) {
					if (message.getMessageID().equals(messageID))
						return message;
				}
			}
		}
		return null;
	}

	/**
	 * Writes this failed object list out to file
	 * 
	 * @throws IOException
	 */
	public void serializeFailedEnhancements() throws IOException {
		serializeFailedEnhancements(this.serializationPath);
	}

	public synchronized void serializeFailedEnhancements(String filePath) throws IOException {
		log.debug("Serializing failed object hashmap to " + filePath);
		FileOutputStream fileOutputStream = new FileOutputStream(filePath);
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
		objectOutputStream.writeObject(this);
	}

	/**
	 * Factory method which creates a Failed object map from the file located at filePath. If the file is not found, then
	 * a new map is returned
	 * 
	 * @param filePath
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static FailedObjectHashMap loadFailedEnhancements(String filePath) throws IOException, ClassNotFoundException {
		try {
			FileInputStream fileInputStream = new FileInputStream(filePath);
			ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
			Object obj = objectInputStream.readObject();
			if (obj instanceof FailedObjectHashMap) {
				((FailedObjectHashMap) obj).setSerializationPath(filePath);
				return (FailedObjectHashMap) obj;
			}
		} catch (FileNotFoundException e) {
			log.info("Failed Enhancement serialization file " + filePath + " was not found, creating new map.");
		} catch (InvalidClassException e) {
			log.warn("Unable to reload failed enhancement instance from " + filePath
					+ ", the serial version ID did not match.  Creating new map.", e);
		}

		return new FailedObjectHashMap(filePath);
	}

	public String getSerializationPath() {
		return serializationPath;
	}

	public void setSerializationPath(String serializationPath) {
		this.serializationPath = serializationPath;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (Entry<String, FailedEnhancementObject> entry : this.entrySet()) {
			sb.append(entry.getKey()).append(": ");
			sb.append(entry.getValue().getFailedServices()).append('\n');
		}

		return sb.toString();
	}
}

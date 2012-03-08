package edu.unc.lib.dl.cdr.services.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.message.ActionMessage;

public class FailedEnhancementObject {
	private Set<Class<?>> failedServices;
	private long timestamp = System.currentTimeMillis();
	private PID pid;
	private List<ActionMessage> messages;
	
	public FailedEnhancementObject(PID pid){
		this.pid = pid;
		failedServices = Collections.synchronizedSet(new HashSet<Class<?>>());
	}
	
	public FailedEnhancementObject(PID pid, Class<?> failedService, ActionMessage message){
		this.pid = pid;
		failedServices = Collections.synchronizedSet(new HashSet<Class<?>>());
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
	
	public void addFailedService(Class<?> serviceName){
		failedServices.add(serviceName);
	}

	public Set<Class<?>> getFailedServices() {
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

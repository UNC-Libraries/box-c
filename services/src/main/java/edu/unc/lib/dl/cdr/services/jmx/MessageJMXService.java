package edu.unc.lib.dl.cdr.services.jmx;

import java.util.List;

import edu.unc.lib.dl.cdr.services.ObjectEnhancementService;
import edu.unc.lib.dl.cdr.services.model.PIDMessage;
import edu.unc.lib.dl.cdr.services.processing.MessageDirector;

public class MessageJMXService {
	private MessageDirector messageDirector;
	private List<ObjectEnhancementService> services;
	
	/**
	 * Pass a pid to the director as a message.
	 * @param pid
	 */
	public void submitMessage(String pid){
		messageDirector.direct(new PIDMessage(pid));
	}
	
	/**
	 * Pass a pid plus a service to the director as a message.
	 * @param pid
	 * @param serviceName
	 */
	public void submitMessage(String pid, String serviceName){
		messageDirector.direct(new PIDMessage(pid, null, serviceName));
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

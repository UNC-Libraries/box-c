package edu.unc.lib.dl.cdr.services.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;

import edu.unc.lib.dl.cdr.services.model.PIDMessage;

public class MessageDirector {
	private Map<String,MessageConductor> conductors = null;
	private List<MessageFilter> filters = null;
	
	public MessageDirector(){
	}
	
	/**
	 * Packages and forwards the message to the appropriate message conductor if
	 * it passes the associated prefilter.
	 * @param message
	 */
	public void direct(Document message){
		PIDMessage pidMessage = new PIDMessage(message);
		for (MessageFilter filter: filters){
			if (filter.filter(pidMessage)){
				conductors.get(filter.getConductor()).add(pidMessage);
			}
		}
	}

	public Map<String, MessageConductor> getConductors() {
		return conductors;
	}

	public void setConductors(Map<String, MessageConductor> conductors) {
		this.conductors = conductors;
	}
	
	public void setConductorsList(List<MessageConductor> conductorsList){
		this.conductors = new HashMap<String, MessageConductor>();
		for (MessageConductor conductor: conductorsList){
			this.conductors.put(conductor.getIdentifier(), conductor);
		}
	}

	public List<MessageFilter> getFilters() {
		return filters;
	}

	public void setFilters(List<MessageFilter> filters) {
		this.filters = filters;
	}
}

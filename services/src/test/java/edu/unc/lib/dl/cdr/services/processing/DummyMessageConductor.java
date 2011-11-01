package edu.unc.lib.dl.cdr.services.processing;

import java.util.ArrayList;

import edu.unc.lib.dl.cdr.services.model.PIDMessage;

public class DummyMessageConductor implements MessageConductor {

	String identifier = null;
	ArrayList<PIDMessage> messageList = null; 
	
	public DummyMessageConductor(){
		messageList = new ArrayList<PIDMessage>();
	}
	
	@Override
	public void add(PIDMessage message) {
		messageList.add(message);
	}

	@Override
	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier){
		this.identifier = identifier;
	}

	public ArrayList<PIDMessage> getMessageList() {
		return messageList;
	}

	public void setMessageList(ArrayList<PIDMessage> messageList) {
		this.messageList = messageList;
	}
}


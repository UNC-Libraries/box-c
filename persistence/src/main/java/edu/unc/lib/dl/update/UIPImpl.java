package edu.unc.lib.dl.update;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.services.AgentManager;
import edu.unc.lib.dl.util.PremisEventLogger;

public class UIPImpl implements UpdateInformationPackage {

	protected PID pid;
	protected PersonAgent user;
	protected UpdateOperation operation;

	protected HashMap<String, ?> incomingData;
	protected HashMap<String, ?> originalData;
	protected HashMap<String, ?> modifiedData;

	protected String message;
	
	protected PremisEventLogger eventLogger = new PremisEventLogger(AgentManager.getRepositorySoftwareAgentStub());

	public UIPImpl(PID pid, PersonAgent user, UpdateOperation operation) {
		this.pid = pid;
		this.user = user;
		this.operation = operation;
		message = null;
	}

	@Override
	public PID getPID() {
		return pid;
	}

	@Override
	public PersonAgent getUser() {
		return user;
	}

	@Override
	public UpdateOperation getOperation() {
		return operation;
	}

	@Override
	public Map<String, ?> getIncomingData() {
		return incomingData;
	}

	@Override
	public Map<String, ?> getOriginalData() {
		return originalData;
	}

	@Override
	public Map<String, ?> getModifiedData() {
		return modifiedData;
	}

	@Override
	public Map<String, File> getModifiedFiles() {
		return null;
	}
	
	@Override
	public String getMimetype(String key) {
		return null;
	}
	
	@Override
	public PremisEventLogger getEventLogger(){
		return this.eventLogger;
	}

	public void setIncomingData(HashMap<String, ?> incomingData) {
		this.incomingData = incomingData;
	}

	public void setOriginalData(HashMap<String, ?> originalData) {
		this.originalData = originalData;
	}

	public void setModifiedData(HashMap<String, ?> modifiedData) {
		this.modifiedData = modifiedData;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}

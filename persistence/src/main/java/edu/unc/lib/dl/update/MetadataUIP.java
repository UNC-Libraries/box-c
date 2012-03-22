package edu.unc.lib.dl.update;

import java.util.Map;

import org.jdom.Element;

import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.PID;

public abstract class MetadataUIP extends UIPImpl {
	
	protected MetadataUIP(PID pid, PersonAgent user, UpdateOperation operation) {
		super(pid, user, operation);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Element> getIncomingData() {
		return (Map<String, Element>) incomingData;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Element> getOriginalData() {
		return (Map<String, Element>) originalData;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Element> getModifiedData() {
		return (Map<String, Element>) modifiedData;
	}

}

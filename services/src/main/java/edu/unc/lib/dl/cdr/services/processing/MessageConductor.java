package edu.unc.lib.dl.cdr.services.processing;

import edu.unc.lib.dl.cdr.services.model.PIDMessage;

public interface MessageConductor {
	void add(PIDMessage message);
	
	String getIdentifier();
}

package edu.unc.lib.dl.update;

import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.PID;

public abstract class FedoraObjectUIP extends UIPImpl {

	public FedoraObjectUIP(PID pid, PersonAgent user, UpdateOperation operation) {
		super(pid, user, operation);
	}

	public abstract void storeOriginalDatastreams(AccessClient accessClient) throws UIPException;
	
}

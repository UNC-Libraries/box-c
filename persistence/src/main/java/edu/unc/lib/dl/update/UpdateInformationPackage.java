package edu.unc.lib.dl.update;

import java.util.Map;

import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.PID;

public interface UpdateInformationPackage {
	
	public PID getPID();
	
	public PersonAgent getUser();
	
	public UpdateOperation getOperation();
	
	public Map<String,?> getIncomingData();
	
	public Map<String,?> getOriginalData();
	
	public Map<String,?> getModifiedData();
	
}

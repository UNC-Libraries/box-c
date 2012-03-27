package edu.unc.lib.dl.update;

import java.io.File;
import java.util.Map;

import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.PremisEventLogger;

public interface UpdateInformationPackage {
	
	public PID getPID();
	
	public PersonAgent getUser();
	
	public UpdateOperation getOperation();
	
	public Map<String,?> getIncomingData();
	
	public Map<String,?> getOriginalData();
	
	public Map<String,?> getModifiedData();
	
	public Map<String,File> getModifiedFiles();
	
	public String getMessage();
	
	public String getMimetype(String key);

	public PremisEventLogger getEventLogger();	
}

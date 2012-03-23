package edu.unc.lib.dl.update;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.services.DigitalObjectManager;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;

public class UIPProcessorImpl implements UIPProcessor {
	private static Logger log = Logger.getLogger(UIPProcessorImpl.class);

	private DigitalObjectManager digitalObjectManager;
	private UIPUpdatePipeline pipeline;
	
	@Override
	public void process(UpdateInformationPackage uip) {
		try {
			uip = pipeline.processUIP(uip);
			Map<String,File> modifiedFiles = uip.getModifiedFiles();
			for (Entry<String,File> modifiedFile: modifiedFiles.entrySet()){
				Datastream datastream = Datastream.getDatastream(modifiedFile.getKey());
				if (datastream != null){
					digitalObjectManager.addOrReplaceDatastream(uip.getPID(), datastream, modifiedFile.getValue(),
							uip.getMimetype(modifiedFile.getKey()), (Agent) uip.getUser(), uip.getMessage());
				}
			}
		} catch (UpdateException e) {
			log.error("Failed to perform update operation on " + uip.getPID().getPid(), e);
		}
	}

	public DigitalObjectManager getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public void setDigitalObjectManager(DigitalObjectManager digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public UIPUpdatePipeline getPipeline() {
		return pipeline;
	}

	public void setPipeline(UIPUpdatePipeline pipeline) {
		this.pipeline = pipeline;
	}
}

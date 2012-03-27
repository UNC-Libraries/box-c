package edu.unc.lib.dl.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;

public class ContentUIP extends FedoraObjectUIP {

	public ContentUIP(PID pid, PersonAgent user, UpdateOperation operation) {
		super(pid, user, operation);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, File> getIncomingData() {
		return (Map<String, File>) incomingData;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, File> getOriginalData() {
		return (Map<String, File>) originalData;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, File> getModifiedData() {
		return (Map<String, File>) modifiedData;
	}

	@Override
	public void storeOriginalDatastreams(AccessClient accessClient) throws UIPException {
		//For efficiency, only pulling down the original if it is being modified, not replaced/deleted
		if (!(this.operation.equals(UpdateOperation.ADD) || this.operation.equals(UpdateOperation.UPDATE))) 
			return;
		
		for (String datastream: modifiedData.keySet()){
			FileOutputStream outputStream = null;
			try {
				MIMETypedStream dsStream = accessClient.getDatastreamDissemination(pid, datastream, null);
				File dsFile = File.createTempFile(datastream, ".tmp");
				outputStream = new FileOutputStream(dsFile);
				outputStream.write(dsStream.getStream());
				this.getOriginalData().put(datastream, dsFile);
			} catch (Exception e) {
				throw new UIPException("Exception occurred while attempting to store datastream " + datastream + " for "
						+ pid.getPid(), e);
			} finally {
				if (outputStream != null)
					try {
						outputStream.close();
					} catch (IOException e) {
						throw new UIPException("Failed to close temporary datastream file for " + pid.getPid(), e);
					}
			}
		}
		
	}
}

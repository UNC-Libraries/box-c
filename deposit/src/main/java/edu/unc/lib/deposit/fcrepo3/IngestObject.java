package edu.unc.lib.deposit.fcrepo3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.ManagementClient.Format;


public class IngestObject implements Runnable {
	@Autowired
	File batchIngestDirectory;
	
	String depositUUID;
	String objectUUID;
	
	@Autowired
	ManagementClient client;

	public IngestObject() {
		super();
	}

	public IngestObject(String depositUUID, String objectUUID) {
		this.depositUUID = depositUUID;
		this.objectUUID = objectUUID;
	}

	@Override
	public void run() {
		File foxml = new File(this.batchIngestDirectory, "foxml/"+objectUUID+".xml");
		FileInputStream is = null;
		try {
			is = new FileInputStream(foxml);
			client.ingestRaw(IOUtils.toByteArray(is), Format.FOXML_1_1, depositUUID);
		} catch (FedoraException e) {
			throw new Error(e);
		} catch (IOException e) {
			throw new Error(e);
		} finally {
			IOUtils.closeQuietly(is);
		}
		// wait for JMS
		// increment the deposit's ingested count
		// enqueue another PID 
	}

}

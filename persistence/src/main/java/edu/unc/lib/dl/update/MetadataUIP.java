package edu.unc.lib.dl.update;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.ClientUtils;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;

public abstract class MetadataUIP extends FedoraObjectUIP {
	private static Logger log = Logger.getLogger(MetadataUIP.class);

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
	
	@Override
	public String getMimetype(String key) {
		return "text/xml";
	}
	
	@Override
	public void storeOriginalDatastreams(AccessClient accessClient) throws UIPException {
		//For efficiency, only pulling down the original if it is being modified, not replaced/deleted
		if (!(this.operation.equals(UpdateOperation.ADD) || this.operation.equals(UpdateOperation.UPDATE))) 
			return;
		
		for (String datastream: incomingData.keySet()){
			ByteArrayInputStream inputStream = null;
			try {
				MIMETypedStream dsStream = accessClient.getDatastreamDissemination(pid, datastream, null);
				if (dsStream != null){
					inputStream = new ByteArrayInputStream(dsStream.getStream());
					SAXBuilder builder = new SAXBuilder();
					Document dsDocument = builder.build(inputStream);
					Element rootElement = dsDocument.detachRootElement();
					this.getOriginalData().put(datastream, rootElement);
				}
			} catch (Exception e) {
				throw new UIPException("Exception occurred while attempting to store datastream " + datastream + " for "
						+ pid.getPid(), e);
			} finally {
				if (inputStream != null)
					try {
						inputStream.close();
					} catch (IOException e) {
						throw new UIPException("Exception occurred while attempting to store datastream " + datastream + " for "
								+ pid.getPid(), e);
					}
			}
		}
		
	}

	/**
	 * Generates temporary files for each metadata datastream and returns a hash of them by datastream
	 */
	@Override
	public Map<String, File> getModifiedFiles() {
		Map<String, File> modifiedFiles = new HashMap<String, File>();
		for (Entry<String, ?> modified : modifiedData.entrySet()) {
			Element modifiedElement = (Element)modified.getValue();
			try {
				File temp = ClientUtils.writeXMLToTempFile(modifiedElement);
				modifiedFiles.put(modified.getKey(), temp);
			} catch (IOException e) {
				log.error("Failed to create temp file", e);
			}
		}
		return modifiedFiles;
	}
}

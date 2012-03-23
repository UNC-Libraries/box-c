package edu.unc.lib.dl.update;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.ClientUtils;
import edu.unc.lib.dl.fedora.PID;

public abstract class MetadataUIP extends UIPImpl {
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

/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.ingest.aip;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.services.AgentManager;
import edu.unc.lib.dl.util.ContainerPlacement;
import edu.unc.lib.dl.util.FileUtils;
import edu.unc.lib.dl.util.IngestProperties;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;

/**
 * This is the only implementation of the AIP contract at the moment. Note sure yet if there is a need for more
 * implementations of this one. This implementation stores foxml files in a local temporary folder and uses an in-memory
 * PREMIS logger.
 *
 * @author count0
 *
 */
public class AIPImpl implements ArchivalInformationPackage {
	private static final Log log = LogFactory.getLog(AIPImpl.class);
	private File prepDir = null;

	private PremisEventLogger eventLogger = new PremisEventLogger(AgentManager.getRepositorySoftwareAgentStub());
	private HashMap<PID, File> pid2FOXMLFile = new HashMap<PID, File>();
	private Set<PID> topPID = null;
	private final Map<PID, ContainerPlacement> topPID2Placement = new HashMap<PID, ContainerPlacement>();
	private boolean sendEmail = false;
	private List<URI> emailRecipients = new ArrayList<URI>();

	/**
	 * Makes an AIP with a pre-populated prep dir.
	 * @param prepDir
	 *           directory containing FOXML files and the data directory
	 */
	public AIPImpl(File prepDir) {
		this.prepDir = prepDir;
	}

	/**
	 * Makes an AIP with a empty prep dir
	 */
	public AIPImpl() {
		try {
			this.prepDir = FileUtils.createTempDirectory("ingest-prep");
		} catch (IOException e) {
			throw new Error("Unexpected", e);
		}
	}

	/**
	 * Destroy this AIP and all relevant resources.
	 */
	public void delete() {
		log.debug("delete called");
		// cleanup *any* remaining files
		if (pid2FOXMLFile != null) {
			pid2FOXMLFile.clear();
			pid2FOXMLFile = null;
		}
		if (prepDir != null && prepDir.exists()) {
			FileUtils.deleteDir(prepDir);
		}
	}

	public PremisEventLogger getEventLogger() {
		return this.eventLogger;
	}

	public File getFileForUrl(String url) {
		File dataDir = new File(this.prepDir, "data");
		try {
			return FileUtils.getFileForUrl(url, dataDir);
		} catch (IOException e) {
			log.error(e);
			return null;
		}
	}

	public Document getFOXMLDocument(PID pid) {
		Document result = null;
		SAXBuilder builder = new SAXBuilder();
		try {
			result = builder.build(this.getFOXMLFile(pid));
		} catch (JDOMException e) {
			throw new Error("The FOXML file in the ingest context is not well-formed XML.", e);
		} catch (IOException e) {
			throw new Error("The FOXML file in the ingest context is not readable.", e);
		}
		return result;
	}

	public File getFOXMLFile(PID pid) {
		return this.pid2FOXMLFile.get(pid);
	}

	public Set<PID> getPIDs() {
		return this.pid2FOXMLFile.keySet();
	}

	public File getTempFOXDir() {
		return prepDir;
	}

	// @Override
	// public Integer getTopPIDContainerOrder(PID pid) {
	// return this.topPID2Order.get(pid);
	// }

	// @Override
	// public String getTopPIDContainerPath(PID topPID) {
	// return this.topPID2Path.get(topPID);
	// }

	public Set<PID> getTopPIDs() {
		return topPID;
	}

	@Override
	public void prepareIngest(String message, String submitter) throws IngestException {
		// write ingest properties
		try {
			serializeLoggerEventsToFoxml();
			IngestProperties props = new IngestProperties(this.prepDir);
			List<String> recipients = new ArrayList<String>();
			for(URI r : this.emailRecipients) {
				recipients.add(r.toString());
			}
			props.setEmailRecipients(recipients.toArray(new String[1]));
			props.setContainerPlacements(topPID2Placement);
			props.setMessage(message);
			props.setSubmitter(submitter);
			props.save();
		} catch (Exception e) {
			throw new IngestException("Cannot create ingest properties.", e);
		}
	}

	/**
	 *
	 */
	private void serializeLoggerEventsToFoxml() {
		for(PID pid : this.getPIDs()) {
			Document doc = this.getFOXMLDocument(pid);
			Element eventsEl = FOXMLJDOMUtil.getDatastream(doc, "MD_EVENTS");
			if (eventsEl == null) {
				eventsEl = this.eventLogger.getObjectEvents(pid);
				Element eventsDS = FOXMLJDOMUtil.makeXMLManagedDatastreamElement("MD_EVENTS", "PREMIS Events Metadata",
						"MD_EVENTS1.0", eventsEl, false);
				doc.getRootElement().addContent(eventsDS);
			} else {
				this.eventLogger.appendLogEvents(pid, eventsEl);
			}
			this.saveFOXMLDocument(pid, doc);
		}
	}

	public void saveFOXMLDocument(PID pid, Document doc) {
		XMLOutputter outputter = new XMLOutputter();
		FileWriter fw = null;
		try {
			File out = this.getFOXMLFile(pid);
			if (out == null) {
				out = File.createTempFile("foxml", ".foxml", this.prepDir);
				out.delete();
				out = new File(this.prepDir, out.getName());
				this.setFOXMLFile(pid, out);
			}
			fw = new FileWriter(out);
			outputter.output(doc, fw);
		} catch (IOException e) {
			throw new Error("Failed to rewrite FOXML file.", e);
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					throw new Error("Failed to close FOXML file writer.", e);
				}
			}
		}
	}

	/**
	 * Set the event logger used for this AIP (logger may be used elsewhere also)
	 *
	 * @param eventLogger
	 *           the PREMIS event logger
	 */
	// public void setEventLogger(PremisEventLogger eventLogger) {
	// this.eventLogger = eventLogger;
	// }
	/**
	 * Set the foxml for a given object by PID.
	 *
	 * @param pid
	 *           the PID of the object this foxml represents
	 * @param file
	 *           a foxml file
	 */
	public void setFOXMLFile(PID pid, File file) {
		this.pid2FOXMLFile.put(pid, file);
	}

	// @Override
	// public void setTopPIDLocation(String path, PID topPID, Integer order) {
	// this.topPID2Path.put(topPID, path);
	// if (order != null) {
	// this.topPID2Order.put(topPID, order);
	// }
	// }

	@Override
	public void setContainerPlacement(PID parentPID, PID topPID, Integer designatedOrder, Integer sipOrder) {
		ContainerPlacement p = new ContainerPlacement();
		p.parentPID = parentPID;
		p.pid = topPID;
		p.designatedOrder = designatedOrder;
		p.sipOrder = sipOrder;
		this.topPID2Placement.put(topPID, p);
	}

	/**
	 * Set the PID of the top object within the AIP
	 *
	 * @param topPID
	 *           the top PID
	 */
	@Override
	public void setTopPIDs(Set<PID> topPID) {
		this.topPID = topPID;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage#getEmailRecipients()
	 */
	@Override
	public List<URI> getEmailRecipients() {
		return this.emailRecipients;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage#getSendEmail()
	 */
	@Override
	public boolean getSendEmail() {
		return this.sendEmail;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage#setEmailRecipients (java.util.List)
	 */
	@Override
	public void setEmailRecipients(List<URI> recipients) {
		this.emailRecipients = recipients;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage#setSendEmail(boolean )
	 */
	@Override
	public void setSendEmail(boolean sendEmail) {
		this.sendEmail = sendEmail;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage#getTopPIDPlacement (edu.unc.lib.dl.fedora.PID)
	 */
	@Override
	public ContainerPlacement getContainerPlacement(PID pid) {
		return this.topPID2Placement.get(pid);
	}

}

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
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.ZipFileUtil;

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
	private PremisEventLogger eventLogger = null;
	private String expectedParentContentModel = null;
	private HashMap<PID, File> pid2FOXMLFile = new HashMap<PID, File>();
	private File tempDataFile = null;
	private Set<File> dataFiles = null;
	private File tempFOXDir = null;
	private Set<PID> topPID = null;
	// private final Map<PID, Integer> topPID2Order = new HashMap<PID,
	// Integer>();
	// private final Map<PID, String> topPID2Path = new HashMap<PID, String>();
	private final Map<PID, ContainerPlacement> topPID2Placement = new HashMap<PID, ContainerPlacement>();
	private boolean deleteFilesOnDestroy = true;
	private boolean sendEmail = false;
	private List<URI> emailRecipients = new ArrayList<URI>();

	public boolean isDeleteFilesOnDestroy() {
		return deleteFilesOnDestroy;
	}

	public void setDeleteFilesOnDestroy(boolean deleteFilesOnDestroy) {
		this.deleteFilesOnDestroy = deleteFilesOnDestroy;
	}

	/**
	 * Creates an AIP
	 *
	 * @param tempFOXDir
	 *           the temporary directory in which FOXML will reside. (used for delete)
	 */
	/**
	 * @param tempDataFile
	 *           directory containing data file(s) or a single data file
	 * @param logger
	 */
	public AIPImpl(File tempDataFile, PremisEventLogger logger) {
		// MAKE A TEMP DIR
		try {
			// get a temporary directory to work with
			tempFOXDir = File.createTempFile("foxml", ".tmp");
			tempFOXDir.delete();
			tempFOXDir.mkdir();
			tempFOXDir.deleteOnExit();
		} catch (IOException e) {
			throw new Error(e);
		}
		this.tempDataFile = tempDataFile;
		this.eventLogger = logger;
	}

	/**
	 * Creates an AIP with a set of data files
	 *
	 * @param tempFOXDir
	 *           the temporary directory in which FOXML will reside. (used for delete)
	 */
	/**
	 * @param tempDataFile
	 *           directory containing data file(s) or a single data file
	 * @param logger
	 */
	public AIPImpl(Set<File> dataFiles, PremisEventLogger logger) {
		// MAKE A TEMP DIR
		try {
			// get a temporary directory to work with
			tempFOXDir = File.createTempFile("foxml", ".tmp");
			tempFOXDir.delete();
			tempFOXDir.mkdir();
			tempFOXDir.deleteOnExit();
		} catch (IOException e) {
			throw new Error(e);
		}
		this.dataFiles = dataFiles;
		this.eventLogger = logger;
	}

	public AIPImpl(PremisEventLogger logger) {
		this.eventLogger = logger;
		try {
			tempFOXDir = File.createTempFile("foxml", null);
			tempFOXDir.delete();
			tempFOXDir.mkdir();
		} catch (IOException e) {
			throw new Error("Could not create a temporary directory for FOXML.", e);
		}
	}

	/**
	 * Destroy this AIP and all relevant resources.
	 */
	public void destroy() {
		log.debug("destroy called");
		// cleanup *any* remaining files
		if (pid2FOXMLFile != null) {
			pid2FOXMLFile.clear();
			pid2FOXMLFile = null;
		}
		if (tempFOXDir != null && tempFOXDir.exists() && this.deleteFilesOnDestroy) {
			ZipFileUtil.deleteDir(tempFOXDir);
		}
		if (tempDataFile != null && tempDataFile.exists() && this.deleteFilesOnDestroy) {
			if (tempDataFile.isDirectory()) {
				ZipFileUtil.deleteDir(tempDataFile);
			} else {
				tempDataFile.delete();
			}
		}
		if (this.dataFiles != null) {
			if (this.deleteFilesOnDestroy) {
				for (File f : this.dataFiles) {
					f.delete();
				}
			}
			this.dataFiles.clear();
			this.dataFiles = null;
		}
	}

	public String getContainerContentModel() {
		return this.expectedParentContentModel;
	}

	public PremisEventLogger getEventLogger() {
		return this.eventLogger;
	}

	public File getFileForUrl(String url) {
		// TODO: create StagingLocationsResolver for staged locations (or rely
		// on Fedora/iRODS)
		if (this.dataFiles != null) { // we have a file map
			File f = new File(url); // this url should be a path
			if (!this.dataFiles.contains(f)) {
				log.error("bad file reference for this AIP:" + url);
				return null;
			} else {
				return f;
			}
		} else if (this.tempDataFile != null) { // directory of data files
			if (this.tempDataFile.isDirectory()) {
				try {
					return ZipFileUtil.getFileForUrl(url, this.tempDataFile);
				} catch (IOException e) {
					log.error(e);
					return null;
				}
			} else { // just one data file w/o temp dir
				return this.tempDataFile;
			}
		} else {
			log.error("bad file reference for this AIP:" + url);
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
		return tempFOXDir;
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
	public void prepareIngest() throws IngestException {
		// nothing to do
	}

	public void saveFOXMLDocument(PID pid, Document doc) {
		XMLOutputter outputter = new XMLOutputter();
		FileWriter fw = null;
		try {
			File out = this.getFOXMLFile(pid);
			if (out == null) {
				out = File.createTempFile("foxml", ".xml", this.tempFOXDir);
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

	public void setContainerContentModel(String model) {
		this.expectedParentContentModel = model;
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
	public void setTopPIDPlacement(PID parentPID, PID topPID, Integer designatedOrder, Integer sipOrder) {
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
	public ContainerPlacement getTopPIDPlacement(PID pid) {
		return this.topPID2Placement.get(pid);
	}

}

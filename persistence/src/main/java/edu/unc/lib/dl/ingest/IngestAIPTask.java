/**
 * Copyright 2011 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.ingest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import edu.unc.lib.dl.agents.AgentManager;
import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.ManagementClient.ControlGroup;
import edu.unc.lib.dl.fedora.ManagementClient.Format;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.fedora.types.Datastream;
import edu.unc.lib.dl.ingest.aip.AIPIngestPipeline;
import edu.unc.lib.dl.ingest.aip.RepositoryPlacement;
import edu.unc.lib.dl.services.DigitalObjectManagerImpl;
import edu.unc.lib.dl.services.IngestService;
import edu.unc.lib.dl.services.MailNotifier;
import edu.unc.lib.dl.services.OperationsMessageSender;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.ZipFileUtil;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * @author Gregory Jansen
 *
 */
public class IngestAIPTask implements Runnable {
	private static final Log log = LogFactory.getLog(IngestAIPTask.class);
	private static final String INGEST_LOG = "ingested.log";
	private static final int INGEST_TIMEOUT_SECS = 1200;

	private static final int ST_INIT = 0;
	private static final int ST_STARTING = 1;
	private static final int ST_INGEST = 2;
	private static final int ST_INGEST_WAIT = 3;
	private static final int ST_INGEST_VERIFY_CHECKSUMS = 4;
	private static final int ST_CONTAINER_UPDATE = 5;
	private static final int ST_SEND_MESSAGES = 6;
	private static final int ST_CLEANUP = 7;
	private static final int ST_FINISHED = 8;

	private int state = ST_INIT;

	/**
	 * This code indicates a container update in the ingest log.
	 */
	private static final String CONTAINER_UPDATE_CODE = "CONTAINER UPDATED";

	/**
	 * Base directory of this ingest batch
	 */
	File baseDir = null;

	/**
	 * Directory for data files in this ingest batch
	 */
	File dataDir = null;
	IngestProperties props = null;

	/**
	 * Event Logger for this batch
	 */
	PremisEventLogger eventLogger = null;
	// TODO load pre-ingest events

	/**
	 * The FOXML files in this batch
	 */
	File[] foxmlFiles = null;

	/**
	 * The PIDs that will contain this batch
	 */
	PID[] containers = null;

	// ingest status
	/**
	 * The last file we attempted to ingest
	 */
	private String lastIngestFilename = null;

	/**
	 * The last PID we attempted to ingest
	 */
	private PID lastIngestPID = null;

	/**
	 * The timestamp at which the batch was started
	 */
	private long startTime = -1;

	/**
	 * The timestamp when the last FOXML ingest finished
	 */
	private long lastIngestTime = -1;

	private BufferedWriter ingestLogWriter = null;

	// injected dependencies
	private AIPIngestPipeline aipIngestPipeline = null;
	private ManagementClient managementClient = null;
	private OperationsMessageSender operationsMessageSender = null;
	private MailNotifier mailNotifier = null;
	private DigitalObjectManagerImpl digitalObjectManager = null;
	private AgentManager agentManager = null;

	public AIPIngestPipeline getAipIngestPipeline() {
		return aipIngestPipeline;
	}

	public void setAipIngestPipeline(AIPIngestPipeline aipIngestPipeline) {
		this.aipIngestPipeline = aipIngestPipeline;
	}

	public ManagementClient getManagementClient() {
		return managementClient;
	}

	public void setManagementClient(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}

	public OperationsMessageSender getOperationsMessageSender() {
		return operationsMessageSender;
	}

	public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
		this.operationsMessageSender = operationsMessageSender;
	}

	public MailNotifier getMailNotifier() {
		return mailNotifier;
	}

	public void setMailNotifier(MailNotifier mailNotifier) {
		this.mailNotifier = mailNotifier;
	}

	public IngestAIPTask(IngestService service, String baseDir) {
		this.baseDir = new File(baseDir);
		log.debug("Ingest task created for " + this.baseDir.getAbsolutePath());
	}

	public void init() {
		try {
			dataDir = new File(this.baseDir, "data");
			File ingestLog = new File(this.baseDir, INGEST_LOG);
			props = new IngestProperties(this.baseDir);
			foxmlFiles = this.baseDir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".foxml");
				}
			});
			Arrays.sort(foxmlFiles, new Comparator<File>() {
				@Override
				public int compare(File o1, File o2) {
					return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
				}
			});

			HashSet<PID> cSet = new HashSet<PID>();
			for(RepositoryPlacement p : props.getPlacements().values()) {
				cSet.add(p.parentPID);
			}
			containers = cSet.toArray(new PID[] {});
			Arrays.sort(containers);

			this.state = ST_STARTING;
			if (ingestLog.exists()) { // this is a resume, find next foxml
				BufferedReader r = new BufferedReader(new FileReader(ingestLog));
				String lastLine = null;
				for (String line = r.readLine(); line != null; line = r.readLine()) {
					lastLine = line;
				}
				r.close();
				if (lastLine != null) {
					// format is <pid>|path
					String[] l = lastLine.split("|");
					if (CONTAINER_UPDATE_CODE.equals(l[1])) {
						this.state = ST_CONTAINER_UPDATE;
						this.lastIngestPID = new PID(l[0]);
					} else {
						this.lastIngestFilename = l[1];
						this.lastIngestPID = new PID(l[0]);
						this.state = ST_INGEST_WAIT;
						log.info("Resuming ingest from " + this.lastIngestFilename + " in " + this.baseDir.getName());
					}
				}
			}
			ingestLogWriter = new BufferedWriter(new FileWriter(ingestLog));

			startTime = System.currentTimeMillis();
		} catch (Exception e) {
			fail("Cannot initialize the ingest task.", e);
		}
	}

	/**
	 * @param string
	 * @param e
	 */
	private void fail(String string, Throwable e) {
		// TODO Auto-generated method stub

	}

	public void logIngestAttempt(PID pid, String filename) {
		try {
			this.ingestLogWriter.write(pid.getPid() + "|" + filename);
			this.ingestLogWriter.flush();
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	public void logIngestComplete() {
		long ingestTime = System.currentTimeMillis() - ((lastIngestTime > 0) ? lastIngestTime : startTime);
		try {
			this.ingestLogWriter.write("|"+ingestTime);
			this.ingestLogWriter.newLine();
			this.ingestLogWriter.flush();
		} catch (IOException e) {
			throw new Error(e);
		}
		lastIngestTime = System.currentTimeMillis();
	}

	/**
	 * Interrupts the batch ingest task as soon as possible. This task, or a new one for the same base dir, may be
	 * resumed.
	 */
	public void pause() {
	}

	/**
	 * Interrupts the batch ingest task, if running, and backs out objects already ingested.
	 */
	public void cancel(boolean purgeIngests) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		init();

		while (this.state != ST_FINISHED) {
			switch (this.state) {
				case ST_STARTING: // wait for fedora availability, check containers, move to ingesting state
					startBatch();
					break;
				case ST_INGEST: // ingest the next foxml file, until none left
					ingestNextObject();
					break;
				case ST_INGEST_WAIT: // poll for last ingested pid (and fedora availability)
					waitForLastIngest();
					break;
				case ST_INGEST_VERIFY_CHECKSUMS: // match local checksums against those generated by Fedora
					verifyLastIngestChecksums();
					break;
				case ST_CONTAINER_UPDATE: // update parent container object
					updateNextContainer();
					break;
				case ST_SEND_MESSAGES: // send cdr JMS and email for this AIP ingest
					this.getOperationsMessageSender().sendAddOperation(props.getSubmitter(), timestamp, destinations,
							props.getPlacements().keySet(), reordered);

					// send successful ingest email
					if (this.mailNotifier != null)
						this.mailNotifier.sendIngestSuccessNotice(props);
				case ST_CLEANUP: // if nothing went wrong, then delete the batch directory
			}
		}
	}

	/**
	 *
	 */
	private void updateNextContainer() {
		int next = 0;
		if(this.lastIngestPID != null) {
			for(int i = 0; i < containers.length; i++) {
				if (containers[i].equals(this.lastIngestPID)) {
					next = i+1;
					break;
				}
			}
		}
		if(next >= containers.length) { // no more to update
			this.state = ST_SEND_MESSAGES;
			return;
		}

		PersonAgent submitter = this.getAgentManager().findPersonByOnyen(props.getSubmitter(), false);
		List<PID> reordered = new ArrayList<PID>();
		// TODO convert to one container update at a time
		try {
			logIngestAttempt(containers[next], CONTAINER_UPDATE_CODE);
			String timestamp = this.digitalObjectManager.addContainerContents(submitter, props.getPlacements().values(),
					containers[next], reordered);
			logIngestComplete();
		} catch (FedoraException e) {
			fail("Cannot update container: "+containers[next], e);
		}
	}

	/**
	 * Polls Fedora for the last ingested PID
	 */
	private void waitForLastIngest() {
		if(!this.managementClient.pollForObject(this.lastIngestPID, 60, INGEST_TIMEOUT_SECS)) {
			// TODO re-attempt last ingest before failing?
			fail("The last ingest before resuming was never completed. " + this.lastIngestPID + " in "
					+ this.lastIngestFilename);
			return;
		} else {
			this.state = ST_INGEST_VERIFY_CHECKSUMS;
		}
	}

	/**
	 * Checks that Fedora is responding, looks for destination containers.
	 */
	private void startBatch() {
		if(!this.managementClient.pollForObject(ContentModelHelper.Administrative_PID.REPOSITORY.getPID(), 30, 600)) {
			fail("Cannot contact Fedora");
		}
		HashSet<PID> containers = new HashSet<PID>();
		for(RepositoryPlacement p : props.getPlacements().values()) {
			containers.add(p.parentPID);
		}
		for(PID container : containers) {
			if(!this.managementClient.pollForObject(container, 10, 30)) {
				fail("Cannot find existing container: "+container);
			}
		}
		this.state = ST_INGEST;
	}

	/**
	 * @param string
	 */
	private void fail(String string) {
		// TODO Auto-generated method stub

	}

	private void ingestNextObject() {
		int next = 0;
		if(this.lastIngestFilename != null) {
			for(int i = 0; i < foxmlFiles.length; i++) {
				if (foxmlFiles[i].getName().equals(this.lastIngestFilename)) {
					next = i+1;
					break;
				}
			}
		}
		if(next >= foxmlFiles.length) { // no more to ingest, next step
			this.state = ST_CONTAINER_UPDATE;
			return;
		}

		Document doc = getFOXMLDocument(foxmlFiles[next]);
		PID pid = new PID(FOXMLJDOMUtil.getPID(doc));

		// handle file locations (upload/rewrite/pass-through)
		for (Element cLocation : FOXMLJDOMUtil.getFileLocators(doc)) {
			String ref = cLocation.getAttributeValue("REF");
			try {
				URI uri = new URI(ref);
				if (uri.getScheme() != null && !uri.getScheme().contains("file")) {
					continue;
				}
			} catch (URISyntaxException e) {
			}
			String newref = null;
			File file;
			try {
				file = ZipFileUtil.getFileForUrl(ref, dataDir);
			} catch (IOException e) {
				fail("Data file missing: "+ref, e);
				break;
			}
			newref = this.getManagementClient().upload(file);
			cLocation.setAttribute("REF", newref);

			// this save can be avoided if we ingest the JDOM in memory!
			// context.saveFOXMLDocument(pid, doc);
			log.debug("uploaded " + ref + " to Fedora " + newref + " for " + pid);
		}

		// log ingest event and upload the MD_EVENTS datastream
		this.eventLogger.logEvent(PremisEventLogger.Type.INGESTION, "ingested as PID:" + pid.getPid(), pid);
		Document MD_EVENTS = new Document(this.eventLogger.getObjectEvents(pid));
		String eventsLoc = this.getManagementClient().upload(MD_EVENTS);
		Element eventsEl = FOXMLJDOMUtil.makeLocatorDatastream("MD_EVENTS", "M", eventsLoc, "text/xml", "URL",
				"PREMIS Events Metadata", false, null);
		doc.getRootElement().addContent(eventsEl);

		// FEDORA INGEST CALL
		PID pidIngested = null;
		try {
			this.lastIngestFilename = foxmlFiles[next].getName();
			this.lastIngestPID = pid;
			pidIngested = this.getManagementClient().ingest(doc, Format.FOXML_1_1, props.getMessage());
			logIngestAttempt(pidIngested, this.lastIngestFilename);
			this.state = ST_INGEST_VERIFY_CHECKSUMS;
		} catch (ServiceException e) { // on timeout poll for the ingested object
			log.info("Fedora Service Exception: "+e.getLocalizedMessage(), e);
			logIngestAttempt(pid, lastIngestFilename);
			this.state = ST_INGEST_WAIT;
			return;
		} catch (FedoraException e) {	// fedora threw a fault, ingest rejected
			fail("Cannot ingest due to Fedora error: "+pid, e);
			return;
		}
	}

	/**
	 * Same as ingest, but verifies any supplied checksums against available repository metadata. If the checksums do not
	 * match, returns false.
	 *
	 * @param xml
	 *           FOXML document to ingest (may include MD5 contentDigest elements)
	 * @param message
	 *           ingest log message
	 * @param ingested
	 *           the log of ingested objects
	 * @return
	 */
	private void verifyLastIngestChecksums() throws FedoraException {
		PID pid = this.lastIngestPID;
		Document xml = getFOXMLDocument(new File(baseDir, this.lastIngestFilename));
		// build a map of checksums for the datastreams
		Map<String, String> dsID2md5 = new HashMap<String, String>();
		for (Element ds : FOXMLJDOMUtil.getAllDatastreams(xml)) {
			if (ControlGroup.MANAGED.toString().equals(ds.getAttributeValue("CONTROL_GROUP"))) {
				Element dsV = ds.getChild("datastreamVersion", JDOMNamespaceUtil.FOXML_NS);
				Element contentDigest = dsV.getChild("contentDigest", JDOMNamespaceUtil.FOXML_NS);
				if (contentDigest != null) { // we have a winner!
					String dsID = ds.getAttributeValue("ID");
					ds.getChild("dataStreamVersion", JDOMNamespaceUtil.FOXML_NS);
					String type = contentDigest.getAttributeValue("TYPE");
					if (type == null) {
						contentDigest.setAttribute("TYPE", "MD5");
					}
					String digest = contentDigest.getAttributeValue("DIGEST");
					if (digest != null && !"none".equals(digest)) {
						// add to verified checksum map
						log.debug("caching checksum for post-ingest verification: " + dsID + " " + digest);
						dsID2md5.put(dsID, digest);
					}
					contentDigest.setAttribute("DIGEST", "none");
				}
			}
		}
		for (String dsID : dsID2md5.keySet()) {
			Datastream d = this.getManagementClient().getDatastream(pid, dsID);
			// compare checksums
			if (!dsID2md5.get(dsID).equals(d.getChecksum())) { // datastream doesn't match
				// TODO purge datastream and re-upload before failing?
				fail("Post-ingest checksum verification failed.  An ingested datastream did not match the supplied checksum.");
				return;
			}
			log.debug("Verified post-ingest checksum: " + pid.getPid() + " " + dsID);
		}
		logIngestComplete();
		this.state = ST_INGEST;
	}

	public DigitalObjectManagerImpl getDigitalObjectManager() {
		return digitalObjectManager;
	}

	public void setDigitalObjectManager(DigitalObjectManagerImpl digitalObjectManager) {
		this.digitalObjectManager = digitalObjectManager;
	}

	public Document getFOXMLDocument(File foxmlFile) {
		Document result = null;
		SAXBuilder builder = new SAXBuilder();
		try {
			result = builder.build(foxmlFile);
		} catch (JDOMException e) {
			throw new Error("The FOXML file in the ingest context is not well-formed XML.", e);
		} catch (IOException e) {
			throw new Error("The FOXML file in the ingest context is not readable.", e);
		}
		return result;
	}

	public AgentManager getAgentManager() {
		return agentManager;
	}

	public void setAgentManager(AgentManager agentManager) {
		this.agentManager = agentManager;
	}

}

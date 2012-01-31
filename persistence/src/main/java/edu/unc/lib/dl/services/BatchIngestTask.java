/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.dl.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.AgentFactory;
import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.FedoraTimeoutException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.ManagementClient.ChecksumType;
import edu.unc.lib.dl.fedora.ManagementClient.ControlGroup;
import edu.unc.lib.dl.fedora.ManagementClient.Format;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.Datastream;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.util.ContainerContentsHelper;
import edu.unc.lib.dl.util.ContainerPlacement;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.FileUtils;
import edu.unc.lib.dl.util.IllegalRepositoryStateException;
import edu.unc.lib.dl.util.IngestProperties;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * @author Gregory Jansen
 *
 */
public class BatchIngestTask implements Runnable {
	/**
	 * States for this task. The ingest states loop until the last object is verified. Container update state repeats
	 * until all containers are updated.
	 *
	 */
	public enum STATE {
		INIT, INGEST, INGEST_WAIT, INGEST_VERIFY_CHECKSUMS, CONTAINER_UPDATES, SEND_MESSAGES, CLEANUP, FINISHED
	}

	private static final Log log = LogFactory.getLog(BatchIngestTask.class);
	private static final String INGEST_LOG = "ingested.log";
	private static final String REORDERED_LOG = "reordered.log";
	private static final String FAIL_LOG = "fail.log";
	private int ingestPollingTimeoutSeconds = -1;
	private boolean saveFinishedBatches = true;

	private int ingestPollingDelaySeconds = -1;

	private STATE state = STATE.INIT;

	/**
	 * Marks this tasks as halting. (Another task may resume ingest later.)
	 */
	private boolean halting = false;

	/**
	 * Marks this task as having failed for collaborators.
	 */
	private boolean failed = false;

	/**
	 * This code indicates a container update in the ingest log.
	 */
	private static final String CONTAINER_UPDATED_CODE = "CONTAINER UPDATED";

	/**
	 * Base directory of this ingest batch
	 */
	private File baseDir = null;

	/**
	 * Directory for data files in this ingest batch
	 */
	File dataDir = null;
	File premisDir = null;
	IngestProperties props = null;

	/**
	 * Event Logger for this batch
	 */
	PremisEventLogger eventLogger = null;

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

	private PersonAgent submitterAgent = null;

	private BufferedWriter ingestLogWriter = null;

	// injected dependencies
	private ManagementClient managementClient = null;
	private AccessClient accessClient = null;
	private OperationsMessageSender operationsMessageSender = null;
	private MailNotifier mailNotifier = null;
	private AgentFactory agentFactory = null;
	private boolean sendJmsMessages = true;
	private boolean sendEmailMessages = true;

	public BatchIngestTask() {
	}

	/**
	 * Updates the RELS-EXT contains relationships and the MD_CONTENTS datastream. Call this method last, after all other
	 * transactions, it will roll itself back on failure and throw an IngestException.
	 *
	 * @param submitter
	 *           the Agent that submitted this change
	 * @param placements
	 *           the container locations of new pids
	 * @param container
	 *           the container added to
	 * @return the list of PIDs reordered by this change
	 * @throws FedoraException
	 */
	private List<PID> addContainerContents(Agent submitter, Collection<ContainerPlacement> placements, PID container)
			throws FedoraException {
		List<PID> reordered = new ArrayList<PID>();

		// beginning of container meddling
		// TODO do this in 1 RELS-EXT edit
		for (ContainerPlacement p : placements) {
			if (container.equals(p.parentPID)) {
				this.getManagementClient().addObjectRelationship(container,
						ContentModelHelper.Relationship.contains.getURI().toString(), p.pid);
			}
		}
		// edit Contents XML of parent container to append/insert
		Document newXML;
		Document oldXML;
		boolean exists = true;
		try {
			MIMETypedStream mts = this.accessClient.getDatastreamDissemination(container, "MD_CONTENTS", null);
			ByteArrayInputStream bais = new ByteArrayInputStream(mts.getStream());
			try {
				oldXML = new SAXBuilder().build(bais);
				bais.close();
			} catch (JDOMException e) {
				throw new IllegalRepositoryStateException("Cannot parse MD_CONTENTS: " + container);
			} catch (IOException e) {
				throw new Error(e);
			}
		} catch (NotFoundException e) {
			oldXML = new Document();
			Element structMap = new Element("structMap", JDOMNamespaceUtil.METS_NS).addContent(new Element("div",
					JDOMNamespaceUtil.METS_NS).setAttribute("TYPE", "Container"));
			oldXML.setRootElement(structMap);
			exists = false;
		}
		newXML = ContainerContentsHelper.addChildContentAIPInCustomOrder(oldXML, container, placements, reordered);
		if (exists) {
			this.getManagementClient().modifyInlineXMLDatastream(container, "MD_CONTENTS", false,
					"adding child resource to container", new ArrayList<String>(), "List of Contents", newXML);
		} else {
			this.getManagementClient().addInlineXMLDatastream(container, "MD_CONTENTS", false,
					"added child resource to container", new ArrayList<String>(), "List of Contents", false, newXML);
		}
		// LOG CHANGES TO THE CONTAINER
		int children = placements.size();
		this.eventLogger.logEvent(PremisEventLogger.Type.INGESTION, "added " + children
				+ " child object(s) to this container", container);
		this.getManagementClient().writePremisEventsToFedoraObject(this.eventLogger, container);
		return reordered;
	}

	/**
	 * @param string
	 */
	private BatchFailedException fail(String message) {
		return fail(message, null);
	}

	/**
	 * @param string
	 * @param e
	 */
	private BatchFailedException fail(String message, Throwable e) {
		this.failed = true;
		this.state = STATE.FINISHED;
		File failLog = new File(this.getBaseDir(), FAIL_LOG);
		PrintWriter w = null;
		try {
			failLog.createNewFile();
			w = new PrintWriter(new FileOutputStream(failLog));
			w.println(message);
			if (e != null) {
				e.printStackTrace(w);
			}
		} catch (IOException e1) {
			throw new Error("Unexpected error", e1);
		} finally {
			if (w != null) {
				try {
					w.flush();
					w.close();
				} catch (Exception ignored) {
				}
			}
		}
		// move batch to failed dir
		try {
			File failedFolder = new File(this.baseDir.getParentFile().getParentFile(), BatchIngestQueue.FAILED_SUBDIR);
			File dest = new File(failedFolder, this.baseDir.getName());
			FileUtils.renameOrMoveTo(this.baseDir, dest);
		} catch (IOException ioe) {
			throw new Error("Unexpected IO error on moving completed ingest batch.", ioe);
		}
		if (e != null) {
			return new BatchFailedException(message, e);
		} else {
			return new BatchFailedException(message);
		}
	}

	public AgentFactory getAgentFactory() {
		return agentFactory;
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

	public int getIngestPollingDelaySeconds() {
		return ingestPollingDelaySeconds;
	}

	public int getIngestPollingTimeoutSeconds() {
		return ingestPollingTimeoutSeconds;
	}

	public MailNotifier getMailNotifier() {
		return mailNotifier;
	}

	public ManagementClient getManagementClient() {
		return managementClient;
	}

	public OperationsMessageSender getOperationsMessageSender() {
		return operationsMessageSender;
	}

	private void ingestNextObject() {
		log.debug("entering ingest next method");
		int next = 0;
		if (this.lastIngestFilename != null) {
			for (int i = 0; i < foxmlFiles.length; i++) {
				if (foxmlFiles[i].getName().equals(this.lastIngestFilename)) {
					next = i + 1;
					break;
				}
			}
		}
		if (next >= foxmlFiles.length) { // no more to ingest, next step
			log.debug("detected that ingests are done, not going to container update state");
			this.state = STATE.CONTAINER_UPDATES;
			return;
		}

		Document doc = getFOXMLDocument(foxmlFiles[next]);
		PID pid = new PID(FOXMLJDOMUtil.getPID(doc));

		log.debug("next ingest is:\t"+foxmlFiles[next].getName()+"\t"+pid.getPid());

		// handle file locations (upload/rewrite/pass-through)
		for (Element cLocation : FOXMLJDOMUtil.getFileLocators(doc)) {
			String ref = cLocation.getAttributeValue("REF");
			String newref = null;
			try {
				URI uri = new URI(ref);
				if (uri.getScheme() == null || uri.getScheme().contains("file")) {
					try {
						File file = FileUtils.getFileForUrl(ref, dataDir);
						log.debug("uploading "+file.getPath());
						newref = this.getManagementClient().upload(file);
						cLocation.setAttribute("REF", newref);
					} catch (IOException e) {
						throw fail("Data file missing: " + ref, e);
					}
				} else if (uri.getScheme().contains("premisEvents")) {
					try {
						File file = new File(premisDir, ref.substring(ref.indexOf(":") + 1));
						Document premis = new SAXBuilder().build(file);
						this.eventLogger.logEvent(PremisEventLogger.Type.INGESTION, "ingested as PID:" + pid.getPid(), pid);
						this.eventLogger.appendLogEvents(pid, premis.getRootElement());
						log.debug("uploading "+file.getPath());
						newref = this.getManagementClient().upload(premis);
						cLocation.setAttribute("REF", newref);
					} catch (Exception e) {
						throw fail("there was a problem uploading ingest events", e);
					}
				} else {
					continue;
				}
			} catch (URISyntaxException e) {
				throw fail("Bad URI syntax for file ref", e);
			}
			log.debug("uploaded " + ref + " to Fedora " + newref + " for " + pid);
		}

		if (log.isDebugEnabled()) {
			String xml = new XMLOutputter().outputString(doc);
			log.debug("INGESTING FOXML:\n" + xml);
		}

		// FEDORA INGEST CALL
		try {
			this.lastIngestFilename = foxmlFiles[next].getName();
			this.lastIngestPID = pid;
			logIngestAttempt(pid, this.lastIngestFilename);
			this.getManagementClient().ingest(doc, Format.FOXML_1_1, props.getMessage());
			this.state = STATE.INGEST_VERIFY_CHECKSUMS;
		} catch (FedoraTimeoutException e) { // on timeout poll for the ingested object
			log.info("Fedora Timeout Exception: " + e.getLocalizedMessage());
			this.state = STATE.INGEST_WAIT;
			return;
		} catch (FedoraException e) { // fedora threw a fault, ingest rejected
			throw fail("Cannot ingest due to Fedora error: " + pid + " " + foxmlFiles[next].getAbsolutePath(), e);
		}
	}

	public void init() {
		log.info("Ingest task created for " + baseDir.getAbsolutePath());
		try {
			dataDir = new File(this.getBaseDir(), "data");
			premisDir = new File(this.getBaseDir(), "premisEvents");
			File ingestLog = new File(this.getBaseDir(), INGEST_LOG);
			props = new IngestProperties(this.getBaseDir());
			foxmlFiles = this.getBaseDir().listFiles(new FilenameFilter() {
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
			for (ContainerPlacement p : props.getContainerPlacements().values()) {
				cSet.add(p.parentPID);
			}
			containers = cSet.toArray(new PID[] {});
			Arrays.sort(containers);

			if (ingestLog.exists()) { // this is a resume, find next foxml
				BufferedReader r = new BufferedReader(new FileReader(ingestLog));
				String lastLine = null;
				for (String line = r.readLine(); line != null; line = r.readLine()) {
					lastLine = line;
				}
				r.close();
				if (lastLine != null) {
					// format is tab separated: <pid>\tpath
					String[] l = lastLine.split("\\t");
					if (CONTAINER_UPDATED_CODE.equals(l[1])) {
						this.state = STATE.CONTAINER_UPDATES;
						this.lastIngestPID = new PID(l[0]);
					} else {
						this.lastIngestFilename = l[1];
						this.lastIngestPID = new PID(l[0]);
						this.state = STATE.INGEST_WAIT;
						log.info("Resuming ingest from " + this.lastIngestFilename + " in " + this.getBaseDir().getName());
					}
				}
			}
			this.ingestLogWriter = new BufferedWriter(new FileWriter(ingestLog, true));
			Agent submitter = this.getAgentFactory().findPersonByOnyen(props.getSubmitter(), false);
			if (submitter == null) {
				submitter = this.getAgentFactory().findSoftwareByName(props.getSubmitter());
				if (submitter == null) {
					throw fail("Cannot look up submitter");
				} else {
					log.warn("Ingest submitter is a software agent: " + submitter.getName() + " (" + submitter.getPID()
							+ ")");
				}
			}
			this.eventLogger = new PremisEventLogger(submitter);
			try {
				if (!this.managementClient.pollForObject(ContentModelHelper.Fedora_PID.FEDORA_OBJECT.getPID(), 30, 600)) {
					throw fail("Cannot poll a basic expected Fedora object: "
							+ ContentModelHelper.Fedora_PID.FEDORA_OBJECT.getPID().getPid());
				}
				HashSet<PID> containers = new HashSet<PID>();
				for (ContainerPlacement p : props.getContainerPlacements().values()) {
					containers.add(p.parentPID);
				}
				for (PID container : containers) {
					if (!this.managementClient.pollForObject(container, 10, 30)) {
						throw fail("Cannot find existing container: " + container);
					}
				}
				this.state = STATE.INGEST;
			} catch (InterruptedException e) {
				log.debug("halting task due to interrupt", e);
				this.halting = true;
			}
		} catch (Exception e) {
			throw fail("Cannot initialize the ingest task.", e);
		}
	}

	public boolean isFailed() {
		return failed;
	}

	private void logIngestAttempt(PID pid, String filename) {
		try {
			this.ingestLogWriter.write(pid.getPid() + "\t" + filename);
			this.ingestLogWriter.flush();
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	private void logIngestComplete() {
		long ingestTime = System.currentTimeMillis() - ((lastIngestTime > 0) ? lastIngestTime : startTime);
		try {
			this.ingestLogWriter.write("\t" + ingestTime);
			this.ingestLogWriter.newLine();
			this.ingestLogWriter.flush();
		} catch (IOException e) {
			throw new Error(e);
		}
		lastIngestTime = System.currentTimeMillis();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		this.state = STATE.INIT;
		startTime = System.currentTimeMillis();
		init();
		while (this.state != STATE.FINISHED) {
			log.debug("Batch ingest: state=" + this.state + ", dir=" + this.getBaseDir());
			if (Thread.interrupted()) {
				log.debug("halting ingest task due to interrupt, in run method");
				this.halting = true;
			}
			if (this.halting && (this.state != STATE.SEND_MESSAGES && this.state != STATE.CLEANUP)) {
				log.debug("Halting this batch ingest task: state=" + this.state + ", dir=" + this.getBaseDir());
				break; // stop immediately as long as not sending msgs or cleaning up.
			}
			try {
				switch (this.state) {
					case INGEST: // ingest the next foxml file, until none left
						ingestNextObject();
						break;
					case INGEST_WAIT: // poll for last ingested pid (and fedora availability)
						waitForLastIngest();
						break;
					case INGEST_VERIFY_CHECKSUMS: // match local checksums against those generated by Fedora
						verifyLastIngestChecksums();
						break;
					case CONTAINER_UPDATES: // update parent container object
						updateNextContainer();
						break;
					case SEND_MESSAGES: // send cdr JMS and email for this AIP ingest
						sendIngestMessages();
						break;
					case CLEANUP:
						deleteDataFiles();
						handleFinishedDir();
						this.state = STATE.FINISHED;
						break;
				}
			} catch (BatchFailedException e) {
				log.error("Batch Ingest Task failed: " + e.getLocalizedMessage(), e);
				return;
			} catch(RuntimeException e) {
				log.error("Unexpected runtime exception", e);
				return;
			}
		}
	}

	/**
	 *
	 */
	private void handleFinishedDir() {
		try {
			if (this.saveFinishedBatches) {
				File finishedFolder = new File(this.baseDir.getParentFile().getParentFile(),
						BatchIngestQueue.FINISHED_SUBDIR);
				File dest = new File(finishedFolder, this.baseDir.getName());
				FileUtils.renameOrMoveTo(this.baseDir, dest);
			} else {
				FileUtils.deleteDir(baseDir);
			}
		} catch (IOException e) {
			throw new Error("Unexpected IO error on moving completed ingest batch.", e);
		}
	}

	/**
	 *
	 */
	private void deleteDataFiles() {
		if (this.dataDir != null) {
			log.debug("Deleting batch ingest data files: " + this.dataDir.getAbsolutePath());
			FileUtils.deleteDir(this.dataDir);
		}
		if (this.premisDir != null) {
			log.debug("Deleting batch ingest premis events files: " + this.premisDir.getAbsolutePath());
			FileUtils.deleteDir(this.premisDir);
		}
	}

	/**
	 *
	 */
	private void sendIngestMessages() {
		// load reordered existing children
		File reorderedFile = new File(this.getBaseDir(), REORDERED_LOG);
		if (reorderedFile.exists()) {
			List<PID> reordered = new ArrayList<PID>();
			BufferedReader r = null;
			try {

				r = new BufferedReader(new FileReader(reorderedFile));
				for (String line = r.readLine(); line != null; line = r.readLine()) {
					reordered.add(new PID(line));
				}
			} catch (IOException e) {
				throw new Error("Unexpected IO error.", e);
			} finally {
				try {
					if (r != null)
						r.close();
				} catch (IOException ignored) {
				}
			}
			Set<PID> containerSet = new HashSet<PID>();
			Collections.addAll(containerSet, this.containers);
			if (this.sendJmsMessages) {
				this.getOperationsMessageSender().sendAddOperation(props.getSubmitter(), containerSet,
						props.getContainerPlacements().keySet(), reordered);
			}
		}
		// send successful ingest email
		if (this.sendEmailMessages && this.mailNotifier != null && props.getEmailRecipients() != null)
			this.mailNotifier.sendIngestSuccessNotice(props, this.foxmlFiles.length);
		this.state = STATE.CLEANUP;
	}

	public void setAgentFactory(AgentFactory agentManager) {
		this.agentFactory = agentManager;
	}

	public void setIngestPollingDelaySeconds(int ingestPollingDelaySeconds) {
		this.ingestPollingDelaySeconds = ingestPollingDelaySeconds;
	}

	public void setIngestPollingTimeoutSeconds(int ingestPollingTimeoutSeconds) {
		this.ingestPollingTimeoutSeconds = ingestPollingTimeoutSeconds;
	}

	public void setMailNotifier(MailNotifier mailNotifier) {
		this.mailNotifier = mailNotifier;
	}

	public void setManagementClient(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}

	public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
		this.operationsMessageSender = operationsMessageSender;
	}

	/**
	 * Interrupts the batch ingest task as soon as possible. This task, or a new one for the same base dir, may be
	 * resumed.
	 */
	public void stop() {
		this.halting = true;
	}

	/**
	 *
	 */
	private void updateNextContainer() {
		int next = 0;
		if (this.lastIngestPID != null) {
			for (int i = 0; i < containers.length; i++) {
				if (containers[i].equals(this.lastIngestPID)) {
					next = i + 1;
					break;
				}
			}
		}
		if (next >= containers.length) { // no more to update
			log.debug("no containers left to update");
			this.state = STATE.SEND_MESSAGES;
			return;
		}
		PrintWriter reorderedWriter = null;
		try {
			if (submitterAgent == null) {
				submitterAgent = this.getAgentFactory().findPersonByOnyen(props.getSubmitter(), false);
			}
			reorderedWriter = new PrintWriter(new FileWriter(new File(this.getBaseDir(), REORDERED_LOG), true));
			logIngestAttempt(containers[next], CONTAINER_UPDATED_CODE);
			this.lastIngestPID = containers[next];
			// add RELS-EXT triples
			// update MD_CONTENTS
			// update MD_EVENTS
			List<PID> reordered = addContainerContents(submitterAgent, props.getContainerPlacements().values(),
					containers[next]);
			logIngestComplete();
			for (PID p : reordered) {
				reorderedWriter.write(p.getPid());
				reorderedWriter.write("\n");
			}
		} catch (FedoraException e) {
			throw fail("Cannot update container: " + containers[next], e);
		} catch (IOException e) {
			throw fail("Cannot update container: " + containers[next], e);
		} finally {
			reorderedWriter.flush();
			reorderedWriter.close();
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
	private void verifyLastIngestChecksums() {
		PID pid = this.lastIngestPID;
		Document xml = getFOXMLDocument(new File(getBaseDir(), this.lastIngestFilename));
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
			Datastream d;
			try {
				d = this.getManagementClient().getDatastream(pid, dsID);
			} catch (FedoraException e) {
				throw fail("Cannot get datastream metadata for checksum comparison:" + pid + dsID, e);
			}
			// compare checksums
			if (!dsID2md5.get(dsID).equals(d.getChecksum())) { // datastream doesn't match
				// TODO purge datastream and re-upload before failing?
				throw fail("Post-ingest checksum verification failed.  An ingested datastream did not match the supplied checksum.");
			}
			log.debug("Verified post-ingest checksum: " + pid.getPid() + " " + dsID);
		}
		logIngestComplete();
		this.state = STATE.INGEST;
	}

	/**
	 * Polls Fedora for the last ingested PID
	 */
	private void waitForLastIngest() {
		try {
			if (!this.managementClient.pollForObject(this.lastIngestPID, ingestPollingDelaySeconds,
					ingestPollingTimeoutSeconds)) {
				// TODO re-attempt last ingest before failing?
				throw fail("The last ingest before resuming was never completed. " + this.lastIngestPID + " in "
						+ this.lastIngestFilename);
			} else {
				log.debug("succeeded in finding last ingested fedora object:"+ this.lastIngestPID.getPid());
				this.state = STATE.INGEST_VERIFY_CHECKSUMS;
			}
		} catch (InterruptedException e) {
			log.debug("halting task due to interrupt", e);
			this.halting = true;
		}
	}

	public AccessClient getAccessClient() {
		return accessClient;
	}

	public void setAccessClient(AccessClient accessClient) {
		this.accessClient = accessClient;
	}

	/**
	 * @param baseDir
	 *           the baseDir to set
	 */
	public void setBaseDir(File baseDir) {
		this.baseDir = baseDir;
	}

	/**
	 * @return the baseDir
	 */
	public File getBaseDir() {
		return baseDir;
	}

	public boolean isSendJmsMessages() {
		return sendJmsMessages;
	}

	public void setSendJmsMessages(boolean sendJmsMessages) {
		this.sendJmsMessages = sendJmsMessages;
	}

	public boolean isSendEmailMessages() {
		return sendEmailMessages;
	}

	public void setSendEmailMessages(boolean sendEmailMessages) {
		this.sendEmailMessages = sendEmailMessages;
	}

	public boolean isSaveFinishedBatches() {
		return saveFinishedBatches;
	}

	public void setSaveFinishedBatches(boolean saveFinishedBatches) {
		this.saveFinishedBatches = saveFinishedBatches;
	}

}

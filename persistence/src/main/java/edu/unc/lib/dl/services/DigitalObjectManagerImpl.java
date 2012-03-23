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
package edu.unc.lib.dl.services;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.joda.time.DateTime;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.PersonAgent;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.ClientUtils;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.ManagementClient.ChecksumType;
import edu.unc.lib.dl.fedora.ManagementClient.State;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.ingest.aip.AIPIngestPipeline;
import edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage;
import edu.unc.lib.dl.ingest.aip.DepositRecord;
import edu.unc.lib.dl.ingest.sip.SIPProcessor;
import edu.unc.lib.dl.ingest.sip.SIPProcessorFactory;
import edu.unc.lib.dl.ingest.sip.SubmissionInformationPackage;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.update.UpdateException;
import edu.unc.lib.dl.util.Checksum;
import edu.unc.lib.dl.util.ContainerContentsHelper;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.FileUtils;
import edu.unc.lib.dl.util.IllegalRepositoryStateException;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.ModsXmlHelper;

/**
 * This class orchestrates the transactions that modify repository objects and update ancillary services.
 *
 * @author count0
 *
 */
public class DigitalObjectManagerImpl implements DigitalObjectManager {
	private static final Log log = LogFactory.getLog(DigitalObjectManagerImpl.class);
	private boolean available = false;
	private String availableMessage = "The repository manager is not available yet.";
	private AccessClient accessClient = null;
	private AIPIngestPipeline aipIngestPipeline = null;
	private ManagementClient managementClient = null;
	private SIPProcessorFactory sipProcessorFactory = null;
	private OperationsMessageSender operationsMessageSender = null;
	private TripleStoreQueryService tripleStoreQueryService = null;
	private SchematronValidator schematronValidator = null;
	private BatchIngestQueue batchIngestQueue = null;
	private BatchIngestTaskFactory batchIngestTaskFactory = null;
	private MailNotifier mailNotifier;

	public void setMailNotifier(MailNotifier mailNotifier) {
		this.mailNotifier = mailNotifier;
	}

	public synchronized void setAvailable(boolean available, String message) {
		this.available = available;
		this.availableMessage = message;
		if (!this.available) {
			this.mailNotifier.sendAdministratorMessage("DO manager became unavailable", message);
		}
	}

	public synchronized void setAvailable(boolean available) {
		this.setAvailable(available, "Repository undergoing maintenance, please contact staff for more information.");
	}

	public SchematronValidator getSchematronValidator() {
		return schematronValidator;
	}

	public void setSchematronValidator(SchematronValidator schematronValidator) {
		this.schematronValidator = schematronValidator;
	}

	public DigitalObjectManagerImpl() {
	}

	public IngestResult addToIngestQueue(SubmissionInformationPackage sip, DepositRecord record) throws IngestException {
		IngestResult result = new IngestResult();
		ArchivalInformationPackage aip = null;
		try {
			availableCheck();
			if (record.getDepositedBy() == null) {
				throw new IngestException("A user must be supplied for every ingest");
			}
			Agent user = record.getDepositedBy();
			// lookup the processor for this SIP
			SIPProcessor processor = this.getSipProcessorFactory().getSIPProcessor(sip);

			// process the SIP into a standard AIP
			aip = processor.createAIP(sip, record);

			// run routine AIP processing steps
			aip = this.getAipIngestPipeline().processAIP(aip);

			// persist the AIP to disk
			String submitter = null;
			if(PersonAgent.class.isInstance(user)) {
				submitter = ((PersonAgent)user).getOnyen();
				try {
					URI email = new URI(submitter+"@email.unc.edu");
					aip.setEmailRecipients(Collections.singletonList(email));
				} catch (URISyntaxException e) {
					log.error("Invalid onyen, cannot create email URI", e);
				}
			} else {
				submitter = user.getName();
			}
			aip.prepareIngest();

			// move the AIP into the ingest queue.
			this.getBatchIngestQueue().add(aip.getTempFOXDir());

			result.originalDepositID = aip.getDepositRecord().getPid();
			result.derivedPIDs = aip.getTopPIDs();
			return result;
		} catch (IngestException e) {
			// exception on AIP preparation, no transaction started
			log.error("Exception during submission or enqueuing", e);
			if(aip != null) {
				aip.delete();
			}
			throw e;
		}
	}

	private void availableCheck() throws IngestException {
		if (!this.available) {
			throw new IngestException(this.availableMessage + "  \nContact repository staff for assistance.");
		}
	}

	/**
	 * @param lastKnownGoodTime
	 * @param pids
	 */
	private void dumpRollbackInfo(DateTime lastKnownGoodTime, List<PID> pids, String reason) {
		StringBuffer sb = new StringBuffer();
		sb.append("DATA CORRUPTION LOG:\n").append("REASON:").append(reason).append("\n")
				.append("LAST KNOWN GOOD TIME: ").append(lastKnownGoodTime.toString()).append("\n").append(pids.size())
				.append(" FEDORA PIDS CREATED OR MODIFIED:\n");
		for (PID p : pids) {
			sb.append(p.getPid()).append("\n");
		}
		log.error(sb.toString());
	}

	public void addRelationship(PID subject, ContentModelHelper.Relationship rel, PID object) throws NotFoundException,
			IngestException {
		try {
			availableCheck();
			this.getManagementClient().addObjectRelationship(subject, rel.getURI().toString(), object);
		} catch (FedoraException e) {
			if (e instanceof NotFoundException) {
				throw (NotFoundException) e;
			} else {
				throw new Error("Unexpected Fedora fault when adding relationship.", e);
			}
		}
	}

	/**
	 * This method destroys a set of objects in Fedora, leaving no preservation data. It will update any ancillary
	 * services and log delete events.
	 *
	 * @param pids
	 *           the PIDs of the objects to purge
	 * @param message
	 *           the reason for the purge
	 * @return a list of PIDs that were purged
	 * @see edu.unc.lib.dl.services.DigitalObjectManager.purge()
	 */
	public List<PID> delete(PID pid, Agent user, String message) throws IngestException, NotFoundException {
		availableCheck();
		List<PID> deleted = new ArrayList<PID>();

		// FIXME disallow delete of "/Collections" and "/admin" folders
		// TODO add protected delete method for force initializing

		List<PID> toDelete = this.getTripleStoreQueryService().fetchAllContents(pid);
		toDelete.add(pid);

		// gathering delete set, checking for object relationships
		List<PID> refs = this.getReferencesToContents(pid);
		refs.removeAll(toDelete);
		if (refs.size() > 0) {
			StringBuffer s = new StringBuffer();
			s.append("Cannot delete ").append(pid).append(" because it will break object references from these PIDs: ");
			for (PID b : refs) {
				s.append("\t").append(b);
			}
			throw new IngestException(s.toString());
		}
		PID container = this.getTripleStoreQueryService().fetchContainer(pid);
		if (container == null & !pid.equals(ContentModelHelper.Administrative_PID.REPOSITORY.getPID())) {
			throw new IllegalRepositoryStateException("Cannot find a container for the specified object: " + pid);
		}

		// begin transaction, must delete all content and modify parent or dump
		// rollback info
		PremisEventLogger logger = new PremisEventLogger(user);
		DateTime transactionStart = new DateTime();
		Throwable thrown = null;
		List<PID> removed = new ArrayList<PID>();
		removed.add(pid);
		List<PID> reordered = new ArrayList<PID>();
		try {
			// update container
			this.removeFromContainer(pid, user, reordered);
			Element event = logger.logEvent(PremisEventLogger.Type.DELETION, "Deleted " + deleted.size()
					+ "contained object(s).", container);
			logger.addDetailedOutcome(event, "success", "Message: " + message, null);
			String logTimestamp = this.managementClient.writePremisEventsToFedoraObject(logger, container);

			// delete set of objects
			for (PID obj : toDelete) {
				try {
					this.getManagementClient().purgeObject(obj, message, false);
					deleted.add(obj);
				} catch (NotFoundException e) {
					log.error("Delete set referenced an object that didn't exist: " + pid.getPid(), e);
				}
			}
			if (this.getOperationsMessageSender() != null) {
				this.getOperationsMessageSender().sendRemoveOperation(user.getName(), container, removed, reordered);
			}
		} catch (FedoraException fault) {
			log.error("Fedora threw an unexpected fault while deleting " + pid.getPid(), fault);
			thrown = fault;
		} catch (RuntimeException e) {
			this.setAvailable(false);
			log.error("Fedora threw an unexpected runtime exception while deleting " + pid.getPid(), e);
			thrown = e;
		} finally {
			if (thrown != null && toDelete.size() > deleted.size()) {
				// some objects not deleted
				List<PID> missed = new ArrayList<PID>();
				missed.addAll(toDelete);
				missed.removeAll(deleted);
				this.dumpRollbackInfo(transactionStart, missed, "Could not complete delete of " + pid.getPid()
						+ ", please purge objects and check container " + container.getPid() + ".");
			}
		}
		if (thrown != null) {
			throw new IngestException("There was a problem completing the delete operation", thrown);
		}
		return deleted;
	}

	public AccessClient getAccessClient() {
		return accessClient;
	}

	public AIPIngestPipeline getAipIngestPipeline() {
		return aipIngestPipeline;
	}

	ManagementClient getManagementClient() {
		return managementClient;
	}

	/**
	 * Generates a list of referring object PIDs. Dependent objects are currently defined as those objects that refer to
	 * the specified pid in RELS-EXT other than it's container.
	 *
	 * @param pid
	 *           the object depended upon
	 * @return a list of dependent object PIDs
	 */
	private List<PID> getReferencesToContents(PID pid) {
		List<PID> refs = this.getTripleStoreQueryService().fetchObjectReferences(pid);

		if (!ContentModelHelper.Administrative_PID.REPOSITORY.equals(pid)) {
			PID container = this.getTripleStoreQueryService().fetchContainer(pid);
			refs.remove(container);
		}
		return refs;
	}

	public SIPProcessorFactory getSipProcessorFactory() {
		return sipProcessorFactory;
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.services.DigitalObjectManager#inactivate(edu.unc.lib.dl .fedora.PID, java.lang.String)
	 */
	// @Deprecated
	// public void inactivate(PID pid, Agent user, String message) throws
	// IngestException {
	// pid = this.tripleStoreQueryService.verify(pid);
	// List<PID> set = this.tripleStoreQueryService.fetchAllContents(pid);
	// set.add(pid);
	// List<PID> refs = this.getReferencesToContents(pid);
	// refs.removeAll(set); // remove internal refs
	// if (refs.size() > 0) {
	// StringBuffer s = new StringBuffer();
	// s.append("Cannot delete ").append(pid).append(" because it will break object references from these PIDs: ");
	// for (PID b : refs) {
	// s.append("\t").append(b);
	// }
	// throw new IngestException(s.toString());
	// }
	// PremisEventLogger logger = new PremisEventLogger(user);
	// for (PID p : set) {
	// // set inactive bit
	// logger.logEvent(PremisEventLogger.Type.DEACCESSION,
	// "Object state set to inactive", p);
	// try {
	// this.getManagementClient().modifyObject(p, null, null,
	// ManagementClient.State.INACTIVE, message);
	// this.writePremisEventsToFedoraObject(logger, p);
	// } catch (NotFoundException e) {
	// log.error("Tried to inactivate an object that was not found.", e);
	// }
	// }
	// if (this.getSolrIndexService() != null) {
	// this.getSolrIndexService().remove(set);
	// }
	// }

	/**
	 * This must be called after properties are set. It checks for basic repository objects and throws a runtime
	 * exception if they don't exist.
	 */
	public void init() {
		// throw a runtime exception?
	}

	public void purgeRelationship(PID subject, ContentModelHelper.Relationship rel, PID object)
			throws NotFoundException, IngestException {
		try {
			availableCheck();
			this.getManagementClient().purgeObjectRelationship(subject, rel.getURI().toString(), object);
		} catch (FedoraException e) {
			if (e instanceof NotFoundException) {
				throw (NotFoundException) e;
			} else {
				throw new Error("Unexpected Fedora fault when purging relationship.", e);
			}
		}
	}

	/**
	 * Just removes object from container, does not log this event. MUST finish operation or dump rollback info and
	 * rethrow exception.
	 *
	 * @param pid
	 *           the PID of the object to remove
	 * @param user
	 *           user removing the object
	 * @return the PID of the old container
	 * @throws FedoraException
	 */
	private PID removeFromContainer(PID pid, Agent user, Collection<PID> reordered) throws FedoraException {
		boolean relsextDone = false;
		PID parent = this.getTripleStoreQueryService().fetchContainer(pid);
		if (parent == null) {
			PID repoPID = this.getTripleStoreQueryService().fetchByRepositoryPath("/");
			if (pid.equals(repoPID)) {
				return null;
			} else {
				throw new NotFoundException("Found an object without a parent that is not the REPOSITORY");
			}
		}
		log.debug("removeFromContainer called on PID: " + parent.getPid());
		try {
			// remove ir:contains statement to RELS-EXT
			relsextDone = this.getManagementClient().purgeObjectRelationship(parent,
					ContentModelHelper.Relationship.contains.getURI().toString(), pid);
			if (relsextDone == false) {
				log.error("failed to purge relationship: " + parent + " contains " + pid);
			}
			// if the parent is a container, then make it orderly
			List<URI> cmtypes = this.getTripleStoreQueryService().lookupContentModels(parent);
			if (cmtypes.contains(ContentModelHelper.Model.CONTAINER.getURI())) {
				// edit Contents XML of parent container to append/insert
				Document newXML;
				Document oldXML;
				MIMETypedStream mts = this.getAccessClient().getDatastreamDissemination(parent, "MD_CONTENTS", null);
				ByteArrayInputStream bais = new ByteArrayInputStream(mts.getStream());
				oldXML = new SAXBuilder().build(bais);
				bais.close();
				newXML = ContainerContentsHelper.remove(oldXML, pid);
				this.getManagementClient().modifyInlineXMLDatastream(parent, "MD_CONTENTS", false,
						"removing child object from this container", new ArrayList<String>(), "List of Contents", newXML);
			}
		} catch (JDOMException e) {
			IllegalRepositoryStateException irs = new IllegalRepositoryStateException(
					"Invalid XML for container MD_CONTENTS: " + parent.getPid(), parent, e);
			log.error(irs);
			throw irs;
		} catch (IOException e) {
			throw new Error("Should not get IOException for reading byte array input", e);
		}
		return parent;
	}

	public void setAccessClient(AccessClient accessClient) {
		this.accessClient = accessClient;
	}

	public void setAipIngestPipeline(AIPIngestPipeline aipIngestPipeline) {
		this.aipIngestPipeline = aipIngestPipeline;
	}

	public void setManagementClient(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}

	public void setSipProcessorFactory(SIPProcessorFactory sipProcessorFactory) {
		this.sipProcessorFactory = sipProcessorFactory;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	@Override
	public String updateSourceData(PID pid, String dsid, File newDataFile, String checksum, String label,
			String mimetype, Agent user, String message) throws IngestException {
		availableCheck();
		String result = null;
		PremisEventLogger logger = new PremisEventLogger(user);

		// make sure the datastream is source data
		if (!this.getTripleStoreQueryService().isSourceData(pid, dsid)) {
			throw new IngestException("You can only update source datastreams.  (marked as <pid-uri> <"
					+ ContentModelHelper.CDRProperty.sourceData + "> <ds-uri> in RELS-EXT)");
		}

		// compare checksum if one is supplied
		if (checksum != null) {
			try {
				String sum = new Checksum().getChecksum(newDataFile);
				if (!checksum.trim().toLowerCase().equals(sum.toLowerCase())) {
					throw new IngestException("MD5 calculated for file (" + sum + ") does not match supplied checksum ("
							+ checksum + ")");
				} else {
					logger.logEvent(Type.DIGITAL_SIGNATURE_VALIDATION,
							"Validated MD5 signature for updated source data file", pid, dsid);
				}
			} catch (FileNotFoundException e1) {
				throw new IngestException("New source data file not found", e1);
			} catch (IOException e1) {
				throw new IngestException("There was a problem read the new source data file", e1);
			}
		}

		String newref = null;
		try {
			newref = this.getManagementClient().upload(newDataFile);
			result = this.getManagementClient().modifyDatastreamByReference(pid, dsid, false, message, null, label,
					mimetype, checksum, ChecksumType.MD5, newref);

			// TODO set cdr:indexText based on mime-type
			if (mimetype != null && mimetype.startsWith("text/")) {
				// triple: pid, ContentModelHelper.CDRProperty.indexText, dsURI
			}

			// update PREMIS log
			logger.logEvent(PremisEventLogger.Type.INGESTION, message, pid, dsid);
			this.managementClient.writePremisEventsToFedoraObject(logger, pid);
		} catch (FedoraException e) {
			throw new IngestException("Could not update the specified object.", e);
		}
		return result;
	}

	@Override
	public String updateDescription(PID pid, File newMODSFile, String checksum, Agent user, String message)
			throws IngestException {
		availableCheck();
		String result = null;
		PremisEventLogger logger = new PremisEventLogger(user);

		// compare checksum if one is supplied
		if (checksum != null) {
			try {
				String sum = new Checksum().getChecksum(newMODSFile);
				if (!checksum.trim().toLowerCase().equals(sum.toLowerCase())) {
					throw new IngestException("MD5 calculated for file (" + sum + ") does not match supplied checksum ("
							+ checksum + ")");
				} else {
					logger.logEvent(Type.DIGITAL_SIGNATURE_VALIDATION,
							"Validated MD5 signature for updated descriptive metadata file", pid, "MD_DESCRIPTIVE");
				}
			} catch (FileNotFoundException e1) {
				throw new IngestException("New MODS file not found", e1);
			} catch (IOException e1) {
				throw new IngestException("There was a problem reading the new MODS file", e1);
			}
		}

		// make sure the supplied XML is valid
		Element event = logger.logEvent(Type.VALIDATION, message, pid, "MD_DESCRIPTIVE");

		Source source;
		try {
			source = new StreamSource(new FileInputStream(newMODSFile));
		} catch (FileNotFoundException e1) {
			throw new Error("Unexpected exception", e1);
		}
		Document svrl = this.getSchematronValidator().validate(source, "vocabularies-mods");
		if (!this.getSchematronValidator().hasFailedAssertions(svrl)) {
			logger.addDetailedOutcome(event, "MODS is valid",
					"The supplied MODS metadata meets all CDR vocabulary requirements.", null);
		} else {
			logger.addDetailedOutcome(event, "MODS is not valid",
					"The supplied MODS metadata does not meet CDR vocabulary requirements.", svrl.detachRootElement());
			IngestException e = new IngestException(
					"The supplied descriptive metadata (MODS) does not meet CDR vocabulary requirements.");
			e.setErrorXML(logger.getAllEvents());
			throw e;
		}

		// handle case where MODS datastream not yet present
		boolean modsExists = false;
		try {
			this.getAccessClient().getDatastreamDissemination(pid, "MD_DESCRIPTIVE", null);
			modsExists = true;
		} catch (FedoraException ignored) {
		}

		String modsID = "MD_DESCRIPTIVE";
		String modsLabel = "Descriptive Metadata (MODS)";
		Document modsContent;
		try {
			modsContent = new SAXBuilder().build(newMODSFile);
		} catch (JDOMException e1) {
			throw new Error("Unexpected JDOM parse exception", e1);
		} catch (IOException e1) {
			throw new Error("Unexpected IOException", e1);
		}

		try {
			if (modsExists) {
				result = this.getManagementClient().modifyInlineXMLDatastream(pid, modsID, false, message,
						new ArrayList<String>(), modsLabel, modsContent);
				logger.logEvent(Type.INGESTION, message, pid, modsID);
			} else {
				result = this.getManagementClient().addInlineXMLDatastream(pid, modsID, false, message,
						new ArrayList<String>(), modsLabel, true, modsContent);
				logger.logEvent(Type.CREATION, message, pid, modsID);
			}
		} catch (FedoraException e) {
			throw new IngestException("Could not update the specified object.", e);
		}

		// update object label based on new MODS title
		String label = ModsXmlHelper.getFormattedLabelText(modsContent.getRootElement());
		if (label != null && label.trim().length() > 0) {
			try {
				this.getManagementClient().modifyObject(pid, label, "", State.ACTIVE, message);
			} catch (FedoraException e) {
				throw new IngestException("Could not update label for " + pid.getPid(), e);
			}
		}

		// Dublin Core crosswalk
		Document dc = new Document();
		try {
			dc = ModsXmlHelper.transform(modsContent.getRootElement());
			this.getManagementClient().modifyInlineXMLDatastream(pid, "DC", false, message, new ArrayList<String>(),
					"Internal XML Metadata", dc);
			String msg = "Metadata Object Description Schema (MODS) data transformed into Dublin Core (DC).";
			logger.logDerivationEvent(PremisEventLogger.Type.NORMALIZATION, msg, pid, "MD_DESCRIPTIVE", "DC");
		} catch (TransformerException e) {
			log.error("Cannot cross walk MODS to Dublin Core on update of " + pid.getPid(), e);
		} catch (FedoraException e) {
			log.error("Cannot cross walk MODS to Dublin Core on update of " + pid.getPid(), e);
		}

		// update PREMIS log
		try {
			this.managementClient.writePremisEventsToFedoraObject(logger, pid);
		} catch (FedoraException e) {
			log.error("Cannot log PREMIS events for " + pid.getPid(), e);
		}
		return result;
	}
	
	public String addOrReplaceDatastream(PID pid, Datastream datastream, File content, String mimetype, Agent user, String message) throws UpdateException {
		return addOrReplaceDatastream(pid, datastream, null, content, mimetype, user, message);
	}
	
	public String addOrReplaceDatastream(PID pid, Datastream datastream, String label, File content, String mimetype, Agent user, String message) throws UpdateException {
		String dsLabel = datastream.getLabel();
		if (label != null)
			dsLabel = label;
		List<String> datastreamNames = tripleStoreQueryService.listDisseminators(pid);
		try {
			if (datastream.getControlGroup().equals(ContentModelHelper.ControlGroup.INTERNAL)){
				//Handle inline datastreams
				if (datastreamNames.contains(datastream)){
					return this.managementClient.modifyDatastreamByValue(pid, datastream.getName(), false, message, new ArrayList<String>(),
							datastream.getLabel(), mimetype, null, null, content);
				} else {
					return this.managementClient.addInlineXMLDatastream(pid, datastream.getName(), false, message, new ArrayList<String>(),
							datastream.getLabel(), datastream.isVersionable(), content);
				}
			} else if (datastream.getControlGroup().equals(ContentModelHelper.ControlGroup.MANAGED)){
				//Handle managed datastreams
				String dsLocation = managementClient.upload(content);
				if (datastreamNames.contains(datastream)){
					return managementClient.modifyDatastreamByReference(pid, datastream.getName(), false, message, Collections.<String>emptyList(), dsLabel, mimetype, null, null, dsLocation);
				} else {
					return managementClient.addManagedDatastream(pid, datastream.getName(), false, message, Collections.<String>emptyList(), dsLabel, datastream.isVersionable(), mimetype, dsLocation);
				}
			}
		} catch (FedoraException e) {
			throw new UpdateException("Failed to modify datastream " + datastream.getName() + " for " + pid.getPid(), e);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.services.DigitalObjectManager#move(edu.unc.lib.dl.fedora .PID, java.lang.String)
	 */
	@Override
	public void move(List<PID> moving, String destinationPath, Agent user, String message) throws IngestException {
		availableCheck();
		PremisEventLogger logger = new PremisEventLogger(user);

		// destination exists
		PID destination = this.getTripleStoreQueryService().fetchByRepositoryPath(destinationPath);
		if (destination == null) {
			throw new IngestException("Cannot find the destination folder: " + destinationPath);
		}

		// TODO TEST destination is not the same as a moving object
		if (moving.contains(destination)) {
			throw new IngestException("Cannot move an object into itself: " + destinationPath);
		}

		// destination is container
		List<URI> cmtypes = this.getTripleStoreQueryService().lookupContentModels(destination);
		if (!cmtypes.contains(ContentModelHelper.Model.CONTAINER.getURI())) {
			throw new IngestException("The destination is not a folder: " + destinationPath + " " + destination.getPid());
		}

		// TODO TEST destination is not below a moving object, repo path must be
		// acyclic
		String myDestinationPath = this.getTripleStoreQueryService().lookupRepositoryPath(destination);
		for (PID p : moving) {
			String movingPath = this.getTripleStoreQueryService().lookupRepositoryPath(p);
			if (myDestinationPath.contains(movingPath)) {
				throw new IngestException("The destination folder is below one of the moving objects: " + myDestinationPath);
			}
		}

		// check for duplicate PIDs in incoming list
		Set<PID> noDups = new HashSet<PID>(moving.size());
		noDups.addAll(moving);
		if (noDups.size() < moving.size()) {
			throw new IngestException("The list of moving PIDs contains duplicates");
		} else {
			noDups = null;
		}

		// check for slug conflicts
		// build a hash of the slugs for existing children
		Map<String, PID> existingSlug2PID = this.getTripleStoreQueryService().fetchChildSlugs(destination);
		StringBuilder conflicts = new StringBuilder();
		for (PID pid : moving) {
			String slug = this.getTripleStoreQueryService().lookupSlug(pid);
			if (existingSlug2PID.containsKey(slug)) {
				conflicts.append("slug: ").append(slug).append("; existing child: ")
						.append(existingSlug2PID.get(slug).getPid()).append("; moving child: ").append(pid.getPid());
			}
		}
		if (conflicts.length() > 0) {
			conflicts.insert(0,
					"Some of the specified objects have slugs that conflict with existing children of the Container:\n");
			throw new IngestException(conflicts.toString());
		}

		// this is the beginning of the transaction
		DateTime knownGoodTime = new DateTime();
		Throwable thrown = null;
		boolean relationshipsUpdated = false;
		boolean destContentInventoryUpdated = false;
		boolean eventsLoggedOkay = false;
		String msgTimestamp = null;
		List<PID> oldParents = new ArrayList<PID>();
		List<PID> reordered = new ArrayList<PID>();
		try {
			// remove pids from old containers, add to new, log
			for (PID pid : moving) {
				PID oldParent = this.removeFromContainer(pid, user, reordered);
				oldParents.add(oldParent);
				this.getManagementClient().addObjectRelationship(destination,
						ContentModelHelper.Relationship.contains.getURI().toString(), pid);
				logger.logEvent(Type.MIGRATION,
						"object moved from Container " + oldParent.getPid() + " to " + destination.getPid(), pid);
			}

			// edit Contents XML of new parent container to append/insert
			Document newXML;
			Document oldXML;
			boolean exists = true;
			try {
				MIMETypedStream mts = this.getAccessClient().getDatastreamDissemination(destination, "MD_CONTENTS", null);
				ByteArrayInputStream bais = new ByteArrayInputStream(mts.getStream());
				try {
					oldXML = new SAXBuilder().build(bais);
					bais.close();
				} catch (JDOMException e) {
					throw new IllegalRepositoryStateException("Cannot parse MD_CONTENTS: " + destination);
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
			newXML = ContainerContentsHelper.addChildContentListInCustomOrder(oldXML, destination, moving, reordered);
			if (exists) {
				msgTimestamp = this.getManagementClient().modifyInlineXMLDatastream(destination, "MD_CONTENTS", false,
						"adding " + moving.size() + " child resources to container", new ArrayList<String>(),
						"List of Contents", newXML);
			} else {
				msgTimestamp = this.getManagementClient().addInlineXMLDatastream(destination, "MD_CONTENTS", false,
						"added " + moving.size() + " child resources to container", new ArrayList<String>(),
						"List of Contents", false, newXML);
			}
			destContentInventoryUpdated = true;
			// write the log events
			for (PID pid : moving) {
				this.managementClient.writePremisEventsToFedoraObject(logger, pid);
			}

			// send message for the operation
			if (this.getOperationsMessageSender() != null) {
				this.getOperationsMessageSender().sendMoveOperation(user.getName(), oldParents, destination, moving,
						reordered);
			}
		} catch (FedoraException e) {
			thrown = e;
		} catch (RuntimeException e) {
			thrown = e;
		} finally {
			if (thrown != null) {
				// some stuff failed to move, log it
				StringBuffer sb = new StringBuffer();
				sb.append("An error occured during a move operation.\n").append("all contains relationships updated: ")
						.append(relationshipsUpdated).append("\n").append("destination (").append(destination.getPid())
						.append(") content inventory updated: ").append(destContentInventoryUpdated)
						.append("events logged on moving objects: ").append(eventsLoggedOkay);
				this.dumpRollbackInfo(knownGoodTime, moving, sb.toString());
				throw new IngestException("There was a problem completing the move operation", thrown);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.services.DigitalObjectManager#isAvailable()
	 */
	@Override
	public boolean isAvailable() {
		return this.available;
	}

	public OperationsMessageSender getOperationsMessageSender() {
		return operationsMessageSender;
	}

	public void setOperationsMessageSender(OperationsMessageSender operationsMessageSender) {
		this.operationsMessageSender = operationsMessageSender;
	}

	@Override
	public IngestResult addWhileBlocking(SubmissionInformationPackage sip, DepositRecord record) throws IngestException {
		boolean reject = true;
		// normal SIP processing
		ArchivalInformationPackage aip = null;
		try {
			availableCheck();
			if (record.getDepositedBy() == null) {
				throw new IngestException("A user must be supplied for every ingest");
			}
			Agent user = record.getDepositedBy();
			// lookup the processor for this SIP
			SIPProcessor processor = this.getSipProcessorFactory().getSIPProcessor(sip);

			// process the SIP into a standard AIP
			aip = processor.createAIP(sip, record);
			
			// run routine AIP processing steps
			aip = this.getAipIngestPipeline().processAIP(aip);

			aip.setEmailRecipients(null); // no emails for blocking ingests

			// persist the AIP to disk
			aip.prepareIngest();

			// move the AIP into the ingest queue.
			// run ingest task immediately and wait for it.
			this.ingestBatchNow(aip.getTempFOXDir());

			// return the newly minted pid
			IngestResult result = new IngestResult();
			result.derivedPIDs = aip.getTopPIDs();
			result.originalDepositID = aip.getDepositRecord().getPid();
			return result;
		} catch (IngestException e) {
			// exception on AIP preparation, no transaction started
			log.info("User level exception on ingest, prior to Fedora transaction", e);
			throw e;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see edu.unc.lib.dl.services.BatchIngestServiceInterface#ingestBatchNow(java.io.File)
	 */
	public void ingestBatchNow(File prepDir) throws IngestException {
		// File queuedDir = moveToInstant(prepDir);
		// do not set marker file!
		log.info("Ingesting batch now, in parallel with queue: " + prepDir.getAbsolutePath());
		BatchIngestTask task = this.batchIngestTaskFactory.createTask();
		task.setBaseDir(prepDir);
		try {
			task.init();
		} catch(BatchFailedException e) {
			throw new IngestException("Batch ingest task failed", e);
		}
		task.run();
		task = null;
	}

	public BatchIngestQueue getBatchIngestQueue() {
		return batchIngestQueue;
	}

	public void setBatchIngestQueue(BatchIngestQueue batchIngestQueue) {
		this.batchIngestQueue = batchIngestQueue;
	}

	public BatchIngestTaskFactory getBatchIngestTaskFactory() {
		return batchIngestTaskFactory;
	}

	public void setBatchIngestTaskFactory(BatchIngestTaskFactory batchIngestTaskFactory) {
		this.batchIngestTaskFactory = batchIngestTaskFactory;
	}
}

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

import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.MD_CONTENTS;
import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.RELS_EXT;
import static edu.unc.lib.dl.util.ContentModelHelper.Relationship.contains;
import static edu.unc.lib.dl.util.ContentModelHelper.Relationship.removedChild;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMSource;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fedora.AccessClient;
import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.fedora.FedoraAccessControlService;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.ManagementClient.ChecksumType;
import edu.unc.lib.dl.fedora.ManagementClient.Format;
import edu.unc.lib.dl.fedora.ManagementClient.State;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.OptimisticLockException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.fedora.types.MIMETypedStream;
import edu.unc.lib.dl.ingest.IngestException;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.update.UpdateException;
import edu.unc.lib.dl.util.Checksum;
import edu.unc.lib.dl.util.ContainerContentsHelper;
import edu.unc.lib.dl.util.ContainerPlacement;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.ContentModelHelper.Model;
import edu.unc.lib.dl.util.IllegalRepositoryStateException;
import edu.unc.lib.dl.util.PremisEventLogger;
import edu.unc.lib.dl.util.PremisEventLogger.Type;
import edu.unc.lib.dl.util.TripleStoreQueryService;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil.ObjectProperty;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.JDOMQueryUtil;
import edu.unc.lib.dl.xml.ModsXmlHelper;

/**
 * This class orchestrates the transactions that modify repository objects and update ancillary services.
 *
 * @author count0
 *
 */
public class DigitalObjectManagerImpl implements DigitalObjectManager {
	private static final Logger log = LoggerFactory.getLogger(DigitalObjectManagerImpl.class);

	private boolean available = false;
	private String availableMessage = "The repository manager is not available yet.";
	private AccessClient accessClient = null;
	private ManagementClient forwardedManagementClient = null;
	private ManagementClient managementClient = null;
	private FedoraAccessControlService aclService = null;
	private OperationsMessageSender operationsMessageSender = null;
	private TripleStoreQueryService tripleStoreQueryService = null;
	private SchematronValidator schematronValidator = null;
	private PID collectionsPid = null;

	public synchronized void setAvailable(boolean available, String message) {
		this.available = available;
		this.availableMessage = message;
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

	@Override
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
	@Override
	public List<PID> delete(PID pid, String user, String message) throws IngestException, NotFoundException {
		availableCheck();

		// Prevent deletion of the repository object and the collections object
		if (pid.equals(ContentModelHelper.Administrative_PID.REPOSITORY.getPID()) || pid.equals(collectionsPid))
			throw new IllegalRepositoryStateException("Cannot delete administrative object: " + pid);

		List<PID> deleted = new ArrayList<PID>();

		// FIXME disallow delete of "/admin" folder
		// TODO add protected delete method for force initializing

		// Get all children and store for deletion
		List<PID> toDelete = this.getTripleStoreQueryService().fetchAllContents(pid);
		toDelete.add(pid);

		// gathering delete set, checking for object relationships
		// Find all relationships which refer to the pid being deleted
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
		if (container == null) {
			throw new IllegalRepositoryStateException("Cannot find a container for the specified object: " + pid);
		}

		// begin transaction, must delete all content and modify parent or dump
		// rollback info
		PremisEventLogger logger = new PremisEventLogger(user);

		DateTime transactionStart = new DateTime();
		Throwable thrown = null;
		List<PID> removed = new ArrayList<PID>();
		removed.add(pid);

		PID parent = null;
		try {
			// update container
			parent = this.removeFromContainer(pid);

			Element event = logger.logEvent(PremisEventLogger.Type.DELETION, "Deleted " + deleted.size()
					+ " contained object(s).", container);
			PremisEventLogger.addDetailedOutcome(event, "success", "Message: " + message, null);
			this.forwardedManagementClient.writePremisEventsToFedoraObject(logger, container);

			// delete object and all of its children
			for (PID obj : toDelete) {
				try {
					this.getManagementClient().purgeObject(obj, message, false);
					deleted.add(obj);
				} catch (NotFoundException e) {
					log.error("Delete set referenced an object that didn't exist: " + pid.getPid(), e);
				}
			}
			// Send message to message queue informing it of the deletion(s)
			if (this.getOperationsMessageSender() != null) {
				this.getOperationsMessageSender().sendRemoveOperation(user, container, removed, null);
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

	ManagementClient getManagementClient() {
		return forwardedManagementClient;
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

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	/**
	 * This must be called after properties are set. It checks for basic repository objects and throws a runtime
	 * exception if they don't exist.
	 */
	public void init() {
		// throw a runtime exception?
	}

	@Override
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
	 * @return the PID of the old container
	 * @throws FedoraException
	 */
	private PID removeFromContainer(PID pid) throws FedoraException {
		boolean relsextDone = false;
		PID parent = this.getTripleStoreQueryService().fetchContainer(pid);
		if (parent == null) {
			// Block removal of repo object
			if (ContentModelHelper.Administrative_PID.REPOSITORY.getPID().equals(pid))
				return null;
			throw new NotFoundException("Found an object without a parent that is not the REPOSITORY");
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
				try {
					Document newXML;
					Document oldXML;
					MIMETypedStream mts = this.getAccessClient().getDatastreamDissemination(parent, "MD_CONTENTS", null);
					try(ByteArrayInputStream bais = new ByteArrayInputStream(mts.getStream())) {
						oldXML = new SAXBuilder().build(bais);
					}
					newXML = ContainerContentsHelper.remove(oldXML, pid);
					this.getManagementClient().modifyInlineXMLDatastream(parent, "MD_CONTENTS", false,
							"removing child object from this container", new ArrayList<String>(), "List of Contents", newXML);
				} catch (NotFoundException e) {
					// MD_CONTENTS was not found, so we will assume this is an unordered container
				}
			}
		} catch (JDOMException e) {
			IllegalRepositoryStateException irs = new IllegalRepositoryStateException(
					"Invalid XML for container MD_CONTENTS: " + parent.getPid(), parent, e);
			log.error("Failed to parse XML", irs);
			throw irs;
		} catch (IOException e) {
			throw new Error("Should not get IOException for reading byte array input", e);
		}
		return parent;
	}

	public void setAccessClient(AccessClient accessClient) {
		this.accessClient = accessClient;
	}

	public void setForwardedManagementClient(ManagementClient managementClient) {
		this.forwardedManagementClient = managementClient;
	}

	public void setManagementClient(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	@Override
	public String updateSourceData(PID pid, String dsid, File newDataFile, String checksum, String label,
			String mimetype, String user, String message) throws IngestException {
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
			// Upload the new file and then update the datastream
			newref = this.getManagementClient().upload(newDataFile);
			result = this.getManagementClient().modifyDatastreamByReference(pid, dsid, false, message, null, label,
					mimetype, checksum, ChecksumType.MD5, newref);
			// update PREMIS log
			logger.logEvent(PremisEventLogger.Type.INGESTION, message, pid, dsid);
			this.forwardedManagementClient.writePremisEventsToFedoraObject(logger, pid);
		} catch (FedoraException e) {
			throw new IngestException("Could not update the specified object.", e);
		}
		return result;
	}

	@Override
	public String updateDescription(PID pid, File newMODSFile, String checksum, String user, String message)
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
			PremisEventLogger.addDetailedOutcome(event, "MODS is valid",
					"The supplied MODS metadata meets all CDR vocabulary requirements.", null);
		} else {
			PremisEventLogger.addDetailedOutcome(event, "MODS is not valid",
					"The supplied MODS metadata does not meet CDR vocabulary requirements.", svrl.detachRootElement());
			IngestException e = new IngestException(
					"The supplied descriptive metadata (MODS) does not meet CDR vocabulary requirements.");
			e.setErrorXML(logger.getAllEvents());
			throw e;
		}

		// Detect if MODS is present by retrieving it.
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
			this.forwardedManagementClient.writePremisEventsToFedoraObject(logger, pid);
		} catch (FedoraException e) {
			log.error("Cannot log PREMIS events for " + pid.getPid(), e);
		}
		return result;
	}

	@Override
	public String addOrReplaceDatastream(PID pid, Datastream datastream, File content, String mimetype, String user,
			String message) throws UpdateException {
		return addOrReplaceDatastream(pid, datastream, null, content, mimetype, user, message);
	}

	@Override
	public String addOrReplaceDatastream(PID pid, Datastream datastream, String label, File content, String mimetype,
			String user, String message) throws UpdateException {
		String dsLabel = datastream.getLabel();
		if (label != null)
			dsLabel = label;
		List<String> datastreamNames = tripleStoreQueryService.listDisseminators(pid);
		log.debug("Current datastreams: " + datastreamNames);
		String datastreamName = pid.getURI() + "/" + datastream.getName();
		log.debug("Adding or replacing datastream: " + datastreamName);
		try {
			if (datastream.getControlGroup().equals(ContentModelHelper.ControlGroup.INTERNAL)) {
				// Handle inline datastreams
				if (datastreamNames.contains(datastreamName)) {
					log.debug("Replacing preexisting internal datastream " + datastreamName);
					return this.forwardedManagementClient.modifyDatastreamByValue(pid, datastream.getName(), false, message,
							new ArrayList<String>(), datastream.getLabel(), mimetype, null, null, content);
				} else {
					log.debug("Adding internal datastream " + datastreamName);
					return this.forwardedManagementClient.addInlineXMLDatastream(pid, datastream.getName(), false, message,
							new ArrayList<String>(), datastream.getLabel(), datastream.isVersionable(), content);
				}
			} else if (datastream.getControlGroup().equals(ContentModelHelper.ControlGroup.MANAGED)) {
				// Handle managed datastreams
				String dsLocation = forwardedManagementClient.upload(content);
				if (datastreamNames.contains(datastreamName)) {
					log.debug("Replacing preexisting managed datastream " + datastreamName);
					return forwardedManagementClient.modifyDatastreamByReference(pid, datastream.getName(), false, message,
							Collections.<String> emptyList(), dsLabel, mimetype, null, null, dsLocation);
				} else {
					log.debug("Adding managed datastream " + datastreamName);
					return forwardedManagementClient.addManagedDatastream(pid, datastream.getName(), false, message,
							Collections.<String> emptyList(), dsLabel, datastream.isVersionable(), mimetype, dsLocation);
				}
			}
		} catch (FedoraException e) {
			throw new UpdateException("Failed to modify datastream " + datastream.getName() + " for " + pid.getPid(), e);
		}
		return null;
	}

	private class DatastreamDocument {
		private final Document document;
		private final String lastModified;

		public DatastreamDocument(Document document, String lastModified) {
			super();
			this.document = document;
			this.lastModified = lastModified;
		}

		public Document getDocument() {
			return document;
		}

		public String getLastModified() {
			return lastModified;
		}
	}


	/**
	 * Returns response containing the jdom document representing the datastream and the last modified date. If it does
	 * not exist, then null is returned. If the document cannot be parsed
	 *
	 * @param pid
	 * @param datastreamName
	 * @return
	 * @throws FedoraException
	 */
	private DatastreamDocument getXMLDatastreamIfExists(PID pid, String datastreamName) throws FedoraException {

		if (tripleStoreQueryService.hasDisseminator(pid, datastreamName)) {
			MIMETypedStream mts = accessClient.getDatastreamDissemination(pid, datastreamName, null);

			// Capture the last modified date of the datastream to return with the document
			// Fri, 06 Feb 2015 14:54:45 GMT
			String lastModified = null;
			for (edu.unc.lib.dl.fedora.types.Property header : mts.getHeader().getProperty()) {
				if ("Last-Modified".equals(header.getName())) {
					lastModified = header.getValue();
					break;
				}
			}

			try (ByteArrayInputStream bais = new ByteArrayInputStream(mts.getStream())) {
				Document dsDoc = new SAXBuilder().build(bais);
				return new DatastreamDocument(dsDoc, lastModified);
			} catch (JDOMException | IOException e) {
				throw new ServiceException("Failed to parse datastream " + datastreamName + " for object " + pid, e);
			}
		}

		return null;
	}

	@Override
	public void move(List<PID> moving, PID destination, String user, String message) throws IngestException {
		availableCheck();

		long startTime = System.currentTimeMillis();

		// Verify the destination exists
		List<PID> destinationPath = this.getTripleStoreQueryService().lookupRepositoryAncestorPids(destination);
		if (destinationPath == null || destinationPath.size() == 0) {
			throw new IngestException("Cannot find the destination folder: " + destinationPath);
		}

		// Verify the destination is a container
		List<URI> cmtypes = this.getTripleStoreQueryService().lookupContentModels(destination);
		if (!cmtypes.contains(ContentModelHelper.Model.CONTAINER.getURI())) {
			throw new IngestException("The destination is not a folder: " + destinationPath + " " + destination.getPid());
		}

		// Check that none of the items being moved are the destination or one of its ancestors
		for (PID pid : moving) {
			if (pid.equals(destination))
				throw new IngestException("The destination folder and one of the moving objects are the same: " + destination);
			for (PID destPid : destinationPath) {
				if (pid.equals(destPid))
					throw new IngestException("The destination folder is below one of the moving objects: " + destination);
			}
		}

		// Determine the set of parents for all of the PIDs to be moved
		Map<PID, List<PID>> sources = getChildrenContainerMap(moving);

		// Check that the user has permissions to add/remove from all sources and the destination
		AccessGroupSet groups = GroupsThreadStore.getGroups();
		List<PID> containerList = new ArrayList<>(sources.keySet());
		containerList.add(destination);
		for (PID container : containerList) {
			if (!aclService.hasAccess(container, groups, Permission.addRemoveContents)) {
				throw new IngestException("Insufficient permissions to perform move operation",
						new AuthorizationException("Cannot complete move operation, user " + user
								+ " does not have permission to modify " + container));
			}
		}

		// Get RELS-EXT documents for the set of parents and replace moved children with tombstones
		try {
			for (Entry<PID, List<PID>> sourceEntry : sources.entrySet()) {
				PID sourcePID = sourceEntry.getKey();
				// replace the moved children with tombstones and clear them out of MD_CONTENTS
				removeChildren(sourcePID, sourceEntry.getValue(), true);
			}
		} catch (Exception e) {
			log.error("Failed to remove children from sources during move, attempting to rollback", e);
			rollbackMove(sources);
			throw new IngestException("Failed to remove " + moving.size() + " objects from their source(s)", e);
		}

		List<PID> reordered = new ArrayList<>();
		// Add the moved children to the destination RELS-EXT
		try {
			addChildren(destination, moving, reordered);
		} catch (Exception e) {
			// Unexpected failure during move, need to fail operation and roll back
			log.error("Failed to add children to destination {} during move, attempting to rollback", destination, e);
			rollbackMove(sources);
			throw new IngestException("Failed to move " + moving.size() + " objects into " + destination, e);
		}

		// Remove tombstones from source containers
		try {
			for (Entry<PID, List<PID>> sourceEntry : sources.entrySet()) {
				cleanupRemovedChildren(sourceEntry.getKey(), sourceEntry.getValue());
			}
		} catch (Exception e) {
			log.error("Failed to cleanup children tombstones from sources during move", e);
			rollbackMove(sources);
			throw new IngestException("Failed to cleanup move of " + moving.size() + " objects to " + destination, e);
		}

		log.info("Move operation of {} items to {} completed in {}ms",
				new Object[] { moving.size(), destination, (System.currentTimeMillis() - startTime) });

		// Send out notification message that the move has completed
		if (this.getOperationsMessageSender() != null) {
			this.getOperationsMessageSender().sendMoveOperation(user, sources.keySet(), destination, moving,
					reordered);
		}
	}

	private void rollbackMove(Map<PID, List<PID>> sources) throws IngestException {
		for (Entry<PID, List<PID>> sourceEntry : sources.entrySet()) {
			rollbackMove(sourceEntry.getKey(), sourceEntry.getValue());
		}
	}

	/**
	 * Attempts to rollback a failed move operation by returning part way moved objects to their original source
	 * container and cleaning up removal markers
	 *
	 * @param source
	 * @param moving
	 * @throws IngestException
	 */
	public void rollbackMove(PID source, List<PID> moving) throws IngestException {

		try {
			DatastreamDocument sourceRelsExtResp = getXMLDatastreamIfExists(source, RELS_EXT.getName());
			if (sourceRelsExtResp == null) {
				log.error("Failed to get source RELS-EXT while attempting to roll back move operating from {}", source);
				return;
			}
			Document sourceRelsExt = sourceRelsExtResp.getDocument();
			Set<PID> removedChildren = JDOMQueryUtil.getRelationSet(sourceRelsExt.getRootElement(), removedChild);

			if (removedChildren.size() == 0) {
				log.debug("No cleanup required for move operation to {}", source);
				return;
			}

			// Clean up the destination(s)
			// Determine where the children ended up getting moved to
			Map<PID, List<PID>> destinationMap = getChildrenContainerMap(moving);
			for (Entry<PID, List<PID>> destEntry : destinationMap.entrySet()) {
				// Remove all of the moved children from the destination they ended up in
				removeChildren(destEntry.getKey(), destEntry.getValue(), false);
			}

			List<PID> reordered = new ArrayList<>();

			// Add the children back to the source
			addChildren(source, new ArrayList<>(removedChildren), reordered);

			// Clean up the tombstones
			cleanupRemovedChildren(source, moving);

		} catch (FedoraException e) {
			log.error("Failed to automatically rollback move operation on source {}", source, e);
		}
	}

	/**
	 * Generates a map of children grouped up by common immediate parents
	 *
	 * @param moving
	 * @return
	 */
	private Map<PID, List<PID>> getChildrenContainerMap(Collection<PID> moving) {
		// Determine the set of parents for all of the PIDs to be moved
		Map<PID, List<PID>> childContainerMap = new HashMap<>();
		for (PID pid : moving) {
			PID source = this.tripleStoreQueryService.fetchContainer(pid);
			if (source == null) {
				log.warn("Attempting to move orphaned object {}", pid);
			} else {
				List<PID> moveFromSource = childContainerMap.get(source);
				if (moveFromSource == null) {
					moveFromSource = new ArrayList<>();
					childContainerMap.put(source, moveFromSource);
				}
				moveFromSource.add(pid);
			}
		}
		return childContainerMap;
	}

	/**
	 * Remove children from the provided list within the specified container. If replaceWithMarkers is true, then instead
	 * of removing the relations, they will be replaced with removedChild markers
	 *
	 * @param container
	 * @param children
	 * @param replaceWithMarkers
	 * @throws FedoraException
	 * @throws IngestException
	 */
	private void removeChildren(PID container, Collection<PID> children, boolean replaceWithMarkers)
			throws FedoraException, IngestException {
		removeRelsExt: do {
			DatastreamDocument relsExtResp = getXMLDatastreamIfExists(container, RELS_EXT.getName());
			if (relsExtResp == null) {
				throw new IngestException("Unable to retrieve RELS-EXT for " + container + ", aborting move operation");
			}
			Document relsExt = relsExtResp.getDocument();

			try {
				Element descriptionEl = relsExt.getDocument().getRootElement().getChild("Description", JDOMNamespaceUtil.RDF_NS);
				List<Element> containsEls = descriptionEl.getChildren(contains.name(), contains.getNamespace());

				Iterator<Element> containsIt = containsEls.iterator();
				while (containsIt.hasNext()) {
					Element containsEl = containsIt.next();
					PID childPID = new PID(containsEl.getAttributeValue("resource", JDOMNamespaceUtil.RDF_NS));

					if (children.contains(childPID)) {
						if (replaceWithMarkers) {
							// Switch the moved children to the tombstone relation
							containsEl.setName(removedChild.name());
						} else {
							// Remove the entry
							containsIt.remove();
						}
					}
				}

				if (log.isDebugEnabled()) {
					XMLOutputter outputter = new XMLOutputter(org.jdom2.output.Format.getPrettyFormat());
					log.info("Removing children from container{}:\n{}", container, outputter.outputString(relsExt));
				}

				managementClient.modifyDatastream(container, RELS_EXT.getName(),
						"Removing moved children", null, null, relsExtResp.getLastModified(), relsExt);
				break removeRelsExt;
			} catch (OptimisticLockException e) {
				log.debug("Unable to update RELS-EXT for {}, retrying", container, e);
			}
			// Repeat rels-ext update if the source changed since the datastream was retrieved
		} while (true);

		// Update source MD_CONTENTS to remove children if it is present
		removeMDContents: do {
			try {
				DatastreamDocument mdContents = getXMLDatastreamIfExists(container, MD_CONTENTS.getName());

				if (mdContents != null) {
					ContainerContentsHelper.remove(mdContents.getDocument(), children);

					if (log.isDebugEnabled()) {
						XMLOutputter outputter = new XMLOutputter(org.jdom2.output.Format.getPrettyFormat());
						log.info("MD_CONTENTS after removing from {}:\n{}", container,
								outputter.outputString(mdContents.getDocument()));
					}

					managementClient.modifyDatastream(container, MD_CONTENTS.getName(),
							"Removing " + children.size() + " moved children", null, null,
							mdContents.getLastModified(), mdContents.getDocument());
				}
				break removeMDContents;
			} catch (OptimisticLockException e) {
				log.debug("Unable to update RELS-EXT for {}, retrying", container, e);
			}
		} while (true);
	}

	/**
	 * Add a list of children to a container, updating MD_CONTENTS as well if present
	 *
	 * @param container
	 * @param moving
	 * @param reordered
	 * @throws FedoraException
	 * @throws IngestException
	 */
	private void addChildren(PID container, List<PID> moving, Collection<PID> reordered) throws FedoraException,
			IngestException {
		updateRelsExt: do {
			try {
				DatastreamDocument relsExtResp = getXMLDatastreamIfExists(container, RELS_EXT.getName());
				if (relsExtResp == null) {
					throw new IngestException("Unable to retrieve RELS-EXT for container " + container
							+ ", aborting move operation");
				}
				Document relsExt = relsExtResp.getDocument();

				Element descriptionEl = relsExt.getRootElement().getChild("Description", JDOMNamespaceUtil.RDF_NS);

				// Get the list of existing contains relations to avoid duplicate relations
				List<Element> containsEls = descriptionEl.getChildren(contains.name(), contains.getNamespace());
				Set<PID> existingChildren = new HashSet<>(containsEls.size());
				for (Element containsEl : containsEls) {
					existingChildren.add(new PID(containsEl.getAttributeValue("resource", JDOMNamespaceUtil.RDF_NS)));
				}

				// Add moved children (which are not duplicates) to container
				for (PID newChild : moving) {
					if (!existingChildren.contains(newChild)) {
						Element newChildEl = new Element(contains.name(), contains.getNamespace());
						newChildEl.setAttribute("resource", newChild.getURI(), JDOMNamespaceUtil.RDF_NS);
						descriptionEl.addContent(newChildEl);
					} else {
						log.warn("container {} already contained moved child {}", container, newChild);
					}
				}

				if (log.isDebugEnabled()) {
					XMLOutputter outputter = new XMLOutputter(org.jdom2.output.Format.getPrettyFormat());
					log.info("RELS-EXT after removing children from {}:\n{}", container, outputter.outputString(relsExt));
				}

				// Push changes out to the container container
				managementClient.modifyDatastream(container, RELS_EXT.getName(), "Adding moved children", null, null,
						relsExtResp.getLastModified(), relsExt);
				break updateRelsExt;
			} catch (OptimisticLockException e) {
				log.debug("Unable to update RELS-EXT for {}, retrying", container, e);
			}
		} while (true);

		updateMDContents: do {
			try {
				// Update MD_CONTENTS to add new children if it is present
				DatastreamDocument mdContentsResp = getXMLDatastreamIfExists(container, MD_CONTENTS.getName());

				if (mdContentsResp != null) {
					Document mdContents = ContainerContentsHelper.addChildContentListInCustomOrder(
							mdContentsResp.getDocument(), container, moving, reordered);

					if (log.isDebugEnabled()) {
						XMLOutputter outputter = new XMLOutputter(org.jdom2.output.Format.getPrettyFormat());
						log.info("MD_CONTENTS after adding children to {}:\n{}", container,
								outputter.outputString(mdContents));
					}

					managementClient.modifyDatastream(container, MD_CONTENTS.getName(), "Adding " + moving.size()
							+ " moved children", null, null, mdContentsResp.getLastModified(), mdContents);
				}
				break updateMDContents;
			} catch (OptimisticLockException e) {
				log.debug("Unable to update MD_CONTENTS for {}, retrying", container, e);
			}
		} while (true);
	}

	/**
	 * Cleanup removedChild references to a list of pids within a particular container.
	 *
	 * @param container
	 * @param children
	 * @throws IngestException
	 * @throws FedoraException
	 */
	private void cleanupRemovedChildren(PID container, List<PID> children) throws IngestException, FedoraException {

		updateRelsExt: do {
			// Get the current time before accessing RELS-EXT for use in optimistic locking
			DatastreamDocument relsExtResp = getXMLDatastreamIfExists(container, RELS_EXT.getName());
			if (relsExtResp == null) {
				throw new IngestException("Unable to retrieve RELS-EXT for " + container + ", aborting move operation");
			}
			Document relsExt = relsExtResp.getDocument();

			try {
				Element descriptionEl = relsExt.getRootElement().getChild("Description", JDOMNamespaceUtil.RDF_NS);
				List<Element> removedEls = descriptionEl.getChildren(removedChild.name(), removedChild.getNamespace());

				Iterator<Element> removedIt = removedEls.iterator();
				while (removedIt.hasNext()) {
					Element removedEl = removedIt.next();
					PID childPID = new PID(removedEl.getAttributeValue("resource", JDOMNamespaceUtil.RDF_NS));

					// Remove the tombstone if it belongs to this source
					if (children.contains(childPID)) {
						removedIt.remove();
					}
				}

				if (log.isDebugEnabled()) {
					XMLOutputter outputter = new XMLOutputter(org.jdom2.output.Format.getPrettyFormat());
					log.info("RELS-EXT after cleaning up children in {}:\n{}", container, outputter.outputString(relsExt));
				}

				managementClient.modifyDatastream(container, RELS_EXT.getName(), "Cleaning up moved children", null, null,
						relsExtResp.getLastModified(), relsExt);
				break updateRelsExt;
			} catch (OptimisticLockException e) {
				log.debug("Unable to update RELS-EXT for {}, retrying", container, e);
			}
			// Repeat rels-ext update if the source changed since the datastream was retrieved
		} while (true);
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
	public PID createContainer(String name, PID parent, Model extraModel,
			String user, byte[] mods) throws IngestException {
		PID containerPid = new PID("uuid:"+UUID.randomUUID());
		Document foxml = FOXMLJDOMUtil.makeFOXMLDocument(containerPid.getPid());
		FOXMLJDOMUtil.setProperty(foxml, ObjectProperty.label, name);
		PremisEventLogger logger = new PremisEventLogger(user);

		// MODS
		if (mods != null) {
			try(ByteArrayInputStream bais = new ByteArrayInputStream(mods)) {
				Document modsDoc = new SAXBuilder().build(bais);
				if(!this.getSchematronValidator().isValid(
						new JDOMSource(modsDoc), "vocabularies-mods")) {
					throw new IngestException("MODS was invalid against vocabularies");
				} else {
					Element event = logger.logEvent(Type.VALIDATION,
							"Validation of Controlled Vocabularies in Descriptive Metadata (MODS)",
							containerPid,
							"MD_DESCRIPTIVE");
					PremisEventLogger.addDetailedOutcome(event, "MODS is valid",
							"The supplied MODS metadata meets all CDR vocabulary requirements.", null);
				}
				Element modsEl = FOXMLJDOMUtil.makeInlineXMLDatastreamElement(
						ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(),
						ContentModelHelper.Datastream.MD_DESCRIPTIVE.getLabel(),
						ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName()+"1.0",
						modsDoc.detachRootElement(), true);
				foxml.getRootElement().addContent(modsEl);
			} catch (IOException e) {
				throw new Error(e);
			} catch (JDOMException e) {
				throw new IngestException("MODS records did not parse", e);
			}
		}

		// RELS
		Element rdfElement = new Element("RDF", JDOMNamespaceUtil.RDF_NS);
		Element descrElement = new Element("Description", JDOMNamespaceUtil.RDF_NS);
		descrElement.setAttribute("about", containerPid.getURI(), JDOMNamespaceUtil.RDF_NS);
		rdfElement.addContent(descrElement);
		descrElement.addContent(
				new Element("hasModel", JDOMNamespaceUtil.FEDORA_MODEL_NS).setAttribute(
						"resource",
						ContentModelHelper.Model.CONTAINER.getURI().toString(),
						JDOMNamespaceUtil.RDF_NS));
		descrElement.addContent(
				new Element("hasModel", JDOMNamespaceUtil.FEDORA_MODEL_NS).setAttribute(
						"resource",
						ContentModelHelper.Model.PRESERVEDOBJECT.getURI().toString(),
						JDOMNamespaceUtil.RDF_NS));
		if(extraModel != null) {
			descrElement.addContent(
					new Element("hasModel", JDOMNamespaceUtil.FEDORA_MODEL_NS).setAttribute(
							"resource",
							extraModel.getURI().toString(),
							JDOMNamespaceUtil.RDF_NS));
		}
		Element relsEl = FOXMLJDOMUtil.makeInlineXMLDatastreamElement(
				ContentModelHelper.Datastream.RELS_EXT.getName(),
				ContentModelHelper.Datastream.RELS_EXT.getLabel(),
				ContentModelHelper.Datastream.RELS_EXT.getName()+"1.0",
				rdfElement,
				ContentModelHelper.Datastream.RELS_EXT.isVersionable());
		foxml.getRootElement().addContent(relsEl);

		// PREMIS
		Element premisEl = new Element("premis", JDOMNamespaceUtil.PREMIS_V2_NS)
				.addContent(PremisEventLogger.getObjectElement(containerPid));
		logger.logEvent(Type.CREATION,
				"Container created", containerPid);
		logger.appendLogEvents(containerPid, premisEl);
		String premisLoc = forwardedManagementClient.upload(new Document(premisEl));
		Element premisDSEl = FOXMLJDOMUtil.makeLocatorDatastream(
				ContentModelHelper.Datastream.MD_EVENTS.getName(), "M", premisLoc, "text/xml", "URL",
				ContentModelHelper.Datastream.MD_EVENTS.getLabel(), false, null);
		foxml.getRootElement().addContent(premisDSEl);

		if(log.isDebugEnabled()) {
			log.debug(new XMLOutputter().outputString(foxml));
		}

		try {
			// Container update
			this.getManagementClient().addObjectRelationship(parent,
					ContentModelHelper.Relationship.contains.getURI().toString(), containerPid);
			try {
				MIMETypedStream mts = this.accessClient.getDatastreamDissemination(parent, Datastream.MD_CONTENTS.getName(), null);
				List<PID> reordered = new ArrayList<PID>();
				try(ByteArrayInputStream bais = new ByteArrayInputStream(mts.getStream())) {
					Document oldContents = new SAXBuilder().build(bais);
					ContainerPlacement cp = new ContainerPlacement();
					cp.label = name;
					cp.parentPID = parent;
					cp.pid = containerPid;
					Collection<ContainerPlacement> placements = Collections.singleton(cp);
					Document newContents = ContainerContentsHelper.addChildContentAIPInCustomOrder(oldContents, parent, placements, reordered);
					this.getManagementClient().modifyInlineXMLDatastream(parent, Datastream.MD_CONTENTS.getName(), false,
							"adding child resource to container", new ArrayList<String>(), Datastream.MD_CONTENTS.getLabel(), newContents);
				} catch (JDOMException e) {
					throw new IngestException("MD_CONTENTS is bad", e);
				} catch (IOException e) {
					throw new IngestException(e.getMessage(), e);
				}
			} catch(NotFoundException e) {
				// no MD_CONTENTS to update, skip it
			}
			forwardedManagementClient.ingest(foxml, Format.FOXML_1_1, "Container created via Admin UI");
		} catch (FedoraException e) {
			throw new IngestException("Failed to ingest container object", e);
		}

		return containerPid;
	}

	public void setCollectionsPid(PID collectionsPid) {
		this.collectionsPid = collectionsPid;
	}

	public FedoraAccessControlService getAclService() {
		return aclService;
	}

	public void setAclService(FedoraAccessControlService aclService) {
		this.aclService = aclService;
	}
}

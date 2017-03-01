/**
 * Copyright 2016 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.deposit.fcrepo4;

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.TECHNICAL_METADATA;
import static edu.unc.lib.dl.util.DepositConstants.TECHMD_DIR;
import static edu.unc.lib.dl.xml.NamespaceConstants.FITS_URI;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.deposit.work.DepositGraphUtils;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FedoraTransaction;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.rdf.IanaRelation;
import edu.unc.lib.dl.reporting.ActivityMetricsClient;
import edu.unc.lib.dl.util.DepositException;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 * Ingests all content objects in the deposit into the Fedora repository.
 * 
 * @author bbpennel
 *
 */
public class IngestContentObjectsJob extends AbstractDepositJob {
	private static final Logger log = LoggerFactory.getLogger(IngestContentObjectsJob.class);

	private boolean resumed;

	// The set of object paths from this deposit which already exist in the repository
	private Set<String> previouslyIngestedSet;

	@Autowired
	private ActivityMetricsClient metricsClient;

	private File techmdDir;

	public IngestContentObjectsJob() {
		super();
	}

	public IngestContentObjectsJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
	}

	public void init() {
		techmdDir = new File(getDepositDirectory(), TECHMD_DIR);
	}

	/**
	 * Determines how many operations will be required to complete this deposit.
	 * 
	 * @param depositBag
	 */
	private void calculateWorkRemaining(Bag depositBag) {
		List<String> contentPaths = new ArrayList<>();
		DepositGraphUtils.walkChildrenDepthFirst(depositBag, contentPaths, true);
		// Number of actions is the number of ingest objects plus deposit record
		setTotalClicks(contentPaths.size());

		// Deposit is restarting from part way through
		resumed = getDepositStatusFactory().isResumedDeposit(getDepositUUID());
		if (resumed) {
			populatePreviouslyIngested(contentPaths);
		}
	}

	/**
	 * Generates a list of objects in this deposit which already exist in the
	 * repository, updating the status of the job to indicate how much work is
	 * left
	 * 
	 * @param contentPaths
	 */
	private void populatePreviouslyIngested(List<String> contentPaths) {
		// Generate a list of object paths from this deposit already in fedora
		previouslyIngestedSet = new HashSet<>();
		for (String path : contentPaths) {
			if (repository.objectExists(PIDs.get(path))) {
				previouslyIngestedSet.add(path);
			}
		}
		// Update the ingested object count to reflect the number in the repo
		getJobStatusFactory().setCompletion(getJobUUID(), previouslyIngestedSet.size());
	}

	@Override
	public void runJob() {

		log.debug("Creating content AIPS for deposit {}", getDepositPID());

		Model model = getReadOnlyModel();

		log.debug("Ingesting content for deposit {} containing {} objects", getDepositPID());

		// Retrieve the object where this deposit will be ingested to.
		Map<String, String> depositStatus = getDepositStatus();
		String destinationPath = depositStatus.get(DepositField.containerId.name());
		PID destinationPid = PIDs.get(destinationPath);
		if (destinationPid == null) {
			failJob("Invalid destination URI", "The provide destination uri " + destinationPath
					+ " was not a valid repository path");
		}

		ContentObject destObj = repository.getContentObject(PIDs.get(
				depositStatus.get(DepositField.containerId.name())));
		if (!(destObj instanceof ContentContainerObject)) {
			failJob("Cannot add children to destination", "Cannot deposit to destination " + destObj.getPid().getRepositoryPath()
					+ ", types does not support children");
		}
		String groups = depositStatus.get(DepositField.permissionGroups.name());
		AccessGroupSet groupSet = new AccessGroupSet(groups);
		// Verify that the depositor is allow to ingest to the given destination
		if (!destObj.getAccessControls().hasPermission(groupSet, Permission.ingest)) {
			failJob("Cannot add children to destination",
					"Depositor does not have permissions to ingest to destination "
							+ destObj.getPid().getRepositoryPath());
		}

		Bag depositBag = model.getBag(getDepositPID().getRepositoryPath());

		calculateWorkRemaining(depositBag);

		// Mark the deposit as in progress, if it was not already
		if (!resumed) {
			getDepositStatusFactory().setIngestInprogress(getDepositUUID(), true);
		}

		// Ingest objects included in this deposit into the destination object
		try {
			ingestChildren((ContentContainerObject) destObj, depositBag);
		} catch (DepositException | FedoraException e) {
			failJob(e, "Failed to ingest content for deposit {0}", getDepositPID().getQualifiedId());
		}
	}

	/**
	 * Ingest the children of parentResc as children ContentObjects of destObj. 
	 * 
	 * @param destObj the repository object which children objects will be added to.
	 * @param parentResc the parent resource where children will listed from
	 * @throws DepositException
	 */
	private void ingestChildren(ContentContainerObject destObj, Resource parentResc) throws DepositException {
		NodeIterator iterator = getChildIterator(parentResc);
		// No more children, nothing further to do in this tree
		if (iterator == null) {
			return;
		}

		try {
			while (iterator.hasNext()) {
				Resource childResc = (Resource) iterator.next();

				// Ingest the child according to its object type
				if (childResc.hasProperty(RDF.type, Cdr.Folder)) {
					ingestFolder(destObj, parentResc, childResc);
				} else if (childResc.hasProperty(RDF.type, Cdr.Work)) {
					ingestWork(destObj, parentResc, childResc);
				} else if (childResc.hasProperty(RDF.type, Cdr.FileObject)) {
					if (destObj instanceof WorkObject) {
						// File object is being added to a work, go ahead
						ingestFileObject(destObj, parentResc, childResc);
					} else {
						// File object is a standalone, so construct a Work around it
						ingestFileObjectAsWork(destObj, parentResc, childResc);
					}
				}
			}
		} finally {
			iterator.close();
		}
	}

	/**
	 * Ingests the object in childResc as a FileObject into an existing
	 * WorkObject.
	 * 
	 * @param parent
	 * @param parentResc
	 * @param childResc
	 * @return
	 * @throws DepositException
	 */
	private void ingestFileObject(ContentObject parent, Resource parentResc, Resource childResc)
			throws DepositException {

		if (skipResumed(childResc)) {
			return;
		}

		// TODO add ACLs
		WorkObject work = (WorkObject) parent;
		FileObject obj = addFileToWork(work, childResc);
		// TODO add description to file object

		// Increment the count of objects deposited
		addClicks(1);

		log.info("Created file object {} for deposit {}", obj.getPid(), getDepositPID());
	}

	/**
	 * Ingests the file object represented by childResc as a FileObject wrapped
	 * in a newly constructed WorkObject. Descriptive information about the file
	 * is migrated to the WorkObject.
	 * 
	 * @param parent
	 * @param parentResc
	 * @param childResc
	 * @return
	 * @throws DepositException
	 */
	private void ingestFileObjectAsWork(ContentContainerObject parent, Resource parentResc, Resource childResc)
			throws DepositException {

		if (skipResumed(childResc)) {
			return;
		}

		PID childPid = PIDs.get(childResc.getURI());

		PID workPid = repository.mintContentPid();

		// Construct a model for the new work using some of the properties from the child
		Model workModel = ModelFactory.createDefaultModel();
		// TODO add ACLs from the original child
		String label = getPropertyValue(childResc, CdrDeposit.label);
		if (label == null) {
			label = getPropertyValue(childResc, CdrDeposit.stagingLocation);
			if (label == null) {
				// throw exception before NPE happens as stagingLocation is required
				throw new DepositException("No staging location provided for file object ("
						+ childResc.getURI() + ")");
			}
			label = new File(label).getName();
		}
		Resource workResc = workModel.createResource(workPid.getRepositoryPath());
		workResc.addProperty(DC.title, label);
		FedoraTransaction tx = repository.startTransaction();
		try {
			WorkObject newWork = repository.createWorkObject(workPid, workModel);

			// TODO add the FileObject's description to the work instead

			addFileToWork(newWork, childResc);
			// Set the file as the primary object for the generated work
			newWork.setPrimaryObject(childPid);

			// Add the newly created work to its parent
			parent.addMember(newWork);

			// Increment the count of objects deposited
			addClicks(1);

			log.info("Created work {} for file object {} for deposit {}",
					new Object[] {workPid, childPid, getDepositPID()});
		} catch(Exception e) {
			tx.cancel(e);
		} finally {
			tx.close();
		}
	}

	/**
	 * Ingests the object represented by childResc as a FileObject as the child
	 * of the given WorkObject, with the file properties provided with
	 * childResc.
	 * 
	 * @param work
	 * @param childResc
	 * @return
	 * @throws DepositException
	 */
	private FileObject addFileToWork(WorkObject work, Resource childResc) throws DepositException {
		PID childPid = PIDs.get(childResc.getURI());

		String stagingPath = getPropertyValue(childResc, CdrDeposit.stagingLocation);
		if (stagingPath == null) {
			// throw exception, child must be a file with a staging path
			throw new DepositException("No staging location provided for child ("
					+ childResc.getURI() + ") of Work object (" + work.getPid().getQualifiedId() + ")");
		}
		// Pull out file properties if they are present
		String mimetype = getPropertyValue(childResc, CdrDeposit.mimetype);
		// TODO accomodate either sha1 or md5 checksums being provided
		String sha1 = getPropertyValue(childResc, CdrDeposit.sha1sum);
		// String md5 = getPropertyValue(childResc, CdrDeposit.md5sum);
		String label = getPropertyValue(childResc, CdrDeposit.label);

		File file = new File(getStagedUri(stagingPath));

		String filename = label != null? label : file.getName();

		FileObject fileObj;
		try (InputStream fileStream = new FileInputStream(file)) {
			// Add the file to the work as the datafile of its own FileObject
			fileObj = work.addDataFile(childPid, fileStream, filename, mimetype, sha1);
			// Record the size of the file for throughput stats
			metricsClient.incrDepositFileThroughput(getDepositUUID(), file.length());

			log.info("Ingested file {} in {} for deposit {}", new Object[] {filename, childPid, getDepositPID()});
		} catch (FileNotFoundException e) {
			throw new DepositException("Data file missing for child (" + childPid.getQualifiedId()
					+ ") of work ("  + work.getPid().getQualifiedId() + "): " + stagingPath, e);
		} catch (IOException e) {
			throw new DepositException("Unable to close inputstream for binary " + childPid.getQualifiedId(), e);
		}

		// Add the FITS report for this file
		addFitsReport(fileObj);

		return fileObj;
	}

	private void addFitsReport(FileObject fileObj) throws DepositException {
		File fitsFile = new File(techmdDir, fileObj.getPid().getUUID() + ".xml");
		try (InputStream fitsStream = new FileInputStream(fitsFile)) {
			fileObj.addBinary(TECHNICAL_METADATA, fitsStream, fitsFile.getName(), "text/xml",
					IanaRelation.derivedfrom, DCTerms.conformsTo, createResource(FITS_URI));
		} catch (IOException e) {
			throw new DepositException("Unable to ingest technical metadata for "
					+ fileObj.getPid().getQualifiedId(), e);
		}
	}

	/**
	 * Ingest childResc as a FolderObject as a member of the provided parent object.
	 * 
	 * @param parent
	 * @param parentResc
	 * @param childResc
	 * @return
	 * @throws DepositException
	 */
	private void ingestFolder(ContentContainerObject parent, Resource parentResc, Resource childResc)
			throws DepositException {

		PID childPid = PIDs.get(childResc.getURI());
		FolderObject obj = null;
		if (skipResumed(childResc)) {
			// Resuming, retrieve the existing folder object
			obj = repository.getFolderObject(childPid);
		} else {
			// Create the new folder
			Model model = ModelFactory.createDefaultModel();
			Resource folderResc = model.getResource(childPid.getRepositoryPath());
			populateAIPProperties(childResc, folderResc);

			// TODO add ACLs
			FedoraTransaction tx = repository.startTransaction();
			try {
				obj = repository.createFolderObject(childPid, model);
				parent.addMember(obj);
				// TODO add description

				// Increment the count of objects deposited prior to adding children
				addClicks(1);

				log.info("Created folder object {} for deposit {}", childPid, getDepositPID());
			} catch(Exception e) {
				tx.cancel(e);
			} finally {
				tx.close();
			}
		}
		// ingest all children of the folder
		ingestChildren(obj, childResc);
	}

	/**
	 * Ingest childResc as a WorkObject containing all of its child objects, as
	 * a member of the provided parent object. Establishes the primaryObject
	 * relationship to one of its children if specified.
	 * 
	 * @param parent
	 * @param parentResc
	 * @param childResc
	 * @return
	 * @throws DepositException
	 */
	private void ingestWork(ContentContainerObject parent, Resource parentResc, Resource childResc)
			throws DepositException {
		PID childPid = PIDs.get(childResc.getURI());

		WorkObject obj;
		boolean skip = skipResumed(childResc);
		if (skip) {
			obj = repository.getWorkObject(childPid);
			ingestChildren(obj, childResc);

			// Avoid adding primaryObject relation for a resuming deposit if already present
			if (!obj.getResource().hasProperty(Cdr.primaryObject)) {
				// Set the primary object for this work if one was specified
				addPrimaryObject(obj, childResc);
			}
		} else {
			Model model = ModelFactory.createDefaultModel();
			Resource workResc = model.getResource(childPid.getRepositoryPath());
			populateAIPProperties(childResc, workResc);
			// send txid along with uris for the following actions
			FedoraTransaction tx = repository.startTransaction();
			// TODO add ACLs
			try {
				obj = repository.createWorkObject(childPid, model);
				parent.addMember(obj);
				// TODO add description

				// Increment the count of objects deposited prior to adding children
				addClicks(1);

				log.info("Created work object {} for deposit {}", childPid, getDepositPID());
				ingestChildren(obj, childResc);

				// Set the primary object for this work if one was specified
				addPrimaryObject(obj, childResc);
			} catch(Exception e) {
				tx.cancel(e);
			} finally {
				tx.close();
			}
		}
	}

	private void addPrimaryObject(WorkObject obj, Resource childResc) {
		Statement primaryStmt = childResc.getProperty(Cdr.primaryObject);
		if (primaryStmt != null) {
			String primaryPath = primaryStmt.getResource().getURI();
			obj.setPrimaryObject(PIDs.get(primaryPath));
		}
	}

	private void populateAIPProperties(Resource dResc, Resource aResc) {
		String label = getPropertyValue(dResc, CdrDeposit.label);
		if (label != null) {
			aResc.addProperty(DC.title, label);
		}
	}

	/**
	 * Returns true if the resource represents an object that does not need to
	 * be deposited due to having been previously ingested
	 * 
	 * @param resc
	 * @return
	 */
	private boolean skipResumed(Resource resc) {
		if (!resumed) {
			return false;
		}
		String path = resc.getURI();
		return previouslyIngestedSet.contains(path);
	}

	/**
	 * Get the String value of the specified property if present, or return null.
	 * 
	 * @param resc
	 * @param property
	 * @return
	 */
	private String getPropertyValue(Resource resc, Property property) {
		Statement stmt = resc.getProperty(property);
		if (stmt == null) {
			return null;
		}
		return stmt.getString();
	}

	/**
	 * Return an iterator for the children of the given resource, base on what
	 * type of container it is.
	 * 
	 * @param resc
	 * @return
	 */
	private NodeIterator getChildIterator(Resource resc) {
		if (resc.hasProperty(RDF.type, RDF.Bag)) {
			return resc.getModel().getBag(resc).iterator();
		} else if (resc.hasProperty(RDF.type, RDF.Seq)) {
			return resc.getModel().getSeq(resc).iterator();
		} else {
			return null;
		}
	}

	private void addAclProperties(Resource depositResc, Model aipModel) {
		// TODO add access control properties
	}
}

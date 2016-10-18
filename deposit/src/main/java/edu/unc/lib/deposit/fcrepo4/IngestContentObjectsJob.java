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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.deposit.work.DepositGraphUtils;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.reporting.ActivityMetricsClient;
import edu.unc.lib.dl.util.DepositException;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 * 
 * @author bbpennel
 *
 */
public class IngestContentObjectsJob extends AbstractDepositJob {
	private static final Logger log = LoggerFactory.getLogger(IngestContentObjectsJob.class);

	@Autowired
	private ActivityMetricsClient metricsClient;
	
	public IngestContentObjectsJob() {
		// TODO Auto-generated constructor stub
	}

	public IngestContentObjectsJob(String uuid, String depositUUID) {
		super(uuid, depositUUID);
		// TODO Auto-generated constructor stub
	}
	
	private void preprocessStructure() {
		
	}

	@Override
	public void runJob() {
		
		log.debug("Creating content AIPS for deposit {}", getDepositPID());
		
		Model model = getReadOnlyModel();
		
		// establish task size
		List<Resource> topDownObjects = DepositGraphUtils.getObjectsBreadthFirst(model, getDepositPID());
		setTotalClicks(topDownObjects.size());
		
		log.debug("Ingesting content for deposit {} containing {} objects", getDepositPID());
		
		// Build the deposit record object
		// DepositRecord depositRecord = repository.getDepositRecord(getDepositPID());
		
		Map<String, String> depositStatus = getDepositStatus();
		ContentObject destObj = repository.getContentObject(PIDs.get(
				depositStatus.get(DepositField.containerId.name())));
		
		Bag depositBag = model.getBag(getDepositPID().getRepositoryPath());
		try {
			ingestChildren(destObj, depositBag);
		} catch (DepositException e) {
			failJob(e, "Failed to ingest content for deposit {}", getDepositPID().getQualifiedId());
		}
	}
	
	/**
	 * Ingest the children of parentResc as child ContentObjects of destObj. 
	 * 
	 * @param destObj the repository object which children objects will be added to.
	 * @param parentResc the parent resource where children will listed from
	 * @throws DepositException
	 */
	private void ingestChildren(ContentObject destObj, Resource parentResc) throws DepositException {
		NodeIterator iterator = getChildIterator(parentResc);
		// No more children, nothing further to do in this tree
		if (iterator == null) {
			return;
		}
		
		try {
			while (iterator.hasNext()) {
				Resource childResc = (Resource) iterator.next();
				
				List<Resource> types = getResourceList(childResc, RDF.type);
				// Ingest the child according to its object type
				if (types.contains(Cdr.Folder)) {
					ingestFolder(destObj, parentResc, childResc);
				} else if (types.contains(Cdr.Work)) {
					ingestWork(destObj, parentResc, childResc);
				} else if (types.contains(Cdr.FileObject)) {
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
	
	private ContentObject ingestFileObject(ContentObject parent, Resource parentResc, Resource childResc)
			throws DepositException {
		Model model = ModelFactory.createDefaultModel();

		// TODO add ACLs
		WorkObject work = (WorkObject) parent;
		
		FileObject obj = addFileToWork(work, childResc);
		// TODO add description to file object
		
		return obj;
	}
	
	private ContentObject ingestFileObjectAsWork(ContentObject parent, Resource parentResc, Resource childResc)
			throws DepositException {
		
		PID childPid = PIDs.get(childResc.getURI());
		
		PID workPid = repository.mintContentPid();
		
		// Construct a model for the new work using some of the properties from the child
		Model workModel = ModelFactory.createDefaultModel();
		// TODO add ACLs from the original child
		WorkObject newWork = repository.createWorkObject(workPid, workModel);
		// TODO add the FileObject's description to the work instead
		
		addFileToWork(newWork, childResc);
		// Set the file as the primary object for the generated work
		newWork.setPrimaryObject(childPid);
		
		// Add the newly created work to its parent
		parent.addMember(newWork);
		
		return newWork;
	}
	
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
		String sha1 = getPropertyValue(childResc, CdrDeposit.sha1sum);
		
		File file = new File(getDepositDirectory(), stagingPath); 
		try (InputStream fileStream = new FileInputStream(file)) {
			
			// Add the file to the work as the datafile of its own FileObject
			return work.addDataFile(childPid, fileStream, file.getName(), mimetype, sha1);
		} catch (FileNotFoundException e) {
			throw new DepositException("Data file missing for child (" + childPid.getQualifiedId()
					+ ") of work ("  + work.getPid().getQualifiedId() + "): " + stagingPath, e);
		} catch (IOException e) {
			throw new DepositException("Unable to close inputstream for binary " + childPid.getQualifiedId(), e);
		}
	}
	
	private ContentObject ingestFolder(ContentObject parent, Resource parentResc, Resource childResc)
			throws DepositException {
		
		PID childPid = PIDs.get(childResc.getURI());
		
		Model model = ModelFactory.createDefaultModel();
		// TODO add ACLs
		
		FolderObject obj = repository.createFolderObject(childPid, model);
		parent.addMember(obj);
		// TODO add description
		
		ingestChildren(obj, childResc);
		
		return obj;
	}
	
	private ContentObject ingestWork(ContentObject parent, Resource parentResc, Resource childResc)
			throws DepositException {
		PID childPid = PIDs.get(childResc.getURI());
		
		Model model = ModelFactory.createDefaultModel();
		// TODO add ACLs
		
		WorkObject obj = repository.createWorkObject(childPid, model);
		parent.addMember(obj);
		// TODO add description
		
		ingestChildren(obj, childResc);
		
		// Set the primary object for this work if one was specified
		Statement primaryStmt = childResc.getProperty(Cdr.primaryObject);
		if (primaryStmt != null) {
			String primaryPath = primaryStmt.getResource().getURI();
			obj.setPrimaryObject(PIDs.get(primaryPath));
		}
		
		return obj;
	}
	
	private String getPropertyValue(Resource resc, Property property) {
		Statement stmt = resc.getProperty(property);
		if (stmt == null) {
			return null;
		}
		return stmt.getString();
	}
	
	private NodeIterator getChildIterator(Resource resc) {
		// Get an iterator for this resource's children, depending on what type of container it is.
		if (resc.hasProperty(RDF.type, RDF.Bag)) {
			return resc.getModel().getBag(resc).iterator();
		} else if (resc.hasProperty(RDF.type, RDF.Seq)) {
			return resc.getModel().getSeq(resc).iterator();
		} else {
			return null;
		}
	}
	
	private ContentObject depositFolder(Resource depositResc) {
		PID objPid = PIDs.get(depositResc.getURI());
		
		Model model = ModelFactory.createDefaultModel();
		
		// Create the folder object in fedora
		FolderObject obj = repository.createFolderObject(objPid, model);
		
		return obj;
	}
	
	private void addAclProperties(Resource depositResc, Model aipModel) {
		// TODO add access control properties
	}
	
	private List<Resource> getResourceList(Resource resc, Property property) {
		List<Resource> types = new ArrayList<>();
		
		for (StmtIterator it = resc.listProperties(RDF.type); it.hasNext();) {
			Statement stmt = it.nextStatement();
			
			types.add(stmt.getResource());
		}
		
		return types;
	}
	
}

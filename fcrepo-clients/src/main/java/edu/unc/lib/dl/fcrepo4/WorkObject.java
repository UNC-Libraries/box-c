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
package edu.unc.lib.dl.fcrepo4;

import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;

import java.io.InputStream;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.InvalidRelationshipException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.PcdmModels;

/**
 * A repository object which represents a single Work, and should contain one or
 * more data files. A work may have a single primary object which is considered
 * the main work file, in which case the other data files are considered to be
 * supplemental.
 * 
 * @author bbpennel
 *
 */
public class WorkObject extends ContentObject {

	public WorkObject(PID pid, Repository repository, RepositoryObjectDataLoader dataLoader) {
		super(pid, repository, dataLoader);
	}

	@Override
	public WorkObject validateType() throws FedoraException {
		if (!isType(Cdr.Work.toString())) {
			throw new ObjectTypeMismatchException("Object " + pid + " is not a Work object.");
		}
		return this;
	}

	/**
	 * Set the object with the given PID as the primary object for this Work.
	 * The primary object must be contained by this Work.
	 * 
	 * @param primaryPid
	 */
	public void setPrimaryObject(PID primaryPid) {
		// Check that the object is contained by this Work
		Resource resc = getResource();
		Resource primaryResc = createResource(primaryPid.getRepositoryPath());
		if (!resc.hasProperty(PcdmModels.hasMember, primaryResc)) {
			throw new InvalidRelationshipException("Primary object must be a member of the Work");
		}

		// Add the relation
		repository.createRelationship(pid, Cdr.primaryObject, primaryResc);
	}

	/**
	 * Get the primary object for this work if one is assigned, otherwise return null.
	 * 
	 * @return
	 */
	public FileObject getPrimaryObject() {
		Resource resc = getResource();
		// Find the primary object relation if it is present
		Statement primaryStmt = resc.getProperty(Cdr.primaryObject);
		if (primaryStmt == null) {
			return null;
		}

		return repository.getFileObject(
				PIDs.get(primaryStmt.getResource().getURI()));
	}

	@Override
	public ContentObject addMember(ContentObject member) throws ObjectTypeMismatchException {
		if (!(member instanceof FileObject)) {
			throw new ObjectTypeMismatchException("Cannot add object of type " + member.getClass().getName()
					+ " as a member of WorkObject " + pid.getQualifiedId());
		}
		
		repository.addMember(this, member);
		return this;
	}
	
	/**
	 * Adds a new file object containing the provided input stream as its original file.
	 * 
	 * @param contentStream
	 * @param filename
	 * @param mimetype
	 * @param sha1Checksum
	 * @return
	 */
	public FileObject addDataFile(String filename, InputStream contentStream, String mimetype,
			String sha1Checksum) {
		PID fileObjPid = repository.mintContentPid();

		return addDataFile(fileObjPid, contentStream, filename, mimetype, sha1Checksum);
	}

	/**
	 * Adds a new file object containing the provided input stream as its
	 * original file, using the provided pid as the identifier for the new
	 * FileObject.
	 * 
	 * @param childPid
	 * @param contentStream
	 * @param filename
	 * @param mimetype
	 * @param sha1Checksum
	 * @return
	 */
	public FileObject addDataFile(PID childPid, InputStream contentStream, String filename,
			String mimetype, String sha1Checksum) {

		// Get a PID for the new file object
		PID fileObjPid = repository.mintContentPid();

		// Create the file object
		FileObject fileObj = repository.createFileObject(fileObjPid, null);
		// Add the content to it as its original file
		fileObj.addOriginalFile(contentStream, filename, mimetype, sha1Checksum);

		// Add the new file object as a member of this Work
		repository.addMember(this, fileObj);

		return fileObj;
	}
}

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

import java.io.InputStream;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.fedora.InvalidRelationshipException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.PcdmModels;

/**
 * Represents a generic repository object within the main content tree.
 * 
 * @author bbpennel
 *
 */
public abstract class ContentObject extends RepositoryObject {

	protected ContentObject(PID pid, Repository repository, RepositoryObjectDataLoader dataLoader) {
		super(pid, repository, dataLoader);
	}

	/**
	 * Adds description information to this object, which includes a MODS record
	 * @param modsStream
	 * @return the FileObject containing the BinaryObject for the MODS
	 */
	public FileObject addDescription(InputStream modsStream) {
		FileObject fileObj = createFileObject();
		
		BinaryObject mods = fileObj.addOriginalFile(modsStream, null, "text/xml", null);
		repository.createRelationship(pid, PcdmModels.hasRelatedObject, fileObj.getResource());
		repository.createRelationship(pid, Cdr.hasMods, mods.getResource());
		return fileObj;
	}
	
	/**
	 * Adds description information to this object, which includes source metadata and a MODS record derived from it
	 * @param sourceMdStream
	 * @param sourceProfile, identifies the encoding, profile, and/or origins of the sourceMdStream using an identifier defined in edu.unc.lib.dl.util.MetadataProfileConstants
	 * @param modsStream
	 * @return a FileObject containing BinaryObjects for source metadata and MODS
	 * @throws InvalidRelationshipException in case no source profile was provided
	 */
	public FileObject addDescription(InputStream sourceMdStream, String sourceProfile,
			InputStream modsStream) throws InvalidRelationshipException {
		if (sourceProfile == null || sourceProfile == "") {
			throw new InvalidRelationshipException("No source profile was provided");
		}
		FileObject fileObj = createFileObject();
		
		BinaryObject orig = fileObj.addOriginalFile(sourceMdStream, null, "text/plain", null);
		repository.createProperty(orig.getPid(), Cdr.hasSourceMetadataProfile, sourceProfile);
		repository.createRelationship(orig.getPid(), RDF.type, Cdr.SourceMetadata);
		repository.createRelationship(pid, PcdmModels.hasRelatedObject, fileObj.getResource());
		
		BinaryObject mods = fileObj.addDerivative(null, modsStream, null, "text/plain", null);
		repository.createRelationship(pid, Cdr.hasMods, mods.getResource());
		
		return fileObj;
	}

	/**
	 * Gets the FileObject with source metadata and MODS for this object
	 * @return the FileObject 
	 */
	public FileObject getDescription() {
		Resource res = this.getResource();
		Statement s = res.getProperty(PcdmModels.hasRelatedObject);
		if (s != null) {
			PID fileObjPid = PIDs.get(s.getResource().getURI());
			return repository.getFileObject(fileObjPid);
		} else {
			return null;
		}
	}
	
	/**
	 * Gets the BinaryObject with the MODS for this object
	 * @return the BinaryObject
	 */
	public BinaryObject getMODS() {
		Resource res = this.getResource();
		Statement s = res.getProperty(Cdr.hasMods);
		if (s != null) {
			PID binPid = PIDs.get(s.getResource().getURI());
			return repository.getBinary(binPid);
		} else {
			return null;
		}
	}
	
	private FileObject createFileObject() {
		PID childPid = repository.mintContentPid();
		FileObject fileObj = repository.createFileObject(childPid, null);
		return fileObj;
	}

	/**
	 * Retrieve access control information for this content object.
	 * 
	 * @return
	 */
	public ObjectAccessControlsBean getAccessControls() {
		return dataLoader.getAccessControls(this);
	}
}

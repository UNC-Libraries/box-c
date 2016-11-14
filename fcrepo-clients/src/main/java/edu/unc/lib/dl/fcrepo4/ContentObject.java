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

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.PcdmModels;
import static com.hp.hpl.jena.rdf.model.ResourceFactory.createResource;

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

	public FileObject addDescription(InputStream modsStream) {
		FileObject fileObj = createFileObject();
		
		BinaryObject mods = fileObj.addOriginalFile(modsStream, null, "text/xml", null);
		repository.createRelationship(pid, PcdmModels.hasRelatedObject, fileObj.getResource());
		repository.createRelationship(pid, Cdr.hasMods, mods.getResource());
		return fileObj;
	}
	
	public FileObject addDescription(InputStream sourceMdStream, String sourceProfile,
			InputStream modsStream) {
		FileObject fileObj = createFileObject();
		
		BinaryObject orig = fileObj.addOriginalFile(sourceMdStream, null, "text/plain", null);
		repository.createRelationship(orig.getPid(), RDF.type, Cdr.SourceMetadata);
		repository.createRelationship(pid, PcdmModels.hasRelatedObject, fileObj.getResource());
		orig.getResource().addProperty(Cdr.hasSourceMetadataProfile, sourceProfile);
		
		BinaryObject mods = fileObj.addDerivative(null, modsStream, null, "text/plain", null);
		repository.createRelationship(pid, Cdr.hasMods, mods.getResource());
		
		return fileObj;
	}

	public FileObject getDescription() {
		Resource sourceMd = this.getResource();
		Statement s = sourceMd.getProperty(PcdmModels.hasRelatedObject);
		if (s != null) {
			PID fileObjPid = PIDs.get(s.getResource().getURI());
			return repository.getFileObject(fileObjPid);
		} else {
			return null;
		}
	}
	
	public BinaryObject getMODS() {
		Resource mods = this.getResource();
		Statement s = mods.getProperty(Cdr.hasMods);
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
}

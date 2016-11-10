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

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.IanaRelation;
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

	public FileObject addDescription(InputStream modsStream) {
		FileObject fileObj = createFileObject();
		
		BinaryObject orig = fileObj.addOriginalFile(modsStream, null, "text/xml", null);
		repository.createRelationship(pid, IanaRelation.describedby, orig.getResource());
		
		return fileObj;
	}
	
	public FileObject addDescription(InputStream sourceMdStream, String sourceProfile,
			InputStream modsStream) {
		FileObject fileObj = createFileObject();
		
		BinaryObject orig = fileObj.addOriginalFile(sourceMdStream, null, "text/plain", null);
		repository.createRelationship(pid, PcdmModels.hasRelatedObject, orig.getResource());
		orig.getResource().addProperty(Cdr.hasSourceMetadataProfile, sourceProfile);
		
		BinaryObject deriv = fileObj.addDerivative(null, modsStream, null, "text/plain", null);
		repository.createRelationship(pid, IanaRelation.describedby, deriv.getResource());
		
		return fileObj;
	}

	public FileObject getDescription() {
		return null;
	}
	
	private FileObject createFileObject() {
		PID childPid = repository.mintContentPid();
		FileObject fileObj = repository.createFileObject(childPid, null);
		return fileObj;
	}
}

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Ldp;

/**
 * A Deposit Record repository object, which tracks information pertaining to a single deposit.
 *
 * @author bbpennel
 *
 */
public class DepositRecord extends RepositoryObject {

	public DepositRecord(PID pid, Repository repository, RepositoryObjectDataLoader dataLoader) {
		super(pid, repository, dataLoader);
	}

	/**
	 * Adds the given file as a manifest for this deposit.
	 *
	 * @param manifest
	 * @return path to the newly created manifest object
	 */
	public String addManifest(File manifest, String mimetype) throws FedoraException {
		try (
				InputStream fis = new FileInputStream(manifest);
				FcrepoResponse response = repository.getClient()
						.post(pid.getRepositoryUri())
						.body(fis, mimetype)
						.filename(manifest.getName())
						.perform();
				) {
			return response.getLocation().toString();
		} catch (IOException e) {
			throw new FedoraException("Unable to create deposit record for " + pid.getRepositoryUri(), e);
		} catch (FcrepoOperationFailedException e) {
			throw ClientFaultResolver.resolve(e);
		}
	}

	public InputStream getManifest() {
		return null;
	}

	public Collection<String> listManifestPaths() throws FedoraException {
		Resource resource = getModel().getResource(pid.getRepositoryUri().toString());
		StmtIterator containsIt = resource.listProperties(Ldp.contains);

		List<String> paths = new ArrayList<>();
		while (containsIt.hasNext()) {
			String path = containsIt.next().getObject().toString();
			paths.add(path);
		}

		return paths;
	}

	public Collection<?> listDepositedObjects() {
		return null;
	}

	public DepositRecord addPremisEvents(Model model) {
		return (DepositRecord) super.addPremisEvents(model);
	}

	/**
	 * Ensure that the object retrieved has the DepositRecord type
	 */
	@Override
	public DepositRecord validateType() throws FedoraException {
		if (!isType(Cdr.DepositRecord.toString())) {
			throw new ObjectTypeMismatchException("Object " + pid + " is not a Deposit Record.");
		}
		return this;
	}

}

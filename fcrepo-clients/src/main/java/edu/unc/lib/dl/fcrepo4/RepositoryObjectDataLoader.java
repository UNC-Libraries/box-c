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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.utils.DateUtils;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;

/**
 * 
 * @author bbpennel
 *
 */
public class RepositoryObjectDataLoader {

	private Repository repository;
	
	private FcrepoClient client;
	
	/**
	 * Loads and assigns the RDF types for the given object
	 * 
	 * @param obj
	 * @return
	 * @throws FedoraException
	 */
	public RepositoryObjectDataLoader loadTypes(RepositoryObject obj) throws FedoraException {
		List<String> types = new ArrayList<>();
		// Iterate through all type properties and add to list
		Resource resc = obj.getModel().getResource(obj.getPid().getRepositoryUri().toString());
		StmtIterator it = resc.listProperties(RDF.type);
		while (it.hasNext()) {
			types.add(it.nextStatement().getResource().getURI());
		}

		obj.setTypes(types);

		return this;
	}

	/**
	 * Loads and assigns the model for direct relationships of the given
	 * repository object
	 * 
	 * @param obj
	 * @return
	 * @throws FedoraException
	 */
	public RepositoryObjectDataLoader loadModel(RepositoryObject obj) throws FedoraException {
		URI metadataUri = obj.getMetadataUri();
		Model model = repository.getObjectModel(metadataUri);

		obj.setModel(model);

		return this;
	}

	public RepositoryObjectDataLoader loadHeaders(RepositoryObject obj) throws FedoraException {
		PID pid = obj.getPid();

		try (FcrepoResponse response = getClient().head(pid.getRepositoryUri()).perform()) {
			if (response.getStatusCode() != HttpStatus.SC_OK) {
				throw new FedoraException("Received " + response.getStatusCode()
						+ " response while retrieving headers for " + pid.getRepositoryUri());
			}

			obj.setEtag(response.getHeaderValue("Etag"));
			String lastModString = response.getHeaderValue("Last-Modified");
			if (lastModString != null) {
				obj.setLastModified(DateUtils.parseDate(lastModString));
			}
		} catch (IOException e) {
			throw new FedoraException("Unable to create deposit record at " + pid.getRepositoryUri(), e);
		} catch (FcrepoOperationFailedException e) {
			throw ClientFaultResolver.resolve(e);
		}

		return this;
	}

	/**
	 * Retrieve the binary content for the given BinaryObject as an inputstream
	 * 
	 * @param obj
	 * @return
	 * @throws FedoraException
	 */
	public InputStream getBinaryStream(BinaryObject obj) throws FedoraException {
		PID pid = obj.getPid();

		try {
			FcrepoResponse response = getClient().get(pid.getRepositoryUri()).perform();
			return response.getBody();
		} catch (FcrepoOperationFailedException e) {
			throw ClientFaultResolver.resolve(e);
		}
	}

	public void setClient(FcrepoClient client) {
		this.client = client;
	}

	public FcrepoClient getClient() {
		return client;
	}

	public Repository getRepository() {
		return repository;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}
}

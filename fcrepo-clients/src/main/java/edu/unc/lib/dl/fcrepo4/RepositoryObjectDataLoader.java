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

import static edu.unc.lib.dl.util.RDFModelUtil.TURTLE_MIMETYPE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.jena.riot.Lang;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.PID;

/**
 * Data loader which retrieves repository data for objects.
 * 
 * @author bbpennel
 *
 */
public class RepositoryObjectDataLoader {
	private static final Logger log = LoggerFactory.getLogger(RepositoryObjectDataLoader.class);

	private Repository repository;

	private AccessControlService aclService;

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
		// If the object is up to date and has already loaded the model then we're done
		if (obj.hasModel() && obj.isUnmodified()) {
			log.debug("Object unchanged, reusing existing model for {}", obj.getPid());
			return this;
		}

		// Need to load the model from fedora
		try (FcrepoResponse response = getClient().get(metadataUri)
				.accept(TURTLE_MIMETYPE)
				.perform()) {

			log.debug("Retrieving new model for {}", obj.getPid());
			Model model = ModelFactory.createDefaultModel();
			model.read(response.getBody(), null, Lang.TURTLE.getName());

			// Store the fresh model
			obj.storeModel(model);

			// Store updated modification info to track if the object changes 
			obj.setEtag(parseEtag(response));

			return this;
		} catch (IOException e) {
			throw new FedoraException("Failed to read model for " + metadataUri, e);
		} catch (FcrepoOperationFailedException e) {
			throw ClientFaultResolver.resolve(e);
		}
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

	/**
	 * Retrieves the etag for the provided object
	 * 
	 * @param obj
	 * @return
	 */
	public String getEtag(RepositoryObject obj) {
		try (FcrepoResponse response = getClient().head(obj.getMetadataUri()).perform()) {
			if (response.getStatusCode() != HttpStatus.SC_OK) {
				throw new FedoraException("Received " + response.getStatusCode()
						+ " response while retrieving headers for " + obj.getPid().getRepositoryUri());
			}

			return parseEtag(response);
		} catch (IOException e) {
			throw new FedoraException("Unable to create deposit record at "
					+ obj.getPid().getRepositoryUri(), e);
		} catch (FcrepoOperationFailedException e) {
			throw ClientFaultResolver.resolve(e);
		}
	}

	/**
	 * Retrieve the ETag of the response, with surrounding quotes stripped.
	 * 
	 * @param response
	 * @return
	 */
	private static String parseEtag(FcrepoResponse response) {
		String etag = response.getHeaderValue("ETag");
		return etag.substring(1, etag.length() - 1);
	}

	/**
	 * Retrieve access control information for the given object
	 * 
	 * @param obj
	 * @return
	 */
	public ObjectAccessControlsBean getAccessControls(RepositoryObject obj) {
		return aclService.getObjectAccessControls(obj.getPid());
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

	public AccessControlService getAclService() {
		return aclService;
	}

	public void setAclService(AccessControlService aclService) {
		this.aclService = aclService;
	}
}

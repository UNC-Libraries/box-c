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
import java.net.URI;

import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import com.hp.hpl.jena.rdf.model.Model;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.RDFModelUtil;

/**
 * Creates objects in the repository matching specific object profile types.
 * 
 * @author bbpennel
 *
 */
public class RepositoryObjectFactory {

	private LdpContainerFactory ldpFactory;

	private FcrepoClient client;

	/**
	 * Creates a deposit record object structure at the given path with the
	 * properties specified in the provided model
	 * 
	 * @param path
	 * @param model
	 * @return
	 * @throws FedoraException
	 */
	public URI createDepositRecord(URI path, Model model) throws FedoraException {
		URI depositRecordUri;

		try (FcrepoResponse response = getClient().put(path)
				.body(RDFModelUtil.streamModel(model), "text/turtle")
				.perform()) {

			depositRecordUri = response.getLocation();

			// Add the manifests container
			ldpFactory.createDirectContainer(depositRecordUri, Cdr.hasManifest,
					RepositoryPathConstants.DEPOSIT_MANIFEST_CONTAINER);

			// Add the premis event container
			ldpFactory.createDirectContainer(depositRecordUri, Premis.hasEvent,
					RepositoryPathConstants.EVENTS_CONTAINER);
		} catch (IOException e) {
			throw new FedoraException("Unable to create deposit record at " + path, e);
		} catch (FcrepoOperationFailedException e) {
			throw ClientFaultResolver.resolve(e);
		}

		return depositRecordUri;
	}

	public URI createPremisEvent(URI objectUri, Model model) throws FedoraException {
		return null;
	}
	
	public void setClient(FcrepoClient client) {
		this.client = client;
	}

	public FcrepoClient getClient() {
		return client;
	}

	public LdpContainerFactory getLdpFactory() {
		return ldpFactory;
	}

	public void setLdpFactory(LdpContainerFactory ldpFactory) {
		this.ldpFactory = ldpFactory;
	}
}

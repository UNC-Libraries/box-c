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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

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
				.body(RDFModelUtil.streamModel(model), TURTLE_MIMETYPE)
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

	/**
	 * Creates a binary resource at the given path.
	 * 
	 * @param path
	 *            Repository path where the binary will be created
	 * @param slug
	 *            Name in the path for the binary resource. Optional.
	 * @param content
	 *            Input stream containing the binary content for this resource.
	 * @param filename
	 *            Filename of the binary content. Optional.
	 * @param mimetype
	 *            Mimetype of the content. Optional.
	 * @param checksum
	 *            SHA-1 digest of the content. Optional.
	 * @param model
	 *            Model containing additional triples to add to the new binary's
	 *            metadata. Optional
	 * @return URI of the newly created binary
	 * @throws FedoraException
	 */
	public URI createBinary(URI path, String slug, InputStream content, String filename, String mimetype, String checksum,
			Model model) throws FedoraException {
		if (content == null) {
			throw new IllegalArgumentException("Cannot create a binary object from a null content stream");
		}

		// Upload the binary and provided technical metadata
		URI resultUri;
		// Track the URI where metadata updates would be made to for this binary
		URI describedBy;
		try (FcrepoResponse response = getClient().post(path)
				.slug(slug)
				.body(content, mimetype)
				.filename(filename)
				.digest(checksum)
				.perform()) {

			resultUri = response.getLocation();
			describedBy = response.getLinkHeaders("describedby").get(0);
		} catch (IOException e) {
			throw new FedoraException("Unable to create binary at " + path, e);
		} catch (FcrepoOperationFailedException e) {
			throw ClientFaultResolver.resolve(e);
		}

		if (model == null) {
			return resultUri;
		}

		// If a model was provided, then add the triples to the new binary's metadata
		// Turn model into sparql update query
		String sparqlUpdate = RDFModelUtil.createSparqlInsert(model);
		InputStream sparqlStream = new ByteArrayInputStream(sparqlUpdate.getBytes(StandardCharsets.UTF_8));

		try (FcrepoResponse response = getClient().patch(describedBy)
				.body(sparqlStream)
				.perform()) {
		} catch (IOException e) {
			throw new FedoraException("Unable to add triples to binary at " + path, e);
		} catch (FcrepoOperationFailedException e) {
			throw ClientFaultResolver.resolve(e);
		}

		return resultUri;
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

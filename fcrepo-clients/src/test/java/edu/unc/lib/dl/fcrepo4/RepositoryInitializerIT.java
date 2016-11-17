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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.apache.http.HttpStatus;
import org.fcrepo.client.FcrepoResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.util.URIUtil;

/**
 * 
 * @author bbpennel
 *
 */
public class RepositoryInitializerIT extends AbstractFedoraIT {

	private RepositoryInitializer repoInitializer;

	@Autowired
	private Repository repository;

	@Autowired
	private RepositoryObjectFactory objFactory;

	@Before
	public void init() {
		repoInitializer = new RepositoryInitializer();
		repoInitializer.setObjFactory(objFactory);
		repoInitializer.setRepository(repository);
		repoInitializer.setFcrepoClient(client);
	}

	/**
	 * Ensure that expected objects were initialized
	 * 
	 * @throws Exception
	 */
	@Test
	public void fullInitializationTest() throws Exception {
		repoInitializer.initializeRepository();

		URI contentContainerUri = getContainerUri(RepositoryPathConstants.CONTENT_BASE);
		assertObjectExists(contentContainerUri);

		String contentRootString = URIUtil.join(
				contentContainerUri, RepositoryPathConstants.CONTENT_ROOT_ID);
		URI contentRootUri = URI.create(contentRootString);
		assertObjectExists(contentRootUri);
		Model crModel = repository.getObjectModel(contentRootUri);
		Resource crResc = crModel.getResource(contentRootUri.toString());
		assertTrue(crResc.hasProperty(RDF.type, Cdr.ContentRoot));

		URI depositContainerUri = getContainerUri(RepositoryPathConstants.DEPOSIT_RECORD_BASE);
		assertObjectExists(depositContainerUri);
	}

	/**
	 * Show that additional initialization calls after the first do not cause
	 * objects to be modified or recreated
	 * 
	 * @throws Exception
	 */
	@Test
	public void multipleInitializeTest() throws Exception {
		repoInitializer.initializeRepository();

		URI contentContainerUri = getContainerUri(RepositoryPathConstants.CONTENT_BASE);
		String contentContainerEtag = getEtag(contentContainerUri);

		String contentRootString = URIUtil.join(
				contentContainerUri, RepositoryPathConstants.CONTENT_ROOT_ID);
		URI contentRootUri = URI.create(contentRootString);
		String contentRootEtag = getEtag(contentRootUri);

		URI depositContainerUri = getContainerUri(RepositoryPathConstants.DEPOSIT_RECORD_BASE);
		String depositContainerEtag = getEtag(depositContainerUri);

		repoInitializer.initializeRepository();

		assertEquals("Content Container object changed after second initialization",
				contentContainerEtag, getEtag(contentContainerUri));
		assertEquals("Content Root object changed after second initialization",
				contentRootEtag, getEtag(contentRootUri));
		assertEquals("Deposit Container object changed after second initialization",
				depositContainerEtag, getEtag(depositContainerUri));
	}

	private String getEtag(URI uri) throws Exception {
		try (FcrepoResponse response = client.head(uri).perform()) {
			assertEquals(HttpStatus.SC_OK, response.getStatusCode());

			String etag = response.getHeaderValue("ETag");
			return etag.substring(1, etag.length() - 1);
		}
	}

	private URI getContainerUri(String id) {
		String containerString = URIUtil.join(repository.getFedoraBase(), id);
		return URI.create(containerString);
	}
}

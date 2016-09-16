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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.stream.Collectors;

import org.apache.activemq.util.ByteArrayInputStream;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoResponse;
import org.jgroups.util.UUID;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Ebucore;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.rdf.Ldp;
import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.util.RDFModelUtil;

/**
 * 
 * @author bbpennel
 *
 */
public class RepositoryObjectFactoryIT extends AbstractFedoraIT {

	private FcrepoClient client;
	private LdpContainerFactory ldpFactory;

	private RepositoryObjectFactory factory;

	@Before
	public void init() {
		client = FcrepoClient.client().build();

		ldpFactory = new LdpContainerFactory();
		ldpFactory.setClient(client);

		factory = new RepositoryObjectFactory();
		factory.setClient(client);
		factory.setLdpFactory(ldpFactory);
	}

	@Test
	public void createDepositRecordTest() throws Exception {
		String path = serverAddress + RepositoryPathConstants.DEPOSIT_RECORD_BASE + "/" + UUID.randomUUID().toString();
		URI uri = URI.create(path);

		Model model = ModelFactory.createDefaultModel();
		Resource resc = model.createResource(path);
		resc.addProperty(RDF.type, Cdr.DepositRecord);

		URI resultUri = factory.createDepositRecord(uri, model);
		assertEquals("Requested URI did not match result", uri, resultUri);

		try (FcrepoResponse resp = client.get(uri).perform()) {
			Model respModel = RDFModelUtil.createModel(resp.getBody());

			Resource respResc = respModel.getResource(path);
			assertTrue("Did not have deposit record type", respResc.hasProperty(RDF.type, Cdr.DepositRecord));

			String manifestPath = path + "/" + RepositoryPathConstants.DEPOSIT_MANIFEST_CONTAINER;
			assertTrue("Manifest container not created",
					respResc.hasProperty(Ldp.contains, createResource(manifestPath)));
			String eventPath = path + "/" + RepositoryPathConstants.EVENTS_CONTAINER;
			assertTrue("Event container not created", respResc.hasProperty(Ldp.contains, createResource(eventPath)));
		}
	}

	@Test
	public void createBinaryTest() throws Exception {
		String path = serverAddress + "binary_test";
		URI uri = URI.create(serverAddress);

		Model model = ModelFactory.createDefaultModel();
		Resource resc = model.createResource(path);
		resc.addProperty(RDF.type, PcdmModels.Object);

		String bodyString = "Test text";
		String filename = "test.txt";
		String mimetype = "text/plain";
		InputStream contentStream = new ByteArrayInputStream("Test text".getBytes());

		URI respUri = factory.createBinary(uri, "binary_test", contentStream, filename, mimetype, null, model);

		try (FcrepoResponse resp = client.get(respUri).perform()) {
			String respString = new BufferedReader(new InputStreamReader(resp.getBody())).lines()
					.collect(Collectors.joining("\n"));

			assertEquals("Binary content did not match submitted value", bodyString, respString);
		}

		// Verify that triples were added
		URI metadataUri = URI.create(path + "/fcr:metadata");
		try (FcrepoResponse resp = client.get(metadataUri).perform()) {
			Model respModel = RDFModelUtil.createModel(resp.getBody());
			Resource respResc = respModel.getResource(path);

			assertTrue(respResc.hasProperty(RDF.type, Fcrepo4Repository.Binary));
			assertTrue(respResc.hasProperty(RDF.type, PcdmModels.Object));

			assertEquals(respResc.getProperty(Ebucore.filename).getString(), filename);
			assertEquals(respResc.getProperty(Ebucore.hasMimeType).getString(), mimetype);
		}
	}
}

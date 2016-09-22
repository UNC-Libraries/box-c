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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.activemq.util.ByteArrayInputStream;
import org.fcrepo.client.FcrepoClient;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.PremisLogger;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

/**
 * 
 * @author bbpennel
 *
 */
public class DepositRecordIT extends AbstractFedoraIT {

	private FcrepoClient client;
	private LdpContainerFactory ldpFactory;

	private RepositoryObjectFactory factory;
	private RepositoryObjectDataLoader dataLoader;

	private Repository repository;

	private PID pid;

	@Before
	public void init() {
		client = FcrepoClient.client().build();

		ldpFactory = new LdpContainerFactory();
		ldpFactory.setClient(client);

		factory = new RepositoryObjectFactory();
		factory.setClient(client);
		factory.setLdpFactory(ldpFactory);

		dataLoader = new RepositoryObjectDataLoader();
		dataLoader.setClient(client);

		repository = new Repository();
		repository.setClient(client);
		repository.setRepositoryObjectDataLoader(dataLoader);
		repository.setRepositoryObjectFactory(factory);
		repository.setFedoraBase(serverAddress);

		PIDs.setRepository(repository);

		// Generate a new ID every time so that tests don't conflict
		pid = PIDs.get(RepositoryPathConstants.DEPOSIT_RECORD_BASE + "/" + UUID.randomUUID().toString());
	}

	@Test
	public void createDepositRecordTest() throws Exception {

		Model model = getDepositRecordModel();

		DepositRecord record = repository.createDepositRecord(pid, model);

		assertNotNull(record);

		assertTrue(record.getTypes().contains(Cdr.DepositRecord.getURI()));
	}

	@Test(expected = ObjectTypeMismatchException.class)
	public void getInvalidDepositRecord() throws Exception {

		Model model = ModelFactory.createDefaultModel();

		repository.createDepositRecord(pid, model);

		repository.getDepositRecord(pid);
	}

	@Test
	public void getDepositRecord() throws Exception {
		Model model = getDepositRecordModel();

		repository.createDepositRecord(pid, model);

		DepositRecord record = repository.getDepositRecord(pid);

		assertTrue(record.getTypes().contains(Cdr.DepositRecord.getURI()));
	}

	@Test
	public void addManifestsTest() throws Exception {

		Model model = getDepositRecordModel();

		DepositRecord record = repository.createDepositRecord(pid, model);

		String bodyString1 = "Manifest info";
		String filename1 = "manifest1.txt";
		String mimetype1 = "text/plain";
		InputStream contentStream1 = new ByteArrayInputStream(bodyString1.getBytes());
		BinaryObject manifest1 = record.addManifest(contentStream1, filename1, mimetype1);

		assertNotNull(manifest1);
		assertEquals(filename1, manifest1.getFilename());
		assertEquals(mimetype1, manifest1.getMimetype());

		String bodyString2 = "Second manifest";
		String filename2 = "manifest2.txt";
		String mimetype2 = "text/plain";
		InputStream contentStream2 = new ByteArrayInputStream(bodyString2.getBytes());
		BinaryObject manifest2 = record.addManifest(contentStream2, filename2, mimetype2);

		assertNotNull(manifest2);

		// Verify that listing returns all the expected manifests
		Collection<PID> manifestPids = record.listManifests();
		assertEquals("Incorrect number of manifests retrieved", 2, manifestPids.size());

		assertTrue("Manifest1 was not listed", manifestPids.contains(manifest1.getPid()));
		assertTrue("Manifest2 was not listed", manifestPids.contains(manifest2.getPid()));

		String respString1 = new BufferedReader(new InputStreamReader(manifest1.getBinaryStream()))
				.lines().collect(Collectors.joining("\n"));
		assertEquals("Manifest content did not match submitted value", bodyString1, respString1);

		// Verify that retrieving the manifest returns the correct object
		BinaryObject gotManifest2 = record.getManifest(manifest2.getPid());
		assertNotNull("Get manifest did not return", gotManifest2);
		assertEquals(filename2, gotManifest2.getFilename());
		assertEquals(mimetype2, gotManifest2.getMimetype());

		String respString2 = new BufferedReader(new InputStreamReader(manifest2.getBinaryStream()))
				.lines().collect(Collectors.joining("\n"));
		assertEquals("Manifest content did not match submitted value", bodyString2, respString2);
	}

	@Test
	public void addPremisEventsTest() throws Exception {
		Model model = getDepositRecordModel();
		
		PremisLogger logger = new PremisLogger(pid);
		logger.buildEvent(Premis.Ingestion)
				.addAuthorizingAgent(SoftwareAgent.depositService.toString())
				.addEventDetail("Event details")
				.write();
		logger.buildEvent(Premis.VirusCheck)
				.addSoftwareAgent(SoftwareAgent.clamav.toString())
				.write();
		
		repository.createDepositRecord(pid, model)
			.addPremisEvents(logger.getModel());

		DepositRecord record = repository.getDepositRecord(pid);

		assertTrue(record.getTypes().contains(Cdr.DepositRecord.getURI()));
	}
	
	private Model getDepositRecordModel() {
		Model model = ModelFactory.createDefaultModel();
		Resource resc = model.createResource(pid.getRepositoryUri().toString());
		resc.addProperty(RDF.type, Cdr.DepositRecord);
		
		return model;
	}
}

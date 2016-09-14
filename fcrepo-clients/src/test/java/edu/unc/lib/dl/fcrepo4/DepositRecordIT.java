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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

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

		Model model = ModelFactory.createDefaultModel();
		Resource resc = model.createResource(pid.getRepositoryUri().toString());
		resc.addProperty(RDF.type, Cdr.DepositRecord);

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
		Model model = ModelFactory.createDefaultModel();
		Resource resc = model.createResource(pid.getRepositoryUri().toString());
		resc.addProperty(RDF.type, Cdr.DepositRecord);

		repository.createDepositRecord(pid, model);

		DepositRecord record = repository.getDepositRecord(pid);

		assertTrue(record.getTypes().contains(Cdr.DepositRecord.getURI()));
	}
}

/**
 * Copyright 2017 The University of North Carolina at Chapel Hill
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

import static org.junit.Assert.*;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.DcElements;
import edu.unc.lib.dl.rdf.PcdmModels;

/**
 * 
 * @author harring
 *
 */
public class FedoraTransactionIT extends AbstractFedoraIT {
	
	private PID pid;
	private Model model;

	@Before
	public void init() {
		pid = repository.mintContentPid();
		
		model = ModelFactory.createDefaultModel();
		Resource resc = model.createResource(pid.getRepositoryPath());
		resc.addProperty(DcElements.title, "Folder Title");
	}
	
	@Test
	public void createTxTest() throws Exception {
		FedoraTransaction tx = repository.startTransaction();
		
		FolderObject obj = repository.createFolderObject(pid, model);

		assertTrue(FedoraTransaction.hasTxId());
		assertTrue(obj.getTypes().contains(Cdr.Folder.getURI()));
		assertTrue(obj.getTypes().contains(PcdmModels.Object.getURI()));
		assertEquals("Folder Title", obj.getResource()
				.getProperty(DcElements.title).getString());
		tx.close();
		assertFalse(FedoraTransaction.hasTxId());
		assertNull(tx.getTxUri());
	}
	
	@Test (expected = TransactionCancelledException.class)
	public void createRollbackTxTest() {
		FedoraTransaction tx = repository.startTransaction();
		
		repository.createFolderObject(pid, model);
		
		tx.cancel();
		
	}

}

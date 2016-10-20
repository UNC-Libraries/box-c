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
package edu.unc.lib.deposit.fcrepo4;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 * 
 * @author bbpennel
 *
 */
public class IngestContentObjectsJobIT extends AbstractFedoraDepositJobIT {

	private IngestContentObjectsJob job;
	
	private PID destinationPid;
	
	@Before
	public void init() throws Exception {
		
		job = new IngestContentObjectsJob();
		job.setDepositUUID(depositUUID);
		job.setDepositDirectory(depositDir);
		job.setRepository(repository);
		setField(job, "dataset", dataset);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "depositStatusFactory", depositStatusFactory);
		setField(job, "jobStatusFactory", jobStatusFactory);
		job.init();
		
		createBaseContainer(RepositoryPathConstants.CONTENT_BASE);
		// Create a destination folder where deposits will be ingested to
		setupDestination();
	}
	
	private void setupDestination() {
		destinationPid = repository.mintContentPid();
		repository.createFolderObject(destinationPid);
		
		Map<String, String> status = new HashMap<>();
		status.put(DepositField.containerId.name(), destinationPid.getRepositoryPath());
		depositStatusFactory.save(depositUUID, status);
	}
	
	@Test
	public void ingestEmptyFolderTest() {
		String label = "testfolder";
		
		// Construct the deposit model, containing a deposit with one empty folder
		Model model = job.getWritableModel();
		Bag depBag = model.createBag(depositPid.getRepositoryPath());
		
		// Constructing the folder in the deposit model with a title
		PID folderPid = repository.mintContentPid();
		Bag folderBag = model.createBag(folderPid.getRepositoryPath());
		folderBag.addProperty(RDF.type, Cdr.Folder);
		folderBag.addProperty(CdrDeposit.label, label);

		depBag.add(folderBag);
		job.closeModel();
		
		// Execute the ingest job
		job.run();
		
		// Verify that the destination has the folder added to it
		ContentObject destObj = repository.getContentObject(destinationPid);
		List<ContentObject> destMembers = destObj.getMembers();
		assertEquals("Incorrect number of children at destination", 1, destMembers.size());
		
		// Make sure that the folder is present and is actually a folder
		ContentObject mFolder = findContentObjectByPid(destMembers, folderPid);
		assertTrue("Child was not a folder", mFolder instanceof FolderObject);
		
		// Try directly retrieving the folder
		FolderObject folder = repository.getFolderObject(folderPid);
		// Verify that its title was set to the expected value
		String title = folder.getResource().getProperty(DC.title).getString();
		assertEquals("Folder title was not correctly set", label, title);
	}
}

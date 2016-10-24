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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.DC;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fcrepo4.WorkObject;
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

		FileUtils.copyDirectory(new File("src/test/resources/examples"), depositDir);
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

	@Test
	public void ingestWorkObjectTest() {
		String label = "testwork";

		// Construct the deposit model with work object
		Model model = job.getWritableModel();
		Bag depBag = model.createBag(depositPid.getRepositoryPath());

		// Constructing the folder in the deposit model with a title
		PID workPid = repository.mintContentPid();
		Bag workBag = model.createBag(workPid.getRepositoryPath());
		workBag.addProperty(RDF.type, Cdr.Work);
		workBag.addProperty(CdrDeposit.label, label);

		String mainLoc = "pdf.pdf";
		String mainMime = "application/pdf";
		String mainSha1 = "7185198c0f158a3b3caa3f387efa3df990d2a904";
		PID mainPid = addFileObject(workBag, mainLoc, mainMime, mainSha1);
		String supLoc = "text.txt";
		String supMime = "text/plain";
		PID supPid = addFileObject(workBag, supLoc, supMime, null);

		depBag.add(workBag);

		workBag.asResource().addProperty(Cdr.primaryObject,
				model.getResource(mainPid.getRepositoryPath()));

		job.closeModel();

		job.run();

		ContentObject destObj = repository.getContentObject(destinationPid);
		List<ContentObject> destMembers = destObj.getMembers();
		assertEquals("Incorrect number of children at destination", 1, destMembers.size());

		// Make sure that the folder is present and is actually a folder
		WorkObject mWork = (WorkObject) findContentObjectByPid(destMembers, workPid);

		String title = mWork.getResource().getProperty(DC.title).getString();
		assertEquals("Work title was not correctly set", label, title);

		// Verify that the properties of the primary object were added
		FileObject primaryObj = mWork.getPrimaryObject();
		BinaryObject primaryBinary = primaryObj.getOriginalFile();
		assertEquals(mainLoc, primaryBinary.getFilename());
		assertEquals("urn:sha1:" + mainSha1, primaryBinary.getChecksum());
		assertEquals(739L, primaryBinary.getFilesize().longValue());

		// Check the right number of members are present
		List<ContentObject> workMembers = mWork.getMembers();
		assertEquals("Incorrect number of members in work", 2, workMembers.size());
		FileObject supObj = (FileObject) findContentObjectByPid(workMembers, supPid);
		assertNotNull(supObj);

		// Verify the properties and content of the supplemental file
		BinaryObject supBinary = supObj.getOriginalFile();
		assertEquals(supLoc, supBinary.getFilename());
		assertNotNull(supBinary.getChecksum());
		assertEquals(4L, supBinary.getFilesize().longValue());
	}
	
	@Test
	public void ingestFileObjectAsWorkTest() {
		Model model = job.getWritableModel();
		Bag depBag = model.createBag(depositPid.getRepositoryPath());

		String mainLoc = "pdf.pdf";
		String mainMime = "application/pdf";
		String mainSha1 = "7185198c0f158a3b3caa3f387efa3df990d2a904";
		PID filePid = addFileObject(depBag, mainLoc, mainMime, mainSha1);

		job.closeModel();

		job.run();
		
		ContentObject destObj = repository.getContentObject(destinationPid);
		List<ContentObject> destMembers = destObj.getMembers();
		assertEquals("Incorrect number of children at destination", 1, destMembers.size());
		
		ContentObject mWork = destMembers.get(0);
		assertTrue("Wrapper object was not a work", mWork instanceof WorkObject);
		String workTitle = mWork.getResource().getProperty(DC.title).getString();
		assertEquals("Work title was not set to filename", mainLoc, workTitle);

		List<ContentObject> workMembers = mWork.getMembers();
		assertEquals("Incorrect number of children on work", 1, workMembers.size());
		
		FileObject primaryFile = ((WorkObject) mWork).getPrimaryObject();
		assertEquals("File object should have the originally provided pid",
				filePid, primaryFile.getPid());
		BinaryObject primaryBinary = primaryFile.getOriginalFile();
		assertEquals(mainLoc, primaryBinary.getFilename());
		assertEquals("urn:sha1:" + mainSha1, primaryBinary.getChecksum());
		assertEquals(739L, primaryBinary.getFilesize().longValue());
	}

	@Test(expected = JobFailedException.class)
	public void ingestFileObjectChecksumMismatch() {
		Model model = job.getWritableModel();
		Bag depBag = model.createBag(depositPid.getRepositoryPath());

		String mainLoc = "pdf.pdf";
		String mainMime = "application/pdf";
		String mainSha1 = "111111111111111111111111111111111111";
		addFileObject(depBag, mainLoc, mainMime, mainSha1);

		job.closeModel();

		job.run();
	}

	private PID addFileObject(Bag parent, String stagingLocation, String mimetype, String sha1) {
		PID filePid = repository.mintContentPid();

		Resource fileResc = parent.getModel().createResource(filePid.getRepositoryPath());
		fileResc.addProperty(RDF.type, Cdr.FileObject);
		fileResc.addProperty(CdrDeposit.stagingLocation, stagingLocation);
		fileResc.addProperty(CdrDeposit.mimetype, mimetype);
		if (sha1 != null) {
			fileResc.addProperty(CdrDeposit.sha1sum, sha1);
		}

		parent.add(fileResc);

		return filePid;
	}
}

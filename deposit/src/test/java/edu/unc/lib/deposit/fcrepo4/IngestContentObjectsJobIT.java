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
import static org.junit.Assert.fail;

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
import edu.unc.lib.dl.util.RedisWorkerConstants.JobField;

/**
 * 
 * @author bbpennel
 *
 */
public class IngestContentObjectsJobIT extends AbstractFedoraDepositJobIT {

	private IngestContentObjectsJob job;

	private PID destinationPid;

	private static final String FILE1_LOC = "pdf.pdf";
	private static final String FILE1_MIMETYPE = "application/pdf";
	private static final String FILE1_SHA1 = "7185198c0f158a3b3caa3f387efa3df990d2a904";
	private static final long FILE1_SIZE = 739L;
	private static final String FILE2_LOC = "text.txt";
	private static final String FILE2_MIMETYPE = "text/plain";
	private static final long FILE2_SIZE = 4L;

	@Before
	public void init() throws Exception {

		job = new IngestContentObjectsJob();
		job.setJobUUID(jobUUID);
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

	/**
	 * Test that a single folder can be created
	 */
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

		assertClickCount(1);
	}

	/**
	 * Test that work objects are created along with relationships to their children.
	 */
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

		PID mainPid = addFileObject(workBag, FILE1_LOC, FILE1_MIMETYPE, FILE1_SHA1);
		PID supPid = addFileObject(workBag, FILE2_LOC, FILE2_MIMETYPE, null);

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
		assertBinaryProperties(primaryObj, FILE1_LOC, FILE1_MIMETYPE, FILE1_SHA1, FILE1_SIZE);

		// Check the right number of members are present
		List<ContentObject> workMembers = mWork.getMembers();
		assertEquals("Incorrect number of members in work", 2, workMembers.size());
		FileObject supObj = (FileObject) findContentObjectByPid(workMembers, supPid);
		assertNotNull(supObj);

		// Verify the properties and content of the supplemental file
		assertBinaryProperties(supObj, FILE2_LOC, FILE2_MIMETYPE, null, FILE2_SIZE);

		assertClickCount(3);
	}

	/**
	 * Verify that when a file is ingested as the child of something other than
	 * a work (such as a folder in this case), that it is placed inside a
	 * generated work object along with the normal file object structure
	 */
	@Test
	public void ingestFileObjectAsWorkTest() {
		Model model = job.getWritableModel();
		Bag depBag = model.createBag(depositPid.getRepositoryPath());

		PID filePid = addFileObject(depBag, FILE1_LOC, FILE1_MIMETYPE, FILE1_SHA1);

		job.closeModel();

		job.run();

		ContentObject destObj = repository.getContentObject(destinationPid);
		List<ContentObject> destMembers = destObj.getMembers();
		assertEquals("Incorrect number of children at destination", 1, destMembers.size());

		ContentObject mWork = destMembers.get(0);
		assertTrue("Wrapper object was not a work", mWork instanceof WorkObject);
		String workTitle = mWork.getResource().getProperty(DC.title).getString();
		assertEquals("Work title was not set to filename", FILE1_LOC, workTitle);

		List<ContentObject> workMembers = mWork.getMembers();
		assertEquals("Incorrect number of children on work", 1, workMembers.size());

		FileObject primaryFile = ((WorkObject) mWork).getPrimaryObject();
		assertEquals("File object should have the originally provided pid",
				filePid, primaryFile.getPid());
		assertBinaryProperties(primaryFile, FILE1_LOC, FILE1_MIMETYPE, FILE1_SHA1, FILE1_SIZE);

		assertClickCount(1);
	}

	/**
	 * Ensure that deposit fails on a checksum mismatch for a single file deposit
	 */
	@Test(expected = JobFailedException.class)
	public void ingestFileObjectChecksumMismatch() {
		Model model = job.getWritableModel();
		Bag depBag = model.createBag(depositPid.getRepositoryPath());

		String badSha1 = "111111111111111111111111111111111111";
		addFileObject(depBag, FILE1_LOC, FILE1_MIMETYPE, badSha1);

		job.closeModel();

		job.run();
	}

	/**
	 * Verify that resuming a completed deposit doesn't result in extra objects
	 */
	@Test
	public void resumeCompletedWorkIngestTest() {
		ingestWorkObjectTest();

		job.run();

		ContentObject destObj = repository.getContentObject(destinationPid);
		List<ContentObject> destMembers = destObj.getMembers();
		assertEquals("Incorrect number of children at destination", 1, destMembers.size());

		List<ContentObject> workMembers = destMembers.get(0).getMembers();
		assertEquals("Incorrect number of members in work", 2, workMembers.size());

		assertClickCount(3);
	}

	/**
	 * Tests ingest of a folder with two files in it, which fails on the second
	 * child and then attempts to resume
	 */
	@Test
	public void resumeIngestFolderTest() {
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

		// Add children, where the second child is invalid due to missing location
		PID file1Pid = addFileObject(folderBag, FILE1_LOC, FILE1_MIMETYPE, FILE1_SHA1);
		PID file2Pid = addFileObject(folderBag, null, FILE2_MIMETYPE, null);

		job.closeModel();

		// Execute the ingest job
		try {
			job.run();
			fail();
		} catch (JobFailedException e) {
			// expected, lets continue
		}

		// Check that the folder and first child successfully made it in
		ContentObject destObj = repository.getContentObject(destinationPid);
		List<ContentObject> destMembersFailed = destObj.getMembers();
		assertEquals("Incorrect number of children at destination", 1, destMembersFailed.size());

		ContentObject mFolderFailed = destMembersFailed.get(0);
		List<ContentObject> folderMembersFailed = mFolderFailed.getMembers();
		assertEquals("Incorrect number of children in folder", 1, folderMembersFailed.size());

		// Fix the staging location of the second file
		model = job.getWritableModel();
		Resource file2Resc = model.getResource(file2Pid.getRepositoryPath());
		file2Resc.addProperty(CdrDeposit.stagingLocation, FILE2_LOC);
		job.closeModel();

		// Second run of job
		job.run();

		List<ContentObject> destMembers = destObj.getMembers();
		assertEquals("Incorrect number of children at destination", 1, destMembers.size());

		ContentObject mFolder = destMembers.get(0);
		List<ContentObject> folderMembers = mFolder.getMembers();
		assertEquals("Incorrect number of children in folder", 2, folderMembers.size());

		// Order of the children isn't guaranteed, so find by primary obj pid
		WorkObject workA = (WorkObject) folderMembers.get(0);
		WorkObject workB = (WorkObject) folderMembers.get(1);

		FileObject file1Obj;
		FileObject file2Obj;
		if (file1Pid.equals(workA.getPrimaryObject().getPid())) {
			file1Obj = workA.getPrimaryObject();
			file2Obj = workB.getPrimaryObject();
		} else {
			file2Obj = workA.getPrimaryObject();
			file1Obj = workB.getPrimaryObject();
		}

		assertBinaryProperties(file1Obj, FILE1_LOC, FILE1_MIMETYPE, FILE1_SHA1, FILE1_SIZE);
		assertBinaryProperties(file2Obj, FILE2_LOC, FILE2_MIMETYPE, null, FILE2_SIZE);

		assertClickCount(3);
	}

	/**
	 * Test a deeply nested hierarchy of folders with binary at the bottom of
	 * the tree
	 */
	@Test
	public void ingestDeepHierarchy() {
		Model model = job.getWritableModel();
		Bag depBag = model.createBag(depositPid.getRepositoryPath());

		// Constructing the nested folders
		int nestingDepth = 6;
		Bag previousBag = depBag;
		for (int i = 0; i < nestingDepth; i++) {
			PID folderPid = repository.mintContentPid();
			Bag folderBag = model.createBag(folderPid.getRepositoryPath());
			folderBag.addProperty(RDF.type, Cdr.Folder);
			previousBag.add(folderBag);
			previousBag = folderBag;
		}

		addFileObject(previousBag, FILE1_LOC, FILE1_MIMETYPE, FILE1_SHA1);

		job.closeModel();

		job.run();

		ContentObject destObj = repository.getContentObject(destinationPid);
		List<ContentObject> members = destObj.getMembers();

		for (int i = 0; i < nestingDepth; i++) {
			assertEquals("Incorrect number of children", 1, members.size());
			FolderObject folder = (FolderObject) members.get(0);
			members = folder.getMembers();
		}

		assertEquals("Incorrect number of children in last tier", 1, members.size());
		WorkObject work = (WorkObject) members.get(0);
		FileObject primaryFile = work.getPrimaryObject();
		assertBinaryProperties(primaryFile, FILE1_LOC, FILE1_MIMETYPE, FILE1_SHA1, FILE1_SIZE);

		assertClickCount(nestingDepth + 1);
	}

	private void assertBinaryProperties(FileObject fileObj, String loc, String mimetype,
			String sha1, long size) {
		BinaryObject binary = fileObj.getOriginalFile();
		assertEquals(loc, binary.getFilename());
		if (sha1 != null) {
			assertEquals("urn:sha1:" + sha1, binary.getChecksum());
		} else {
			assertNotNull(binary.getChecksum());
		}
		assertEquals(size, binary.getFilesize().longValue());
		assertEquals(mimetype, binary.getMimetype());
	}

	private void assertClickCount(int count) {
		Map<String, String> jobStatus = jobStatusFactory.get(jobUUID);
		assertEquals(count, Integer.parseInt(jobStatus.get(JobField.num.name())));
	}

	private PID addFileObject(Bag parent, String stagingLocation, String mimetype, String sha1) {
		PID filePid = repository.mintContentPid();

		Resource fileResc = parent.getModel().createResource(filePid.getRepositoryPath());
		fileResc.addProperty(RDF.type, Cdr.FileObject);
		if (stagingLocation != null) {
			fileResc.addProperty(CdrDeposit.stagingLocation, stagingLocation);
		}
		fileResc.addProperty(CdrDeposit.mimetype, mimetype);
		if (sha1 != null) {
			fileResc.addProperty(CdrDeposit.sha1sum, sha1);
		}

		parent.add(fileResc);

		return filePid;
	}
}

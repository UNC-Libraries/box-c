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
package edu.unc.lib.deposit.fcrepo4;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static edu.unc.lib.dl.util.DepositConstants.TECHMD_DIR;
import static edu.unc.lib.dl.xml.NamespaceConstants.FITS_URI;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.event.FilePremisLogger;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FedoraTransaction;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.PremisEventObject;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fcrepo4.TransactionCancelledException;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.JobField;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

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

	private File techmdDir;

	@Before
	public void init() throws Exception {

		job = new IngestContentObjectsJob();
		job.setJobUUID(jobUUID);
		job.setDepositUUID(depositUUID);
		job.setDepositDirectory(depositDir);
		job.setRepository(repository);
		job.setPremisLoggerFactory(premisLoggerFactory);
		setField(job, "dataset", dataset);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "depositStatusFactory", depositStatusFactory);
		setField(job, "jobStatusFactory", jobStatusFactory);
		setField(job, "metricsClient", metricsClient);
		job.init();

		createBaseContainer(RepositoryPathConstants.CONTENT_BASE);
		// Create a destination folder where deposits will be ingested to
		setupDestination();

		FileUtils.copyDirectory(new File("src/test/resources/examples"), depositDir);

		techmdDir = new File(depositDir, TECHMD_DIR);
		techmdDir.mkdir();
	}

	private void setupDestination() {
		destinationPid = repository.mintContentPid();
		repository.createFolderObject(destinationPid);

		Map<String, String> status = new HashMap<>();
		status.put(DepositField.containerId.name(), destinationPid.getRepositoryPath());
		status.put(DepositField.permissionGroups.name(), "depositor");
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
		List<ContentObject> destMembers = ((ContentContainerObject) destObj).getMembers();
		assertEquals("Incorrect number of children at destination", 1, destMembers.size());

		// Make sure that the folder is present and is actually a folder
		ContentObject mFolder = findContentObjectByPid(destMembers, folderPid);
		assertTrue("Child was not a folder", mFolder instanceof FolderObject);

		// Try directly retrieving the folder
		FolderObject folder = repository.getFolderObject(folderPid);
		// Verify that its title was set to the expected value
		String title = folder.getResource().getProperty(DC.title).getString();
		assertEquals("Folder title was not correctly set", label, title);
		// Verify that ingestion event gets added for folder
		List<PremisEventObject> events = folder.getPremisLog().getEvents();
		assertEquals(1, events.size());
		assertEquals("added 0 child objects to this container", events.get(0).getResource()
			.getProperty(Premis.hasEventDetail).getString());

		assertClickCount(1);
	}

	/**
	 * Test that work objects are created along with relationships to their children.
	 */
	@Test
	public void ingestWorkObjectTest() throws Exception {
		String label = "testwork";

		// Construct the deposit model with work object
		Model model = job.getWritableModel();
		Bag depBag = model.createBag(depositPid.getRepositoryPath());

		// Constructing the work in the deposit model with a label
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

		ContentContainerObject destObj = (ContentContainerObject) repository.getContentObject(destinationPid);
		List<ContentObject> destMembers = destObj.getMembers();
		assertEquals("Incorrect number of children at destination", 1, destMembers.size());

		// Make sure that the work is present and is actually a work
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
		// Verify that ingestion event gets added for work
		List<PremisEventObject> eventsWork = mWork.getPremisLog().getEvents();
		assertEquals(1, eventsWork.size());
		assertEquals("added 2 child objects to this container", eventsWork.get(0).getResource()
			.getProperty(Premis.hasEventDetail).getString());
		
		// Verify that ingestion event gets added for primary object
		List<PremisEventObject> eventsPrimObj = primaryObj.getPremisLog().getEvents();
		assertEquals(1, eventsPrimObj.size());
		assertEquals("ingested as PID: " + primaryObj.getPid().toString() + "\n ingested as filename: "
				+ primaryObj.getOriginalFile().getFilename(), eventsPrimObj.get(0).getResource()
				.getProperty(Premis.hasEventDetail).getString());
		
		// Verify that ingestion event gets added for supplementary object
		List<PremisEventObject> eventsSupObj = supObj.getPremisLog().getEvents();
		assertEquals(1, eventsSupObj.size());
		assertEquals("ingested as PID: " + supObj.getPid().toString() + "\n ingested as filename: "
				+ supObj.getOriginalFile().getFilename(), eventsSupObj.get(0).getResource()
				.getProperty(Premis.hasEventDetail).getString());
		
		assertClickCount(3);
	}

	/**
	 * Verify that when a file is ingested as the child of something other than
	 * a work (such as a folder in this case), that it is placed inside a
	 * generated work object along with the normal file object structure
	 */
	@Test
	public void ingestFileObjectAsWorkTest() throws Exception {
		Model model = job.getWritableModel();
		Bag depBag = model.createBag(depositPid.getRepositoryPath());

		PID filePid = addFileObject(depBag, FILE1_LOC, FILE1_MIMETYPE, FILE1_SHA1);

		job.closeModel();

		job.run();

		ContentObject destObj = repository.getContentObject(destinationPid);
		List<ContentObject> destMembers = ((ContentContainerObject) destObj).getMembers();
		assertEquals("Incorrect number of children at destination", 1, destMembers.size());

		ContentObject mWork = destMembers.get(0);
		assertTrue("Wrapper object was not a work", mWork instanceof WorkObject);
		String workTitle = mWork.getResource().getProperty(DC.title).getString();
		assertEquals("Work title was not set to filename", FILE1_LOC, workTitle);

		List<ContentObject> workMembers = ((ContentContainerObject) mWork).getMembers();
		assertEquals("Incorrect number of children on work", 1, workMembers.size());

		FileObject primaryFile = ((WorkObject) mWork).getPrimaryObject();
		assertEquals("File object should have the originally provided pid",
				filePid, primaryFile.getPid());
		assertBinaryProperties(primaryFile, FILE1_LOC, FILE1_MIMETYPE, FILE1_SHA1, FILE1_SIZE);

		List<BinaryObject> fileBinaries = primaryFile.getBinaryObjects();
		assertEquals("Incorrect number of binaries for file", 2, fileBinaries.size());
		BinaryObject fitsBinary = fileBinaries.stream()
				.filter(p -> p.getResource().hasProperty(DCTerms.conformsTo, createResource(FITS_URI)))
				.findAny().get();
		assertNotNull("FITS report not present on binary", fitsBinary);

		assertClickCount(1);
	}

	/**
	 * Ensure that deposit fails on a checksum mismatch for a single file deposit
	 */
	@Test(expected = TransactionCancelledException.class)
	public void ingestFileObjectChecksumMismatch() throws Exception {
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
	public void resumeCompletedWorkIngestTest() throws Exception {
		ingestWorkObjectTest();

		job.run();

		ContentObject destObj = repository.getContentObject(destinationPid);
		List<ContentObject> destMembers = ((ContentContainerObject) destObj).getMembers();
		assertEquals("Incorrect number of children at destination", 1, destMembers.size());

		List<ContentObject> workMembers = ((ContentContainerObject) destMembers.get(0)).getMembers();
		assertEquals("Incorrect number of members in work", 2, workMembers.size());

		assertClickCount(3);
	}

	/**
	 * Tests ingest of a folder with two files in it, which fails on the second
	 * child and then attempts to resume
	 */
	@Test
	public void resumeIngestFolderTest() throws Exception {
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
		List<ContentObject> destMembersFailed = ((ContentContainerObject) destObj).getMembers();
		assertEquals("Incorrect number of children at destination", 1, destMembersFailed.size());

		ContentObject mFolderFailed = destMembersFailed.get(0);
		List<ContentObject> folderMembersFailed = ((ContentContainerObject) mFolderFailed).getMembers();
		assertEquals("Incorrect number of children in folder", 1, folderMembersFailed.size());

		// Fix the staging location of the second file
		model = job.getWritableModel();
		Resource file2Resc = model.getResource(file2Pid.getRepositoryPath());
		file2Resc.addProperty(CdrDeposit.stagingLocation, Paths.get(depositDir.getAbsolutePath(),
				FILE2_LOC).toUri().toString());
		job.closeModel();

		// Second run of job
		job.run();

		List<ContentObject> destMembers = ((ContentContainerObject) destObj).getMembers();
		assertEquals("Incorrect number of children at destination", 1, destMembers.size());

		ContentObject mFolder = destMembers.get(0);
		List<ContentObject> folderMembers = ((ContentContainerObject) mFolder).getMembers();
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
	public void ingestDeepHierarchy() throws Exception {
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
		List<ContentObject> members = ((ContentContainerObject) destObj).getMembers();

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

	@Test (expected = TransactionCancelledException.class)
	public void transactionCancelledTest() throws Exception {
		String label = "testwork";

		// Construct the deposit model with work object
		Model model = job.getWritableModel();
		Bag depBag = model.createBag(depositPid.getRepositoryPath());

		// Constructing the folder in the deposit model with a title
		PID workPid = repository.mintContentPid();
		Bag workBag = model.createBag(workPid.getRepositoryPath());

		// Add incorrect resource type to bag, which causes a DepositException to be thrown
		// and the tx to be cancelled
		workBag.addProperty(RDF.type, Cdr.FileObject);
		workBag.addProperty(CdrDeposit.label, label);

		PID mainPid = addFileObject(workBag, FILE1_LOC, FILE1_MIMETYPE, FILE1_SHA1);

		depBag.add(workBag);

		workBag.asResource().addProperty(Cdr.primaryObject,
				model.getResource(mainPid.getRepositoryPath()));

		job.closeModel();

		try {
			job.run();
		} finally {
			assertFalse(FedoraTransaction.hasTxId());
			assertFalse(FedoraTransaction.isStillAlive());
			assertFalse(repository.objectExists(workPid));
		}
	}
	
	@Test
	public void addDescriptionTest() throws IOException {
		File modsFolder = job.getDescriptionDir();
		modsFolder.mkdir();
		
		PID folderPid = repository.mintContentPid();
		File modsFile = new File(modsFolder, folderPid.getUUID() + ".xml");
		modsFile.createNewFile();
		
		String label = "testfolder";

		// Construct the deposit model, containing a deposit with one empty folder
		Model model = job.getWritableModel();
		Bag depBag = model.createBag(depositPid.getRepositoryPath());

		// Constructing the folder in the deposit model with a title
		Bag folderBag = model.createBag(folderPid.getRepositoryPath());
		folderBag.addProperty(RDF.type, Cdr.Folder);
		folderBag.addProperty(CdrDeposit.label, label);

		depBag.add(folderBag);

		job.closeModel();

		job.run();
		
		ContentContainerObject destObj = (ContentContainerObject) repository.getContentObject(destinationPid);
		List<ContentObject> destMembers = destObj.getMembers();
		// Make sure that the folder is present and is actually a folder
		FolderObject folderObj = (FolderObject) findContentObjectByPid(destMembers, folderPid);
		
		assertNotNull(folderObj.getDescription());
	}
	
	@Test
	public void noDescriptionAddedTest() {
		PID folderPid = repository.mintContentPid();
		
		String label = "testfolder";

		// Construct the deposit model, containing a deposit with one empty folder
		Model model = job.getWritableModel();
		Bag depBag = model.createBag(depositPid.getRepositoryPath());

		// Constructing the folder in the deposit model with a title
		Bag folderBag = model.createBag(folderPid.getRepositoryPath());
		folderBag.addProperty(RDF.type, Cdr.Folder);
		folderBag.addProperty(CdrDeposit.label, label);

		depBag.add(folderBag);

		job.closeModel();

		job.run();
		
		ContentContainerObject destObj = (ContentContainerObject) repository.getContentObject(destinationPid);
		List<ContentObject> destMembers = destObj.getMembers();
		// Make sure that the folder is present and is actually a folder
		FolderObject folderObj = (FolderObject) findContentObjectByPid(destMembers, folderPid);
		
		assertNull(folderObj.getDescription());
	}
	
	@Test
	public void addPremisEventsTest() throws IOException {
		File premisEventsDir = job.getEventsDirectory();
		premisEventsDir.mkdir();
		
		PID folderObjPid = repository.mintContentPid();
		
		File premisEventsFile = new File(premisEventsDir, folderObjPid.getUUID() + ".ttl");
		premisEventsFile.createNewFile();
		
		String label = "testfolder";

		// Construct the deposit model, containing a deposit with one empty folder
		Model model = job.getWritableModel();
		Bag depBag = model.createBag(depositPid.getRepositoryPath());

		// Constructing the folder in the deposit model with a title
		Bag folderBag = model.createBag(folderObjPid.getRepositoryPath());
		folderBag.addProperty(RDF.type, Cdr.Folder);
		folderBag.addProperty(CdrDeposit.label, label);

		depBag.add(folderBag);
		
		FilePremisLogger premisLogger = new FilePremisLogger(folderObjPid, premisEventsFile, repository);
		Resource event1 = premisLogger.buildEvent(Premis.Normalization)
				.addEventDetail("Event 1")
				.addAuthorizingAgent(SoftwareAgent.depositService.getFullname())
				.write();

		Resource event2 = premisLogger.buildEvent(Premis.VirusCheck)
				.addEventDetail("Event 2")
				.addSoftwareAgent(SoftwareAgent.clamav.getFullname())
				.write();
		
		job.closeModel();

		job.run();
		
		FolderObject folder = repository.getFolderObject(folderObjPid);
		List<PremisEventObject> events = folder.getPremisLog().getEvents();
		// there should be three events total: the ingestion event, plus the two added in the test
		assertEquals(3, events.size());
		assertTrue(events.contains(findPremisEventByType(events, Premis.Normalization)));
		assertTrue(events.contains(findPremisEventByType(events, Premis.VirusCheck)));
	}
	
	@Test
	public void onlyIngestionEventAddedTest() throws IOException {
		File premisEventsDir = job.getEventsDirectory();
		premisEventsDir.mkdir();
		
		PID folderObjPid = repository.mintContentPid();
		
		File premisEventsFile = new File(premisEventsDir, folderObjPid.getUUID() + ".xml");
		premisEventsFile.createNewFile();
		
		String label = "testfolder";

		// Construct the deposit model, containing a deposit with one empty folder
		Model model = job.getWritableModel();
		Bag depBag = model.createBag(depositPid.getRepositoryPath());

		// Constructing the folder in the deposit model with a title
		Bag folderBag = model.createBag(folderObjPid.getRepositoryPath());
		folderBag.addProperty(RDF.type, Cdr.Folder);
		folderBag.addProperty(CdrDeposit.label, label);

		depBag.add(folderBag);
		
		job.closeModel();

		job.run();
		
		FolderObject folder = repository.getFolderObject(folderObjPid);
		List<PremisEventObject> events = folder.getPremisLog().getEvents();
		assertEquals(1, events.size());
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

	private PID addFileObject(Bag parent, String stagingLocation,
				String mimetype, String sha1) throws Exception {
		PID filePid = repository.mintContentPid();

		Resource fileResc = parent.getModel().createResource(filePid.getRepositoryPath());
		fileResc.addProperty(RDF.type, Cdr.FileObject);
		if (stagingLocation != null) {
			fileResc.addProperty(CdrDeposit.stagingLocation, Paths.get(depositDir.getAbsolutePath(),
					stagingLocation).toUri().toString());
		}
		fileResc.addProperty(CdrDeposit.mimetype, mimetype);
		if (sha1 != null) {
			fileResc.addProperty(CdrDeposit.sha1sum, sha1);
		}

		parent.add(fileResc);

		// Create the accompanying fake premis report file
		new File(techmdDir, filePid.getUUID() + ".xml").createNewFile();

		return filePid;
	}
	
	private PremisEventObject findPremisEventByType(List<PremisEventObject> objs, final Resource type) {
		return objs.stream()
				.filter(p -> p.getResource().getPropertyResourceValue(Premis.hasEventType).equals(type)).findAny().get();
	}
}

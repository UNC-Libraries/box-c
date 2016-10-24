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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.deposit.work.JobFailedException;
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
public class IngestContentObjectsJobTest extends AbstractDepositJobTest {

	private IngestContentObjectsJob job;

	private PID depositPid;

	private PID destinationPid;

	private FolderObject destinationObj;

	@Before
	public void init() throws Exception {
		initMocks(this);

		Dataset dataset = TDBFactory.createDataset();

		job = new IngestContentObjectsJob();
		job.setDepositUUID(depositUUID);
		job.setDepositDirectory(depositDir);
		job.setRepository(repository);
		setField(job, "dataset", dataset);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "depositStatusFactory", depositStatusFactory);
		setField(job, "jobStatusFactory", jobStatusFactory);
		job.init();

		depositPid = job.getDepositPID();

		setupDestination();

		FileUtils.copyDirectory(new File("src/test/resources/examples"), depositDir);
	}

	private void setupDestination() {
		// Establish a destination object to deposit to
		destinationPid = makePid(RepositoryPathConstants.CONTENT_BASE);

		Map<String, String> depositStatus = new HashMap<>();
		depositStatus.put(DepositField.containerId.name(), destinationPid.getQualifiedId());

		when(depositStatusFactory.get(anyString())).thenReturn(depositStatus);

		destinationObj = mock(FolderObject.class);
		when(repository.getContentObject(eq(destinationPid))).thenReturn(destinationObj);
	}

	/**
	 * Test that an empty folder is successfully deposited
	 */
	@Test
	public void ingestEmptyFolderTest() {

		FolderObject folder = mock(FolderObject.class);
		when(repository.createFolderObject(any(PID.class), any(Model.class))).thenReturn(folder);

		Model model = job.getWritableModel();
		Bag depBag = model.createBag(depositPid.getRepositoryPath());

		PID folderPid = makePid(RepositoryPathConstants.CONTENT_BASE);
		Bag folderBag = model.createBag(folderPid.getRepositoryPath());
		folderBag.addProperty(RDF.type, Cdr.Folder);

		depBag.add(folderBag);

		job.closeModel();

		job.run();

		verify(repository).createFolderObject(eq(folderPid), any(Model.class));
		verify(destinationObj).addMember(eq(folder));
	}

	private Bag setupWork(PID workPid, WorkObject work, Model model) {
		when(repository.createWorkObject(any(PID.class), any(Model.class))).thenReturn(work);
		when(work.getPid()).thenReturn(workPid);

		Bag depBag = model.createBag(depositPid.getRepositoryPath());

		Bag workBag = model.createBag(workPid.getRepositoryPath());
		workBag.addProperty(RDF.type, Cdr.Work);

		depBag.add(workBag);

		return workBag;
	}

	/**
	 * Test that a deposit involving a work containing two files is successful
	 */
	@Test
	public void ingestWorkObjectTest() {
		PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
		Model model = job.getWritableModel();
		WorkObject work = mock(WorkObject.class);
		Bag workBag = setupWork(workPid, work, model);

		String mainLoc = "pdf.pdf";
		String mainMime = "application/pdf";
		PID mainPid = addFileObject(workBag, mainLoc, mainMime);
		String supLoc = "text.txt";
		String supMime = "text/plain";
		PID supPid = addFileObject(workBag, supLoc, supMime);

		workBag.asResource().addProperty(Cdr.primaryObject,
				model.getResource(mainPid.getRepositoryPath()));

		job.closeModel();

		job.run();

		verify(repository).createWorkObject(eq(workPid), any(Model.class));
		verify(destinationObj).addMember(eq(work));

		verify(work).addDataFile(eq(mainPid), any(InputStream.class), eq(mainLoc),
				eq(mainMime), anyString());
		verify(work).addDataFile(eq(supPid), any(InputStream.class), eq(supLoc),
				eq(supMime), anyString());
		verify(work).setPrimaryObject(mainPid);
	}

	/**
	 * Test that the deposit fails if a file object is specified but not given a
	 * staging location
	 */
	@Test(expected = JobFailedException.class)
	public void ingestWorkWithFileWithoutLocation() {
		PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
		Model model = job.getWritableModel();
		WorkObject work = mock(WorkObject.class);
		Bag workBag = setupWork(workPid, work, model);

		PID filePid = makePid(RepositoryPathConstants.CONTENT_BASE);

		Resource fileResc = model.createResource(filePid.getRepositoryPath());
		fileResc.addProperty(RDF.type, Cdr.FileObject);
		fileResc.addProperty(CdrDeposit.mimetype, "text/plain");
		workBag.add(fileResc);

		job.closeModel();

		job.run();
	}

	/**
	 * Test that deposit fails if depositing a file path that doesn't exist
	 */
	@Test(expected = JobFailedException.class)
	public void ingestWorkWithFileDoesNotExist() {
		PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
		Model model = job.getWritableModel();
		WorkObject work = mock(WorkObject.class);
		Bag workBag = setupWork(workPid, work, model);

		addFileObject(workBag, "doesnotexist.txt", "text/plain");

		job.closeModel();

		job.run();
	}

	/**
	 * Test that a standalone file object ingested to a destination which is not
	 * a work successfully ingests as the child of a generated work object
	 */
	@Test
	public void ingestFileObjectAsWorkTest() {
		PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
		when(repository.mintContentPid()).thenReturn(workPid);
		WorkObject work = mock(WorkObject.class);
		when(work.getPid()).thenReturn(workPid);
		when(repository.createWorkObject(eq(workPid), any(Model.class))).thenReturn(work);

		Model model = job.getWritableModel();
		Bag depBag = model.createBag(depositPid.getRepositoryPath());

		String fileLoc = "text.txt";
		String fileMime = "text/plain";
		PID filePid = addFileObject(depBag, fileLoc, fileMime);

		job.closeModel();

		job.run();

		// Verify that a work object was generated and added to the destination
		verify(repository).mintContentPid();
		verify(repository).createWorkObject(eq(workPid), any(Model.class));
		verify(destinationObj).addMember(eq(work));

		// Verify that a FileObject was added to the generated work as primary obj
		verify(work).setPrimaryObject(filePid);
		verify(work).addDataFile(eq(filePid), any(InputStream.class), eq(fileLoc),
				eq(fileMime), anyString());
	}
	
	@Test(expected = JobFailedException.class)
	public void ingestfileObjectAsWorkNoStaging() {
		PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
		when(repository.mintContentPid()).thenReturn(workPid);
		WorkObject work = mock(WorkObject.class);
		when(work.getPid()).thenReturn(workPid);
		when(repository.createWorkObject(eq(workPid), any(Model.class))).thenReturn(work);

		Model model = job.getWritableModel();
		Bag depBag = model.createBag(depositPid.getRepositoryPath());
		
		PID filePid = makePid(RepositoryPathConstants.CONTENT_BASE);
		
		Resource fileResc = model.createResource(filePid.getRepositoryPath());
		fileResc.addProperty(RDF.type, Cdr.FileObject);
		fileResc.addProperty(CdrDeposit.mimetype, "text/plain");
		depBag.add(fileResc);

		job.closeModel();

		job.run();
	}

	private PID addFileObject(Bag parent, String stagingLocation, String mimetype) {
		PID filePid = makePid(RepositoryPathConstants.CONTENT_BASE);

		Resource fileResc = parent.getModel().createResource(filePid.getRepositoryPath());
		fileResc.addProperty(RDF.type, Cdr.FileObject);
		fileResc.addProperty(CdrDeposit.stagingLocation, stagingLocation);
		fileResc.addProperty(CdrDeposit.mimetype, mimetype);

		parent.add(fileResc);

		return filePid;
	}
}

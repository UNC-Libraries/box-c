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

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
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

	public IngestContentObjectsJobTest() {
		// TODO Auto-generated constructor stub
	}

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

		destinationPid = makePid(RepositoryPathConstants.CONTENT_BASE);
	}

	@Test
	public void ingestEmptyFolderTest() throws Exception {
		Map<String, String> depositStatus = new HashMap<>();
		depositStatus.put(DepositField.containerId.name(), destinationPid.getQualifiedId());

		when(depositStatusFactory.get(anyString())).thenReturn(depositStatus);

		FolderObject destinationObj = mock(FolderObject.class);
		when(repository.getContentObject(eq(destinationPid))).thenReturn(destinationObj);

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
}

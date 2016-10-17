/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
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
package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.deposit.work.DepositGraphUtils.fprop;
import static edu.unc.lib.dl.test.TestHelpers.setField;
import static edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty.hasModel;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.CONTAINER;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.SIMPLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.tdb.TDBFactory;

import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.staging.Stages;

public class BagIt2N3BagJobTest extends AbstractNormalizationJobTest {

	private BagIt2N3BagJob job;

	private Map<String, String> status;
	
	private Stages stages;

	@Before
	public void setup() throws Exception {
		stages = mock(Stages.class);
		
		status = new HashMap<String, String>();

		when(depositStatusFactory.get(anyString())).thenReturn(status);

		Dataset dataset = TDBFactory.createDataset();

		job = new BagIt2N3BagJob();
		job.setDepositUUID(depositUUID);
		job.setDepositDirectory(depositDir);
		job.setStages(stages);
		setField(job, "dataset", dataset);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "depositStatusFactory", depositStatusFactory);
		job.init();
	}

	@Test
	public void testConversion() throws Exception {
		status.put(DepositField.sourcePath.name(), "src/test/resources/paths/valid-bag");
		status.put(DepositField.fileName.name(), "Test File");
		status.put(DepositField.extras.name(), "{\"accessionNumber\" : \"123456\", \"mediaId\" : \"789\"}");
		
		when(stages.getStagedURI(any(URI.class))).thenAnswer(new Answer<URI>() {
			public URI answer(InvocationOnMock invocation) throws URISyntaxException {
				Object[] args = invocation.getArguments();
				URI uri = (URI) args[0];
				String path = uri.toString();
				int index = path.lastIndexOf("/paths");
				path = path.substring(index + 6);
				
				return new URI("tag:" + path);
			}
		});

		job.run();

		Model model = job.getReadOnlyModel();
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		
		assertEquals(depositBag.size(), 1);
		
		Bag bagFolder = model.getBag((Resource) depositBag.iterator().next());
		assertEquals("Bag folder label was not set", "Test File", bagFolder.getProperty(CdrDeposit.label).getString());
		assertEquals("Content model was not set", CONTAINER.toString(),
				bagFolder.getPropertyResourceValue(fprop(model, hasModel)).getURI());
		
		Resource folder = (Resource) bagFolder.iterator().next();
		
		assertEquals("Folder label was not set", folder.getProperty(CdrDeposit.label).getString(), "test");
		assertEquals("Content model was not set", CONTAINER.toString(),
				folder.getPropertyResourceValue(fprop(model, hasModel)).getURI());
		
		Bag childrenBag = model.getBag(folder.getURI());
		
		assertEquals(childrenBag.size(), 2);

		// Put children into a map since we can't guarantee order from jena
		Map<String, Resource> children = new HashMap<>(2);
		NodeIterator childIt = childrenBag.iterator();
		while (childIt.hasNext()) {
			Resource file = (Resource) childIt.next();
			children.put(file.getProperty(CdrDeposit.label).getString(), file);
		}
		
		ArgumentCaptor<String> filePathCaptor = ArgumentCaptor.forClass(String.class);
		verify(depositStatusFactory, times(2)).addManifest(anyString(), filePathCaptor.capture());
		List<String> capturedFilePaths = Arrays.asList("tag:/valid-bag/bagit.txt", "tag:/valid-bag/manifest-md5.txt");
		assertEquals(capturedFilePaths, filePathCaptor.getAllValues());
		
		Resource file = children.get("lorem.txt");
		assertEquals("Content model was not set", SIMPLE.toString(),
				file.getPropertyResourceValue(fprop(model, hasModel)).getURI());
		assertEquals("Checksum was not set", "fa5c89f3c88b81bfd5e821b0316569af",
				file.getProperty(CdrDeposit.md5sum).getString());
		assertEquals("File location not set", "tag:/valid-bag/data/test/lorem.txt",
				file.getProperty(CdrDeposit.stagingLocation).getString());
		
		Resource file2 = children.get("ipsum.txt");
		assertEquals("Content model was not set", SIMPLE.toString(),
				file2.getPropertyResourceValue(fprop(model, hasModel)).getURI());
		assertEquals("Checksum was not set", "e78f5438b48b39bcbdea61b73679449d",
				file2.getProperty(CdrDeposit.md5sum).getString());
		assertEquals("File location not set", "tag:/valid-bag/data/test/ipsum.txt",
				file2.getProperty(CdrDeposit.stagingLocation).getString());
		
		File modsFile = new File(job.getDescriptionDir(), new PID(bagFolder.getURI()).getUUID() + ".xml");
		assertTrue(modsFile.exists());
		
		Set<String> cleanupSet = new HashSet<>();
		StmtIterator it = depositBag.listProperties(CdrDeposit.cleanupLocation);
		while (it.hasNext()) {
			Statement stmt = it.nextStatement();
			cleanupSet.add(stmt.getString());
		}
		
		assertEquals("Incorrect number of objects identified for cleanup", 3, cleanupSet.size());
		assertTrue("Cleanup of bag not set", cleanupSet.contains("tag:/valid-bag/"));
		assertTrue("Cleanup of manifest not set", cleanupSet.contains("tag:/valid-bag/bagit.txt"));
		assertTrue("Cleanup of manifest not set", cleanupSet.contains("tag:/valid-bag/manifest-md5.txt"));
	}

	@Test(expected = JobFailedException.class)
	public void testInvalid() throws Exception {
		status.put(DepositField.sourcePath.name(), "src/test/resources/paths/invalid-bag");

		job.run();
	}

}

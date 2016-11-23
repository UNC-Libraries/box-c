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

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.CONTAINER;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.SIMPLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

public class DirectoryToBagJobTest extends AbstractNormalizationJobTest {
	
	@Rule
	public final TemporaryFolder tmpDir = new TemporaryFolder();

	private DirectoryToBagJob job;

	private Map<String, String> status;
	
	private File depositDirectory;

	@Before
	public void setup() throws Exception {
		depositDirectory = tmpDir.newFolder("directory-deposit");
		
		File emptyDir = new File(depositDirectory, "empty_test");
		emptyDir.mkdir();
		
		File testDirectory = new File(depositDirectory, "test");
		testDirectory.mkdir();
		
		File testFile = new File(testDirectory, "lorem.txt");
		testFile.createNewFile();
		
		status = new HashMap<String, String>();

		when(depositStatusFactory.get(anyString())).thenReturn(status);

		Dataset dataset = TDBFactory.createDataset();

		job = new DirectoryToBagJob();
		job.setDepositUUID(depositUUID);
		job.setDepositDirectory(depositDir);
		setField(job, "dataset", dataset);
		setField(job, "depositsDirectory", depositDirectory);
		setField(job, "depositStatusFactory", depositStatusFactory);

		job.init();
	}

	@Test
	public void testConversion() throws Exception {
		status.put(DepositField.sourcePath.name(), depositDirectory.getAbsolutePath());
		status.put(DepositField.fileName.name(), "Test File");
		status.put(DepositField.extras.name(), "{\"accessionNumber\" : \"123456\", \"mediaId\" : \"789\"}");

		job.run();

		Model model = job.getReadOnlyModel();
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		
		assertEquals(depositBag.size(), 1);
		
		Bag bagFolder = model.getBag((Resource) depositBag.iterator().next());
		assertEquals("Bag folder label was not set", "Test File", bagFolder.getProperty(CdrDeposit.label).getString());
		assertEquals("Content model was not set", CONTAINER.toString(),
				bagFolder.getPropertyResourceValue(RDF.type).getURI());
		
		NodeIterator iterator = bagFolder.iterator();
		Resource emptyFolder = (Resource) iterator.next();
		assertEquals("Folder label was not set", emptyFolder.getProperty(CdrDeposit.label).getString(), "empty_test");
		assertEquals("Content model was not set", CONTAINER.toString(),
				emptyFolder.getPropertyResourceValue(RDF.type).getURI());
		
		Bag emptyBag = model.getBag(emptyFolder.getURI());
		
		assertEquals(emptyBag.size(), 0);
		
		Resource folder = (Resource) iterator.next();
		
		assertEquals("Folder label was not set", folder.getProperty(CdrDeposit.label).getString(), "test");
		assertEquals("Content model was not set", CONTAINER.toString(),
				folder.getPropertyResourceValue(RDF.type).getURI());
		
		Bag childrenBag = model.getBag(folder.getURI());
		
		assertEquals(childrenBag.size(), 1);

		Resource file = (Resource) childrenBag.iterator().next();
		
		assertEquals("File label was not set", "lorem.txt",
				file.getProperty(CdrDeposit.label).getString());
		assertEquals("Content model was not set", SIMPLE.toString(),
				file.getPropertyResourceValue(RDF.type).getURI());
		assertEquals("Checksum was not set", "d41d8cd98f00b204e9800998ecf8427e",
				file.getProperty(CdrDeposit.md5sum).getString());
		
		String tagPath = file.getProperty(CdrDeposit.stagingLocation).getString();
		assertTrue(tagPath.endsWith("directory-deposit/test/lorem.txt"));
		
		File modsFile = new File(job.getDescriptionDir(), new PID(bagFolder.getURI()).getUUID() + ".xml");
		assertTrue(modsFile.exists());
	}
}
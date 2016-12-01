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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.vocabulary.RDF;

import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 * @author bbpennel
 * @date Jul 22, 2014
 */
public class Simple2N3BagJobTest extends AbstractNormalizationJobTest {

	private Simple2N3BagJob job;

	private Map<String, String> status;

	@Before
	public void setup() throws Exception {
		
		status = new HashMap<String, String>();

		when(depositStatusFactory.get(anyString())).thenReturn(status);
		
		Dataset dataset = TDBFactory.createDataset();

		job = new Simple2N3BagJob();
		job.setDepositUUID(depositUUID);
		job.setDepositDirectory(depositDir);
		job.setRepository(repo);
		setField(job, "dataset", dataset);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "depositStatusFactory", depositStatusFactory);

		job.init();
	}

	@Test
	public void depositContainerTest() throws Exception {
		String name = "testFolder";

		status.put(DepositField.depositSlug.name(), name);
		status.put(RDF.type.toString(), Cdr.Folder.toString());

		job.run();

		Model model = job.getWritableModel();
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource primaryResource = (Resource) depositBag.iterator().next();

		assertEquals("Folder label was not set", primaryResource.getProperty(CdrDeposit.label).getString(), name);

		assertEquals("Does not have Folder type", primaryResource.getPropertyResourceValue(RDF.type)
				.getURI(), Cdr.Folder.toString());
	}

	@Test
	public void depositContainerWithMODSTest() throws Exception {
		String name = "testFolder";

		copyTestPackage("src/test/resources/simpleMods.xml", "mods.xml", job);

		status.put(DepositField.depositSlug.name(), name);
		status.put(RDF.type.toString(), Cdr.Folder.toString());

		job.run();

		Model model = job.getWritableModel();
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource primaryResource = (Resource) depositBag.iterator().next();

		assertEquals("Folder label was not set", primaryResource.getProperty(CdrDeposit.label).getString(), name);

		assertEquals("Does not have Folder type", primaryResource.getPropertyResourceValue(RDF.type)
				.getURI(), Cdr.Folder.toString());

		File descDir = new File(depositDir, DepositConstants.DESCRIPTION_DIR);
		File destinationFile = new File(descDir, new PID(primaryResource.toString()).getUUID() + ".xml");

		assertTrue("MODS file was not moved into place", destinationFile.exists());
	}

	@Test
	public void depositCollection() throws Exception {
		String name = "testFolder";

		status.put(DepositField.depositSlug.name(), name);
		status.put(RDF.type.toString(), Cdr.Collection.toString());

		job.run();

		Model model = job.getWritableModel();
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource primaryResource = (Resource) depositBag.iterator().next();

		assertEquals("Folder label was not set", primaryResource.getProperty(CdrDeposit.label).getString(), name);

	}

	@Test
	public void depositSimple() throws Exception {
		String name = "objectName";
		copyTestPackage("src/test/resources/simpleMods.xml", "data_file.xml", job);

		status.put(DepositField.depositSlug.name(), name);
		status.put(DepositField.fileName.name(), "data_file.xml");

		job.run();

		Model model = job.getWritableModel();
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource primaryResource = (Resource) depositBag.iterator().next();

		assertEquals("Folder label was not set", primaryResource.getProperty(CdrDeposit.label).getString(), name);

		assertFalse("No RDF types assigned", primaryResource.hasProperty(RDF.type));
	}

	@Test(expected = JobFailedException.class)
	public void depositSimpleMissingFile() throws Exception {

		status.put(DepositField.depositSlug.name(), "name");
		status.put(DepositField.fileName.name(), "data_file.xml");

		job.run();

	}
}

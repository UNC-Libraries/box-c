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

import static edu.unc.lib.deposit.work.DepositGraphUtils.dprop;
import static edu.unc.lib.deposit.work.DepositGraphUtils.fprop;
import static edu.unc.lib.dl.test.TestHelpers.setField;
import static edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship.label;
import static edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty.hasModel;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.COLLECTION;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.CONTAINER;
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

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.fedora.PID;
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

		job = new Simple2N3BagJob();
		job.setDepositUUID(depositUUID);
		job.setDepositDirectory(depositDir);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "depositStatusFactory", depositStatusFactory);

		job.init();
	}

	@Test
	public void depositContainerTest() throws Exception {
		String name = "testFolder";

		status.put(DepositField.depositSlug.name(), name);
		status.put(hasModel.toString(), CONTAINER.toString());

		job.run();

		Model model = getModel(job);
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource primaryResource = (Resource) depositBag.iterator().next();

		assertEquals("Folder label was not set", primaryResource.getProperty(dprop(model, label)).getString(), name);

		assertEquals("Content model was not set", primaryResource.getPropertyResourceValue(fprop(model, hasModel))
				.getURI(), CONTAINER.toString());
	}

	@Test
	public void depositContainerWithMODSTest() throws Exception {
		String name = "testFolder";

		copyTestPackage("src/test/resources/simpleMods.xml", "mods.xml", job);

		status.put(DepositField.depositSlug.name(), name);
		status.put(hasModel.toString(), CONTAINER.toString());

		job.run();

		Model model = getModel(job);
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource primaryResource = (Resource) depositBag.iterator().next();

		assertEquals("Folder label was not set", primaryResource.getProperty(dprop(model, label)).getString(), name);

		assertEquals("Content model was not set", primaryResource.getPropertyResourceValue(fprop(model, hasModel))
				.getURI(), CONTAINER.toString());

		File descDir = new File(depositDir, DepositConstants.DESCRIPTION_DIR);
		File destinationFile = new File(descDir, new PID(primaryResource.toString()).getUUID() + ".xml");

		assertTrue("MODS file was not moved into place", destinationFile.exists());
	}

	@Test
	public void depositCollection() throws Exception {
		String name = "testFolder";

		status.put(DepositField.depositSlug.name(), name);
		status.put(hasModel.toString(), COLLECTION.toString());

		job.run();

		Model model = getModel(job);
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource primaryResource = (Resource) depositBag.iterator().next();

		assertEquals("Folder label was not set", primaryResource.getProperty(dprop(model, label)).getString(), name);

		assertTrue("Did not have collect content models", isContainerType(primaryResource, COLLECTION, model));

	}

	@Test
	public void depositSimple() throws Exception {
		String name = "objectName";
		copyTestPackage("src/test/resources/simpleMods.xml", "data_file.xml", job);

		status.put(DepositField.depositSlug.name(), name);
		status.put(DepositField.fileName.name(), "data_file.xml");

		job.run();

		Model model = getModel(job);
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource primaryResource = (Resource) depositBag.iterator().next();

		assertEquals("Folder label was not set", primaryResource.getProperty(dprop(model, label)).getString(), name);

		assertFalse("Content models incorrectly assigned", primaryResource.hasProperty(fprop(model, hasModel)));
	}

	@Test(expected = JobFailedException.class)
	public void depositSimpleMissingFile() throws Exception {

		status.put(DepositField.depositSlug.name(), "name");
		status.put(DepositField.fileName.name(), "data_file.xml");

		job.run();

	}
}

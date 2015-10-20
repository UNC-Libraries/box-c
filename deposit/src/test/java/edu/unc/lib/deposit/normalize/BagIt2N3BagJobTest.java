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
import static edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship.md5sum;
import static edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty.hasModel;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.CONTAINER;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.SIMPLE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;

import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

public class BagIt2N3BagJobTest extends AbstractNormalizationJobTest {

	private BagIt2N3BagJob job;

	private Map<String, String> status;

	@Before
	public void setup() throws Exception {
		status = new HashMap<String, String>();

		when(depositStatusFactory.get(anyString())).thenReturn(status);

		Dataset dataset = TDBFactory.createDataset();

		job = new BagIt2N3BagJob();
		job.setDepositUUID(depositUUID);
		job.setDepositDirectory(depositDir);
		setField(job, "dataset", dataset);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "depositStatusFactory", depositStatusFactory);

		job.init();
	}

	@Test
	public void testConversion() {
		status.put(DepositField.sourcePath.name(), "src/test/resources/paths/valid-bag");

		job.run();

		Model model = job.getWritableModel();
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		
		assertEquals(depositBag.size(), 1);
		
		Resource folder = (Resource) depositBag.iterator().next();
		
		assertEquals("Folder label was not set", folder.getProperty(dprop(model, label)).getString(), "test");
		assertEquals("Content model was not set", folder.getPropertyResourceValue(fprop(model, hasModel)).getURI(), CONTAINER.toString());
		
		Bag childrenBag = model.getBag(folder.getURI());
		
		assertEquals(childrenBag.size(), 1);
		
		Resource file = (Resource) childrenBag.iterator().next();
		
		assertEquals("File label was not set", file.getProperty(dprop(model, label)).getString(), "lorem.txt");
		assertEquals("Content model was not set", file.getPropertyResourceValue(fprop(model, hasModel)).getURI(), SIMPLE.toString());
		assertEquals("Checksum was not set", file.getProperty(dprop(model, md5sum)).getString(), "fa5c89f3c88b81bfd5e821b0316569af");
	}

	@Test(expected = JobFailedException.class)
	public void testInvalid() throws Exception {
		status.put(DepositField.sourcePath.name(), "src/test/resources/paths/invalid-bag");

		job.run();
	}

}

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
import static edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship.stagingLocation;
import static edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty.hasModel;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.CONTAINER;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.SIMPLE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
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
		
		when(stages.getStagedURI(any(URI.class))).thenReturn(new URI("tag:/path/data/test/lorem.txt"));

		job.run();

		Model model = job.getReadOnlyModel();
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		
		assertEquals(depositBag.size(), 1);
		
		Bag bagFolder = model.getBag((Resource) depositBag.iterator().next());
		assertEquals("Bag folder label was not set", bagFolder.getProperty(dprop(model, label)).getString(), "valid-bag");
		assertEquals("Content model was not set", CONTAINER.toString(),
				bagFolder.getPropertyResourceValue(fprop(model, hasModel)).getURI());
		
		Resource folder = (Resource) bagFolder.iterator().next();
		
		assertEquals("Folder label was not set", folder.getProperty(dprop(model, label)).getString(), "test");
		assertEquals("Content model was not set", CONTAINER.toString(),
				folder.getPropertyResourceValue(fprop(model, hasModel)).getURI());
		
		Bag childrenBag = model.getBag(folder.getURI());
		
		assertEquals(childrenBag.size(), 1);

		Resource file = (Resource) childrenBag.iterator().next();
		
		assertEquals("File label was not set", "lorem.txt",
				file.getProperty(dprop(model, label)).getString());
		assertEquals("Content model was not set", SIMPLE.toString(),
				file.getPropertyResourceValue(fprop(model, hasModel)).getURI());
		assertEquals("Checksum was not set", "fa5c89f3c88b81bfd5e821b0316569af",
				file.getProperty(dprop(model, md5sum)).getString());
		assertEquals("File location not set", "tag:/path/data/test/lorem.txt",
				file.getProperty(dprop(model, stagingLocation)).getString());
	}

	@Test(expected = JobFailedException.class)
	public void testInvalid() throws Exception {
		status.put(DepositField.sourcePath.name(), "src/test/resources/paths/invalid-bag");

		job.run();
	}

}

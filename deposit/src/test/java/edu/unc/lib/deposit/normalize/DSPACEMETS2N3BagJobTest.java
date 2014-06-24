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
import static edu.unc.lib.dl.test.TestHelpers.setField;
import static edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship.stagingLocation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.xml.transform.Transformer;
import javax.xml.validation.Schema;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.unc.lib.deposit.DepositTestUtils;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.DepositConstants;

/**
 * @author bbpennel
 * @date Jun 18, 2014
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/dspacemets-context.xml" })
public class DSPACEMETS2N3BagJobTest extends AbstractNormalizationJobTest {

	private static final Logger log = LoggerFactory.getLogger(DSPACEMETS2N3BagJobTest.class);

	@Autowired
	private Transformer epdcx2modsTransformer;
	@Autowired
	private Schema metsSipSchema;
	@Autowired
	private SchematronValidator validator;

	private DSPACEMETS2N3BagJob job;

	@Before
	public void init() throws Exception {

		job = new DSPACEMETS2N3BagJob(jobUUID, depositUUID);
		job.setEpdcx2modsTransformer(epdcx2modsTransformer);
		job.setDepositUUID(depositUUID);
		job.setDepositDirectory(depositDir);
		job.setMetsSipSchema(metsSipSchema);
		job.setSchematronValidator(validator);
		setField(job, "depositsDirectory", depositsDirectory);

		job.init();
	}

	@Test
	public void test() {

		String workDir = DepositTestUtils.makeTestDir(depositsDirectory, job.getDepositUUID(),
				new File("src/test/resources/biomedDspaceMETS.zip"));

		long start = System.currentTimeMillis();
		job.run();
		log.info("Run dspace mets: {}", (System.currentTimeMillis() - start));

		File modelFile = new File(workDir, DepositConstants.MODEL_FILE);
		assertTrue("N3 model file must exist after conversion", modelFile.exists());

		Model model = getModel(job);
		assertFalse("Model was empty", model.isEmpty());

		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		assertNotNull("Deposit object not found", depositBag);

		Resource primaryResource = (Resource) depositBag.iterator().next();
		assertNotNull("Main object from the deposit not found", primaryResource);

		assertTrue("Primary resource was not assigned content models to be an aggregate",
				isAggregate(primaryResource, model));

		Property stagingLoc = dprop(model, stagingLocation);

		NodeIterator childIt = model.getBag(primaryResource).iterator();
		int childCount = 0;
		while (childIt.hasNext()) {
			childCount++;

			Resource child = (Resource) childIt.next();
			verifyStagingLocationExists(child, stagingLoc, job.getDepositDirectory(), "Child content");
		}

		assertEquals("Incorrect aggregate child count", 5, childCount);
	}
}

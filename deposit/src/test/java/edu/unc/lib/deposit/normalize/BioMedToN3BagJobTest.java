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
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.validation.Schema;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;

import edu.unc.lib.deposit.DepositTestUtils;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship;

/**
 * @author bbpennel
 * @date Jun 18, 2014
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/dspacemets-context.xml" })
public class BioMedToN3BagJobTest extends AbstractNormalizationJobTest {

	private static final Logger log = LoggerFactory.getLogger(BioMedToN3BagJobTest.class);

	@Autowired
	private Transformer epdcx2modsTransformer;
	@Autowired
	private Schema metsSipSchema;
	@Autowired
	private SchematronValidator validator;

	private BioMedToN3BagJob job;

	@Before
	public void init() throws Exception {
		
		Dataset dataset = TDBFactory.createDataset();

		job = new BioMedToN3BagJob(jobUUID, depositUUID);
		job.setEpdcx2modsTransformer(epdcx2modsTransformer);
		job.setDepositUUID(depositUUID);
		job.setDepositDirectory(depositDir);
		job.setMetsSipSchema(metsSipSchema);
		job.setSchematronValidator(validator);
		setField(job, "dataset", dataset);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "jobStatusFactory", jobStatusFactory);
		setField(job, "depositStatusFactory", depositStatusFactory);

		job.init();
	}

	@Test
	public void test() {

		DepositTestUtils.makeTestDir(depositsDirectory, job.getDepositUUID(),
				new File("src/test/resources/biomedDspaceMETS.zip"));

		long start = System.currentTimeMillis();
		job.run();
		log.info("Run dspace mets: {}", (System.currentTimeMillis() - start));

		Model model = job.getWritableModel();
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
	
	@Test
	public void testSuccessful() throws Exception {

		job.setDepositUUID("ad42cdd6-69c1-444b-9291-9374b40bf7fb");

		job.init();

		DepositTestUtils.makeTestDir(depositsDirectory, job.getDepositUUID(), new File(
				"src/test/resources/biomedDspaceMETS.zip"));

		long start = System.currentTimeMillis();
		job.run();
		log.info("Successful: {}", (System.currentTimeMillis() - start));

		//assertTrue("N3 model file must exist after conversion", everythingFile.exists());

		Model model = job.getReadOnlyModel();
		
		assertFalse("Model was empty", model.isEmpty());

		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource primaryResource = (Resource) depositBag.iterator().next();

		File descriptionFile = new File(job.getDescriptionDir(), new PID(primaryResource.getURI()).getUUID() + ".xml");
		assertTrue("Descriptive metadata file did not exist", descriptionFile.exists());

		// Check that labels were assigned to the children
		Property labelP = model.createProperty(DepositRelationship.label.getURI().toString());
		NodeIterator childIt = model.getBag(primaryResource).iterator();
		while (childIt.hasNext()) {
			Resource child = childIt.nextNode().asResource();

			assertNotNull("Supplemental should have been assigned a label", child.getProperty(labelP));
		}
	}

	@Test
	public void testExistingMODS() throws Exception {
		job.setDepositUUID("ad42cdd6-69c1-444b-9291-9374b40bf7fb");

		job.init();

		DepositTestUtils.makeTestDir(depositsDirectory, job.getDepositUUID(), new File(
				"src/test/resources/biomedDspaceMETS.zip"));

		long start = System.currentTimeMillis();
		job.run();
		log.info("Existing mods: {}", (System.currentTimeMillis() - start));
		
		Model m = job.getReadOnlyModel();
		Bag depositBag = m.getBag(job.getDepositPID().getURI());
		Resource primaryResource = depositBag.iterator().nextNode().asResource();
		
		File descriptionFile = new File(job.getDescriptionDir(), new PID(primaryResource.getURI()).getUUID() + ".xml");

		assertTrue("Descriptive metadata file did not exist", descriptionFile.exists());

		SAXBuilder sb = new SAXBuilder(false);
		Document modsDoc = sb.build(descriptionFile);

		List<?> originalNameObjects = xpath("//mods:namePart[text()='Test']", modsDoc);
		assertEquals("Original name element should have been removed", 0, originalNameObjects.size());

		List<?> nameObjects = xpath("//mods:namePart", modsDoc);
		assertTrue(nameObjects.size() > 0);

		List<?> languageTerms = xpath("//mods:languageTerm", modsDoc);
		assertEquals("Original language should have been retained", 1, languageTerms.size());
	}
}

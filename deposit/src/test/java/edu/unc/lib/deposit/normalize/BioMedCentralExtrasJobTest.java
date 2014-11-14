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
import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.defaultWebObject;
import static edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty.label;
import static edu.unc.lib.dl.util.MetadataProfileConstants.BIOMED_ARTICLE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;

import edu.unc.lib.deposit.DepositTestUtils;
import edu.unc.lib.dl.fedora.PID;

/**
 * @author bbpennel
 * @date Jun 17, 2014
 */
public class BioMedCentralExtrasJobTest extends AbstractNormalizationJobTest {

	private static final Logger log = LoggerFactory.getLogger(BioMedCentralExtrasJobTest.class);

	private BioMedCentralExtrasJob job;

	@Before
	public void init() {

		Dataset dataset = TDBFactory.createDataset();

		job = new BioMedCentralExtrasJob();
		job.setDepositUUID(depositUUID);
		job.setDepositDirectory(depositDir);
		setField(job, "dataset", dataset);
		setField(job, "depositsDirectory", depositsDirectory);

	}

	@Test
	public void testSuccessful() throws Exception {

		job.setDepositUUID("ad42cdd6-69c1-444b-9291-9374b40bf7fb");

		job.init();

		DepositTestUtils.makeTestDir(depositsDirectory, job.getDepositUUID(), new File(
				"src/test/resources/biomedDspaceMETS.zip"));

		Model m = job.getWritableModel();
		File testModel = new File("src/test/resources/aggregate-deposit.n3");
		m.read(testModel.toURI().toURL().toString());
		job.closeModel();

		long start = System.currentTimeMillis();
		job.run();
		log.info("Successful: {}", (System.currentTimeMillis() - start));

		//assertTrue("N3 model file must exist after conversion", everythingFile.exists());

		Model model = job.getWritableModel();
		assertFalse("Model was empty", model.isEmpty());

		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource primaryResource = (Resource) depositBag.iterator().next();

		// Check for default web object
		Resource dwo = primaryResource.getProperty(model.createProperty(defaultWebObject.toString())).getResource();
		assertNotNull("Default web object was not set", dwo);

		File descriptionFile = new File(job.getDescriptionDir(), new PID(primaryResource.getURI()).getUUID() + ".xml");
		assertTrue("Descriptive metadata file did not exist", descriptionFile.exists());

		verifyMetadataSourceAssigned(model, primaryResource, job.getDepositDirectory(), BIOMED_ARTICLE, ".xml");

		// Check that labels were assigned to the children
		Property labelP = model.createProperty(label.getURI().toString());
		NodeIterator childIt = model.getBag(primaryResource).iterator();
		while (childIt.hasNext()) {
			Resource child = childIt.nextNode().asResource();

			if (child.equals(dwo)) {
				assertNull("Default web object should not have a label", child.getProperty(labelP));
			} else {
				// assertNotNull("Supplemental should have been assigned a label", child.getProperty(labelP));
			}
		}
	}

	@Test
	public void testExistingMODS() throws Exception {
		job.setDepositUUID("ad42cdd6-69c1-444b-9291-9374b40bf7fb");

		job.init();

		DepositTestUtils.makeTestDir(depositsDirectory, job.getDepositUUID(), new File(
				"src/test/resources/biomedDspaceMETS.zip"));

		Model m = job.getWritableModel();
		File testModel = new File("src/test/resources/aggregate-deposit.n3");
		m.read(testModel.toURI().toURL().toString());
		job.closeModel();

		job.getDescriptionDir().mkdir();
		File descriptionFile = new File(job.getDescriptionDir(), "c647de74-bf11-41fd-acf1-9da03dc9e6ad.xml");
		Files.copy(Paths.get("src/test/resources/simpleMods.xml"), descriptionFile.toPath());

		long start = System.currentTimeMillis();
		job.run();
		log.info("Existing mods: {}", (System.currentTimeMillis() - start));

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

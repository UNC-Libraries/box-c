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

import static edu.unc.lib.deposit.normalize.Proquest2N3BagJob.DATA_SUFFIX;
import static edu.unc.lib.dl.test.TestHelpers.setField;
import static edu.unc.lib.dl.util.MetadataProfileConstants.PROQUEST_ETD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Test;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.tdb.TDBFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrAcl;
import edu.unc.lib.dl.rdf.CdrDeposit;

/**
 * @author bbpennel
 * @date Apr 25, 2014
 */
public class Proquest2N3BagJobTest extends AbstractNormalizationJobTest {

	private Transformer proquest2ModsTransformer;

	private Proquest2N3BagJob job;

	@Before
	public void setup() throws Exception {

		TransformerFactory factory = TransformerFactory.newInstance();
		URL xslURL = this.getClass().getResource("/proquest-to-mods.xsl");
		StreamSource xslStream = new StreamSource();
		xslStream.setSystemId(xslURL.toExternalForm());
		proquest2ModsTransformer = factory.newTransformer(xslStream);
		
		Dataset dataset = TDBFactory.createDataset();

		job = new Proquest2N3BagJob();
		job.setDepositUUID(depositUUID);
		job.setDepositDirectory(depositDir);
		job.setProquest2ModsTransformer(proquest2ModsTransformer);
		job.setRepository(repo);
		setField(job, "dataset", dataset);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "jobStatusFactory", jobStatusFactory);
		setField(job, "depositStatusFactory", depositStatusFactory);

		job.init();
	}

	@Test
	public void testNoAttachments() throws Exception {
		copyTestPackage("src/test/resources/proquest-noattach.zip", job);

		job.run();

		Model model = job.getWritableModel();
		assertFalse("Model was empty", model.isEmpty());

		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		assertNotNull("Deposit object not found", depositBag);

		Resource primaryResource = (Resource) depositBag.iterator().next();

		testNoAttachments(model, primaryResource);

	}

	@SuppressWarnings("deprecation")
	public void testNoAttachments(Model model, Resource primaryResource) throws Exception {

		assertNotNull("Main object from the deposit not found", primaryResource);

		// Check that the main content file is assigned to the primary resource
		verifyStagingLocationExists(primaryResource, job.getDepositDirectory(), "Content");

		verifyMetadataSourceAssigned(model, primaryResource, job.getDepositDirectory(), PROQUEST_ETD, DATA_SUFFIX);

		// Verify that the MODS was created
		File descriptionFile = new File(job.getDescriptionDir(), new PID(primaryResource.getURI()).getUUID() + ".xml");
		assertTrue("Descriptive metadata file did not exist", descriptionFile.exists());

		SAXBuilder sb = new SAXBuilder(false);
		Document modsDoc = sb.build(descriptionFile);

		Element semesterEl = element("/mods:mods/mods:note[@type='thesis']", modsDoc);
		assertEquals("Graduation semester was not correctly set", "Spring 2011", semesterEl.getText());

		Element pubDateEl = element("/mods:mods/mods:originInfo/mods:dateIssued", modsDoc);
		assertEquals("Publication date was not correctly set", "2013", pubDateEl.getText());
	}

	@Test
	public void testWithAttachments() {
		copyTestPackage("src/test/resources/proquest-attach.zip", job);

		job.run();

		Model model = job.getWritableModel();
		assertFalse("Model was empty", model.isEmpty());

		Bag depositBag = model.getBag(job.getDepositPID().getURI());

		Resource primaryResource = (Resource) depositBag.iterator().next();

		testWithAttachments(model, primaryResource);
	}

	private void testWithAttachments(Model model, Resource primaryResource) {

		Bag primaryBag = model.getBag(primaryResource);

		assertNotNull("Main object from the deposit not found", primaryResource);

		verifyMetadataSourceAssigned(model, primaryResource, job.getDepositDirectory(), PROQUEST_ETD, DATA_SUFFIX);

		// Verify that the MODS was created
		File descriptionFile = new File(job.getDescriptionDir(), new PID(primaryResource.getURI()).getUUID() + ".xml");
		assertTrue("Descriptive metadata file did not exist", descriptionFile.exists());

		// Check for default web object
		Resource dwo = primaryResource.getProperty(Cdr.primaryObject).getResource();
		assertNotNull("Default web object was not set", dwo);

		// Make sure the content file is assigned as a child rather than a data stream of the primary resource
		assertNull("Content file incorrectly assigned to primary resource", primaryResource.getProperty(CdrDeposit.stagingLocation));
		// Check that the content is assigned to the default web object
		String dwoLocation = dwo.getProperty(CdrDeposit.stagingLocation).getString();
		assertTrue("Default web object file did not exist", new File(job.getDepositDirectory(), dwoLocation).exists());

		// Check that attachments were added
		NodeIterator childIt = primaryBag.iterator();
		int countChildren = 0;
		while (childIt.hasNext()) {
			countChildren++;
			Resource child = (Resource) childIt.next();

			// Make sure all of the children have valid staging locations assigned
			File childFile = verifyStagingLocationExists(child, job.getDepositDirectory(), "Child content");

			// Make sure the label is being set, using the description if provided
			if ("attached1.pdf".equals(childFile.getName())) {
				assertEquals("Provided label was not set for child", "Attached pdf", child.getProperty(CdrDeposit.label)
						.getString());
			} else {
				assertEquals("File name not set as child label", childFile.getName(), child.getProperty(CdrDeposit.label)
						.getString());
			}
		}

		assertEquals("Incorrect aggregate child count", 3, countChildren);
	}

	/**
	 * Test active embargo starting from the current timestamp
	 */
	@Test
	public void testEmbargoedUsingCurrentTime() {
		// Forever 2014, as far as this test is concerned
		DateTime newTime = new DateTime(2014, 5, 5, 0, 0, 0, 0);
		DateTimeUtils.setCurrentMillisFixed(newTime.getMillis());

		copyTestPackage("src/test/resources/proquest-embargo.zip", job);

		job.run();

		Model model = job.getWritableModel();
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource primaryResource = (Resource) depositBag.iterator().next();

		String embargoValue = primaryResource.getProperty(CdrAcl.embargoUntil).getString();
		assertEquals("Embargo value did not match the expected valued", "2015-05-05T00:00:00", embargoValue);

		// Restore the system clock
		DateTimeUtils.setCurrentMillisSystem();
	}

	/**
	 * Test an active embargo assignment coming from the graduation date rather than the deposit time
	 */
	@Test
	public void testEmbargoedUsingCompDate() {
		// Fake the current time to be a little bit after the 2014 graduation date
		DateTime newTime = new DateTime(2015, 1, 1, 0, 0, 0, 0);
		DateTimeUtils.setCurrentMillisFixed(newTime.getMillis());

		copyTestPackage("src/test/resources/proquest-embargo.zip", job);

		job.run();

		Model model = job.getWritableModel();
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource primaryResource = (Resource) depositBag.iterator().next();

		String embargoValue = primaryResource.getProperty(CdrAcl.embargoUntil).getString();
		assertEquals("Embargo value did not match the graduation date + 1 year", "2015-12-31T00:00:00", embargoValue);

		// Restore the system clock
		DateTimeUtils.setCurrentMillisSystem();
	}

	/**
	 * Test an active embargo assignment coming from the graduation date rather than the deposit time
	 */
	@Test
	public void testEmbargoInactive() {
		// Fake the current time to be way after the graduation date
		DateTime newTime = new DateTime(2016, 1, 1, 0, 0, 0, 0);
		DateTimeUtils.setCurrentMillisFixed(newTime.getMillis());

		copyTestPackage("src/test/resources/proquest-embargo.zip", job);

		job.run();

		Model model = job.getWritableModel();
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource primaryResource = (Resource) depositBag.iterator().next();

		assertNull("No embargo should be set since it has expired", primaryResource.getProperty(CdrAcl.embargoUntil));

		// Restore the system clock
		DateTimeUtils.setCurrentMillisSystem();
	}

	@Test
	public void testLongEmbargoed() {
		// Forever 2014, as far as this test is concerned
		DateTime newTime = new DateTime(2014, 5, 5, 0, 0, 0, 0);
		DateTimeUtils.setCurrentMillisFixed(newTime.getMillis());

		copyTestPackage("src/test/resources/proquest-embargo-code4.zip", job);

		job.run();

		Model model = job.getWritableModel();
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource primaryResource = (Resource) depositBag.iterator().next();

		String embargoValue = primaryResource.getProperty(CdrAcl.embargoUntil).getString();
		assertEquals("Embargo value did not match the expected valued", "2016-05-05T00:00:00", embargoValue);

		// Restore the system clock
		DateTimeUtils.setCurrentMillisSystem();
	}

	@Test
	public void testMultiplePackages() throws Exception {
		copyTestPackage("src/test/resources/proquest-noattach.zip", job);
		copyTestPackage("src/test/resources/proquest-attach.zip", job);

		job.run();

		Model model = job.getWritableModel();
		assertFalse("Model was empty", model.isEmpty());

		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		assertNotNull("Deposit object not found", depositBag);

		int childCount = 0;
		NodeIterator primaryIt = depositBag.iterator();
		while (primaryIt.hasNext()) {
			childCount++;

			Resource primaryResource = (Resource) primaryIt.next();

			Statement labelStatement = primaryResource.getProperty(CdrDeposit.label);
			if (labelStatement != null && labelStatement.getString().contains("noattach")) {
				this.testNoAttachments(model, primaryResource);
			} else {
				this.testWithAttachments(model, primaryResource);
			}
		}

		assertEquals("Incorrect number of objects in the deposit", 2, childCount);
	}


}

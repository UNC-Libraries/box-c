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

import static edu.unc.lib.deposit.work.DepositGraphUtils.cdrprop;
import static edu.unc.lib.deposit.work.DepositGraphUtils.dprop;
import static edu.unc.lib.deposit.work.DepositGraphUtils.fprop;
import static edu.unc.lib.dl.test.TestHelpers.setField;
import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.defaultWebObject;
import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.embargoUntil;
import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.hasSourceMetadataProfile;
import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.sourceMetadata;
import static edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship.hasDatastream;
import static edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship.label;
import static edu.unc.lib.dl.util.ContentModelHelper.DepositRelationship.stagingLocation;
import static edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty.hasModel;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.AGGREGATE_WORK;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.CONTAINER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.deposit.work.JobStatusFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.DepositStatusFactory;

/**
 * @author bbpennel
 * @date Apr 25, 2014
 */
public class Proquest2N3BagJobTest {

	@Rule
	public final TemporaryFolder tmpFolder = new TemporaryFolder();

	@Mock
	private JobStatusFactory jobStatusFactory;
	@Mock
	private DepositStatusFactory depositStatusFactory;

	private File depositsDirectory;

	private Transformer proquest2ModsTransformer;

	private Proquest2N3BagJob job;

	@Before
	public void setup() throws Exception {

		initMocks(this);

		depositsDirectory = tmpFolder.newFolder("deposits");

		String depositUUID = UUID.randomUUID().toString();
		File depositDir = new File(depositsDirectory, depositUUID);
		depositDir.mkdir();

		TransformerFactory factory = TransformerFactory.newInstance();
		URL xslURL = this.getClass().getResource("/proquest/proquest-to-mods.xsl");
		StreamSource xslStream = new StreamSource();
		xslStream.setSystemId(xslURL.toExternalForm());
		proquest2ModsTransformer = factory.newTransformer(xslStream);

		job = new Proquest2N3BagJob();
		job.setDepositUUID(depositUUID);
		job.setDepositDirectory(depositDir);
		job.setProquest2ModsTransformer(proquest2ModsTransformer);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "jobStatusFactory", jobStatusFactory);
		setField(job, "depositStatusFactory", depositStatusFactory);

		job.init();
	}

	@Test
	public void testNoAttachments() {
		copyTestPackage("src/test/resources/proquest-noattach.zip");

		job.run();

		Model model = getModel(job);
		assertFalse("Model was empty", model.isEmpty());

		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		assertNotNull("Deposit object not found", depositBag);

		Resource primaryResource = (Resource) depositBag.iterator().next();

		testNoAttachments(model, primaryResource);

	}

	public void testNoAttachments(Model model, Resource primaryResource) {
		Property stagingLoc = dprop(model, stagingLocation);

		assertNotNull("Main object from the deposit not found", primaryResource);

		// Check that the main content file is assigned to the primary resource
		verifyStagingLocationExists(primaryResource, stagingLoc, job.getDepositDirectory(), "Content");

		verifyMetadataSourceAssigned(model, primaryResource, job.getDepositDirectory());

		// Verify that the MODS was created
		File descriptionFile = new File(job.getDescriptionDir(), new PID(primaryResource.getURI()).getUUID() + ".xml");
		assertTrue("Descriptive metadata file did not exist", descriptionFile.exists());
	}

	@Test
	public void testWithAttachments() {
		copyTestPackage("src/test/resources/proquest-attach.zip");

		job.run();

		Model model = getModel(job);
		assertFalse("Model was empty", model.isEmpty());

		Bag depositBag = model.getBag(job.getDepositPID().getURI());

		Resource primaryResource = (Resource) depositBag.iterator().next();

		testWithAttachments(model, primaryResource);
	}

	private void testWithAttachments(Model model, Resource primaryResource) {
		Property stagingLoc = dprop(model, stagingLocation);
		Property hasContentModel = fprop(model, hasModel);
		Property labelProperty = dprop(model, label);

		Bag primaryBag = model.getBag(primaryResource);

		assertNotNull("Main object from the deposit not found", primaryResource);

		verifyMetadataSourceAssigned(model, primaryResource, job.getDepositDirectory());

		// Verify that the MODS was created
		File descriptionFile = new File(job.getDescriptionDir(), new PID(primaryResource.getURI()).getUUID() + ".xml");
		assertTrue("Descriptive metadata file did not exist", descriptionFile.exists());

		// Make sure the object is an aggregate
		StmtIterator cmIt = primaryResource.listProperties(hasContentModel);
		boolean isAggregate = false, isContainer = false;
		while (cmIt.hasNext()) {
			String cmValue = cmIt.next().getResource().getURI();
			if (AGGREGATE_WORK.equals(cmValue)) {
				isAggregate = true;
			}
			if (CONTAINER.equals(cmValue)) {
				isContainer = true;
			}
		}
		assertTrue("Primary resource was not assigned aggregate content model", isAggregate);
		assertTrue("Primary resource was not assigned container content model", isContainer);

		// Check for default web object
		Resource dwo = primaryResource.getProperty(model.createProperty(defaultWebObject.toString())).getResource();
		assertNotNull("Default web object was not set", dwo);

		// Make sure the content file is assigned as a child rather than a data stream of the primary resource
		assertNull("Content file incorrectly assigned to primary resource", primaryResource.getProperty(stagingLoc));
		// Check that the content is assigned to the default web object
		String dwoLocation = dwo.getProperty(stagingLoc).getString();
		assertTrue("Default web object file did not exist", new File(job.getDepositDirectory(), dwoLocation).exists());

		// Check that attachments were added
		NodeIterator childIt = primaryBag.iterator();
		int countChildren = 0;
		while (childIt.hasNext()) {
			countChildren++;
			Resource child = (Resource) childIt.next();

			// Make sure all of the children have valid staging locations assigned
			File childFile = verifyStagingLocationExists(child, stagingLoc, job.getDepositDirectory(), "Child content");

			// Make sure the label is being set, using the description if provided
			if ("attached1.pdf".equals(childFile.getName())) {
				assertEquals("Provided label was not set for child", "Attached pdf", child.getProperty(labelProperty)
						.getString());
			} else {
				assertEquals("File name not set as child label", childFile.getName(), child.getProperty(labelProperty)
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

		copyTestPackage("src/test/resources/proquest-embargo.zip");

		job.run();

		Model model = getModel(job);
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource primaryResource = (Resource) depositBag.iterator().next();

		Property embargoUntilP = cdrprop(model, embargoUntil);
		String embargoValue = primaryResource.getProperty(embargoUntilP).getString();
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

		copyTestPackage("src/test/resources/proquest-embargo.zip");

		job.run();

		Model model = getModel(job);
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource primaryResource = (Resource) depositBag.iterator().next();

		Property embargoUntilProperty = cdrprop(model, embargoUntil);
		String embargoValue = primaryResource.getProperty(embargoUntilProperty).getString();
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

		copyTestPackage("src/test/resources/proquest-embargo.zip");

		job.run();

		Model model = getModel(job);
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource primaryResource = (Resource) depositBag.iterator().next();

		Property embargoUntilProperty = cdrprop(model, embargoUntil);
		assertNull("No embargo should be set since it has expired", primaryResource.getProperty(embargoUntilProperty));

		// Restore the system clock
		DateTimeUtils.setCurrentMillisSystem();
	}

	private void verifyMetadataSourceAssigned(Model model, Resource primaryResource, File depositDirectory) {
		Property stagingLoc = dprop(model, stagingLocation);
		Property hasSourceMetadata = cdrprop(model, hasSourceMetadataProfile);
		Property sourceMD = cdrprop(model, sourceMetadata);
		Property hasDS = dprop(model, hasDatastream);

		assertEquals("Did not have metadata source type", "proquest", primaryResource.getProperty(hasSourceMetadata)
				.getLiteral().getString());

		// Verify that the metadata source attribute is present and transitively points to the file
		Resource sourceMDResource = primaryResource.getProperty(sourceMD).getResource();
		assertNotNull("Source metdata was not assigned to main resource", sourceMDResource);

		File sourceMDFile = verifyStagingLocationExists(sourceMDResource, stagingLoc, depositDirectory,
				"Original metadata");
		assertTrue("Original metadata file did not have the correct suffix, most likely the wrong file",
				sourceMDFile.getName().endsWith(Proquest2N3BagJob.DATA_SUFFIX));

		// Verify that the extra datastream being added is the same as the source metadata
		String sourceMDDatastream = primaryResource.getProperty(hasDS).getResource().getURI();
		assertEquals("Source datastream path did not match the sourceMetadata", sourceMDResource.getURI(),
				sourceMDDatastream);
	}

	@Test
	public void testMultiplePackages() {
		copyTestPackage("src/test/resources/proquest-noattach.zip");
		copyTestPackage("src/test/resources/proquest-attach.zip");

		job.run();

		Model model = getModel(job);
		assertFalse("Model was empty", model.isEmpty());

		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		assertNotNull("Deposit object not found", depositBag);

		Property labelProperty = dprop(model, label);

		int childCount = 0;
		NodeIterator primaryIt = depositBag.iterator();
		while (primaryIt.hasNext()) {
			childCount++;

			Resource primaryResource = (Resource) primaryIt.next();

			Statement labelStatement = primaryResource.getProperty(labelProperty);
			if (labelStatement != null && labelStatement.getString().contains("noattach")) {
				this.testNoAttachments(model, primaryResource);
			} else {
				this.testWithAttachments(model, primaryResource);
			}
		}

		assertEquals("Incorrect number of objects in the deposit", 2, childCount);
	}

	private File verifyStagingLocationExists(Resource resource, Property stagingLoc, File depositDirectory,
			String fileLabel) {
		String filePath = resource.getProperty(stagingLoc).getLiteral().getString();
		File file = new File(depositDirectory, filePath);
		assertTrue(fileLabel + " file did not exist", file.exists());

		return file;
	}

	private Model getModel(AbstractDepositJob job) {
		File modelFile = new File(job.getDepositDirectory(), DepositConstants.MODEL_FILE);
		Model model = ModelFactory.createDefaultModel();
		model.read(modelFile.toURI().toString());

		return model;
	}

	private void copyTestPackage(String filename) {
		job.getDataDirectory().mkdir();
		Path packagePath = Paths.get(filename);
		try {
			Files.copy(packagePath, job.getDataDirectory().toPath().resolve(packagePath.getFileName()));
		} catch (Exception e) {
		}
	}
}

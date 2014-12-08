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
package edu.unc.lib.deposit.fcrepo3;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.defaultWebData;
import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.defaultWebObject;
import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.hasSourceMetadataProfile;
import static edu.unc.lib.dl.util.ContentModelHelper.CDRProperty.sourceData;
import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.DATA_FILE;
import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.MD_DESCRIPTIVE;
import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.MD_EVENTS;
import static edu.unc.lib.dl.util.ContentModelHelper.Datastream.MD_SOURCE;
import static edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty.hasModel;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.DEPOSIT_RECORD;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.PRESERVEDOBJECT;
import static edu.unc.lib.dl.util.ContentModelHelper.Model.SIMPLE;
import static edu.unc.lib.dl.util.ContentModelHelper.Relationship.contains;
import static edu.unc.lib.dl.util.ContentModelHelper.Relationship.originalDeposit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Bag;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.tdb.TDBFactory;

import edu.unc.lib.deposit.DepositTestUtils;
import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.DepositConstants;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.JobStatusFactory;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * @author bbpennel
 * @date Apr 29, 2014
 */
public class MakeFOXMLTest {

	@Rule
	public final TemporaryFolder tmpFolder = new TemporaryFolder();

	@Mock
	private JobStatusFactory jobStatusFactory;
	@Mock
	private DepositStatusFactory depositStatusFactory;

	private File depositsDirectory;

	private MakeFOXML job;

	@Before
	public void setup() throws Exception {

		initMocks(this);

		depositsDirectory = tmpFolder.newFolder("deposits");

		String depositUUID = UUID.randomUUID().toString();
		File depositDir = new File(depositsDirectory, depositUUID);
		depositDir.mkdir();
	}

	private void initializeJob(String depositUUID, String ingestZipPath) {
		String workDir = DepositTestUtils.makeTestDir(depositsDirectory, depositUUID, new File(ingestZipPath));

		Dataset dataset = TDBFactory.createDataset();

		job = new MakeFOXML();
		job.setDepositUUID(depositUUID);
		setField(job, "dataset", dataset);
		setField(job, "depositsDirectory", depositsDirectory);
		setField(job, "jobStatusFactory", jobStatusFactory);
		setField(job, "depositStatusFactory", depositStatusFactory);

		job.init();

		Model model = job.getWritableModel();
		model.read(new File(workDir, "everything.n3").getAbsolutePath());
		job.closeModel();

	}

	@Test
	public void testProquestAggregateBag() throws Exception {
		String depositUUID = "55c262bf-9f15-4184-9979-3d8816d40103";

		initializeJob(depositUUID, "src/test/resources/ingest-bags/proquest-bag.zip");

		job.run();

		// Verify that the foxml directory was created and populated
		File foxmlDir = new File(job.getDepositDirectory(), DepositConstants.FOXML_DIR);
		assertTrue("Foxml directory did not exist", foxmlDir.exists());

		File foxmlFiles[] = foxmlDir.listFiles();
		assertEquals("Incorrect number of foxml files generated", 5, foxmlFiles.length);

		// Check that the deposit record entry was generated and has the correct model
		File depositRecordFile = new File(foxmlDir, depositUUID + ".xml");
		assertTrue("Deposit record foxml did not exist", depositRecordFile.exists());

		Document depositFOXML = deserializeFOXML(depositRecordFile);
		Element depositRelsExt = FOXMLJDOMUtil.getRelsExt(depositFOXML);
		assertEquals("Deposit record did not have correct content model set", DEPOSIT_RECORD.toString(),
				FOXMLJDOMUtil.getRelationValue(hasModel.name(), hasModel.getNamespace(), depositRelsExt));

		// Verify that the events file is added
		verifyDatastreamExists(depositFOXML, MD_EVENTS, job);

		// Track down the aggregate object record
		Model model = getModel(job);
		Bag depositBag = model.getBag(job.getDepositPID().getURI());
		Resource aggResource = (Resource) depositBag.iterator().next();
		Bag aggBag = model.getBag(aggResource);

		File aggFile = new File(foxmlDir, getUUID(aggResource) + ".xml");
		assertTrue("Main aggregate foxml did not exist", aggFile.exists());

		Document aggFOXML = deserializeFOXML(aggFile);
		Element aggRelsExt = FOXMLJDOMUtil.getRelsExt(aggFOXML);

		String metadataProfile = FOXMLJDOMUtil.getRelationValue(hasSourceMetadataProfile.name(),
				hasSourceMetadataProfile.getNamespace(), aggRelsExt);
		assertNotNull("Metadata profile was not set", metadataProfile);

		// Make sure contains were set (from the DepositRelationship)
		List<String> containsValues = FOXMLJDOMUtil.getRelationValues(contains.name(), contains.getNamespace(),
				aggRelsExt);
		// Checking that all the expected children were added and their foxml were all generated
		NodeIterator childIt = aggBag.iterator();
		while (childIt.hasNext()) {
			String childURI = childIt.next().asResource().getURI();
			assertTrue("Expected contains relation to " + childURI + " not found", containsValues.contains(childURI));

			PID childPID = new PID(childURI);
			File childFile = new File(foxmlDir, childPID.getUUID() + ".xml");
			assertTrue("Aggregate child's foxml did not exist", childFile.exists());

			Document childFOXML = deserializeFOXML(childFile);
			Element childRelsExt = FOXMLJDOMUtil.getRelsExt(childFOXML);
			// Check that it has been assigned the necessary content models
			List<String> childModels = FOXMLJDOMUtil.getRelationValues(hasModel.name(), hasModel.getNamespace(), childRelsExt);
			assertTrue("Simple object model was not added", childModels.contains(SIMPLE.toString()));
			assertTrue("Preserved object model was not added", childModels.contains(PRESERVEDOBJECT.toString()));

			// Check that the default datastream, DATA_FILE was added
			verifyDatastreamExists(childFOXML, DATA_FILE, job);

			assertNotNull("Default web data was not set for child",
					FOXMLJDOMUtil.getRelationValue(defaultWebData.name(), defaultWebData.getNamespace(), childRelsExt));
			assertNotNull("Source data was not set for child",
					FOXMLJDOMUtil.getRelationValue(sourceData.name(), sourceData.getNamespace(), childRelsExt));

			assertEquals("Original deposit link not set", job.getDepositPID().getURI(),
					FOXMLJDOMUtil.getRelationValue(originalDeposit.name(), originalDeposit.getNamespace(), childRelsExt));
		}

		// Verify that extra datastreams are getting set
		verifyDatastreamExists(aggFOXML, MD_SOURCE, job);

		// Check that the implied descriptive metadata was added
		verifyDatastreamExists(aggFOXML, MD_DESCRIPTIVE, job);

		// Make sure that the data file was not mistakenly added to the aggregate object
		assertNull("DATA_FILE added to incorrect location", FOXMLJDOMUtil.getDatastream(aggFOXML, DATA_FILE.getName()));

		// Check for the default web object
		String dwoValue = FOXMLJDOMUtil.getRelationValue(defaultWebObject.name(), defaultWebObject.getNamespace(),
				aggRelsExt);
		PID dwoPID = new PID(dwoValue);
		File dwoFile = new File(foxmlDir, dwoPID.getUUID() + ".xml");
		assertTrue("Default web object foxml did not exist", dwoFile.exists());

		// Check that the right number of clicks were registered
		verify(jobStatusFactory).setTotalCompletion(anyString(), eq(5));
		verify(jobStatusFactory, times(5)).incrCompletion(anyString(), eq(1));
	}

	private void verifyDatastreamExists(Document foxml, ContentModelHelper.Datastream datastream, AbstractDepositJob job)
			throws Exception {
		Element dsEl = FOXMLJDOMUtil.getDatastream(foxml, datastream.getName());
		assertNotNull("Datastream " + datastream.getName() + " was not added to the FOXML", dsEl);

		// Check that the staging location is being set
		Element dsLocation = dsEl.getChild("datastreamVersion", JDOMNamespaceUtil.FOXML_NS).getChild("contentLocation",
				JDOMNamespaceUtil.FOXML_NS);
		if (dsLocation == null) {
			return;
		}
		
		String dsLocationValue = dsLocation.getAttributeValue("REF");
		File dsFile = getFileForUrl(dsLocationValue, job.getDepositDirectory());
		
		assertTrue("Location referenced by datastream not found", dsFile.exists());
	}

	private String getUUID(Resource resource) {
		return new PID(resource.getURI()).getUUID();
	}

	private Model getModel(AbstractDepositJob job) {
		return job.getWritableModel();
	}

	private Document deserializeFOXML(File foxml) {
		// Load objects foxml
		SAXBuilder builder = new SAXBuilder();
		try {
			return builder.build(foxml);
		} catch (Exception e) {
		}
		return null;
	}
	
	private static Pattern filePathPattern = null;

	static {
		filePathPattern = Pattern.compile("^(file:)?([/\\\\]{0,3})?(.+)$");
	}

	/**
	 * Safely returns a File object for a "file:" URL. The base and root directories are both considered to be the dir.
	 * URLs that point outside of that directory will throw an IOException.
	 *
	 * @param url
	 *           the URL
	 * @param dir
	 *           the directory within which to resolve the "file:" URL.
	 * @return a File object
	 * @throws IOException
	 *            when URL improperly formatted or points outside of the dir.
	 */
	public static File getFileForUrl(String url, File dir) throws IOException {
		File result = null;

		// remove any file: prefix and beginning slashes or backslashes
		Matcher m = filePathPattern.matcher(url);

		if (m.find()) {
			String path = m.group(3); // grab the path group
			path = path.replaceAll("\\\\", File.pathSeparator);
			result = new File(dir, path);
			if (result.getCanonicalPath().startsWith(dir.getCanonicalPath())) {
				return result;
			} else {
				throw new IOException("Bad locator for a file in SIP:" + url);
			}
		} else {
			throw new IOException("Bad locator for a file in SIP:" + url);
		}
	}
}

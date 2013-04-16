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
package edu.unc.lib.dl.cdr.services.techmd;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.util.UriUtils;

import edu.unc.lib.dl.cdr.services.Enhancement;
import edu.unc.lib.dl.cdr.services.model.EnhancementMessage;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ManagementClient;
import edu.unc.lib.dl.fedora.ManagementClient.ChecksumType;
import edu.unc.lib.dl.fedora.ManagementClient.Context;
import edu.unc.lib.dl.fedora.ManagementClient.Format;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.fedora.types.Datastream;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.JMSMessageUtil;
import edu.unc.lib.dl.util.TripleStoreQueryService;

/**
 * Copyright 2010 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @author Gregory Jansen
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context-it.xml" })
public class TechnicalMetadataEnhancementServiceITCase {

	private static final Logger LOG = LoggerFactory.getLogger(TechnicalMetadataEnhancementServiceITCase.class);

	@Resource
	private TripleStoreQueryService tripleStoreQueryService = null;

	@Resource
	private ManagementClient managementClient = null;

	@Resource
	private TechnicalMetadataEnhancementService technicalMetadataEnhancementService = null;

	private Set<PID> samples = new HashSet<PID>();

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	private EnhancementMessage ingestSample(String filename, String dataFilename, String mimetype) throws Exception {
		String altIDPrefixSample = "irods://mtuomala@cdr-vault.libint.unc.edu:3333/cdrZone/home/mtuomala/staging/history/histdeptdigitalphotos/End%20of%20Year%20%20Party%202007/Originals/spacetest%20";
		File file = new File("src/test/resources", filename);
		Document doc = new SAXBuilder().build(new FileReader(file));
		PID pid = this.getManagementClient().ingest(doc, Format.FOXML_1_1, "ingesting test object");
		if (dataFilename != null) {
			List<String> altIDs = new ArrayList<String>();
			altIDs.add(altIDPrefixSample+UriUtils.encodePathSegment(dataFilename, "utf-8"));
			File dataFile = new File("src/test/resources", dataFilename);
			String uploadURI = this.getManagementClient().upload(dataFile);
			this.getManagementClient().addManagedDatastream(pid, "DATA_FILE", false, "Thumbnail Test",
					altIDs, dataFilename, true, mimetype, uploadURI);
			PID dataFilePID = new PID(pid.getPid() + "/DATA_FILE");
			this.getManagementClient().addObjectRelationship(pid,
					ContentModelHelper.CDRProperty.sourceData.getURI().toString(), dataFilePID);
		}
		EnhancementMessage result = new EnhancementMessage(pid, JMSMessageUtil.servicesMessageNamespace, 
				JMSMessageUtil.ServicesActions.APPLY_SERVICE_STACK.getName());
		samples.add(pid);
		return result;
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		try {
			for(PID p : samples) {
				this.getManagementClient().purgeObject(p, "removing test object", false);
			}
			samples.clear();
		} catch (FedoraException e) {
			e.printStackTrace();
			throw new Error("Cannot purge test object", e);
		}
	}

	@Test
	public void testFindCandidateObjects() throws Exception {
		EnhancementMessage pid1 = ingestSample("techmd-test-single.xml", "sample.pdf", "application/pdf");
		List<PID> results = this.getTechnicalMetadataEnhancementService().findCandidateObjects(50, 0);
		for (PID p : results) {
			LOG.debug("found candidate: " + p);
		}
		assertTrue("The query for candidate objects must return at least 1 result", results.size() > 0);
		assertTrue("The candidates must include our test object: " + pid1.getTargetID(), results.contains(pid1.getPid()));
	}

	@Test
	public void testIsApplicable() throws Exception {
		EnhancementMessage pid1 = ingestSample("techmd-test-single.xml", "sample.pdf", "application/pdf");
		TechnicalMetadataEnhancementService s = this.getTechnicalMetadataEnhancementService();
		// return false for a PID w/o sourcedata
		// return true for a PID w/o techData
		// return true for a PID w/techData older than sourceData
		// return false for a PID w/techData younger than sourceData
		assertTrue("The PID " + pid1 + " must return true.", s.isApplicable(pid1));
	}

	// @Test
	public void testJMSTriggeringServices() throws Exception {
		EnhancementMessage pid1 = ingestSample("techmd-test-single.xml", "sample.pdf", "application/pdf");
		// load bean context w/JMS attached to running fedora
		// ingest a file with generic mime type
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// check to make sure methods were called.
		// verify(this.getenhancementConductor(), atLeastOnce()).onMessage(any(Message.class));
		verify(this.technicalMetadataEnhancementService, atLeastOnce()).getEnhancement(pid1);

		// check to make sure mimetype was updated.
		// should be "image/x-mrsid-image"
		// try {
		// Datastream ds = this.getManagementClient().getDatastream(pid1, "DATA_FILE1SOURCE");
		// String fmime = ds.getMIMEType();
		// LOG.debug("found SOURCE MIMETYPE:"+fmime);
		// assertTrue("MimeType must be changed to something other than application/octet-stream, found: "+fmime,
		// !"application/octet-stream".equals(fmime));
		// } catch (FedoraException e) {
		// throw new Error(e);
		// }
		// check to make sure non source data was not updated
		// try {
		// Datastream ds = this.getManagementClient().getDatastream(pid1, "DATA_FILE2NOTSOURCE");
		// String fmime = ds.getMIMEType();
		// assertTrue("MimeType must not be changed from application/octet-stream, found: "+fmime,
		// "application/octet-stream".equals(fmime));
		// } catch (FedoraException e) {
		// throw new Error(e);
		// }
	}

	@Test
	public void testExtractionTask() throws Exception {
		EnhancementMessage pid1 = ingestSample("techmd-test-single.xml", "sample.pdf", "application/pdf");

		assertTrue("The test pid should be applicable before the service runs: " + pid1, this
				.getTechnicalMetadataEnhancementService().isApplicable(pid1));

		String originalVID = this.getManagementClient().getDatastream(pid1.getPid(), "DATA_FILE").getVersionID();
		Enhancement<Element> en = this.getTechnicalMetadataEnhancementService().getEnhancement(pid1);
		en.call();

		Datastream thb = this.getManagementClient().getDatastream(pid1.getPid(), "MD_TECHNICAL");
		assertNotNull("technical metadata stream must not be null", thb);

		String currentVID = this.getManagementClient().getDatastream(pid1.getPid(), "DATA_FILE").getVersionID();
		assertTrue("The data stream version ID must not change", originalVID.equals(currentVID));

		Document foxml = this.getManagementClient().export(Context.PUBLIC, Format.FOXML_1_1, pid1.getTargetID());
		String foxstr = new XMLOutputter().outputString(foxml);
		LOG.debug("FOXML AFTER TECHMD:\n" + foxstr);

		boolean stillApplicable = this.getTechnicalMetadataEnhancementService().isApplicable(pid1);
		assertFalse("A pid should no longer be applicable after the service runs: " + pid1, stillApplicable);

		List<PID> candidates = this.getTechnicalMetadataEnhancementService().findCandidateObjects(50, 0);
		assertFalse("Test pid should no longer be a candidate", candidates.contains(pid1.getPid()));

		// now update source
		File dataFile = new File("src/test/resources", "sample.pdf");
		String uploadURI = this.getManagementClient().upload(dataFile);
		this.getManagementClient().modifyDatastreamByReference(pid1.getPid(), "DATA_FILE", false, "Thumbnail Test",
				Collections.<String>emptyList(), "sample.pdf", "application/pdf", null, ChecksumType.DEFAULT, uploadURI);
		assertTrue("The test pid should be applicable again after source DS is updated: " + pid1, this
				.getTechnicalMetadataEnhancementService().isApplicable(pid1));
	}

	/**
	 * We should ignore Exiftool's identification of image/vnd.fpx and going with ppt from another tool.
	 * @throws Exception
	 */
	@Test
	public void testExtractionTaskWithIdentificationConflict() throws Exception {
		EnhancementMessage pidConflict = ingestSample("techmd-test-single-conflict.xml", "fits_conflict.ppt", "application/octet-stream");
		Enhancement<Element> en = this.getTechnicalMetadataEnhancementService().getEnhancement(pidConflict);
		en.call();
		Datastream thb = this.getManagementClient().getDatastream(pidConflict.getPid(), "MD_TECHNICAL");
		Document foxml = this.getManagementClient().getObjectXML(pidConflict.getPid());
		new XMLOutputter().output(foxml, System.err);
		assertNotNull("technical metadata stream must not be null", thb);
		List<String> mimes = this.getTripleStoreQueryService().fetchAllTriples(pidConflict.getPid()).get("http://cdr.unc.edu/definitions/1.0/base-model.xml#hasSourceMimeType");
		assertTrue("data stream mimetype must match powerpoint, but got: "+mimes.get(0), mimes.size() == 1 && mimes.get(0).contains("powerpoint"));
	}

	// irods://cdr-vault.libint.unc.edu:3333/cdrZone/home/eomeara/staging/Tucasi_live/originals/TUCASI/Vocabulary%20Development%20Week%202/Vocabulary%20Development%20Week%202%20(default).zip

	/**
	 * We should ignore Exiftool's identification of image/vnd.fpx and going with ppt from another tool.
	 * @throws Exception
	 */
	@Test
	public void testCase20110603() throws Exception {
		EnhancementMessage pidConflict = ingestSample("techmd-test-single-conflict.xml", "20110603testcase.zip", "application/octet-stream");
		List<String> alts = new ArrayList<String>();
		alts.add("irods://cdr-vault.libint.unc.edu:3333/cdrZone/home/eomeara/staging/Tucasi_live/originals/TUCASI/Vocabulary%20Development%20Week%202/Vocabulary%20Development%20Week%202%20(default).zip");
		this.getManagementClient().modifyDatastreamByReference(pidConflict.getPid(), "DATA_FILE", false, "test", alts, null, null, null, null, null);
		Enhancement<Element> en = this.getTechnicalMetadataEnhancementService().getEnhancement(pidConflict);
		en.call();
		Datastream thb = this.getManagementClient().getDatastream(pidConflict.getPid(), "MD_TECHNICAL");
		Document foxml = this.getManagementClient().getObjectXML(pidConflict.getPid());
		LOG.debug("HEREHERE");
		new XMLOutputter().output(foxml, System.err);
		assertNotNull("technical metadata stream must not be null", thb);
		List<String> mimes = this.getTripleStoreQueryService().fetchAllTriples(pidConflict.getPid()).get("http://cdr.unc.edu/definitions/1.0/base-model.xml#hasSourceMimeType");
		assertTrue("data stream mimetype must match powerpoint, but got: "+mimes.get(0), mimes.size() == 1 && mimes.get(0).contains("zip"));
	}

	/**
	 * We should ignore Exiftool's identification of image/vnd.fpx and going with ppt from another tool.
	 * @throws Exception
	 */
	@Test
	public void testExtractionTaskWithIdentificationWithoutStatus() throws Exception {
		EnhancementMessage pidConflict = ingestSample("techmd-test-single-conflict.xml", "fits_conflict.ppt", "application/octet-stream");
		// update source data stream
		File dataFile = new File("src/test/resources", "sample_fits_no_id_status.png");
		String uploadURI = this.getManagementClient().upload(dataFile);
		this.getManagementClient().modifyDatastreamByReference(pidConflict.getPid(), "DATA_FILE", false, "Techmd Test",
				Collections.<String>emptyList(), "sample_fits_no_id_status.png", "application/octet-stream", null, ChecksumType.DEFAULT, uploadURI);
		Enhancement<Element> en = this.getTechnicalMetadataEnhancementService().getEnhancement(pidConflict);
		en.call();

		Datastream thb = this.getManagementClient().getDatastream(pidConflict.getPid(), "MD_TECHNICAL");
		assertNotNull("technical metadata stream must not be null", thb);
		List<String> mimes = this.getTripleStoreQueryService().fetchAllTriples(pidConflict.getPid()).get("http://cdr.unc.edu/definitions/1.0/base-model.xml#hasSourceMimeType");
		assertTrue("data stream mimetype must match image/png, but is "+mimes.get(0), mimes.size() == 1 && "image/png".equals(mimes.get(0)));
	}

	@Test
	public void testExtractionTaskWithErrorMessage() throws Exception {
		EnhancementMessage pidErr = ingestSample("techmd-test-single-error.xml", "sample_fits_error.pdf", "application/pdf");
		assertTrue("The test pid should be applicable before the service runs: " + pidErr, this
				.getTechnicalMetadataEnhancementService().isApplicable(pidErr));
		Enhancement<Element> en = this.getTechnicalMetadataEnhancementService().getEnhancement(pidErr);
		en.call();

		Datastream thb = this.getManagementClient().getDatastream(pidErr.getPid(), "MD_TECHNICAL");
		assertNotNull("technical metadata stream must not be null", thb);

		Document foxml = this.getManagementClient().export(Context.PUBLIC, Format.FOXML_1_1, pidErr.getTargetID());
		String foxstr = new XMLOutputter().outputString(foxml);
		LOG.debug("FOXML AFTER TECHMD:\n" + foxstr);

		boolean stillApplicable = this.getTechnicalMetadataEnhancementService().isApplicable(pidErr);
		assertFalse("A pid should no longer be applicable after the service runs: " + pidErr, stillApplicable);

		List<PID> candidates = this.getTechnicalMetadataEnhancementService().findCandidateObjects(50, 0);
		assertFalse("Test pid should no longer be a candidate", candidates.contains(pidErr.getPid()));
	}

	public TripleStoreQueryService getTripleStoreQueryService() {
		return tripleStoreQueryService;
	}

	public void setTripleStoreQueryService(TripleStoreQueryService tripleStoreQueryService) {
		this.tripleStoreQueryService = tripleStoreQueryService;
	}

	public ManagementClient getManagementClient() {
		return managementClient;
	}

	public void setManagementClient(ManagementClient managementClient) {
		this.managementClient = managementClient;
	}

	/**
	 * @param filePath
	 *           name of file to open. The file can reside anywhere in the classpath
	 */
	@SuppressWarnings("unused")
	private Document readFileAsString(String filePath) throws Exception {
		return new SAXBuilder().build(new InputStreamReader(this.getClass().getResourceAsStream(filePath)));
	}

	public TechnicalMetadataEnhancementService getTechnicalMetadataEnhancementService() {
		return technicalMetadataEnhancementService;
	}

	public void setTechnicalMetadataEnhancementService(
			TechnicalMetadataEnhancementService technicalMetadataEnhancementService) {
		this.technicalMetadataEnhancementService = technicalMetadataEnhancementService;
	}

}

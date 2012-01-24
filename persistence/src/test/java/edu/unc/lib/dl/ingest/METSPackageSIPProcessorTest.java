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
package edu.unc.lib.dl.ingest;

import static edu.unc.lib.dl.util.FileUtils.tempCopy;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.annotation.Resource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage;
import edu.unc.lib.dl.ingest.aip.MODSValidationFilter;
import edu.unc.lib.dl.ingest.sip.FilesDoNotMatchManifestException;
import edu.unc.lib.dl.ingest.sip.InvalidMETSException;
import edu.unc.lib.dl.ingest.sip.METSPackageSIP;
import edu.unc.lib.dl.ingest.sip.METSPackageSIPProcessor;
import edu.unc.lib.dl.schematron.SchematronValidator;
import edu.unc.lib.dl.services.AgentManager;
import edu.unc.lib.dl.util.PremisEventLogger;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class METSPackageSIPProcessorTest extends Assert {

@SuppressWarnings("unused")
private static final Logger LOG = LoggerFactory.getLogger(METSPackageSIPProcessorTest.class);

	PID containerPID = new PID("test:1");

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Resource
	private METSPackageSIPProcessor metsPackageSIPProcessor = null;
	@Resource
	private SchematronValidator schematronValidator; 

	private void exceptionTest(String testFilePath, String testMsg) throws IngestException {
		File testFile = tempCopy(new File(testFilePath));
		Agent adminGroup = AgentManager.getAdministrativeGroupAgentStub();
		PremisEventLogger logger = new PremisEventLogger(adminGroup);
		METSPackageSIP sip = null;
		try {
			sip = new METSPackageSIP(containerPID, testFile, adminGroup, true);
		} catch (IOException e) {
			LOG.debug("STACK TRACE: ", e);
			fail("EXPECTED: " + testMsg + "\nTHROWN: " + e.getMessage());
		}
		ArchivalInformationPackage aip = this.getMetsPackageSIPProcessor().createAIP(sip);
	}

	public METSPackageSIPProcessor getMetsPackageSIPProcessor() {
		return metsPackageSIPProcessor;
	}

	public void setMetsPackageSIPProcessor(METSPackageSIPProcessor metsPackageSIPProcessor) {
		this.metsPackageSIPProcessor = metsPackageSIPProcessor;
	}

	public SchematronValidator getSchematronValidator() {
		return schematronValidator;
	}

	public void setSchematronValidator(SchematronValidator schematronValidator) {
		this.schematronValidator = schematronValidator;
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	/*
	 * Tests a SIP that references all the bundled files properly from METS and has a bad checksum.
	 */
	@Test
	public void testBadChecksumSIP() {
		Exception e = null;
		try {
			exceptionTest("src/test/resources/badchecksum.zip", "bad checksum files in the SIP");
		} catch (IngestException ex) {
			// detect the right exception
			e = ex;
		}
		if (e != null && e instanceof FilesDoNotMatchManifestException) {
			FilesDoNotMatchManifestException real = (FilesDoNotMatchManifestException) e;
			if (real.getBadChecksumFiles() != null) {
				int size = real.getBadChecksumFiles().size();
				assertTrue("Should have found 2 bad checksum files in SIP but found " + size, size == 2);
			} else {
				fail("got a null back for bad checksum files list");
			}
		} else {
			fail("Expected FilesDoNotMatchManifestException got \n  " + e);
		}
	}

	// @Test
	public void testBigSIP() {
		// testing for successful conversion of Large SIP, 2014 objects
		File testFile = tempCopy(new File("src/test/resources/METS.xml"));
		Agent user = AgentManager.getAdministrativeGroupAgentStub();
		METSPackageSIP sip = null;
		ArchivalInformationPackage aip = null;
		try {
			sip = new METSPackageSIP(containerPID, testFile, user, false);
			sip.setDiscardDataFilesOnDestroy(false);
		} catch (IOException e) {
			throw new Error(e);
		}
		try {
			aip = this.getMetsPackageSIPProcessor().createAIP(sip);
		} catch (IngestException e) {
			throw new Error(e);
		}
		assertNotNull("The result ingest context is null.", aip);
		int count = aip.getPIDs().size();
		assertTrue("There should be 2014 PIDs in the resulting AIP, found " + count, count == 2014);
	}

	/*
	 * Tests a SIP that references all the bundled files properly from METS and has an extra file.
	 */
	@Test
	public void testExtraFilesSIP() {
		Exception e = null;
		try {
			exceptionTest("src/test/resources/extrafiles.zip", "extra files in the SIP");
		} catch (IngestException ex) {
			// detect the right exception
			e = ex;
		}
		if (e != null && e instanceof FilesDoNotMatchManifestException) {
			FilesDoNotMatchManifestException real = (FilesDoNotMatchManifestException) e;
			if (real.getExtraFiles() != null) {
				int size = real.getExtraFiles().size();
				assertTrue("Should have found 2 extra files in SIP but found " + size, size == 2);
			} else {
				fail("got a null back for extra files list");
			}
		} else {
			fail("Expected FilesDoNotMatchManifestException got \n  " + e);
		}
	}

	@Test
	public void testGoodSIP() {
		// testing for successful conversion of SIP w/simple content model
		File testFile = tempCopy(new File("src/test/resources/simple.zip"));
		Agent user = AgentManager.getAdministrativeGroupAgentStub();
		METSPackageSIP sip = null;
		ArchivalInformationPackage aip = null;
		try {
			sip = new METSPackageSIP(containerPID, testFile, user, true);
			sip.setDiscardDataFilesOnDestroy(false);
			sip.getPreIngestEventLogger().addMD5ChecksumCalculation(new Date(System.currentTimeMillis()), "ClamAV v2.1",
					"Jane Smith");
			sip.getPreIngestEventLogger().addVirusScan(new Date(System.currentTimeMillis()), "ClamAV", "Bob Smith");
		} catch (IOException e) {
			throw new Error(e);
		}
		try {
			aip = this.getMetsPackageSIPProcessor().createAIP(sip);
		} catch (IngestException e) {
			throw new Error(e);
		}
		assertNotNull("The result ingest context is null.", aip);
		int count = aip.getPIDs().size();
		assertTrue("There should be 14 PIDs in the resulting AIP, found " + count, count == 14);
	}

	@Test
	public void testInvalidAgainstSimpleProfile() {
		Exception e = null;
		try {
			exceptionTest("src/test/resources/simple_bad_profile.zip", "METS does not match simple profile.");
		} catch (IngestException ex) {
			e = ex;
		}
		if (e != null && e instanceof InvalidMETSException) {
			// expected exception, yay!
		} else {
			fail("did not get the expected InvalidMETSException");
		}
	}

	// @Test
	// public void testInvalidMETSXML() {
	// exceptionTest("src/test/resources/invalid_mets.zip", "invalid METS");
	// }

	/*
	 * Tests a SIP that references all the bundled files properly from METS, but one file is missing.
	 */
	@Test
	public void testMissingFilesSIP() {
		Exception e = null;
		try {
			exceptionTest("src/test/resources/missingfiles.zip", "missing files in the SIP");
		} catch (IngestException ex) {
			// detect the right exception
			e = ex;
		}
		if (e != null && e instanceof FilesDoNotMatchManifestException) {
			FilesDoNotMatchManifestException real = (FilesDoNotMatchManifestException) e;
			if (real.getMissingFiles() != null) {
				int size = real.getMissingFiles().size();
				assertTrue("Should have found 2 missing files in SIP but found " + size, size == 2);
			} else {
				fail("got a null back for missing files list");
			}
		} else {
			fail("Expected FilesDoNotMatchManifestException got \n  " + e);
		}
	}

	@Test
	public void testDSpaceMETS() {
		//5 files + the container
		int numberOfPIDs = 6;
		// testing for successful conversion of SIP w/simple content model
		File testFile = tempCopy(new File("src/test/resources/dspaceMets.zip"));
		Agent user = AgentManager.getAdministrativeGroupAgentStub();
		METSPackageSIP sip = null;
		ArchivalInformationPackage aip = null;
		try {
			sip = new METSPackageSIP(containerPID, testFile, user, true);
			sip.setDiscardDataFilesOnDestroy(false);
			sip.getPreIngestEventLogger().addMD5ChecksumCalculation(new Date(System.currentTimeMillis()), "ClamAV v2.1",
					"Jane Smith");
			sip.getPreIngestEventLogger().addVirusScan(new Date(System.currentTimeMillis()), "ClamAV", "Bob Smith");
		} catch (IOException e) {
			throw new Error(e);
		}
		try {
			java.util.Iterator<String> iterator = this.metsPackageSIPProcessor.getSchematronValidator().getSchemas().keySet().iterator();
			while (iterator.hasNext()){
				LOG.info("Schema " + iterator.next());
			}
			aip = this.getMetsPackageSIPProcessor().createAIP(sip);
			
			MODSValidationFilter modsFilter = new MODSValidationFilter();
			modsFilter.setSchematronValidator(schematronValidator);
			modsFilter.doFilter(aip);
		} catch (Exception e) {
			LOG.error("DSpace mets failed", e);
			fail();
		}
		assertNotNull("The result ingest context is null.", aip);
		int count = aip.getPIDs().size();
		assertTrue("There should be " + numberOfPIDs + " PIDs in the resulting AIP, found " + count, count == numberOfPIDs);
		
		
	}
}

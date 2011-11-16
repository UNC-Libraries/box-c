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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.dl.agents.Agent;
import edu.unc.lib.dl.agents.AgentManager;
import edu.unc.lib.dl.agents.GroupAgent;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.ingest.aip.ArchivalInformationPackage;
import edu.unc.lib.dl.ingest.sip.FilesDoNotMatchManifestException;
import edu.unc.lib.dl.ingest.sip.InvalidMETSException;
import edu.unc.lib.dl.ingest.sip.METSPackageSIP;
import edu.unc.lib.dl.ingest.sip.METSPackageSIPProcessor;
import edu.unc.lib.dl.util.PremisEventLogger;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class METSPackageSIPProcessorTest extends Assert {
	PID containerPID = new PID("test:1");

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Resource
	private METSPackageSIPProcessor metsPackageSIPProcessor = null;

	private void exceptionTest(String testFilePath, String testMsg) throws IngestException {
		File testFile = new File(testFilePath);
		Agent adminGroup = AgentManager.getAdministrativeGroupAgentStub();
		PremisEventLogger logger = new PremisEventLogger(adminGroup);
		METSPackageSIP sip = null;
		try {
			sip = new METSPackageSIP(containerPID, testFile, adminGroup, true);
			sip.setDiscardDataFilesOnDestroy(false);
		} catch (IOException e) {
			fail("EXPECTED: " + testMsg + "\nTHROWN: " + e.getMessage());
		}
		ArchivalInformationPackage aip = this.getMetsPackageSIPProcessor().createAIP(sip, logger);
		aip.setDeleteFilesOnDestroy(false);
	}

	public METSPackageSIPProcessor getMetsPackageSIPProcessor() {
		return metsPackageSIPProcessor;
	}

	public void setMetsPackageSIPProcessor(METSPackageSIPProcessor metsPackageSIPProcessor) {
		this.metsPackageSIPProcessor = metsPackageSIPProcessor;
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
		File testFile = new File("src/test/resources/METS.xml");
		Agent user = AgentManager.getAdministrativeGroupAgentStub();
		PremisEventLogger logger = new PremisEventLogger(new GroupAgent("foo"));
		METSPackageSIP sip = null;
		ArchivalInformationPackage aip = null;
		try {
			sip = new METSPackageSIP(containerPID, testFile, user, false);
			sip.setDiscardDataFilesOnDestroy(false);
		} catch (IOException e) {
			throw new Error(e);
		}
		try {
			aip = this.getMetsPackageSIPProcessor().createAIP(sip, logger);
			aip.setDeleteFilesOnDestroy(false);
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
		File testFile = new File("src/test/resources/simple.zip");
		Agent user = AgentManager.getAdministrativeGroupAgentStub();
		PremisEventLogger logger = new PremisEventLogger(new GroupAgent("foo"));
		// PersonAgent user = new PersonAgent("Testy Testworthy", "onyen");
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
			aip = this.getMetsPackageSIPProcessor().createAIP(sip, logger);
			aip.setDeleteFilesOnDestroy(false);
		} catch (IngestException e) {
			throw new Error(e);
		}
		assertNotNull("The result ingest context is null.", aip);
		int count = aip.getPIDs().size();
		assertTrue("There should be 14 PIDs in the resulting AIP, found " + count, count == 14);
	}

	/**
	 * Test successful conversion of the repository bootstrap SIP
	 */
	@Test
	public void testRepoBootstrapSIP() {
		// the METS file will be deleted, so copy it to temp space first..
		File upload = null;
		try {
			upload = File.createTempFile("repoinit", ".xml");
			BufferedInputStream in = new BufferedInputStream(this.getClass().getClassLoader()
					.getResourceAsStream("repository_bootstrap_mets.xml"));
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(upload), 4096);
			byte[] bytes = new byte[4096];
			for (int len = in.read(bytes); len > 0; len = in.read(bytes)) {
				out.write(bytes, 0, len);
			}
			in.close();
			out.close();
		} catch (IOException e) {
			throw new Error(e);
		}
		Agent user = AgentManager.getAdministrativeGroupAgentStub();
		PremisEventLogger logger = new PremisEventLogger(new GroupAgent("foo"));
		// PersonAgent user = new PersonAgent("Testy Testworthy", "onyen");
		METSPackageSIP sip = null;
		ArchivalInformationPackage aip = null;
		try {
			sip = new METSPackageSIP(containerPID, upload, user, false);
			sip.setDiscardDataFilesOnDestroy(false); // don't delete test files!
		} catch (IOException e) {
			throw new Error(e);
		}
		try {
			aip = this.getMetsPackageSIPProcessor().createAIP(sip, logger);
			aip.setDeleteFilesOnDestroy(false);
		} catch (IngestException e) {
			throw new Error(e);
		}
		assertNotNull("The result ingest context is null.", aip);
		int count = aip.getPIDs().size();
		assertTrue("There should be 4 PIDs in the repository bootstrap AIP, found " + count, count == 4);
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

}

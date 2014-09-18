package edu.unc.lib.deposit.normalize;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import net.greghaines.jesque.Job;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.deposit.DepositTestUtils;
import edu.unc.lib.deposit.work.SpringJobFactory;
import edu.unc.lib.dl.util.DepositConstants;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class CDRMETS2N3BagJobTest {
	@Autowired
	File depositsDirectory;
	
	@Autowired
	SpringJobFactory springJobFactory = null;
	
	@Test
	public void test() throws ClassNotFoundException {
		String depositUUID = "bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae";
		String workDir = DepositTestUtils.makeTestDir(depositsDirectory, depositUUID, new File("src/test/resources/cdrMETS.zip"));
		Job job = new Job("CDRMETS2N3BagJob", UUID.randomUUID().toString(), depositUUID);
		Object j = springJobFactory.materializeJob(job);
		Runnable r = (Runnable)j;
		r.run();
		
		File modelFile = new File(workDir, DepositConstants.JENA_TDB_DIR);
		assertTrue("Jena model must exist after conversion", modelFile.exists());
	}
	
	@Test
	public void testAltMETSFilename() throws ClassNotFoundException {
		String depositUUID = "bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae";
		String workDir = DepositTestUtils.makeTestDir(depositsDirectory, depositUUID, new File("src/test/resources/cdrMETS.zip"));
		File mets = new File(workDir, "mets.xml");
		mets.renameTo(new File(workDir, "METS.xml"));
		Job job = new Job("CDRMETS2N3BagJob", UUID.randomUUID().toString(), depositUUID);
		Object j = springJobFactory.materializeJob(job);
		Runnable r = (Runnable)j;
		r.run();

		File modelFile = new File(workDir, DepositConstants.JENA_TDB_DIR);
		assertTrue("N3 model file must exist after conversion", modelFile.exists());
	}

	@Test
	public void testAccessControlsMETSXML() throws ClassNotFoundException, IOException {
		String depositUUID = "cdrff703-9c2e-466b-b4cc-15bbfd03c8ae";
		File workDir = new File(depositsDirectory, depositUUID);
		org.apache.commons.io.FileUtils.deleteDirectory(workDir);
		workDir.mkdirs();
		File test = new File("src/test/resources/accessControlsTest.cdr.xml");
		File metsPlace = new File(workDir, "METS.xml");
		FileUtils.copyFile(test, metsPlace);
		
		Job job = new Job("CDRMETS2N3BagJob", UUID.randomUUID().toString(), depositUUID);
		Object j = springJobFactory.materializeJob(job);
		Runnable r = (Runnable)j;
		r.run();

		File modelFile = new File(workDir, DepositConstants.JENA_TDB_DIR);
		assertTrue("N3 model file must exist after conversion", modelFile.exists());
	}
}

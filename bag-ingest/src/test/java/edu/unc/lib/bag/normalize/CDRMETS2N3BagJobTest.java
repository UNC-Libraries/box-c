package edu.unc.lib.bag.normalize;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.bag.normalize.CDRMETS2N3BagJob;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.ZipFileUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class CDRMETS2N3BagJobTest {
	@Autowired
	private CDRMETS2N3BagJob job = null;

	public CDRMETS2N3BagJob getJob() {
		return job;
	}

	public void setJob(CDRMETS2N3BagJob job) {
		this.job = job;
	}

	@Test
	public void test() {
		File testFile = new File("src/test/resources/bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae.zip");
		File testDir = new File("/tmp/cdrMets2N3BagTest_"+testFile.getName().hashCode());
		try {
			ZipFileUtil.unzipToDir(testFile, testDir);
		} catch (IOException e) {
			throw new Error("Unable to unpack your deposit: "+testFile, e);
		}
		this.getJob().setBagDirectory(testDir);
		this.getJob().setDepositPID(new PID("uuid:bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae"));
		job.run();
	}

}

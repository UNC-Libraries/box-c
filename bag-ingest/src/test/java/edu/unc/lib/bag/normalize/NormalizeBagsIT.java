package edu.unc.lib.bag.normalize;

import static net.greghaines.jesque.utils.JesqueUtils.createKey;
import static net.greghaines.jesque.utils.ResqueConstants.FAILED;
import static net.greghaines.jesque.utils.ResqueConstants.PROCESSED;
import static net.greghaines.jesque.utils.ResqueConstants.QUEUE;
import static net.greghaines.jesque.utils.ResqueConstants.STAT;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import net.greghaines.jesque.Job;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import edu.unc.lib.bag.AbstractResqueIT;
import edu.unc.lib.dl.util.FileUtils;
import edu.unc.lib.dl.util.ZipFileUtil;
import edu.unc.lib.workers.JedisFactory;

public class NormalizeBagsIT extends AbstractResqueIT {
	private static final org.slf4j.Logger log = LoggerFactory
			.getLogger(NormalizeBagsIT.class);
	
	@Before
	public void reset() {
		resetRedis();
	}

	private void testBag(File testBagResource, String depositId, int expectedJobsProcessed) {
		File workingDir = new File("/tmp/bagTest_"
				+ testBagResource.getName().substring(0, testBagResource.getName().lastIndexOf(".")));
		try {
			if (workingDir.exists()) {
				FileUtils.deleteDir(workingDir);
			}
			ZipFileUtil.unzipToDir(testBagResource, workingDir);
		} catch (IOException e) {
			throw new Error(
					"Unable to unpack your deposit: " + testBagResource, e);
		}
		Job job = new Job("NormalizeBag", UUID.randomUUID().toString(), workingDir, depositId);
		getClient().enqueue("Deposit", job);
		getClient().end();
		log.debug("Enqueued depositId: {}, working dir: {}", depositId,
				workingDir.getPath());
		try {
			Thread.sleep(120*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// Assert that the job was run by the worker
		Jedis jedis = JedisFactory.createJedis(getConfig());
		try {
			Assert.assertEquals("Wrong number of jobs processed", String.valueOf(expectedJobsProcessed), jedis.get(createKey(getConfig().getNamespace(),
					STAT, PROCESSED)));
			Assert.assertNull("Jobs failed", jedis.get(createKey(getConfig().getNamespace(), STAT,
					FAILED)));
			Assert.assertEquals("Queue not empty", Long.valueOf(0), jedis.llen(createKey(
					getConfig().getNamespace(), QUEUE, "Deposit")));
		} finally {
			jedis.quit();
		}
	}

	@Test
	public void testCdrMetsBag() throws InterruptedException {
		File testBagResource = new File("src/test/resources/cdrMETS.zip");
		String depositId = "info:fedora/uuid:bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae";
		testBag(testBagResource, depositId, 2);
	}

	@Test
	public void testBiomedDspaceMetsBag() {
		File testBagResource = new File(
				"src/test/resources/biomedDspaceMETS.zip");
		String depositId = "info:fedora/uuid:4b81ce23-8171-4d28-a440-b4cf8c8a781c";
		testBag(testBagResource, depositId, 3);
	}
	// TODO test invalid MODS against XSD
	// TODO test invalid MODS against schematron
	//
	// @Test
	// TODO public void testSimpleBag() {
	//
	// }
	//
	@Test
	public void testAtomBag() {
		File testBagResource = new File("src/test/resources/atomPubEntryBag.zip");
		String depositId = "info:fedora/uuid:31e06abf-c365-4761-bed2-ba03934c815f";
		testBag(testBagResource, depositId, 2);
	}
	
	// @Test
	// public void testBigBag() {
	//
	// }

}

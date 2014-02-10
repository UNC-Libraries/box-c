package edu.unc.lib.bag.normalize;

import java.util.UUID;
import java.util.concurrent.Callable;

import net.greghaines.jesque.Job;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import edu.unc.lib.bag.AbstractResqueIT;

public class NormalizeBagsIT extends AbstractResqueIT {
	private static final org.slf4j.Logger log = LoggerFactory
			.getLogger(NormalizeBagsIT.class);
	
	@Before
	public void reset() {
		resetRedis();
	}

	
	
//	private void testBag(Class jobclass, File testBagResource, String depositId, int expectedJobsProcessed) {
//		Job job = new Job(jobclass.getName(), UUID.randomUUID().toString(), workingDir, depositId);
//		getClient().enqueue("Deposit", job);
//		getClient().end();
//		log.debug("Enqueued depositId: {}, working dir: {}", depositId,
//				workingDir.getPath());
//		// TODO use a listener to wait for job completion w/o failure
//		try {
//			Thread.sleep(20*1000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		// Assert that the job was run by the worker
//		Jedis jedis = JedisFactory.createJedis(getConfig());
//		try {
//			Assert.assertEquals("Wrong number of jobs processed", String.valueOf(expectedJobsProcessed), (String)jedis.get(createKey(getConfig().getNamespace(),
//					STAT, PROCESSED)));
//			Assert.assertNull("Jobs failed", jedis.get(createKey(getConfig().getNamespace(), STAT,
//					FAILED)));
//			Assert.assertEquals("Queue not empty", Long.valueOf(0), jedis.llen(createKey(
//					getConfig().getNamespace(), QUEUE, "Deposit")));
//		} finally {
//			jedis.quit();
//		}
//	}

	@Test
	public void testCdrMetsBag() throws ClassNotFoundException {
		String workDir = makeWorkingDir("src/test/resources/cdrMETS.zip");
		String depositId = "info:fedora/uuid:bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae";
		Job job = new Job("CDRMETS2N3BagJob", UUID.randomUUID().toString(), workDir, depositId);
		Object j = getSpringJobFactory().materializeJob(job);
		Runnable r = (Runnable)j;
		r.run();
		
		// validate N3 model (#objs, #files, #mods, 
		// create ValidateRdfGraph job
	}

	@Test
	public void testBiomedDspaceMetsBag() throws Exception {
		String workDir = makeWorkingDir("src/test/resources/biomedDspaceMETS.zip");
		String depositId = "info:fedora/uuid:4b81ce23-8171-4d28-a440-b4cf8c8a781c";
		Job job = new Job("DSPACEMETS2N3BagJob", UUID.randomUUID().toString(), workDir, depositId);
		Object j = getSpringJobFactory().materializeJob(job);
		Object result = ((Callable)j).call();
		Job r = (Job)result;
		Assert.assertEquals("should return biomed extras job", BioMedCentralExtrasJob.class.getName(), r.getClassName());
	}
	
	// TODO test invalid MODS against XSD
	// TODO test invalid MODS against schematron
	//
	// @Test
	// TODO public void testSimpleBag() {
	//
	// }
	//
//	@Test
//	public void testAtomBag() {
//		String workDir = makeWorkingDir("src/test/resources/atomPubEntryBag.zip");
//		String depositId = "info:fedora/uuid:31e06abf-c365-4761-bed2-ba03934c815f";
//		Job job = new Job("CDRMETS2N3BagJob", UUID.randomUUID().toString(), workDir, depositId);
		//testBag(testBagResource, depositId, 1);
	//}
	
	// @Test
	// public void testBigBag() {
	//
	// }

}

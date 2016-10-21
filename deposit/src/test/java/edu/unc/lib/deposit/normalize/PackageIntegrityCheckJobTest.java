package edu.unc.lib.deposit.normalize;

import static org.mockito.Matchers.anyString;

import java.io.File;
import java.util.HashMap;
import java.util.UUID;

import net.greghaines.jesque.Job;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import edu.unc.lib.deposit.DepositTestUtils;
import edu.unc.lib.deposit.work.AbstractDepositJob;
import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.SpringJobFactory;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class PackageIntegrityCheckJobTest extends AbstractNormalizationJobTest {

	@Autowired
	File depositsDirectory;
	
	@Mock
	private DepositStatusFactory depositStatusFactory;
	
	@Before
	public void setup() {
	    // Initialize mocks created above
	    MockitoAnnotations.initMocks(this);
	}
	
	@Autowired
	SpringJobFactory springJobFactory = null;
	
	@Test
	public void test() throws ClassNotFoundException {
		String depositUUID = "bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae";
		DepositTestUtils.makeTestDir(
				depositsDirectory,
				depositUUID, new File("src/test/resources/depositFileZipped.zip"));
		Job job = new Job("PackageIntegrityCheckJob", UUID.randomUUID().toString(), depositUUID);
		Object j = springJobFactory.materializeJob(job);
		AbstractDepositJob aj = (AbstractDepositJob)j;
		
		HashMap<String, String> status = new HashMap<String, String>();
		status.put(DepositField.depositMd5.name(), "c949138500f67e8617ac9968d2632d4e");
		status.put(DepositField.fileName.name(), "cdrMETS.zip");
		Mockito.when(depositStatusFactory.get(anyString())).thenReturn(status);
		aj.setDepositStatusFactory(depositStatusFactory);
		
		Runnable r = (Runnable)j;
		r.run();
	}
	
	@Test
	public void testFileCorrupted() throws ClassNotFoundException {
		String depositUUID = "bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae";
		DepositTestUtils.makeTestDir(
				depositsDirectory,
				depositUUID, new File("src/test/resources/depositFileZipped.zip"));
		Job job = new Job("PackageIntegrityCheckJob", UUID.randomUUID().toString(), depositUUID);
		Object j = springJobFactory.materializeJob(job);
		AbstractDepositJob aj = (AbstractDepositJob)j;
		
		HashMap<String, String> status = new HashMap<String, String>();
		status.put(DepositField.depositMd5.name(), "a949138500f67e8617ac9968d2632d4e");
		status.put(DepositField.fileName.name(), "cdrMETS.zip");
		Mockito.when(depositStatusFactory.get(anyString())).thenReturn(status);
		aj.setDepositStatusFactory(depositStatusFactory);
		
		Runnable r = (Runnable)j;
		Exception expected = null;
		try {
			r.run();
		} catch(JobFailedException e) {
			expected = e;
		}
		
		Assert.notNull(expected, "Job should throw a failure exception");
	}
}

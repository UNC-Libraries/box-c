package edu.unc.lib.deposit.integration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.deposit.DepositTestUtils;
import edu.unc.lib.dl.util.DepositStatusFactory;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
public class DepositSupervisorIT {
	
	@Autowired
	File depositsDirectory;
	
	@Autowired
	DepositStatusFactory depositStatusFactory;
	
	@Test
	public void testCDRMETS() throws ClassNotFoundException, InterruptedException {
		String workDir = DepositTestUtils.makeTestDir(depositsDirectory, "bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae", new File("src/test/resources/depositFileZipped.zip"));
		String depositUUID = "bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae";
		Map<String, String> status = new HashMap<String, String>();
		status.put("metsProfile", "http://cdr.unc.edu/METS/profiles/Simple");
		status.put("createTime", "2009-07-16T22:56:00-05:00");
		status.put("depositMd5", "d2b88d292e2c47943231205ed36f6c94");
		status.put("status","registered");
		status.put("submitTime","1395158020363");
		status.put("permissionGroups","classpath:server.properties,https://localhost/services/sword");
		status.put("depositorEmail","test-owner@email.unc.edu");
		status.put("depositSlug","metsbagittest");
		status.put("intSenderDescription","Greg Jansen");
		status.put("packagingType","http://cdr.unc.edu/METS/profiles/Simple");
		status.put("fileName","cdrMETS.zip");
		status.put("depositorName","test-owner");
		status.put("uuid",depositUUID);
		status.put("depositMethod","SWORD 1.3");
		status.put("containerId","uuid:destination");
		depositStatusFactory.save(depositUUID, status);
		Thread.sleep(1000*60*4);
	}
	
}

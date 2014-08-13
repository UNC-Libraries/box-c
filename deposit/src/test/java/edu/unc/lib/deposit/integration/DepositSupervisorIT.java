package edu.unc.lib.deposit.integration;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.deposit.DepositTestUtils;
import edu.unc.lib.deposit.work.DepositSupervisor;
import edu.unc.lib.dl.util.DepositStatusFactory;
import edu.unc.lib.dl.util.JobStatusFactory;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/service-context.xml" })
@DirtiesContext
public class DepositSupervisorIT {

	private static boolean started = false;

	@Autowired
	File depositsDirectory;

	@Autowired
	DepositStatusFactory depositStatusFactory;

	@Autowired
	JobStatusFactory jobStatusFactory;

	@Autowired
	DepositSupervisor depositSupervisor;

	// TODO test a pile of deposits at once

	@Before
	public void setup() {
		if(!started) {
			depositSupervisor.start();
			started = true;
		}
	}

	@Test
	public void testWorkbenchZIP() throws ClassNotFoundException, InterruptedException {
		DepositTestUtils.makeTestDir(depositsDirectory, "84f69180-3e40-4152-be80-a30c60c3f846", new File("src/test/resources/workbench.zip"));
		String depositUUID = "84f69180-3e40-4152-be80-a30c60c3f846";
		depositStatusFactory.delete(depositUUID);
		jobStatusFactory.deleteAll(depositUUID);
		Map<String, String> status = new HashMap<String, String>();
		status.put("metsProfile", "http://cdr.unc.edu/METS/profiles/Simple");
		status.put("createTime", "2009-07-16T22:56:00-05:00");
		status.put("depositMd5", "4554cedb53c8fa58cbbea52691085b88");
		status.put("state","unregistered");
		status.put("actionRequest", "register");
		status.put("submitTime","1395158020363");
		status.put("permissionGroups","");
		status.put("depositorEmail","test-owner@email.unc.edu");
		status.put("depositSlug","cdrworkbenchtestslug");
		status.put("intSenderDescription","Greg Jansen");
		status.put("packagingType","http://cdr.unc.edu/METS/profiles/Simple");
		status.put("fileName","testPipeline.zip");
		status.put("depositorName","test-owner");
		status.put("uuid",depositUUID);
		status.put("depositMethod","SWORD 1.3");
		status.put("containerId","uuid:destination");
		depositStatusFactory.save(depositUUID, status);
		Thread.sleep(1000*30);
	}

	@Test
	public void testCDRMETS() throws ClassNotFoundException, InterruptedException {
		DepositTestUtils.makeTestDir(depositsDirectory, "bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae", new File("src/test/resources/depositFileZipped.zip"));
		String depositUUID = "bd5ff703-9c2e-466b-b4cc-15bbfd03c8ae";
		depositStatusFactory.delete(depositUUID);
		jobStatusFactory.deleteAll(depositUUID);
		Map<String, String> status = new HashMap<String, String>();
		status.put("metsProfile", "http://cdr.unc.edu/METS/profiles/Simple");
		status.put("createTime", "2009-07-16T22:56:00-05:00");
		status.put("depositMd5", "c949138500f67e8617ac9968d2632d4e");
		status.put("state","unregistered");
		status.put("actionRequest", "register");
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
		Thread.sleep(1000*30);
	}

	@Test
	public void testCDRMETSwACL() throws ClassNotFoundException, InterruptedException, IOException, JDOMException {
		File workingDir = new File(depositsDirectory, "fooff703-9c2e-466b-b4cc-15bbfd03c8ae");
		if(workingDir.exists()) FileUtils.deleteDirectory(workingDir);
		workingDir.mkdirs();
		File testMETS = new File("src/test/resources/accessControlsTest.cdr.xml");
		File mets = new File(workingDir, "METS.xml");
		Files.copy(testMETS.toPath(), mets.toPath());
		String depositUUID = "fooff703-9c2e-466b-b4cc-15bbfd03c8ae";
		depositStatusFactory.delete(depositUUID);
		jobStatusFactory.deleteAll(depositUUID);
		Map<String, String> status = new HashMap<String, String>();
		status.put("metsProfile", "http://cdr.unc.edu/METS/profiles/Simple");
		status.put("createTime", "2009-07-16T22:56:00-05:00");
		status.put("state","unregistered");
		status.put("actionRequest", "register");
		status.put("submitTime","1395158020363");
		status.put("permissionGroups","classpath:server.properties,https://localhost/services/sword");
		status.put("depositorEmail","test-owner@email.unc.edu");
		status.put("depositSlug","metsbagittest");
		status.put("intSenderDescription","Greg Jansen");
		status.put("packagingType","http://cdr.unc.edu/METS/profiles/Simple");
		status.put("depositorName","test-owner");
		status.put("uuid",depositUUID);
		status.put("depositMethod","SWORD 1.3");
		status.put("containerId","uuid:destination");
		depositStatusFactory.save(depositUUID, status);
		Thread.sleep(1000*30);

		File depositevents = new File(workingDir, "events/"+depositUUID+".xml");
		Document doc = new SAXBuilder().build(depositevents);
		@SuppressWarnings("rawtypes")
		List events = doc.getRootElement().getChildren("event", JDOMNamespaceUtil.PREMIS_V2_NS);
		assertEquals("Expected 5 PREMIS events for this ingest", 5, events.size());
	}

}

package edu.unc.lib.dl.cdr.services.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.cdr.services.imaging.ThumbnailEnhancementService;
import edu.unc.lib.dl.cdr.services.model.FailedEnhancementMap.FailedEnhancementEntry;
import edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService;
import edu.unc.lib.dl.fedora.PID;

public class FailedEnhancementMapTest extends Assert {

	@Test
	public void test() throws Exception {
		java.util.List<String> test = Arrays.asList("one", "two");
		test.set(1, "barf");
		System.out.println(test);
	}
	
	private FailedEnhancementMap createExampleMap(String baseFolderPath) {
		FailedEnhancementMap failedMap = new FailedEnhancementMap();
		failedMap.setFailureLogPath(baseFolderPath);
		failedMap.init();

		failedMap.add(new PID("uuid:test1"), TechnicalMetadataEnhancementService.class,
				new EnhancementMessage(), new Exception("Test exception"));
		
		failedMap.add(new PID("uuid:test1"), ThumbnailEnhancementService.class,
				new EnhancementMessage("uuid:test1", ThumbnailEnhancementService.class.getName(), "hammertime"), new Exception("Test thumb exception"));
		
		failedMap.add(new PID("uuid:test2"), TechnicalMetadataEnhancementService.class,
				new EnhancementMessage(), new Exception("Test exception"));
		return failedMap;
	}
	
	@Test
	public void mapCreationTest() throws Exception {
		File baseFolder = null;
		try {
			String baseFolderPath = "target/failedEnhancementMapTest";
			baseFolder = new File(baseFolderPath);
			boolean madeDir = baseFolder.mkdir();
			assertTrue("Failed to create test directory", madeDir);
			
			FailedEnhancementMap failedMap = createExampleMap(baseFolderPath);
			
			assertEquals(2, failedMap.getPIDCache(TechnicalMetadataEnhancementService.class.getName()).size());
			assertEquals(1, failedMap.getPIDCache(ThumbnailEnhancementService.class.getName()).size());
			
			File[] pidFiles = baseFolder.listFiles();
			assertEquals(2, pidFiles.length);
			
			FailedEnhancementEntry entry = failedMap.get("uuid:test1", ThumbnailEnhancementService.class.getName());
			assertEquals("hammertime", entry.message.getAction());
			assertTrue(failedMap.contains("uuid:test2", TechnicalMetadataEnhancementService.class.getName()));
			
		} finally {
			if (baseFolder != null && baseFolder.exists())
				FileUtils.deleteDirectory(baseFolder);
		}
	}
	
	@Test
	public void deserializationTest() throws Exception {
		File baseFolder = null;
		try {
			String baseFolderPath = "target/failedEnhancementMapTest";
			baseFolder = new File(baseFolderPath);
			boolean madeDir = baseFolder.mkdir();
			assertTrue("Failed to create test directory", madeDir);
			
			FailedEnhancementMap failedMap = createExampleMap(baseFolderPath);
			failedMap = null;
			
			failedMap = new FailedEnhancementMap();
			assertEquals(0, failedMap.size());
			
			failedMap.setFailureLogPath(baseFolderPath);
			failedMap.init();
			
			assertEquals(3, failedMap.size());
			
			FailedEnhancementEntry entry = failedMap.get("uuid:test2", TechnicalMetadataEnhancementService.class.getName());
			assertNotNull(entry.stackTrace);
			assertEquals(TechnicalMetadataEnhancementService.class.getName(), entry.serviceName);
			assertTrue(entry.message instanceof EnhancementMessage);
			
			assertTrue(failedMap.contains("uuid:test2", TechnicalMetadataEnhancementService.class.getName()));
			
			java.util.Set<String> pidSet = failedMap.getServiceToPID().get(TechnicalMetadataEnhancementService.class.getName()).keySet();
			System.out.println(pidSet.contains("uuid:test3"));
			
			failedMap.add(new PID("uuid:test3"), TechnicalMetadataEnhancementService.class,
					new EnhancementMessage(), new Exception("Test exception"));
			
			System.out.println(pidSet.contains("uuid:test3"));
			
		} finally {
			if (baseFolder != null && baseFolder.exists())
				FileUtils.deleteDirectory(baseFolder);
		}
	}
	
	@Test
	public void getOrCreateServiceCache() throws Exception {
		File baseFolder = null;
		try {
			String baseFolderPath = "target/failedEnhancementMapTest";
			baseFolder = new File(baseFolderPath);
			boolean madeDir = baseFolder.mkdir();
			assertTrue("Failed to create test directory", madeDir);
			
			FailedEnhancementMap failedMap = new FailedEnhancementMap();
			assertEquals(0, failedMap.size());
			failedMap.setFailureLogPath(baseFolderPath);
			failedMap.init();
			
			assertNull(failedMap.getServiceToPID().get(TechnicalMetadataEnhancementService.class.getName()));
			
			java.util.Set<String> pidSet = failedMap.getOrCreateServicePIDSet(new TechnicalMetadataEnhancementService());
			assertNotNull(pidSet);
			assertEquals(0, pidSet.size());
			
			failedMap.add(new PID("uuid:test1"), TechnicalMetadataEnhancementService.class,
					new EnhancementMessage(), new Exception("Test exception"));
			
			assertEquals(1, pidSet.size());
			
			pidSet = failedMap.getOrCreateServicePIDSet(new TechnicalMetadataEnhancementService());
			assertEquals(1, pidSet.size());
			
			pidSet = failedMap.getServiceToPID().get(TechnicalMetadataEnhancementService.class.getName()).keySet();
			assertEquals(1, pidSet.size());
		} finally {
			if (baseFolder != null && baseFolder.exists())
				FileUtils.deleteDirectory(baseFolder);
		}
	}
	
	@Test
	public void removeTest() throws Exception {
		File baseFolder = null;
		try {
			String baseFolderPath = "target/failedEnhancementMapTest";
			baseFolder = new File(baseFolderPath);
			boolean madeDir = baseFolder.mkdir();
			assertTrue("Failed to create test directory", madeDir);
			
			FailedEnhancementMap failedMap = createExampleMap(baseFolderPath);
			
			failedMap.remove("uuid:test1");
			
			assertEquals(1, failedMap.size());
			
			FailedEnhancementEntry entry = failedMap.get("uuid:test1", TechnicalMetadataEnhancementService.class.getName());
			assertNull(entry);
			
			entry = failedMap.get("uuid:test2", TechnicalMetadataEnhancementService.class.getName());
			assertNotNull(entry);
			
			File[] pidFiles = baseFolder.listFiles();
			assertEquals(1, pidFiles.length);
		} finally {
			if (baseFolder != null && baseFolder.exists())
				FileUtils.deleteDirectory(baseFolder);
		}
	}
	
	@Test
	public void clearTest() throws Exception {
		File baseFolder = null;
		try {
			String baseFolderPath = "target/failedEnhancementMapTest";
			baseFolder = new File(baseFolderPath);
			boolean madeDir = baseFolder.mkdir();
			assertTrue("Failed to create test directory", madeDir);
			
			FailedEnhancementMap failedMap = createExampleMap(baseFolderPath);
			
			failedMap.clear();
			
			assertEquals(0, failedMap.size());
			
			File[] pidFiles = baseFolder.listFiles();
			assertEquals(0, pidFiles.length);
		} finally {
			if (baseFolder != null && baseFolder.exists())
				FileUtils.deleteDirectory(baseFolder);
		}
	}
}

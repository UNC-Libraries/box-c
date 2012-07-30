package edu.unc.lib.dl.cdr.services.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.cdr.services.model.FailedEnhancementObject.MessageFailure;
import edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.message.ActionMessage;
import static org.mockito.Mockito.*;

public class FailedObjectHashMapTest extends Assert {

	@Test
	public void serializationReloadTest() throws IOException, ClassNotFoundException{
		String filePath = "failedEnhancements.data";
		File failFile = new File(filePath);
		try {
			FailedObjectHashMap failedEnhancements = FailedObjectHashMap.loadFailedEnhancements(failFile.getAbsolutePath(), null);
			assertEquals("Newly created map should have no entries", 0, failedEnhancements.size());
			
			TechnicalMetadataEnhancementService service = new TechnicalMetadataEnhancementService();
			EnhancementMessage message = new EnhancementMessage("uuid:test", null, "testAction");
			ArrayList<String> filteredServices = new ArrayList<String>();
			filteredServices.add(service.getClass().getName());
			message.setFilteredServices(filteredServices);
			
			failedEnhancements.add(new PID("uuid:test"), service.getClass(), message);
			assertTrue(failedEnhancements.contains("uuid:test", service.getClass().getName()));
			
			assertEquals(1, failedEnhancements.size());
			
			failedEnhancements.serializeFailedEnhancements();
			
			//Discarding failed list, maybe GC will dispose of it, maybe it won't.
			failedEnhancements = null;
			
			failedEnhancements = FailedObjectHashMap.loadFailedEnhancements(failFile.getAbsolutePath(), null);
			assertEquals("Reloaded map should have 1 entry", 1, failedEnhancements.size());
		} finally {
			failFile.delete();
		}
	}
	
	@Test
	public void serializationMultipleFileTest() throws IOException, ClassNotFoundException{
		String filePath1 = "failedEnhancements1.data";
		String filePath2 = "failedEnhancements2.data";
		File failFile1 = new File(filePath1);
		File failFile2 = new File(filePath2);
		try {
			FailedObjectHashMap failedEnhancements = FailedObjectHashMap.loadFailedEnhancements(failFile1.getAbsolutePath(), null);
			assertEquals("Newly created map should have no entries", 0, failedEnhancements.size());
			
			ActionMessage message = new EnhancementMessage("uuid:test", null, "testAction");
			
			failedEnhancements.add(new PID("uuid:test"), TechnicalMetadataEnhancementService.class, message);
			
			assertEquals(1, failedEnhancements.size());
			
			failedEnhancements.serializeFailedEnhancements();
			
			failedEnhancements.add(new PID("uuid:test2"), TechnicalMetadataEnhancementService.class, message);
			//Change files
			failedEnhancements.setSerializationPath(failFile2.getAbsolutePath());
			failedEnhancements.serializeFailedEnhancements();
			
			//Discarding failed list, maybe GC will dispose of it, maybe it won't.
			failedEnhancements = null;
			
			failedEnhancements = FailedObjectHashMap.loadFailedEnhancements(failFile1.getAbsolutePath(), null);
			assertEquals("Reloaded map should have 1 entry", 1, failedEnhancements.size());
			
			failedEnhancements = FailedObjectHashMap.loadFailedEnhancements(failFile2.getAbsolutePath(), null);
			assertEquals("Second reloaded map should have 2 entries", 2, failedEnhancements.size());
		} finally {
			failFile1.delete();
			failFile2.delete();
		}
	}
	
	@Test
	public void storeTraceTest() throws IOException, ClassNotFoundException{
		String filePath = "failedEnhancements.data";
		String tracePath = "traces/";
		File failFile = new File(filePath);
		File tracePathFile = new File(tracePath);
		tracePathFile.mkdir();
		FailedObjectHashMap failedEnhancements = null;
		try {
			failedEnhancements = FailedObjectHashMap.loadFailedEnhancements(failFile.getAbsolutePath(), tracePathFile.getAbsolutePath());
			
			TechnicalMetadataEnhancementService service = new TechnicalMetadataEnhancementService();
			EnhancementMessage message = new EnhancementMessage("uuid:test", null, "testAction");
			message.setMessageID("messageid");
			ArrayList<String> filteredServices = new ArrayList<String>();
			filteredServices.add(service.getClass().getName());
			message.setFilteredServices(filteredServices);
			
			failedEnhancements.add(new PID("uuid:test"), service.getClass(), message, new Exception("Test exception"));
			
			MessageFailure messageFailure = failedEnhancements.getMessageFailure("messageid");
			String failureLog = messageFailure.getFailureLog();
			assertNotNull(failureLog);
			assertTrue(failureLog.startsWith("java.lang.Exception: Test exception"));
			assertEquals("There must be 1 file in the failure log directory", 1, tracePathFile.listFiles().length);
			
			// Serialize and reload the fail list
			failedEnhancements.serializeFailedEnhancements();
			failedEnhancements = FailedObjectHashMap.loadFailedEnhancements(failFile.getAbsolutePath(), tracePathFile.getAbsolutePath());
			
			// Ensure the failure log persisted
			failureLog = messageFailure.getFailureLog();
			assertNotNull(failureLog);
			assertTrue(failureLog.startsWith("java.lang.Exception: Test exception"));
			assertEquals("There must be 1 file in the failure log directory after reloading", 1, tracePathFile.listFiles().length);
			
			// Clean up the failure logs
			failedEnhancements.clear();
			assertEquals("Failure list should be empty", 0, failedEnhancements.size());
			assertEquals("There must be 0 files in the failure log directory after clearing", 0, tracePathFile.listFiles().length);
		} finally {
			failFile.delete();
			if (failedEnhancements != null)
				failedEnhancements.clear();
			tracePathFile.delete();
		}
	}
	
	@Test
	public void removeFailedEnhancementFileCleanupTest() throws IOException, ClassNotFoundException{
		String filePath = "failedEnhancements.data";
		String tracePath = "traces/";
		File failFile = new File(filePath);
		File tracePathFile = new File(tracePath);
		tracePathFile.mkdir();
		FailedObjectHashMap failedEnhancements = null;
		try {
			failedEnhancements = FailedObjectHashMap.loadFailedEnhancements(failFile.getAbsolutePath(), tracePathFile.getAbsolutePath());
			
			TechnicalMetadataEnhancementService service = new TechnicalMetadataEnhancementService();
			EnhancementMessage message = new EnhancementMessage("uuid:test", null, "testAction");
			message.setMessageID("messageid");
			
			EnhancementMessage message2 = new EnhancementMessage("uuid:test2", null, "testAction");
			message2.setMessageID("messageid2");
			
			failedEnhancements.add(new PID("uuid:test"), service.getClass(), message, new Exception("Test exception"));
			failedEnhancements.add(new PID("uuid:test2"), service.getClass(), message2, new Exception("Test exception2"));
			
			MessageFailure messageFailure = failedEnhancements.getMessageFailure("messageid");
			String failureLog = messageFailure.getFailureLog();
			assertNotNull(failureLog);
			assertEquals("There must be 2 files in the failure log directory", 2, tracePathFile.listFiles().length);
			
			// Remove failure for the test pid.
			failedEnhancements.remove("uuid:test");
			assertEquals("Should be one remaining item in failure list", 1, failedEnhancements.size());
			assertEquals("There must be 1 file in the failure log directory after removal", 1, tracePathFile.listFiles().length);
			assertNull(failedEnhancements.getFailureByMessageID("messageid"));
			assertNotNull(failedEnhancements.getFailureByMessageID("messageid2"));
		} finally {
			failFile.delete();
			if (failedEnhancements != null)
				failedEnhancements.clear();
			tracePathFile.delete();
		}
	}
}

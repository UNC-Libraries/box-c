package edu.unc.lib.dl.cdr.services.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.cdr.services.techmd.TechnicalMetadataEnhancementService;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.message.ActionMessage;

public class FailedObjectHashMapTest extends Assert {

	@Test
	public void serializationReloadTest() throws IOException, ClassNotFoundException{
		String filePath = "failedEnhancements.data";
		File failFile = new File(filePath);
		try {
			FailedObjectHashMap failedEnhancements = FailedObjectHashMap.loadFailedEnhancements(failFile.getAbsolutePath());
			assertEquals("Newly created map should have no entries", 0, failedEnhancements.size());
			
			TechnicalMetadataEnhancementService service = new TechnicalMetadataEnhancementService();
			EnhancementMessage message = new EnhancementMessage("uuid:test", null, "testAction");
			ArrayList<String> filteredServices = new ArrayList<String>();
			filteredServices.add(service.getClass().getName());
			message.setFilteredServices(filteredServices);
			
			failedEnhancements.add(new PID("uuid:test"), service.getClass(), message);
			
			assertEquals(1, failedEnhancements.size());
			
			failedEnhancements.serializeFailedEnhancements();
			
			//Discarding failed list, maybe GC will dispose of it, maybe it won't.
			failedEnhancements = null;
			
			failedEnhancements = FailedObjectHashMap.loadFailedEnhancements(failFile.getAbsolutePath());
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
			FailedObjectHashMap failedEnhancements = FailedObjectHashMap.loadFailedEnhancements(failFile1.getAbsolutePath());
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
			
			failedEnhancements = FailedObjectHashMap.loadFailedEnhancements(failFile1.getAbsolutePath());
			assertEquals("Reloaded map should have 1 entry", 1, failedEnhancements.size());
			
			failedEnhancements = FailedObjectHashMap.loadFailedEnhancements(failFile2.getAbsolutePath());
			assertEquals("Second reloaded map should have 2 entries", 2, failedEnhancements.size());
		} finally {
			failFile1.delete();
			failFile2.delete();
		}
	}
}

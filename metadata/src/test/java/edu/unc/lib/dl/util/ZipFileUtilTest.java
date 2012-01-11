package edu.unc.lib.dl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class ZipFileUtilTest extends Assert {
	private static Logger LOG = Logger.getLogger(ZipFileUtilTest.class);
	
	@Test
	public void metsZip(){
		try {
			ClassPathResource resource = new ClassPathResource("/samples/test-mets-zip.zip");
			InputStream fis = resource.getInputStream();
			
			ZipFileUtil.unzipToTemp(fis);
		} catch (Exception e){
			LOG.error("Test failed", e);
			fail();
		}
		
	}
	
	/*@Test
	public void biomedZip(){
		
		try {
			ClassPathResource resource = new ClassPathResource("/samples/1471-2458-11-702.zip");
			InputStream fis = resource.getInputStream();
			
			File tempDir = ZipFileUtil.unzipToTemp(fis);
		} catch (Exception e){
			LOG.error("Test failed", e);
		}
		
	}*/
}

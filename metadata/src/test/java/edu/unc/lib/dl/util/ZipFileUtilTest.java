package edu.unc.lib.dl.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

public class ZipFileUtilTest {
	
	/*
	 * Test that entries with relative paths pointing outside the directory to
	 * which we're unzipping will cause an IOException.
	 */
	@Test
	public void testUnzipRelativePathsOutsideDir() throws IOException {
		
		File zipFile;
		
		ClassPathResource resource = new ClassPathResource("/samples/bad-zip-with-relative-path.zip");
		
		zipFile = resource.getFile();
		
		File tempDir = File.createTempFile("test", null);
		tempDir.delete();
		tempDir.mkdir();
		tempDir.deleteOnExit();
		
		try {
			ZipFileUtil.unzipToDir(zipFile, tempDir);
			fail("Expected IOException to be thrown");
		} catch (IOException e) {
		}
		
	}

}

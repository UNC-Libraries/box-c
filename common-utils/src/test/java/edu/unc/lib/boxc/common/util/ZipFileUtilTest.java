package edu.unc.lib.boxc.common.util;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.Test;

public class ZipFileUtilTest {

    /*
     * Test that entries with relative paths pointing outside the directory to
     * which we're unzipping will cause an IOException.
     */
    @Test
    public void testUnzipRelativePathsOutsideDir() throws Exception {

        URI zipUri = this.getClass().getResource("/samples/bad-zip-with-relative-path.zip").toURI();
        File zipFile = new File(zipUri);

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

    /*
     * Test that entries not pointing outside the directory to which we're unzipping
     * will NOT cause an IOException.
     */
    @Test
    public void testUnzipInsideDir() throws Exception {

        URI zipUri = this.getClass().getResource("/samples/test-mets-zip.zip").toURI();
        File zipFile = new File(zipUri);

        File tempDir = File.createTempFile("test", null);
        tempDir.delete();
        tempDir.mkdir();
        tempDir.deleteOnExit();

        try {
            ZipFileUtil.unzipToDir(zipFile, tempDir);
        } catch (IOException e) {
            fail("Expected IOException to not be thrown");
        }

    }

}

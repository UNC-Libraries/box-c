/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.boxc.common.util;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.junit.Test;

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

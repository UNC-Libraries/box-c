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
package edu.unc.lib.dl.persist.services.transfer;

import static edu.unc.lib.dl.model.DatastreamPids.getOriginalFilePid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.transfer.BinaryAlreadyExistsException;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferException;

/**
 * @author bbpennel
 *
 */
public class StreamToFSTransferClientTest {

    protected static final String TEST_UUID = "a168cf29-a2a9-4da8-9b8d-025855b180d5";
    protected static final String ORIGINAL_CONTENT = "Some original stuff";
    protected static final String STREAM_CONTENT = "Stream content";

    protected StreamToFSTransferClient client;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();
    protected Path storagePath;
    @Mock
    private StorageLocation storageLoc;

    protected PID binPid;
    protected Path binDestPath;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        tmpFolder.create();
        storagePath = tmpFolder.newFolder("storage").toPath();

        client = new StreamToFSTransferClient(storageLoc);

        binPid = getOriginalFilePid(PIDs.get(TEST_UUID));
        binDestPath = storagePath.resolve(binPid.getComponentId());

        when(storageLoc.getStorageUri(binPid)).thenReturn(binDestPath.toUri());
    }

    @Test
    public void transfer_NewFile() throws Exception {
        InputStream sourceStream = toStream(STREAM_CONTENT);

        client.transfer(binPid, sourceStream);

        assertContent(binDestPath, STREAM_CONTENT);
    }

    @Test(expected = BinaryAlreadyExistsException.class)
    public void transfer_ExistingFile() throws Exception {
        // Create existing file content
        FileUtils.copyInputStreamToFile(toStream(ORIGINAL_CONTENT), binDestPath.toFile());

        // Attempt to transfer new content
        InputStream sourceStream = toStream(STREAM_CONTENT);

        try {
            client.transfer(binPid, sourceStream);
        } finally {
            // Verify that the file was not replaced
            assertContent(binDestPath, ORIGINAL_CONTENT);
        }
    }

    @Test(expected = BinaryTransferException.class)
    public void transfer_StreamThrowsException() throws Exception {
        InputStream sourceStream = mock(InputStream.class);
        when(sourceStream.read(any())).thenThrow(new IOException());

        client.transfer(binPid, sourceStream);

        assertContent(binDestPath, STREAM_CONTENT);
    }

    @Test
    public void transferReplaceExisting_ExistingFile() throws Exception {
        // Create existing file content
        FileUtils.copyInputStreamToFile(toStream(ORIGINAL_CONTENT), binDestPath.toFile());

        InputStream sourceStream = toStream(STREAM_CONTENT);

        client.transferReplaceExisting(binPid, sourceStream);
        // Verify that the file was replaced
        assertContent(binDestPath, STREAM_CONTENT);
    }

    @Test(expected = BinaryTransferException.class)
    public void transferReplaceExisting_ExistingFile_WriteFails() throws Exception {
        InputStream sourceStream = mock(InputStream.class);
        when(sourceStream.read(any())).thenThrow(new IOException());

        // Create existing file content
        FileUtils.copyInputStreamToFile(toStream(ORIGINAL_CONTENT), binDestPath.toFile());

        try {
            client.transferReplaceExisting(binPid, sourceStream);
        } finally {
            // Verify that the content was not replaced
            assertContent(binDestPath, ORIGINAL_CONTENT);
        }
    }

    protected void assertContent(Path path, String content) throws Exception {
        assertTrue("Source content was not present at " + path, path.toFile().exists());
        assertEquals(content, FileUtils.readFileToString(path.toFile(), "UTF-8"));
    }

    protected InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }
}

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.ingest.IngestSource;
import edu.unc.lib.dl.persist.api.storage.BinaryDetails;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.transfer.BinaryAlreadyExistsException;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferException;

/**
 * @author bbpennel
 *
 */
public class FSToFSTransferClientTest {

    protected static final String TEST_UUID = "a168cf29-a2a9-4da8-9b8d-025855b180d5";
    protected static final String FILE_CONTENT = "File content";

    protected FSToFSTransferClient client;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();
    protected Path sourcePath;
    protected Path storagePath;
    @Mock
    protected IngestSource ingestSource;
    @Mock
    private StorageLocation storageLoc;

    protected PID binPid;
    protected Path binDestPath;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        tmpFolder.create();
        sourcePath = tmpFolder.newFolder("source").toPath();
        storagePath = tmpFolder.newFolder("storage").toPath();

        client = new FSToFSTransferClient(ingestSource, storageLoc);

        binPid = getOriginalFilePid(PIDs.get(TEST_UUID));
        binDestPath = storagePath.resolve(binPid.getComponentId());

        when(storageLoc.getStorageUri(binPid)).thenReturn(binDestPath.toUri());
    }

    @Test(expected = BinaryTransferException.class)
    public void transferFileThatDoesNotExist() {
        Path sourceFile = sourcePath.resolve("nofilehere.txt");
        client.transfer(binPid, sourceFile.toUri());
    }

    @Test
    public void transferFileReadOnlySource() throws Exception {
        when(ingestSource.isReadOnly()).thenReturn(true);

        Path sourceFile = createSourceFile();

        client.transfer(binPid, sourceFile.toUri());

        assertIsSourceFile(binDestPath);
        assertIsSourceFile(sourceFile);
    }

    @Test
    public void transferFileModifiableSource() throws Exception {
        when(ingestSource.isReadOnly()).thenReturn(false);

        Path sourceFile = createSourceFile();

        client.transfer(binPid, sourceFile.toUri());

        assertIsSourceFile(binDestPath);
    }

    @Test(expected = BinaryAlreadyExistsException.class)
    public void transferFileAlreadyExists() throws Exception {
        String existingContent = "I exist";

        Files.createDirectories(binDestPath.getParent());
        createFile(binDestPath, existingContent);
        Path sourceFile = createSourceFile();

        client.transfer(binPid, sourceFile.toUri());
    }

    @Test(expected = BinaryTransferException.class)
    public void transferReplaceFileThatDoesNotExist() {
        Path sourceFile = sourcePath.resolve("nofilehere.txt");
        client.transferReplaceExisting(binPid, sourceFile.toUri());
    }

    @Test
    public void transferReplaceFileReadOnlySource() throws Exception {
        when(ingestSource.isReadOnly()).thenReturn(true);

        Path sourceFile = createSourceFile();

        client.transferReplaceExisting(binPid, sourceFile.toUri());

        assertIsSourceFile(binDestPath);
        assertIsSourceFile(sourceFile);
    }

    @Test
    public void transferReplaceFileModifiableSource() throws Exception {
        when(ingestSource.isReadOnly()).thenReturn(false);

        Path sourceFile = createSourceFile();

        client.transferReplaceExisting(binPid, sourceFile.toUri());

        assertIsSourceFile(binDestPath);
    }

    @Test
    public void transferReplaceFileAlreadyExists() throws Exception {
        String existingContent = "I exist";

        Files.createDirectories(binDestPath.getParent());
        createFile(binDestPath, existingContent);
        Path sourceFile = createSourceFile();

        client.transferReplaceExisting(binPid, sourceFile.toUri());

        assertIsSourceFile(binDestPath);
    }

    @Test
    public void rollbackOnTransferInterruption() throws Exception {
        String existingContent = "I exist";
        Path parentPath = binDestPath.getParent();
        Files.createDirectories(parentPath);

        createFile(binDestPath, existingContent);
        Path sourceFile = createSourceFile();

        File destFile = binDestPath.toFile();
        File parentDir = parentPath.toFile();
        parentDir.setReadOnly();

        try {
            client.transferReplaceExisting(binPid, sourceFile.toUri());
        } catch (BinaryTransferException e) {
            assertTrue("Original file should be present", destFile.exists());
            assertEquals(1, binDestPath.getParent().toFile().listFiles().length);
        } finally {
            parentDir.setWritable(true);
            destFile.delete();
        }
    }

    @Test(expected = NotImplementedException.class)
    public void transferVersion() throws Exception {
        Path sourceFile = createSourceFile();
        client.transferVersion(binPid, sourceFile.toUri());
    }

    @Test
    public void interruptTransfer() throws Exception {
        when(ingestSource.isReadOnly()).thenReturn(true);

        // Generate a 10mb file for transferring
        Path sourceFile = sourcePath.resolve("file.txt");
        try (RandomAccessFile filler = new RandomAccessFile(sourceFile.toFile(), "rw")) {
            filler.setLength(100 * 1024 * 1024);
        }

        Thread copyThread = new Thread() {
            @Override
            public void run() {
                try {
                    client.transfer(binPid, sourceFile.toUri());
                    fail("Transfer must be interrupted");
                } catch (BinaryTransferException e) {
                    // expected
                }
            }
        };

        copyThread.start();
        // Wait briefly to let transfer start
        Thread.sleep(10);

        copyThread.interrupt();

        assertFalse(Files.exists(binDestPath));
    }

    @Test
    public void shutdownClientSucceeds() throws Exception {
        client.shutdown();
    }

    @Test
    public void getStoredBinaryDetailsForAbsentFile() {
        assertNull(client.getStoredBinaryDetails(binPid));
    }

    @Test
    public void getStoredBinaryDetailsTransferredFile() throws Exception {
        Path sourceFile = createSourceFile();

        client.transfer(binPid, sourceFile.toUri());

        BinaryDetails details = client.getStoredBinaryDetails(binPid);
        assertNotNull(details);
        assertNotNull(details.getLastModified());
        assertEquals(12, details.getSize());
    }

    @Test
    public void isTransferredNotPresent() throws Exception {
        assertFalse(client.isTransferred(binPid, sourcePath.resolve("absent.txt").toUri()));
    }

    @Test
    public void isTransferredMatch() throws Exception {
        Path sourceFile = createSourceFile();
        client.transfer(binPid, sourceFile.toUri());

        assertTrue(client.isTransferred(binPid, sourceFile.toUri()));
    }

    @Test
    public void isTransferredDoNotMatch() throws Exception {
        Path sourceFile = createSourceFile();
        client.transfer(binPid, sourceFile.toUri());

        FileUtils.writeStringToFile(sourceFile.toFile(), "changed content", "UTF-8");
        assertFalse(client.isTransferred(binPid, sourceFile.toUri()));
    }

    @Test
    public void isTransferredSourceGoneReadOnly() throws Exception {
        when(ingestSource.isReadOnly()).thenReturn(true);

        Path sourceFile = createSourceFile();
        client.transfer(binPid, sourceFile.toUri());
        Files.delete(sourceFile);

        try {
            client.isTransferred(binPid, sourceFile.toUri());
            fail("Expected BinaryTransferException");
        } catch (BinaryTransferException e) {
            // expected
        }
    }

    @Test
    public void isTransferredSourceGoneWritable() throws Exception {
        when(ingestSource.isReadOnly()).thenReturn(false);

        Path sourceFile = createSourceFile();
        client.transfer(binPid, sourceFile.toUri());

    //    assertFalse(Files.exists(sourceFile));
        assertTrue(client.isTransferred(binPid, sourceFile.toUri()));
    }

    protected Path createSourceFile() throws Exception {
        return createFile(sourcePath.resolve("file.txt"), FILE_CONTENT);
    }

    private Path createFile(Path filePath, String content) throws Exception {
        FileUtils.writeStringToFile(filePath.toFile(), content, "UTF-8");
        return filePath;
    }

    protected void assertIsSourceFile(Path path) throws Exception {
        assertTrue("Source file was not present at " + path, path.toFile().exists());
        assertEquals(FILE_CONTENT, FileUtils.readFileToString(path.toFile(), "UTF-8"));
    }
}

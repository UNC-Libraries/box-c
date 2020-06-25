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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

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
        assertFalse("Source file should not exist after transfer", sourceFile.toFile().exists());
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
        assertFalse("Source file should not exist after transfer", sourceFile.toFile().exists());
    }

    @Test
    public void transferReplaceFileAlreadyExists() throws Exception {
        String existingContent = "I exist";

        Files.createDirectories(binDestPath.getParent());
        createFile(binDestPath, existingContent);
        Path sourceFile = createSourceFile();

        client.transferReplaceExisting(binPid, sourceFile.toUri());

        assertIsSourceFile(binDestPath);
        assertFalse("Source file should not exist after transfer", sourceFile.toFile().exists());
    }

    @Test
    public void rollbackOnTransferInterruption() throws Exception {
        String existingContent = "I exist";

        Files.createDirectories(binDestPath.getParent());
        createFile(binDestPath, existingContent);
        Path sourceFile = createSourceFile();

        try {
            client.transfer(binPid, sourceFile.toUri());
        } catch (BinaryAlreadyExistsException e) {
            assertTrue("Original file should be present", sourceFile.toFile().exists());
            assertEquals(1, binDestPath.getParent().toFile().listFiles().length);
        }
    }

    @Test(expected = NotImplementedException.class)
    public void transferVersion() throws Exception {
        Path sourceFile = createSourceFile();
        client.transferVersion(binPid, sourceFile.toUri());
    }

    @Test
    public void shutdownClientSucceeds() throws Exception {
        client.shutdown();
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

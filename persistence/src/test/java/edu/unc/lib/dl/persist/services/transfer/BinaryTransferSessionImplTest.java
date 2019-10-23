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

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.DATA_FILE_FILESET;
import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.ORIGINAL_FILE;
import static edu.unc.lib.dl.persist.services.storage.StorageType.FILESYSTEM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.ingest.IngestSource;
import edu.unc.lib.dl.persist.services.ingest.IngestSourceManager;
import edu.unc.lib.dl.persist.services.storage.StorageLocation;

/**
 * @author bbpennel
 *
 */
public class BinaryTransferSessionImplTest {

    private static final String FILE_CONTENT = "File content";

    private BinaryTransferSessionImpl session;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();
    private Path sourcePath;
    private Path storagePath;
    @Mock
    private IngestSourceManager sourceManager;
    @Mock
    private IngestSource ingestSource;
    @Mock
    private StorageLocation storageLoc;

    private PID binPid;
    private Path binDestPath;


    @Before
    public void setup() throws Exception {
        initMocks(this);
        tmpFolder.create();
        sourcePath = tmpFolder.newFolder("source").toPath();
        storagePath = tmpFolder.newFolder("storage").toPath();

        String binId = makeBinId();
        binPid = PIDs.get(binId);
        binDestPath = storagePath.resolve(binId);

        when(sourceManager.getIngestSourceForUri(any(URI.class))).thenReturn(ingestSource);
        when(ingestSource.getId()).thenReturn("source1");

        when(storageLoc.getStorageUri(binPid)).thenReturn(binDestPath.toUri());
        when(storageLoc.getId()).thenReturn("loc1");
    }

    @Test
    public void transferFSToFSInTry() throws Exception {
        when(ingestSource.getStorageType()).thenReturn(FILESYSTEM);
        when(storageLoc.getStorageType()).thenReturn(FILESYSTEM);

        try (BinaryTransferSessionImpl session = new BinaryTransferSessionImpl(sourceManager)) {
            Path sourceFile = createSourceFile();

            session.transfer(binPid, sourceFile.toUri(), storageLoc);

            assertIsSourceFile(binDestPath);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void transferNoDestination() throws Exception {
        initSession();

        Path sourceFile = createSourceFile();

        session.transfer(binPid, sourceFile.toUri(), null);
    }

    @Test(expected = NotImplementedException.class)
    public void transferUnknownStorageTypeMatchup() throws Exception {
        when(ingestSource.getStorageType()).thenReturn(FILESYSTEM);
        when(storageLoc.getStorageType()).thenReturn(null);

        initSession();

        Path sourceFile = createSourceFile();

        session.transfer(binPid, sourceFile.toUri(), storageLoc);
    }

    @Test
    public void transferFSToFSMultipleFilesSameDestination() throws Exception {
        when(ingestSource.getStorageType()).thenReturn(FILESYSTEM);
        when(storageLoc.getStorageType()).thenReturn(FILESYSTEM);

        String binId2 = makeBinId();
        PID binPid2 = PIDs.get(binId2);
        Path binDestPath2 = storagePath.resolve(binId2);
        when(storageLoc.getStorageUri(binPid2)).thenReturn(binDestPath2.toUri());

        Path sourceFile = createSourceFile();
        Path sourceFile2 = createSourceFile("another.txt", "stuff");

        // Initialize session with the destination
        try (BinaryTransferSessionImpl session = new BinaryTransferSessionImpl(sourceManager, storageLoc)) {
            URI result1 = session.transfer(binPid, sourceFile.toUri());
            URI result2 = session.transfer(binPid2, sourceFile2.toUri());

            assertEquals(Paths.get(result1), binDestPath);
            assertEquals(Paths.get(result2), binDestPath2);
            assertFileContent(Paths.get(result1), FILE_CONTENT);
            assertFileContent(Paths.get(result2), "stuff");
        }
    }

    @Test
    public void transferFSToFSMultipleFilesDifferentDestination() throws Exception {
        when(ingestSource.getStorageType()).thenReturn(FILESYSTEM);
        when(storageLoc.getStorageType()).thenReturn(FILESYSTEM);

        // Setup a second storage location
        StorageLocation storageLoc2 = mock(StorageLocation.class);
        when(storageLoc2.getId()).thenReturn("loc2");
        when(storageLoc2.getStorageType()).thenReturn(FILESYSTEM);
        Path storagePath2 = tmpFolder.newFolder("storage2").toPath();

        String binId2 = makeBinId();
        PID binPid2 = PIDs.get(binId2);
        Path binDestPath2 = storagePath2.resolve(binId2);
        // Establish destination path for second binary in second location
        when(storageLoc2.getStorageUri(binPid2)).thenReturn(binDestPath2.toUri());

        Path sourceFile = createSourceFile();
        Path sourceFile2 = createSourceFile("another.txt", "stuff");

        try (BinaryTransferSessionImpl session = new BinaryTransferSessionImpl(sourceManager)) {
            URI result1 = session.transfer(binPid, sourceFile.toUri(), storageLoc);
            URI result2 = session.transfer(binPid2, sourceFile2.toUri(), storageLoc2);

            // Verify that results ended up in the right storage locations
            assertTrue(result1.toString().contains("storage/"));
            assertTrue(result2.toString().contains("storage2/"));
            assertFileContent(Paths.get(result1), FILE_CONTENT);
            assertFileContent(Paths.get(result2), "stuff");
        }
    }

    @Test
    public void transferFSToFSMultipleFilesTwoSourcesOneDest() throws Exception {
        when(ingestSource.getStorageType()).thenReturn(FILESYSTEM);
        when(storageLoc.getStorageType()).thenReturn(FILESYSTEM);

        Path sourceFile = createSourceFile();
        Path sourcePath2 = tmpFolder.newFolder("source2").toPath();
        Path sourceFile2 = createFile(sourcePath2.resolve("another.txt"), "stuff");

        String binId2 = makeBinId();
        PID binPid2 = PIDs.get(binId2);
        Path binDestPath2 = storagePath.resolve(binId2);
        when(storageLoc.getStorageUri(binPid2)).thenReturn(binDestPath2.toUri());

        // Create second ingest source, which is not read only
        IngestSource source2 = mock(IngestSource.class);
        when(source2.getId()).thenReturn("source2");
        when(sourceManager.getIngestSourceForUri(sourceFile2.toUri())).thenReturn(source2);
        when(source2.getStorageType()).thenReturn(FILESYSTEM);

        // Make first ingest source read only, so it will be different from the second
        when(ingestSource.isReadOnly()).thenReturn(true);

        try (BinaryTransferSessionImpl session = new BinaryTransferSessionImpl(sourceManager)) {
            URI result1 = session.transfer(binPid, sourceFile.toUri(), storageLoc);
            URI result2 = session.transfer(binPid2, sourceFile2.toUri(), storageLoc);

            // Verify that results ended up in the right storage locations
            assertTrue(result1.toString().contains("storage/"));
            assertTrue(result2.toString().contains("storage/"));
            assertFileContent(Paths.get(result1), FILE_CONTENT);
            assertFileContent(Paths.get(result2), "stuff");
            // First source file should still be present
            assertIsSourceFile(sourceFile);
            assertFalse("Second source file should no longer exist", sourceFile2.toFile().exists());
        }
    }

    @Test
    public void replaceBinaryFSToFS() throws Exception {
        when(ingestSource.getStorageType()).thenReturn(FILESYSTEM);
        when(storageLoc.getStorageType()).thenReturn(FILESYSTEM);

        Files.createDirectories(binDestPath.getParent());
        createFile(binDestPath, "some stuff");
        Path sourceFile = createSourceFile();

        try (BinaryTransferSessionImpl session = new BinaryTransferSessionImpl(sourceManager, storageLoc)) {
            session.transferReplaceExisting(binPid, sourceFile.toUri());

            assertIsSourceFile(binDestPath);
        }
    }

    @Test(expected = NotImplementedException.class)
    public void transferVersionFSToFS() throws Exception {
        when(ingestSource.getStorageType()).thenReturn(FILESYSTEM);
        when(storageLoc.getStorageType()).thenReturn(FILESYSTEM);

        Path sourceFile = createSourceFile();

        try (BinaryTransferSessionImpl session = new BinaryTransferSessionImpl(sourceManager, storageLoc)) {
            session.transferVersion(binPid, sourceFile.toUri());
        }
    }

    private void initSession() {
        session = new BinaryTransferSessionImpl(sourceManager);
    }

    private Path createSourceFile() throws Exception {
        return createFile(sourcePath.resolve("file.txt"), FILE_CONTENT);
    }

    private Path createSourceFile(String filename, String content) throws Exception {
        return createFile(sourcePath.resolve(filename), content);
    }

    private Path createFile(Path path, String content) throws Exception {
        FileUtils.writeStringToFile(path.toFile(), content, "UTF-8");
        return path;
    }

    private void assertIsSourceFile(Path path) throws Exception {
        assertFileContent(path, FILE_CONTENT);
    }

    private void assertFileContent(Path path, String content) throws Exception {
        assertTrue("File was not present at " + path, path.toFile().exists());
        assertEquals(content, FileUtils.readFileToString(path.toFile(), "UTF-8"));
    }

    private String makeBinId() {
        return UUID.randomUUID().toString() + "/" + DATA_FILE_FILESET + "/" + ORIGINAL_FILE;
    }
}

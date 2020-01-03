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

import static edu.unc.lib.dl.persist.services.storage.StorageType.FILESYSTEM;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.ingest.IngestSource;
import edu.unc.lib.dl.persist.services.ingest.IngestSourceManager;
import edu.unc.lib.dl.persist.services.storage.StorageLocation;

/**
 * @author bbpennel
 *
 */
public class MultiDestinationTransferSessionImplTest extends AbstractBinaryTransferTest {

    private static final String FILE_CONTENT = "File content";

    private Path storagePath2;
    @Mock
    private IngestSourceManager sourceManager;
    @Mock
    private IngestSource ingestSource;
    @Mock
    private StorageLocation storageLoc;
    @Mock
    private StorageLocation storageLoc2;

    private PID binPid;
    private Path binDestPath;

    @Before
    public void setup() throws Exception {
        initMocks(this);
        createPaths();

        binPid = makeBinPid();
        binDestPath = storagePath.resolve(binPid.getComponentId());

        when(sourceManager.getIngestSourceForUri(any(URI.class))).thenReturn(ingestSource);
        when(ingestSource.getId()).thenReturn("source1");

        when(ingestSource.getStorageType()).thenReturn(FILESYSTEM);
        when(storageLoc.getStorageType()).thenReturn(FILESYSTEM);
        when(storageLoc.getStorageUri(binPid)).thenReturn(binDestPath.toUri());
        when(storageLoc.getId()).thenReturn("loc1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void noDestination() throws Exception {
        try (MultiDestinationTransferSessionImpl session = new MultiDestinationTransferSessionImpl(sourceManager)) {
            session.forDestination(null);
        }
    }

    @Test
    public void transfer_FSToFS_MultipleFiles_SameDestination() throws Exception {
        PID binPid2 = makeBinPid();
        Path binDestPath2 = storagePath.resolve(binPid2.getComponentId());
        // Establish destination path for second binary in second location
        when(storageLoc.getStorageUri(binPid2)).thenReturn(binDestPath2.toUri());

        Path sourceFile = createSourceFile();
        Path sourceFile2 = createSourceFile("another.txt", "stuff");

        try (MultiDestinationTransferSessionImpl session = new MultiDestinationTransferSessionImpl(sourceManager)) {
            URI result1 = session.forDestination(storageLoc).transfer(binPid, sourceFile.toUri());
            URI result2 = session.forDestination(storageLoc).transfer(binPid2, sourceFile2.toUri());

            // Verify that results ended up in the right storage locations
            assertTrue(result1.toString().contains("storage/"));
            assertTrue(result2.toString().contains("storage/"));
            assertFileContent(Paths.get(result1), FILE_CONTENT);
            assertFileContent(Paths.get(result2), "stuff");
        }
    }

    @Test
    public void transfer_FSToFS_MultipleFiles_DifferentDestination() throws Exception {
        when(storageLoc2.getId()).thenReturn("loc2");
        when(storageLoc2.getStorageType()).thenReturn(FILESYSTEM);
        storagePath2 = tmpFolder.newFolder("storage2").toPath();

        PID binPid2 = makeBinPid();
        Path binDestPath2 = storagePath2.resolve(binPid2.getComponentId());
        // Establish destination path for second binary in second location
        when(storageLoc2.getStorageUri(binPid2)).thenReturn(binDestPath2.toUri());

        Path sourceFile = createSourceFile();
        Path sourceFile2 = createSourceFile("another.txt", "stuff");

        try (MultiDestinationTransferSessionImpl session = new MultiDestinationTransferSessionImpl(sourceManager)) {
            URI result1 = session.forDestination(storageLoc).transfer(binPid, sourceFile.toUri());
            URI result2 = session.forDestination(storageLoc2).transfer(binPid2, sourceFile2.toUri());

            // Verify that results ended up in the right storage locations
            assertTrue(result1.toString().contains("storage/"));
            assertTrue(result2.toString().contains("storage2/"));
            assertFileContent(Paths.get(result1), FILE_CONTENT);
            assertFileContent(Paths.get(result2), "stuff");
        }
    }
}

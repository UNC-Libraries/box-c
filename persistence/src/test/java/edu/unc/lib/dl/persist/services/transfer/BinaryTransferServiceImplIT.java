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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.ingest.IngestSourceManagerImpl;
import edu.unc.lib.dl.persist.services.ingest.IngestSourceTestHelper;
import edu.unc.lib.dl.persist.services.storage.HashedFilesystemStorageLocation;
import edu.unc.lib.dl.persist.services.storage.StorageLocation;
import edu.unc.lib.dl.persist.services.storage.StorageLocationManagerImpl;

/**
 * @author bbpennel
 *
 */
public class BinaryTransferServiceImplIT {

    private BinaryTransferServiceImpl transferService;

    private IngestSourceManagerImpl sourceManager;
    private StorageLocationManagerImpl storageManager;
    private RepositoryPIDMinter pidMinter;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();
    private Path sourceConfigPath;
    private Path storageConfigPath;

    private Path sourcePath1;
    private Path sourcePath2;
    private Path storagePath1;
    private Path storagePath2;

    @Before
    public void setup() throws Exception {
        tmpFolder.create();

        pidMinter = new RepositoryPIDMinter();

        File sourceMappingFile = new File(tmpFolder.getRoot(), "sourceMapping.json");
        FileUtils.writeStringToFile(sourceMappingFile, "[]", "UTF-8");

        sourcePath1 = tmpFolder.newFolder("source_wr").toPath();
        sourcePath2 = tmpFolder.newFolder("source_ro").toPath();
        sourceConfigPath = IngestSourceTestHelper.createConfigFile(
                IngestSourceTestHelper.createFilesystemConfig("source_wr", "Mod", sourcePath1, asList("*")),
                IngestSourceTestHelper.createFilesystemConfig("source_ro", "RO", sourcePath2, asList("*"), true));

        sourceManager = new IngestSourceManagerImpl();
        sourceManager.setConfigPath(sourceConfigPath.toString());
        sourceManager.setMappingPath(sourceMappingFile.toString());
        sourceManager.init();

        File storageMappingFile = new File(tmpFolder.getRoot(), "storageMapping.json");
        FileUtils.writeStringToFile(storageMappingFile, "[]", "UTF-8");

        storagePath1 = tmpFolder.newFolder("storage1").toPath();
        storagePath2 = tmpFolder.newFolder("storage2").toPath();
        storageConfigPath = createStorageConfigFile(
                addStorageLocation("loc1", "Loc 1", storagePath1),
                addStorageLocation("loc2", "Loc 2", storagePath2));

        storageManager = new StorageLocationManagerImpl();
        storageManager.setConfigPath(storageConfigPath.toString());
        storageManager.setMappingPath(storageMappingFile.toString());
        storageManager.init();

        transferService = new BinaryTransferServiceImpl();
        transferService.setIngestSourceManager(sourceManager);
    }

    @Test
    public void singleDestinationTransfer() throws Exception {
        PID binPid = pidMinter.mintContentPid();

        StorageLocation destination = storageManager.getStorageLocationById("loc1");
        Path sourceFile = createSourceFile(sourcePath1, "myfile.txt", "some content");

        try (BinaryTransferSession session = transferService.getSession(destination)) {
            URI destUri = session.transfer(binPid, sourceFile.toUri());

            assertTrue(new File(destUri).exists());
            assertFalse(sourceFile.toFile().exists());
        }
    }

    @Test
    public void multiSourceAndDestinationTransfer() throws Exception {
        PID binPid1 = pidMinter.mintContentPid();
        PID binPid2 = pidMinter.mintContentPid();

        Path sourceFile1 = createSourceFile(sourcePath1, "myfile.txt", "some content");
        Path sourceFile2 = createSourceFile(sourcePath2, "other.txt", "stuff");

        try (MultiDestinationTransferSession session = transferService.getSession()) {
            StorageLocation dest1 = storageManager.getStorageLocationById("loc1");
            URI destUri1 = session.forDestination(dest1).transfer(binPid1, sourceFile1.toUri());
            assertTrue(new File(destUri1).exists());
            assertFalse(sourceFile1.toFile().exists());

            StorageLocation dest2 = storageManager.getStorageLocationById("loc2");
            URI destUri2 = session.forDestination(dest2).transfer(binPid2, sourceFile2.toUri());
            assertTrue(new File(destUri2).exists());
            assertTrue("Transfer from read only source should not delete source file",
                    sourceFile2.toFile().exists());
        }
    }

    @Test(expected = BinaryAlreadyExistsException.class)
    public void repeatTransfer() throws Exception {
        PID binPid = pidMinter.mintContentPid();

        StorageLocation destination = storageManager.getStorageLocationById("loc1");
        Path sourceFile = createSourceFile(sourcePath2, "myfile.txt", "some content");

        try (BinaryTransferSession session = transferService.getSession(destination)) {
            URI destUri = session.transfer(binPid, sourceFile.toUri());

            assertTrue(new File(destUri).exists());
            assertTrue("Transfer from read only source should not delete source file",
                    sourceFile.toFile().exists());

            // Try to transfer the file again
            session.transfer(binPid, sourceFile.toUri());
        }
    }

    private Map<String, Object> addStorageLocation(String id, String name, Path base) throws IOException {
        Map<String, Object> info = new HashMap<>();
        info.put("id", id);
        info.put("name", name);
        info.put("type", HashedFilesystemStorageLocation.TYPE_NAME);
        info.put("base", base.toUri().toString());
        return info;
    }

    @SafeVarargs
    public static final Path createStorageConfigFile(Map<String, Object>... configs) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Path configPath = Files.createTempFile("storageConfig", ".json");
        mapper.writeValue(configPath.toFile(), configs);
        return configPath;
    }

    private Path createSourceFile(Path sourcePath, String filename, String content) throws Exception {
        Path sourceFile = sourcePath.resolve(filename);
        FileUtils.writeStringToFile(sourceFile.toFile(), content, "UTF-8");
        return sourceFile;
    }
}

package edu.unc.lib.boxc.persist.impl.transfer;

import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.transfer.BinaryAlreadyExistsException;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.boxc.persist.api.transfer.MultiDestinationTransferSession;
import edu.unc.lib.boxc.persist.impl.sources.IngestSourceManagerImpl;
import edu.unc.lib.boxc.persist.impl.sources.IngestSourceTestHelper;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationManagerImpl;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationTestHelper;
import org.apache.commons.io.FileUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author bbpennel
 *
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/cdr-client-container.xml")
})
public class BinaryTransferServiceImplIT {

    @Autowired
    private String baseAddress;
    @Autowired
    private BinaryTransferServiceImpl transferService;

    private IngestSourceManagerImpl sourceManager;
    @Autowired
    private StorageLocationManagerImpl storageManager;
    @Autowired
    private PIDMinter pidMinter;
    @Autowired
    private TransactionManager txManager;
    @Autowired
    private RepositoryObjectFactory repoObjFactory;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;

    @TempDir
    public Path tmpFolder;
    private Path sourceConfigPath;
    private StorageLocationTestHelper storageHelper;

    private Path sourcePath1;
    private Path sourcePath2;
    private Path storagePath1;
    private Path storagePath2;

    @BeforeEach
    public void setup() throws Exception {
        TestHelper.setContentBase(baseAddress);

        storageHelper = new StorageLocationTestHelper();
        File sourceMappingFile = new File(tmpFolder.toFile(), "sourceMapping.json");
        FileUtils.writeStringToFile(sourceMappingFile, "[]", "UTF-8");

        sourcePath1 = tmpFolder.resolve("source_wr");
        Files.createDirectory(sourcePath1);
        sourcePath2 = tmpFolder.resolve("source_ro");
        Files.createDirectory(sourcePath2);
        sourceConfigPath = IngestSourceTestHelper.createConfigFile(
                IngestSourceTestHelper.createFilesystemConfig("source_wr", "Mod", sourcePath1, asList("*")),
                IngestSourceTestHelper.createFilesystemConfig("source_ro", "RO", sourcePath2, asList("*"), true));

        sourceManager = new IngestSourceManagerImpl();
        sourceManager.setConfigPath(sourceConfigPath.toString());
        sourceManager.setMappingPath(sourceMappingFile.toString());
        sourceManager.init();

        transferService.setIngestSourceManager(sourceManager);

        storagePath1 = storageHelper.getBaseStoragePath().resolve("storage1");
        storageHelper.addStorageLocation("loc1", "Loc 1", storagePath1.toUri().toString());
        storagePath2 = storageHelper.getBaseStoragePath().resolve("storage2");
        storageHelper.addStorageLocation("loc2", "Loc 2", storagePath2.toUri().toString());
        var storageMappingFile = storageHelper.serializeLocationMappings();
        var storageConfigFile = storageHelper.serializeLocationConfig();

        storageManager.setConfigPath(storageConfigFile);
        storageManager.setMappingPath(storageMappingFile);
        storageManager.init();
    }

    @Test
    public void singleDestinationTransfer() throws Exception {
        PID binPid = pidMinter.mintContentPid();

        StorageLocation destination = storageManager.getStorageLocationById("loc1");
        Path sourceFile = createSourceFile(sourcePath1, "myfile.txt", "some content");

        try (BinaryTransferSession session = transferService.getSession(destination)) {
            BinaryTransferOutcome outcome = session.transfer(binPid, sourceFile.toUri());

            assertTrue(new File(outcome.getDestinationUri()).exists());
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
            BinaryTransferOutcome outcome1 = session.forDestination(dest1).transfer(binPid1, sourceFile1.toUri());
            assertTrue(new File(outcome1.getDestinationUri()).exists());

            StorageLocation dest2 = storageManager.getStorageLocationById("loc2");
            BinaryTransferOutcome outcome2 = session.forDestination(dest2).transfer(binPid2, sourceFile2.toUri());
            assertTrue(new File(outcome2.getDestinationUri()).exists());
            assertTrue(sourceFile2.toFile().exists(),
                    "Transfer from read only source should not delete source file");
        }
    }

    @Test
    public void repeatTransfer() throws Exception {
        Assertions.assertThrows(BinaryAlreadyExistsException.class, () -> {
            PID binPid = pidMinter.mintContentPid();

            StorageLocation destination = storageManager.getStorageLocationById("loc1");
            Path sourceFile = createSourceFile(sourcePath2, "myfile.txt", "some content");

            try (BinaryTransferSession session = transferService.getSession(destination)) {
                BinaryTransferOutcome outcome = session.transfer(binPid, sourceFile.toUri());

                assertTrue(new File(outcome.getDestinationUri()).exists());
                assertTrue(sourceFile.toFile().exists(),
                        "Transfer from read only source should not delete source file");

                // Try to transfer the file again
                session.transfer(binPid, sourceFile.toUri());
            }
        });
    }

    @Test
    public void checkIfTransferred() throws Exception {
        PID binPid = pidMinter.mintContentPid();
        StorageLocation destination = storageManager.getStorageLocationById("loc1");
        Path sourceFile = createSourceFile(sourcePath1, "myfile.txt", "some content");
        URI sourceUri = sourceFile.toUri();

        try (BinaryTransferSession session = transferService.getSession(destination)) {
            assertFalse(session.isTransferred(binPid, sourceUri));

            // Perform transfer, should now return true
            session.transfer(binPid, sourceFile.toUri());
            assertTrue(session.isTransferred(binPid, sourceUri));

            // Change the file and see if its still considered transferred
            FileUtils.writeStringToFile(sourceFile.toFile(), "updated", "UTF-8");
            assertFalse(session.isTransferred(binPid, sourceUri));
        }
    }

    @Test
    public void rollbackNewFiles() throws Exception {
        PID binPid1 = pidMinter.mintContentPid();
        PID binPid2 = pidMinter.mintContentPid();
        StorageLocation destination = storageManager.getStorageLocationById("loc1");
        Path sourceFile1 = createSourceFile(sourcePath1, "myfile.txt", "some content");
        Path sourceFile2 = createSourceFile(sourcePath1, "myfile2.txt", "some more");
        URI sourceUri1 = sourceFile1.toUri();
        URI sourceUri2 = sourceFile2.toUri();

        FedoraTransaction tx = txManager.startTransaction();
        try (BinaryTransferSession session = transferService.getSession(destination)) {
            session.transfer(binPid1, sourceUri1);
            session.transfer(binPid2, sourceUri2);

            assertTrue(session.isTransferred(binPid1, sourceUri1));
            assertTrue(session.isTransferred(binPid2, sourceUri2));

            tx.cancelAndIgnore();

            // Binaries should no longer exist, after a short delay
            Awaitility.await().atMost(Duration.ofSeconds(2))
                .until(() -> !session.isTransferred(binPid1, sourceUri1)
                        && !session.isTransferred(binPid2, sourceUri2));
        } finally {
            tx.close();
        }
    }

    @Test
    public void rollbackUpdatedFile() throws Exception {
        StorageLocation destination = storageManager.getStorageLocationById("loc1");
        String filename = "myfile.txt";
        Path sourceFile1 = createSourceFile(sourcePath1, filename, "some content");
        Path sourceFile2 = createSourceFile(sourcePath2, filename, "updated content");
        URI sourceUri1 = sourceFile1.toUri();
        URI sourceUri2 = sourceFile2.toUri();

        // Create work with the initial state of the binary
        WorkObject workObj = repoObjFactory.createWorkObject(null);
        FileObject fileObj = repoObjFactory.createFileObject(null);
        workObj.addMember(fileObj);
        PID originalPid = DatastreamPids.getOriginalFilePid(fileObj.getPid());
        URI firstVersionContentUri;
        try (BinaryTransferSession session = transferService.getSession(destination)) {
            BinaryTransferOutcome outcome = session.transfer(originalPid, sourceUri1);
            firstVersionContentUri = outcome.getDestinationUri();
            BinaryObject binObj = fileObj.addOriginalFile(firstVersionContentUri, filename, "text/plain", null, null);

            assertTrue(session.isTransferred(originalPid, sourceUri1));
            assertTrue(FileUtils.contentEquals(new File(sourceUri1), new File(binObj.getContentUri())));
        }

        // Change the contents of the file in a transaction
        FedoraTransaction tx = txManager.startTransaction();
        try (BinaryTransferSession session = transferService.getSession(destination)) {

            BinaryTransferOutcome outcome = session.transferReplaceExisting(originalPid, sourceUri2);

            repoObjFactory.createOrUpdateBinary(originalPid,
                    outcome.getDestinationUri(), filename, "text/plain", null, null, null);

            BinaryObject originalBinary = repoObjLoader.getBinaryObject(originalPid);
            assertTrue(FileUtils.contentEquals(new File(sourceUri2), new File(originalBinary.getContentUri())),
                    "Content of the binary in tx must match the updated content");

            assertTrue(new File(firstVersionContentUri).exists(), "First version must still exist");
            assertTrue(new File(outcome.getDestinationUri()).exists(), "New file version must exist");

            // Rollback the transaction
            tx.cancelAndIgnore();

            originalBinary = repoObjLoader.getBinaryObject(originalPid);
            assertTrue(FileUtils.contentEquals(new File(sourceUri1), new File(originalBinary.getContentUri())),
                    "Content of the binary after canceling tx must be the original file");

            // old binary should still exist
            assertTrue(new File(firstVersionContentUri).exists());
            // New binary should no longer exist
            Awaitility.await().atMost(Duration.ofSeconds(2))
                .until(() -> !new File(outcome.getDestinationUri()).exists());
        } finally {
            tx.close();
        }
    }

    // Ensure that rollback succeeds when there is nothing to undo
    @Test
    public void rollbackWithNoBinaryChanges() throws Exception {
        FedoraTransaction tx = txManager.startTransaction();
        try {
            WorkObject workObj = repoObjFactory.createWorkObject(null);
            assertTrue(repoObjFactory.objectExists(workObj.getUri()));
            tx.cancelAndIgnore();
            assertFalse(repoObjFactory.objectExists(workObj.getUri()));
        } finally {
            tx.close();
        }
    }

    private Path createSourceFile(Path sourcePath, String filename, String content) throws Exception {
        Path sourceFile = sourcePath.resolve(filename);
        FileUtils.writeStringToFile(sourceFile.toFile(), content, "UTF-8");
        return sourceFile;
    }
}

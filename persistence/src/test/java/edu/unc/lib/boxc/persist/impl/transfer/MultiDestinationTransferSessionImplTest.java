package edu.unc.lib.boxc.persist.impl.transfer;

import static edu.unc.lib.boxc.persist.api.storage.StorageType.FILESYSTEM;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.persist.api.sources.IngestSource;
import edu.unc.lib.boxc.persist.api.sources.IngestSourceManager;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;

/**
 * @author bbpennel
 *
 */
public class MultiDestinationTransferSessionImplTest extends AbstractBinaryTransferTest {

    private static final String FILE_CONTENT = "File content";

    private Path storagePath2;
    private AutoCloseable closeable;
    @Mock
    private IngestSourceManager sourceManager;
    @Mock
    private IngestSource ingestSource;
    @Mock
    private StorageLocation storageLoc;
    @Mock
    private StorageLocation storageLoc2;
    @Mock
    private StorageLocationManager storageLocationManager;
    private BinaryTransferService bts;

    private PID binPid;
    private Path binDestPath;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);
        createPaths();

        binPid = makeBinPid();
        binDestPath = storagePath.resolve(binPid.getComponentId());

        when(sourceManager.getIngestSourceForUri(any(URI.class))).thenReturn(ingestSource);
        when(ingestSource.getId()).thenReturn("source1");

        when(ingestSource.getStorageType()).thenReturn(FILESYSTEM);
        when(storageLoc.getStorageType()).thenReturn(FILESYSTEM);
        when(storageLoc.getNewStorageUri(binPid)).thenReturn(binDestPath.toUri());
        when(storageLoc.getId()).thenReturn("loc1");

        bts = new BinaryTransferServiceImpl();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void noDestination() throws Exception {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            try (MultiDestinationTransferSessionImpl session = new MultiDestinationTransferSessionImpl(
                    sourceManager, storageLocationManager, bts)) {
                session.forDestination(null);
            }
        });
    }

    @Test
    public void transfer_FSToFS_MultipleFiles_SameDestination() throws Exception {
        PID binPid2 = makeBinPid();
        Path binDestPath2 = storagePath.resolve(binPid2.getComponentId());
        // Establish destination path for second binary in second location
        when(storageLoc.getNewStorageUri(binPid2)).thenReturn(binDestPath2.toUri());

        Path sourceFile = createSourceFile();
        Path sourceFile2 = createSourceFile("another.txt", "stuff");

        try (MultiDestinationTransferSessionImpl session = new MultiDestinationTransferSessionImpl(
                sourceManager, storageLocationManager, bts)) {
            BinaryTransferSession destSession = session.forDestination(storageLoc);
            BinaryTransferOutcome result1 = destSession.transfer(binPid, sourceFile.toUri());
            BinaryTransferOutcome result2 = destSession.transfer(binPid2, sourceFile2.toUri());

            // Verify that results ended up in the right storage locations
            assertTrue(result1.getDestinationUri().toString().contains("storage/"));
            assertTrue(result2.getDestinationUri().toString().contains("storage/"));
            assertFileContent(Paths.get(result1.getDestinationUri()), FILE_CONTENT);
            assertFileContent(Paths.get(result2.getDestinationUri()), "stuff");
        }
    }

    @Test
    public void transfer_FSToFS_MultipleFiles_DifferentDestination() throws Exception {
        when(storageLoc2.getId()).thenReturn("loc2");
        when(storageLoc2.getStorageType()).thenReturn(FILESYSTEM);
        storagePath2 = tmpFolder.resolve("storage2");
        Files.createDirectory(storagePath2);

        PID binPid2 = makeBinPid();
        Path binDestPath2 = storagePath2.resolve(binPid2.getComponentId());
        // Establish destination path for second binary in second location
        when(storageLoc2.getNewStorageUri(binPid2)).thenReturn(binDestPath2.toUri());

        Path sourceFile = createSourceFile();
        Path sourceFile2 = createSourceFile("another.txt", "stuff");

        try (MultiDestinationTransferSessionImpl session = new MultiDestinationTransferSessionImpl(
                sourceManager, storageLocationManager, bts)) {
            BinaryTransferOutcome result1 = session
                    .forDestination(storageLoc).transfer(binPid, sourceFile.toUri());
            BinaryTransferOutcome result2 = session
                    .forDestination(storageLoc2).transfer(binPid2, sourceFile2.toUri());

            // Verify that results ended up in the right storage locations
            assertTrue(result1.getDestinationUri().toString().contains("storage/"));
            assertTrue(result2.getDestinationUri().toString().contains("storage2/"));
            assertFileContent(Paths.get(result1.getDestinationUri()), FILE_CONTENT);
            assertFileContent(Paths.get(result2.getDestinationUri()), "stuff");
        }
    }
}

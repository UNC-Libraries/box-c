package edu.unc.lib.boxc.persist.impl.transfer;

import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getOriginalFilePid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.MockitoAnnotations.openMocks;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.boxc.persist.impl.storage.HashedPosixStorageLocation;

/**
 * @author bbpennel
 */
public class FSToPosixTransferClientTest extends FSToFSTransferClientTest {

    private FSToPosixTransferClient posixClient;
    private AutoCloseable closeable;

    @Override
    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);
        sourcePath = tmpFolder.resolve("source");
        Files.createDirectory(sourcePath);
        storagePath = tmpFolder.resolve("storage");
        Files.createDirectory(storagePath);

        HashedPosixStorageLocation hashedLoc = new HashedPosixStorageLocation();
        hashedLoc.setBase(storagePath.toString());
        hashedLoc.setId("loc1");
        storageLoc = hashedLoc;

        this.posixClient = new FSToPosixTransferClient(ingestSource, storageLoc);
        this.client = posixClient;

        binPid = getOriginalFilePid(PIDs.get(TEST_UUID));
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void transfer_NewFile_WithPermissions() throws Exception {
        ((HashedPosixStorageLocation) storageLoc).setPermissions("0600");

        Path sourceFile = createSourceFile();

        BinaryTransferOutcome outcome = client.transfer(binPid, sourceFile.toUri());
        URI binUri = outcome.getDestinationUri();
        Path binPath = Paths.get(binUri);

        assertIsSourceFile(binPath);

        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(binPath);
        assertEquals(2, perms.size());
        assertTrue(perms.contains(PosixFilePermission.OWNER_READ));
        assertTrue(perms.contains(PosixFilePermission.OWNER_WRITE));
    }
}

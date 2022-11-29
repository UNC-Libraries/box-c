package edu.unc.lib.boxc.persist.impl.transfer;

import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getOriginalFilePid;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.boxc.persist.impl.storage.HashedPosixStorageLocation;
import edu.unc.lib.boxc.persist.impl.transfer.FSToPosixTransferClient;

/**
 * @author bbpennel
 */
public class FSToPosixTransferClientTest extends FSToFSTransferClientTest {

    private FSToPosixTransferClient posixClient;

    @Override
    @Before
    public void setup() throws Exception {
        initMocks(this);
        tmpFolder.create();
        sourcePath = tmpFolder.newFolder("source").toPath();
        storagePath = tmpFolder.newFolder("storage").toPath();

        HashedPosixStorageLocation hashedLoc = new HashedPosixStorageLocation();
        hashedLoc.setBase(storagePath.toString());
        hashedLoc.setId("loc1");
        storageLoc = hashedLoc;

        this.posixClient = new FSToPosixTransferClient(ingestSource, storageLoc);
        this.client = posixClient;

        binPid = getOriginalFilePid(PIDs.get(TEST_UUID));
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

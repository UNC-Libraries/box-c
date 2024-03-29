package edu.unc.lib.boxc.persist.impl.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.unc.lib.boxc.persist.api.storage.StorageType;
import edu.unc.lib.boxc.persist.impl.storage.HashedPosixStorageLocation;

/**
 * @author bbpennel
 */
public class HashedPosixStorageLocationTest extends HashedFilesystemStorageLocationTest {

    private HashedPosixStorageLocation posixLoc;

    @Override
    @BeforeEach
    public void setup() throws Exception {
        storagePath = tmpFolder.resolve("storage");
        Files.createDirectory(storagePath);

        posixLoc = new HashedPosixStorageLocation();
        posixLoc.setBase(storagePath.toString());
        loc = posixLoc;
    }

    @Override
    @Test
    public void getStorageType() {
        assertEquals(StorageType.POSIX_FS, loc.getStorageType());
    }

    @Test
    public void setPermissionsInvalidFormat() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            posixLoc.setPermissions("what");
        });
    }

    @Test
    public void getPermissionsNoPermissions() {
        assertNull(posixLoc.getPermissions());
    }

    @Test
    public void getPermissionsWithOctal() {
        posixLoc.setPermissions("0644");
        Set<PosixFilePermission> perms = posixLoc.getPermissions();
        assertEquals(4, perms.size());
        assertTrue(perms.contains(PosixFilePermission.OWNER_WRITE));
        assertTrue(perms.contains(PosixFilePermission.OWNER_READ));
        assertTrue(perms.contains(PosixFilePermission.GROUP_READ));
        assertTrue(perms.contains(PosixFilePermission.OTHERS_READ));
    }
}

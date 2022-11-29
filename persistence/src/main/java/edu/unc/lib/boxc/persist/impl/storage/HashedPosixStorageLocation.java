package edu.unc.lib.boxc.persist.impl.storage;

import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import edu.unc.lib.boxc.persist.api.storage.StorageType;

/**
 * A hashed posix filesystem based storage location, which allows setting of file permissions.
 *
 * @author bbpennel
 */
public class HashedPosixStorageLocation extends HashedFilesystemStorageLocation {
    public static final String TYPE_NAME = "hashed_posix";

    private final static Pattern PERMS_PATTERN = Pattern.compile("[0-2][0-7][0-7][0-7]");
    private static final Map<Integer, PosixFilePermission> PERM_MAPPING = new HashMap<>();
    static {
        PERM_MAPPING.put(0001, PosixFilePermission.OTHERS_EXECUTE);
        PERM_MAPPING.put(0002, PosixFilePermission.OTHERS_WRITE);
        PERM_MAPPING.put(0004, PosixFilePermission.OTHERS_READ);
        PERM_MAPPING.put(0010, PosixFilePermission.GROUP_EXECUTE);
        PERM_MAPPING.put(0020, PosixFilePermission.GROUP_WRITE);
        PERM_MAPPING.put(0040, PosixFilePermission.GROUP_READ);
        PERM_MAPPING.put(0100, PosixFilePermission.OWNER_EXECUTE);
        PERM_MAPPING.put(0200, PosixFilePermission.OWNER_WRITE);
        PERM_MAPPING.put(0400, PosixFilePermission.OWNER_READ);
    }

    private Set<PosixFilePermission> permissions;

    public void setPermissions(String perms) {
        if (!PERMS_PATTERN.matcher(perms).matches()) {
            throw new IllegalArgumentException("Invalid permissions value " + perms);
        }
        // Build list of individual permissions by masking the input octal mode
        int mode = Integer.parseInt(perms, 8);
        permissions = new HashSet<>();
        for (int mask: PERM_MAPPING.keySet()) {
            if (mask == (mode & mask)) {
                permissions.add(PERM_MAPPING.get(mask));
            }
        }
    }

    /**
     * Get the set of posix file permissions to use for files created in this location
     *
     * @return set of permissions, or null if no permissions were defined
     */
    public Set<PosixFilePermission> getPermissions() {
        return permissions;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.POSIX_FS;
    }
}

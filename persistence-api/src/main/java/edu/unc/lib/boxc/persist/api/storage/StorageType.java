package edu.unc.lib.boxc.persist.api.storage;

/**
 * @author bbpennel
 *
 */
public enum StorageType {
    FILESYSTEM("filesystem"),
    POSIX_FS("posix");

    private final String id;

    private StorageType(String id) {
        this.id = id;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }
}

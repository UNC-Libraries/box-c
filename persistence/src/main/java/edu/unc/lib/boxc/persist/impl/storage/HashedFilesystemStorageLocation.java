package edu.unc.lib.boxc.persist.impl.storage;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.storage.StorageType;
import edu.unc.lib.boxc.persist.impl.transfer.FileSystemTransferHelpers;

/**
 * A filesystem based storage location which locates files in a hashed structure based on
 * the PID of the object.
 *
 * @author bbpennel
 *
 */
public class HashedFilesystemStorageLocation implements StorageLocation {
    public static final String TYPE_NAME = "hashed_fs";

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneId.from(ZoneOffset.UTC));

    private String id;
    private String name;
    // The base path for files in this storage location
    private String base;
    private URI baseUri;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.FILESYSTEM;
    }

    public String getBase() {
        return base;
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public void setBase(String base) {
        this.base = base.replaceFirst(":///", ":/");
        if (!this.base.endsWith("/")) {
            this.base += "/";
        }
        baseUri = URI.create(this.base).normalize();
        if (baseUri.getScheme() == null) {
            this.base = "file:" + base;
            baseUri = URI.create(this.base).normalize();
        } else if (!"file".equals(baseUri.getScheme())) {
            throw new IllegalArgumentException("Only file URIs are acceptable in locations of type "
                    + getClass().getName());
        } else {
            // Ensure base string representation is normalized to match the uri representation
            this.base = baseUri.toString();
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String getBaseStoragePath(PID pid) {
        String objId = pid.getId();
        String derivativePath = RepositoryPaths
                .idToPath(objId, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);

        if (pid.getComponentPath() != null) {
            return URIUtil.join(baseUri, derivativePath, objId, pid.getComponentPath());
        } else {
            return URIUtil.join(baseUri, derivativePath, objId);
        }
    }

    @Override
    public URI getNewStorageUri(PID pid) {
        String base = getBaseStoragePath(pid);
        // Add timestamp to base path, combining wall time millisecond with relative nanotime

        String timestamp = TIME_FORMATTER.format(Instant.now());
        String path = base + "." + timestamp + System.nanoTime();
        return URI.create(path).normalize();
    }

    @Override
    public URI getCurrentStorageUri(PID pid) {
        String path = getBaseStoragePath(pid);
        return FileSystemTransferHelpers.getMostRecentStorageUri(URI.create(path));
    }

    @Override
    public List<URI> getAllStorageUris(PID pid) {
        String path = getBaseStoragePath(pid);
        return FileSystemTransferHelpers.getAllStorageUris(URI.create(path));
    }

    @Override
    public boolean isValidUri(URI uri) {
        return uri.toString().startsWith(baseUri.toString());
    }
}

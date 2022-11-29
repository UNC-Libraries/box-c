package edu.unc.lib.boxc.persist.api.storage;

import java.net.URI;
import java.util.List;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;

/**
 * Manager for preservation storage locations
 *
 * @author bbpennel
 *
 */
public interface StorageLocationManager {

    /**
     * Get the default storage location for the given PID
     *
     * @param pid
     * @return default storage location
     */
    StorageLocation getDefaultStorageLocation(PID pid);

    /**
     * List all of the storage locations which are available for the given PID
     *
     * @param pid
     * @return a list of storage locations available to the specified object
     */
    List<StorageLocation> listAvailableStorageLocations(PID pid);

    /**
     * Get the storage location assigned to the given PID
     *
     * @param pid
     * @return storage location assigned to the given pid
     */
    StorageLocation getStorageLocation(PID pid);

    /**
     * Get the storage location assigned to the given object
     *
     * @param repoObj
     * @return storage location assigned to the given object
     */
    StorageLocation getStorageLocation(RepositoryObject repoObj);

    /**
     * Get storage location by id
     *
     * @param id
     * @return the storage location with matching id, or null
     */
    StorageLocation getStorageLocationById(String id);

    /**
     * Return the storage location that would contain the provided URI
     *
     * @param uri
     * @return storage location containing the provided URI, or null if no locations match
     */
    StorageLocation getStorageLocationForUri(URI uri);
}

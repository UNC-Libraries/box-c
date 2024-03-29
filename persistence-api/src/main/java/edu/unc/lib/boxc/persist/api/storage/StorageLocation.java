package edu.unc.lib.boxc.persist.api.storage;

import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * A location where preserved content within the repository may be stored.
 *
 * @author bbpennel
 *
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
public interface StorageLocation {

    /**
     * Get the identifier for this storage location
     *
     * @return
     */
    String getId();

    /**
     * Get the display name for this storage location
     *
     * @return
     */
    String getName();

    /**
     * Get the type of storage represented by this location
     *
     * @return
     */
    StorageType getStorageType();

    /**
     * Get a new URI where a resource with the given PID should be stored.
     *
     * @param pid
     * @return new storage URI
     */
    URI getNewStorageUri(PID pid);

    /**
     * Return the existing current URI where the resource with the given PID is stored
     *
     * @param pid
     * @return existing current storage URI for the given PID, or null it it does not exist
     */
    URI getCurrentStorageUri(PID pid);

    /**
     * Get a list containing all binary storage URIs for the resource with the given PID
     * @param pid
     * @return List of binary URIs
     */
    List<URI> getAllStorageUris(PID pid);

    /**
     * Returns true if the provided URI is a valid within this storage location.
     *
     * @param uri
     * @return
     */
    boolean isValidUri(URI uri);
}

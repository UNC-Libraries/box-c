package edu.unc.lib.boxc.persist.api.sources;

import java.net.URI;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import edu.unc.lib.boxc.persist.api.storage.StorageType;

/**
 * A staging location from which objects may be ingested into the repository.
 *
 * @author bbpennel
 *
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
public interface IngestSource {

    /**
     * Get the identifier for this ingest source
     *
     * @return
     */
    String getId();

    /**
     * Get the descriptive name for this source
     *
     * @return
     */
    String getName();

    /**
     * Returns true if this source is read only
     *
     * @return
     */
    boolean isReadOnly();

    /**
     * Returns true if this source is only intended for internal usage
     *
     * @return
     */
    boolean isInternal();

    /**
     * Get the type of storage represented by this source
     *
     * @return
     */
    @JsonIgnore
    StorageType getStorageType();

    /**
     * Get the type identifier for the storage represented by this source
     *
     * @return
     */
    String getType();

    /**
     * Returns true if the provided URI is a valid within this ingest source.
     *
     * @param uri
     * @return
     */
    boolean isValidUri(URI uri);

    /**
     * Returns true if the provided URI exists within this ingest source
     *
     * @param uri
     * @return
     */
    boolean exists(URI uri);

    /**
     * List the potential candidates for ingest from this source location.
     *
     * @return
     */
    List<IngestSourceCandidate> listCandidates();

    /**
     * Returns a URI representing the given path resolved against this ingest source
     *
     * @param uri
     * @return
     */
    URI resolveRelativePath(String relative);
}

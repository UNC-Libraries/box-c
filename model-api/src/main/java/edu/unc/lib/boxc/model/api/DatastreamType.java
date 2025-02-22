package edu.unc.lib.boxc.model.api;

import static edu.unc.lib.boxc.model.api.StoragePolicy.EXTERNAL;
import static edu.unc.lib.boxc.model.api.StoragePolicy.INTERNAL;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.DATA_FILE_FILESET;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.METADATA_CONTAINER;

/**
 * Predefined binary datastream types which may be associated with repository objects.
 *
 * @author bbpennel
 *
 */
public enum DatastreamType {
    ACCESS_SURROGATE("access_surrogate", "application/octet-stream", null, null, EXTERNAL),
    ALT_TEXT("alt_text", "text/plain", "txt", METADATA_CONTAINER, INTERNAL),
    ALT_TEXT_HISTORY("alt_text_history", Constants.TEXT_XML, "xml", METADATA_CONTAINER, INTERNAL),
    AUDIO_ACCESS_COPY("audio", "audio/aac", "m4a", null, EXTERNAL),
    FULLTEXT_EXTRACTION("fulltext", "text/plain", "txt", null, EXTERNAL),
    JP2_ACCESS_COPY("jp2", "image/jp2", "jp2", null, EXTERNAL),
    MD_DESCRIPTIVE("md_descriptive", Constants.TEXT_XML, "xml", METADATA_CONTAINER, INTERNAL),
    MD_DESCRIPTIVE_HISTORY("md_descriptive_history", Constants.TEXT_XML, "xml", METADATA_CONTAINER, INTERNAL),
    MD_EVENTS("event_log", "application/n-triples", "nt", METADATA_CONTAINER, INTERNAL),
    ORIGINAL_FILE("original_file", null, null, DATA_FILE_FILESET, INTERNAL),
    TECHNICAL_METADATA("techmd_fits", Constants.TEXT_XML, "xml", DATA_FILE_FILESET, INTERNAL),
    TECHNICAL_METADATA_HISTORY("techmd_fits_history", Constants.TEXT_XML, "xml", DATA_FILE_FILESET, INTERNAL);

    private final String id;
    private final String mimetype;
    private final String extension;
    private final String container;
    private final StoragePolicy storagePolicy;

    DatastreamType(String identifier, String mimetype, String extension, String container,
            StoragePolicy storagePolicy) {
        this.id = identifier;
        this.mimetype = mimetype;
        this.extension = extension;
        this.container = container;
        this.storagePolicy = storagePolicy;
    }

    /**
     * @return name identifier for the datastream.
     */
    public String getId() {
        return id;
    }

    /**
     * @return Mimetype for datastreams of this type
     */
    public String getMimetype() {
        return mimetype;
    }

    /**
     * @return File extension used with this datastream
     */
    public String getExtension() {
        return extension;
    }

    /**
     * @return The name of the fedora container where this datastream is stored
     */
    public String getContainer() {
        return container;
    }

    /**
     * @return the default filename for datastreams of this type
     */
    public String getDefaultFilename() {
        return id + "." + extension;
    }

    /**
     * @return Policy indicating how this datastream should be stored and accessed.
     */
    public StoragePolicy getStoragePolicy() {
        return storagePolicy;
    }

    /**
     * Get the DatastreamType with the given identifier.
     *
     * @param id identifier of the datastream.
     * @return datastream type for identifier, or null if there is no matching type.
     */
    public static DatastreamType getByIdentifier(String id) {
        for (DatastreamType type : values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }

    private static class Constants {
        public static final String TEXT_XML = "text/xml";
    }
}

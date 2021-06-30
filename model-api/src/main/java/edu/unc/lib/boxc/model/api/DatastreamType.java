/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.boxc.model.api;

import static edu.unc.lib.boxc.model.api.StoragePolicy.EXTERNAL;
import static edu.unc.lib.boxc.model.api.StoragePolicy.INTERNAL;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.DATA_FILE_FILESET;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.METADATA_CONTAINER;
import static edu.unc.lib.dl.acl.util.Permission.viewAccessCopies;
import static edu.unc.lib.dl.acl.util.Permission.viewHidden;
import static edu.unc.lib.dl.acl.util.Permission.viewMetadata;
import static edu.unc.lib.dl.acl.util.Permission.viewOriginal;

/**
 * Predefined binary datastream types which may be associated with repository objects.
 *
 * @author bbpennel
 *
 */
public enum DatastreamType {
    FULLTEXT_EXTRACTION("fulltext", "text/plain", "txt", null, EXTERNAL, viewHidden),
    JP2_ACCESS_COPY("jp2", "image/jp2", "jp2", null, EXTERNAL, viewAccessCopies),
    MD_DESCRIPTIVE("md_descriptive", "text/xml", "xml", METADATA_CONTAINER, INTERNAL, viewMetadata),
    MD_DESCRIPTIVE_HISTORY("md_descriptive_history", "text/xml", "xml", METADATA_CONTAINER, INTERNAL, viewHidden),
    MD_EVENTS("event_log", "application/n-triples", "nt", METADATA_CONTAINER, INTERNAL, viewHidden),
    ORIGINAL_FILE("original_file", null, null, DATA_FILE_FILESET, INTERNAL, viewOriginal),
    TECHNICAL_METADATA("techmd_fits", "text/xml", "xml", DATA_FILE_FILESET, INTERNAL, viewHidden),
    TECHNICAL_METADATA_HISTORY("techmd_fits_history", "text/xml", "xml", DATA_FILE_FILESET, INTERNAL, viewHidden),
    THUMBNAIL_SMALL("thumbnail_small", "image/png", "png", null, EXTERNAL, viewMetadata),
    THUMBNAIL_LARGE("thumbnail_large", "image/png", "png", null, EXTERNAL, viewMetadata);

    private final String id;
    private final String mimetype;
    private final String extension;
    private final String container;
    private final StoragePolicy storagePolicy;
    private final Permission accessPermission;

    private DatastreamType(String identifier, String mimetype, String extension, String container,
            StoragePolicy storagePolicy, Permission accessPermission) {
        this.id = identifier;
        this.mimetype = mimetype;
        this.extension = extension;
        this.container = container;
        this.storagePolicy = storagePolicy;
        this.accessPermission = accessPermission;
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
     * @return Permission required in order to access this datastream
     */
    public Permission getAccessPermission() {
        return accessPermission;
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
}

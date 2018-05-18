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
package edu.unc.lib.dl.model;

import static edu.unc.lib.dl.acl.util.Permission.viewAccessCopies;
import static edu.unc.lib.dl.acl.util.Permission.viewHidden;
import static edu.unc.lib.dl.acl.util.Permission.viewMetadata;
import static edu.unc.lib.dl.acl.util.Permission.viewOriginal;
import static edu.unc.lib.dl.model.StoragePolicy.EXTERNAL;
import static edu.unc.lib.dl.model.StoragePolicy.INTERNAL;

import edu.unc.lib.dl.acl.util.Permission;

/**
 * Predefined binary datastream types which may be associated with repository objects.
 *
 * @author bbpennel
 *
 */
public enum DatastreamType {
    FULLTEXT_EXTRACTION("fulltext", "text/plain", "txt", EXTERNAL, viewHidden),
    JP2_ACCESS_COPY("jp2", "image/jp2", "jp2", EXTERNAL, viewAccessCopies),
    ORIGINAL_FILE("original_file", null, null, INTERNAL, viewOriginal),
    TECHNICAL_METADATA("techmd_fits", "text/xml", "xml", INTERNAL, viewHidden),
    THUMBNAIL_SMALL("thumbnail_small", "image/png", "png", EXTERNAL, viewMetadata),
    THUMBNAIL_LARGE("thumbnail_large", "image/png", "png", EXTERNAL, viewMetadata);

    private final String id;
    private final String mimetype;
    private final String extension;
    private final StoragePolicy storagePolicy;
    private final Permission accessPermission;

    private DatastreamType(String identifier, String mimetype, String extension, StoragePolicy storagePolicy,
            Permission accessPermission) {
        this.id = identifier;
        this.mimetype = mimetype;
        this.extension = extension;
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

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
package edu.unc.lib.dl.persist.services.storage;

import java.net.URI;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import edu.unc.lib.dl.fedora.PID;

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
@JsonSubTypes({
        @Type(value = HashedFilesystemStorageLocation.class, name = HashedFilesystemStorageLocation.TYPE_NAME) })
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
     * Return the URI where a resource with the given PID should be stored.
     *
     * @param pid
     * @return
     */
    URI getStorageUri(PID pid);

    /**
     * Returns true if the provided URI is a valid within this storage location.
     *
     * @param uri
     * @return
     */
    boolean isValidUri(URI uri);
}

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

import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fedora.PID;

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

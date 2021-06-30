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
package edu.unc.lib.dl.persist.api.transfer;

import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;

/**
 * A session for transferring one or more binaries to preservation storage locations
 *
 * @author bbpennel
 *
 */
public interface MultiDestinationTransferSession extends AutoCloseable {

    /**
     * Get a transfer session to a single destination within this multi-destination session.
     *
     * @param dest storage location to transfer to.
     * @return Transfer session to the provided destination
     */
    BinaryTransferSession forDestination(StorageLocation dest);

    /**
     * Get a transfer session for a specific object within this multi-destination session.
     *
     * @param repoObj object for the session
     * @return Transfer session for the object
     */
    BinaryTransferSession forObject(RepositoryObject repoObj);

    /**
     * Closes the transfer session. If there are any failures, they will be RuntimeExceptions.
     */
    @Override
    void close();
}

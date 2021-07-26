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
package edu.unc.lib.boxc.persist.api.transfer;

import java.net.URI;

import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;

/**
 * A service for transferring binaries to preservation storage locations.
 *
 * @author bbpennel
 *
 */
public interface BinaryTransferService {

    /**
     * Get a new binary transfer session.
     *
     * @return new binary transfer session
     */
    MultiDestinationTransferSession getSession();

    /**
     * Get a new session for transferring binaries to the specified storage location
     *
     * @param destination the storage location to transfer to
     * @return new binary transfer session
     */
    BinaryTransferSession getSession(StorageLocation destination);

    /**
     * Get a new session for transferring binaries to the specified repository object
     *
     * @param repoObj object to open the session for
     * @return new binary transfer session
     */
    BinaryTransferSession getSession(RepositoryObject repoObj);

    /**
     * Rolls back the binary transfers associated with the provided repository transaction
     * @param txUri URI of the repository transaction to roll back
     */
    void rollbackTransaction(URI txUri);

    /**
     * Commits the binary transfers associated with the provided transaction
     * @param txUri URI of the repository transaction committed
     */
    void commitTransaction(URI txUri);

    /**
     * Register the outcome of a transfer
     * @param outcome results of the transfer
     */
    BinaryTransferOutcome registerOutcome(BinaryTransferOutcome outcome);
}

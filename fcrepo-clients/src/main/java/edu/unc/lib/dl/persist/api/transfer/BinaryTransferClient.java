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

import java.net.URI;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.dl.persist.api.storage.BinaryDetails;

/**
 * Client for transferring binaries between a category of ingest sources and a
 * preservation storage location.
 *
 * @author bbpennel
 *
 */
public interface BinaryTransferClient {

    /**
     * Transfer the specified binary to the preservation storage location.
     *
     * @param binPid PID of the binary object the binary is associated with
     * @param sourceFileUri URI of the binary located in an Ingest Source.
     * @return information describing the outcome of the transfer
     * @throws BinaryAlreadyExistsException thrown if the binary already exists
     */
    BinaryTransferOutcome transfer(PID binPid, URI sourceFileUri);

    /**
     * Transfer a binary to the preservation storage location. If a binary already
     * exists at the expected destination, it will be overwritten.
     *
     * @param binPid PID of the binary object the binary is associated with
     * @param sourceFileUri URI of the binary located in an IngestSource.
     * @return information describing the outcome of the transfer
     */
    BinaryTransferOutcome transferReplaceExisting(PID binPid, URI sourceFileUri);

    /**
     * Transfer a new version of binary to the preservation storage location. Previous
     * versions will not be overwritten.
     *
     * @param binPid PID of the binary object the binary is associated with
     * @param sourceFileUri URI of the binary located in an IngestSource.
     * @return information describing the outcome of the transfer
     */
    BinaryTransferOutcome transferVersion(PID binPid, URI sourceFileUri);

    /**
     * Checks if a storage location already contains the source binary in its current state.
     *
     * @param binPid PID of the binary object the binary is associated with
     * @param sourceUri URI of the binary located in an IngestSource
     * @return true if the storage location contains the source file
     */
    boolean isTransferred(PID binPid, URI sourceUri);

    /**
     * Get details of a binary located in a storage location
     *
     * @param binPid PID of the binary object the binary is associated with
     * @return object containing binary details
     */
    BinaryDetails getStoredBinaryDetails(PID binPid);

    /**
     * Shut down this transfer client.
     */
    void shutdown();
}

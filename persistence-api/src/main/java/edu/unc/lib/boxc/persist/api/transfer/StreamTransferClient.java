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

import java.io.InputStream;
import java.net.URI;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.persist.api.storage.BinaryDetails;

/**
 * Client for streaming content to a preservation storage location
 *
 * @author bbpennel
 *
 */
public interface StreamTransferClient {

    /**
     * Write the provided stream to a preservation storage location.
     *
     * @param binPid PID of the binary object the binary is associated with
     * @param sourceStream InputStream of the content to transfer.
     * @return information describing the outcome of the transfer
     * @throws BinaryAlreadyExistsException thrown if the binary already exists
     */
    BinaryTransferOutcome transfer(PID binPid, InputStream sourceStream);

    /**
     * Write the provided stream to a preservation storage location. If a binary already
     * exists at the expected destination, it will be overwritten.
     *
     * @param binPid PID of the binary object the binary is associated with
     * @param sourceStream InputStream of the content to transfer.
     * @return information describing the outcome of the transfer
     */
    BinaryTransferOutcome transferReplaceExisting(PID binPid, InputStream sourceStream);

    /**
     * Write the provided stream as a new version of binary in a preservation storage location. Previous
     * versions will not be overwritten.
     *
     * @param binPid PID of the binary object the binary is associated with
     * @param sourceStream InputStream of the content to transfer
     * @return information describing the outcome of the transfer
     */
    BinaryTransferOutcome transferVersion(PID binPid, InputStream sourceStream);

    /**
     * Delete a binary in a preservation location.
     *
     * @param fileUri uri of the file to delete
     */
    void delete(URI fileUri);

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

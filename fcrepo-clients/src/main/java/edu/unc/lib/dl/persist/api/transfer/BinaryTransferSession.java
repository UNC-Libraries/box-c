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

import java.io.InputStream;
import java.net.URI;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.storage.BinaryDetails;

/**
 * A session for transferring one or more binaries to a preservation storage location
 *
 * @author bbpennel
 *
 */
public interface BinaryTransferSession extends AutoCloseable {

    /**
     * Transfer a binary to the preservation storage location for this session. If the
     * binary already exists in the destination, an exception will be thrown.
     *
     * @param binPid PID of the binary object the binary is associated with
     * @param sourceFileUri URI of the binary located in an IngestSource.
     * @return information describing the outcome of the transfer
     * @throws BinaryAlreadyExistsException thrown if the binary already exists
     */
    BinaryTransferOutcome transfer(PID binPid, URI sourceFileUri);

    /**
     * Write the provided stream to the preservation storage location.
     *
     * @param binPid PID of the binary object the binary is associated with
     * @param sourceStream InputStream of the content to transfer.
     * @return information describing the outcome of the transfer
     * @throws BinaryAlreadyExistsException thrown if the binary already exists
     */
    BinaryTransferOutcome transfer(PID binPid, InputStream sourceStream);

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
     * Write the provided stream to the preservation storage location. If a binary already
     * exists at the expected destination, it will be overwritten.
     *
     * @param binPid PID of the binary object the binary is associated with
     * @param sourceStream InputStream of the content to transfer.
     * @return information describing the outcome of the transfer
     */
    BinaryTransferOutcome transferReplaceExisting(PID binPid, InputStream sourceStream);

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
     * Write the provided stream as a new version of binary in the preservation storage location. Previous
     * versions will not be overwritten.
     *
     * @param binPid PID of the binary object the binary is associated with
     * @param sourceStream InputStream of the content to transfer
     * @return information describing the outcome of the transfer
     */
    BinaryTransferOutcome transferVersion(PID binPid, InputStream sourceStream);

    /**
     * Delete a binary from a preservation storage location. If the binary cannot
     * be deleted, then a BinaryTransferException will be thrown.
     *
     * @param fileUri uri of the binary to delete.
     */
    void delete(URI fileUri);

    /**
     * Get details of a binary located in a storage location
     *
     * @param binPid PID of the binary object the binary is associated with
     * @return object containing binary details, or null if not found
     */
    BinaryDetails getStoredBinaryDetails(PID binPid);

    /**
     * Checks if a storage location already contains the source binary in its current state.
     *
     * @param binPid PID of the binary object the binary is associated with
     * @param sourceUri URI of the binary located in an IngestSource
     * @return true if the storage location contains the source file
     */
    boolean isTransferred(PID binPid, URI sourceUri);

    /**
     * Closes the binary session. If there are any failures, they will be RuntimeExceptions.
     */
    @Override
    void close();
}

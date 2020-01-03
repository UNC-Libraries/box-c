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
package edu.unc.lib.dl.persist.services.transfer;

import java.io.InputStream;
import java.net.URI;

import edu.unc.lib.dl.fedora.PID;

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
     * @return URI of the file in its destination
     * @throws BinaryAlreadyExistsException thrown if the binary already exists
     */
    URI transfer(PID binPid, InputStream sourceStream);

    /**
     * Write the provided stream to a preservation storage location. If a binary already
     * exists at the expected destination, it will be overwritten.
     *
     * @param binPid PID of the binary object the binary is associated with
     * @param sourceStream InputStream of the content to transfer.
     * @return the URI of the binary in its destination.
     */
    URI transferReplaceExisting(PID binPid, InputStream sourceStream);

    /**
     * Write the provided stream as a new version of binary in a preservation storage location. Previous
     * versions will not be overwritten.
     *
     * @param binPid PID of the binary object the binary is associated with
     * @param sourceStream InputStream of the content to transfer
     * @return the URI of the binary in its destination.
     */
    URI transferVersion(PID binPid, InputStream sourceStream);

    /**
     * Shut down this transfer client.
     */
    void shutdown();
}

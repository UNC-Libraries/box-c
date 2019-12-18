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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.services.storage.StorageLocation;

/**
 * Client for transferring content to a filesystem storage location from input streams
 *
 * @author bbpennel
 *
 */
public class StreamToFSTransferClient implements StreamTransferClient {

    private StorageLocation destination;

    /**
     * @param destination destination storage location
     */
    public StreamToFSTransferClient(StorageLocation destination) {
        this.destination = destination;
    }

    @Override
    public URI transfer(PID binPid, InputStream sourceStream) {
        return writeStream(binPid, sourceStream, false);
    }

    @Override
    public URI transferReplaceExisting(PID binPid, InputStream sourceStream) {
        return writeStream(binPid, sourceStream, true);
    }

    private URI writeStream(PID binPid, InputStream sourceStream, boolean allowOverwrite) {
        URI destUri = destination.getStorageUri(binPid);
        Path destPath = Paths.get(destUri);

        if (!allowOverwrite && destPath.toFile().exists()) {
            throw new BinaryAlreadyExistsException("Failed to write stream, a binary already exists in "
                     + destination.getId() + " at path " + destUri);
        }

        try {
            // Fill in parent directories if they are not present
            Path parentPath = Paths.get(destUri).getParent();
            Files.createDirectories(parentPath);

            // Write content to temp file in case of interruption
            Path tmpPath = Files.createTempFile(parentPath, null, ".new");
            FileUtils.copyInputStreamToFile(sourceStream, tmpPath.toFile());

            // Move temp file into final location
            Files.move(tmpPath, destPath, REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BinaryTransferException("Failed to write stream to destination "
                    + destination.getId(), e);
        }

        return destUri;
    }

    @Override
    public URI transferVersion(PID binPid, InputStream sourceStream) {
        throw new NotImplementedException("Versioning not yet implemented");
    }

    @Override
    public void shutdown() {
        // No finalization needed at this time
    }
}

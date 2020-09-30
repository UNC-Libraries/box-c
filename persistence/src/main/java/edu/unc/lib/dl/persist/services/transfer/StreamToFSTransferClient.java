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

import static java.nio.file.Files.createDirectories;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.storage.BinaryDetails;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.transfer.BinaryAlreadyExistsException;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferException;
import edu.unc.lib.dl.persist.api.transfer.StreamTransferClient;
import edu.unc.lib.dl.util.FileTransferHelpers;

/**
 * Client for transferring content to a filesystem storage location from input streams
 *
 * @author bbpennel
 *
 */
public class StreamToFSTransferClient implements StreamTransferClient {

    protected StorageLocation destination;
    private static final Logger log = LoggerFactory.getLogger(StreamToFSTransferClient.class);

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

    protected URI writeStream(PID binPid, InputStream sourceStream, boolean allowOverwrite) {
        URI destUri = destination.getStorageUri(binPid);
        Path destPath = Paths.get(destUri);
        boolean destFileExists = destPath.toFile().exists();

        if (!allowOverwrite && destFileExists) {
            throw new BinaryAlreadyExistsException("Failed to write stream, a binary already exists in "
                     + destination.getId() + " at path " + destUri);
        }

        long currentTime = System.nanoTime();
        Path oldFilePath = FileTransferHelpers.createFilePath(destUri, "old", currentTime);
        Path newFilePath = FileTransferHelpers.createFilePath(destUri, "new", currentTime);

        Thread cleanupThread = null;

        try {
            // Fill in parent directories if they are not present
            Path parentPath = Paths.get(destUri).getParent();
            createDirectories(parentPath);

            cleanupThread = FileTransferHelpers.registerCleanup(oldFilePath, newFilePath, destPath);

            // Write content to temp file in case of interruption
            copyInputStreamToFile(sourceStream, newFilePath.toFile());

            // Rename old file to .old extension
            if (destFileExists) {
                Files.move(destPath, oldFilePath);
            }

            // Move temp file into final location
            Files.copy(newFilePath, destPath, REPLACE_EXISTING);

            // Delete old file.
            try {
                Files.deleteIfExists(oldFilePath);
            } catch (IOException e) {
                // Ignore. New file is already in place
                log.warn("Unable to delete {}. Reason {}", oldFilePath, e.getMessage());
            }

            // Remove temp file after it's moved into its final location
            try {
                Files.deleteIfExists(newFilePath);
            } catch (IOException e) {
                log.warn("Unable to delete source file {}. Reason {}", newFilePath, e.getMessage());
            }
        } catch (IOException e) {
            FileTransferHelpers.rollBackOldFile(oldFilePath, newFilePath, destPath);
            throw new BinaryTransferException("Failed to write stream to destination "
                    + destination.getId(), e);
        } finally {
            FileTransferHelpers.clearCleanupHook(cleanupThread);
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

    @Override
    public void delete(URI fileUri) {
        try {
            Files.delete(Paths.get(fileUri));
        } catch (IOException e) {
            throw new BinaryTransferException("Failed to delete file from destination "
                    + destination.getId(), e);
        }
    }

    @Override
    public BinaryDetails getStoredBinaryDetails(PID binPid) {
        return FileSystemTransferHelpers.getStoredBinaryDetails(destination, binPid);
    }
}

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

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.net.URI;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.ingest.IngestSource;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.transfer.BinaryAlreadyExistsException;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferClient;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferException;

/**
 * Client for transferring files from a filesystem ingest source to a filesystem
 * storage location
 *
 * @author bbpennel
 *
 */
public class FSToFSTransferClient implements BinaryTransferClient {

    private IngestSource source;
    protected StorageLocation destination;

    private static final CopyOption[] COPY_NO_OVERWRITE = { COPY_ATTRIBUTES };
    private static final CopyOption[] COPY_ALLOW_OVERWRITE = { COPY_ATTRIBUTES, REPLACE_EXISTING };

    private static final Logger log = LoggerFactory.getLogger(FSToFSTransferClient.class);

    public FSToFSTransferClient(IngestSource source, StorageLocation destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    public URI transfer(PID binPid, URI sourceFileUri) {
        return transfer(binPid, sourceFileUri, false);
    }

    @Override
    public URI transferReplaceExisting(PID binPid, URI sourceFileUri) {
        return transfer(binPid, sourceFileUri, true);
    }

    public URI transfer(PID binPid, URI sourceFileUri, boolean allowOverwrite) {
        URI destUri = destination.getStorageUri(binPid);
        long currentTime = System.nanoTime();
        Path oldFilePath = createFilePath(destUri, "old", currentTime);
        Path newFilePath = createFilePath(destUri, "new", currentTime);
        Path destinationPath = Paths.get(destUri);

        try {
            // Fill in parent directories if they are not present
            Path parentPath = Paths.get(destUri).getParent();
            Files.createDirectories(parentPath);

            boolean destFileExists = Files.exists(destinationPath);

            if (!allowOverwrite && destFileExists) {
                throw new BinaryAlreadyExistsException("Failed to transfer " + sourceFileUri
                        + ", a binary already exists in " + destination.getId() + " at path " + destUri);
            }

            // Copy/move new file
            if (source.isReadOnly()) {
                Files.copy(Paths.get(sourceFileUri), newFilePath,
                        allowOverwrite ? COPY_ALLOW_OVERWRITE : COPY_NO_OVERWRITE);
            } else {
                Files.move(Paths.get(sourceFileUri), newFilePath);
            }

            // Rename old file to .old extension
            if (destFileExists) {
                Files.move(destinationPath, oldFilePath);
            }
            // Rename new file from .new extension
            Files.move(newFilePath, destinationPath);

            // Delete old file.
            try {
                Files.deleteIfExists(oldFilePath);
            } catch (IOException e) {
                // Ignore. New file is already in place
                log.warn("Unable to delete {}. Reason {}", oldFilePath, e.getMessage());
            }
        } catch (IOException e) {
            rollBackOldFile(oldFilePath, newFilePath, destinationPath);
            throw new BinaryTransferException("Failed to transfer " + sourceFileUri
                    + " to destination " + destination.getId(), e);
        }

        return destUri;
    }

    private Path createFilePath(URI destUri, String type, long currentTime) {
            URI fileUri = URI.create(destUri + "." + type + "-" + currentTime);
            return Paths.get(fileUri);
    }

    /**
     * Roll back the file name if an error is thrown
     * @param oldFilePath
     * @param destPath
     */
    private void rollBackOldFile(Path oldFilePath, Path newFilePath, Path destPath) {
        try {
            if (Files.exists(oldFilePath)) {
                Files.move(oldFilePath, destPath);
            }

            Files.deleteIfExists(newFilePath);
        } catch (IOException e) {
            throw new BinaryTransferException("Failed to roll back " + oldFilePath.toString()
                    + "  in transfer to destination " + destination.getId(), e);
        }
    }

    @Override
    public URI transferVersion(PID binPid, URI sourceFileUri) {
        throw new NotImplementedException("Versioning not yet implemented");
    }

    @Override
    public void shutdown() {
        // No finalization needed for FS to FS transfer
    }
}

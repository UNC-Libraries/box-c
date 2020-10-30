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

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.ingest.IngestSource;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.dl.persist.services.storage.HashedPosixStorageLocation;

/**
 * Client for transferring files from a filesystem ingest source to a
 * posix filesystem storage location.
 *
 * @author bbpennel
 */
public class FSToPosixTransferClient extends FSToFSTransferClient {
    private static final Logger log = getLogger(FSToPosixTransferClient.class);

    /**
     * @param source
     * @param destination
     */
    public FSToPosixTransferClient(IngestSource source, StorageLocation destination) {
        super(source, destination);
    }

    @Override
    public BinaryTransferOutcome transfer(PID binPid, URI sourceFileUri, boolean allowOverwrite) {
        BinaryTransferOutcome outcome = super.transfer(binPid, sourceFileUri, allowOverwrite);
        HashedPosixStorageLocation posixLoc = (HashedPosixStorageLocation) destination;

        if (posixLoc.getPermissions() != null) {
            Path binPath = Paths.get(outcome.getDestinationUri());

            try {
                Files.setPosixFilePermissions(binPath, posixLoc.getPermissions());
            } catch (IOException e) {
                log.debug("Failed to set permissions in destination {}", destination.getId());
            }
        }

        return outcome;
    }
}

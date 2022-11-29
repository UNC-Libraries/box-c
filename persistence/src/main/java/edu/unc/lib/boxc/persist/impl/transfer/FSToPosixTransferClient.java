package edu.unc.lib.boxc.persist.impl.transfer;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.persist.api.sources.IngestSource;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.boxc.persist.impl.storage.HashedPosixStorageLocation;

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

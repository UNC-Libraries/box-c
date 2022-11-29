package edu.unc.lib.boxc.persist.impl.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferException;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferOutcome;
import edu.unc.lib.boxc.persist.impl.storage.HashedPosixStorageLocation;

/**
 * Client for transferring content to a posix filesystem storage location from input streams
 *
 * @author bbpennel
 */
public class StreamToPosixTransferClient extends StreamToFSTransferClient {

    /**
     * @param destination
     */
    public StreamToPosixTransferClient(StorageLocation destination) {
        super(destination);
    }

    @Override
    protected BinaryTransferOutcome writeStream(PID binPid, InputStream sourceStream, boolean allowOverwrite) {
        BinaryTransferOutcome outcome = super.writeStream(binPid, sourceStream, allowOverwrite);
        HashedPosixStorageLocation posixLoc = (HashedPosixStorageLocation) destination;

        if (posixLoc.getPermissions() != null) {
            Path binPath = Paths.get(outcome.getDestinationUri());

            try {
                Files.setPosixFilePermissions(binPath, posixLoc.getPermissions());
            } catch (IOException e) {
                throw new BinaryTransferException("Failed to set permissions in destination "
                        + destination.getId(), e);
            }
        }

        return outcome;
    }
}

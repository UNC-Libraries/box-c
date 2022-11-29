package edu.unc.lib.boxc.persist.api.transfer;

import java.net.URI;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.persist.api.storage.BinaryDetails;

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

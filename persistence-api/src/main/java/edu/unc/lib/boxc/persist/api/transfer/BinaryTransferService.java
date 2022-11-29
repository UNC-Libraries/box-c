package edu.unc.lib.boxc.persist.api.transfer;

import java.net.URI;

import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;

/**
 * A service for transferring binaries to preservation storage locations.
 *
 * @author bbpennel
 *
 */
public interface BinaryTransferService {

    /**
     * Get a new binary transfer session.
     *
     * @return new binary transfer session
     */
    MultiDestinationTransferSession getSession();

    /**
     * Get a new session for transferring binaries to the specified storage location
     *
     * @param destination the storage location to transfer to
     * @return new binary transfer session
     */
    BinaryTransferSession getSession(StorageLocation destination);

    /**
     * Get a new session for transferring binaries to the specified repository object
     *
     * @param repoObj object to open the session for
     * @return new binary transfer session
     */
    BinaryTransferSession getSession(RepositoryObject repoObj);

    /**
     * Rolls back the binary transfers associated with the provided repository transaction
     * @param txUri URI of the repository transaction to roll back
     */
    void rollbackTransaction(URI txUri);

    /**
     * Commits the binary transfers associated with the provided transaction
     * @param txUri URI of the repository transaction committed
     */
    void commitTransaction(URI txUri);

    /**
     * Register the outcome of a transfer
     * @param outcome results of the transfer
     */
    BinaryTransferOutcome registerOutcome(BinaryTransferOutcome outcome);
}

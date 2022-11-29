package edu.unc.lib.boxc.persist.api.transfer;

import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;

/**
 * A session for transferring one or more binaries to preservation storage locations
 *
 * @author bbpennel
 *
 */
public interface MultiDestinationTransferSession extends AutoCloseable {

    /**
     * Get a transfer session to a single destination within this multi-destination session.
     *
     * @param dest storage location to transfer to.
     * @return Transfer session to the provided destination
     */
    BinaryTransferSession forDestination(StorageLocation dest);

    /**
     * Get a transfer session for a specific object within this multi-destination session.
     *
     * @param repoObj object for the session
     * @return Transfer session for the object
     */
    BinaryTransferSession forObject(RepositoryObject repoObj);

    /**
     * Closes the transfer session. If there are any failures, they will be RuntimeExceptions.
     */
    @Override
    void close();
}

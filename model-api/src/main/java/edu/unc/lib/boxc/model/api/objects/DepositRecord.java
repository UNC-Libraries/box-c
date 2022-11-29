package edu.unc.lib.boxc.model.api.objects;

import java.net.URI;
import java.util.List;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.ids.PID;

/**
 * A Deposit Record repository object, which tracks information pertaining to a single deposit.
 * @author bbpennel
 */
public interface DepositRecord extends RepositoryObject {

    /**
     * Adds the given inputstream as the content of a manifest for this deposit.
     *
     * @param manifestUri URI of the binary content for this manifest
     * @param filename filename for the manifest
     * @param mimetype mimetype for the content of the manifest
     * @param sha1
     * @param md5
     * @return representing the newly created manifest object
     * @throws FedoraException
     */
    BinaryObject addManifest(URI manifestUri, String filename, String mimetype, String sha1, String md5)
            throws FedoraException;

    /**
     * Retrieves the requested manifest of this deposit record
     *
     * @param pid
     * @return The requested manifest as a BinaryObject or null if the pid was
     *         not a component of this deposit record
     * @throws FedoraException
     */
    BinaryObject getManifest(PID pid) throws FedoraException;

    /**
     * Retrieves a list of pids for manifests contained by this deposit record
     *
     * @return
     * @throws FedoraException
     */
    List<PID> listManifests() throws FedoraException;

    /**
     * Retrieves a list of pids for objects contained by this deposit record
     * @return
     * @throws FedoraException
     */
    List<PID> listDepositedObjects() throws FedoraException;

}
package edu.unc.lib.boxc.model.api.objects;

import java.io.InputStream;
import java.net.URI;

import edu.unc.lib.boxc.model.api.exceptions.FedoraException;


/**
 * A binary resource object in the repository. Represents a single binary file
 * and its properties.
 * @author bbpennel
 */
public interface BinaryObject extends RepositoryObject {
    /**
     * Get an inputstream of the binary content
     *
     * @return
     * @throws FedoraException
     */
    InputStream getBinaryStream() throws FedoraException;

    /**
     * @return the URI where the content for this binary is located
     */
    URI getContentUri();

    /**
     * Non-RDF resources, like binaries, have to retrieve metadata from a different path
     */
    @Override
    URI getMetadataUri();

    /**
     * Get the filename of the binary content if it was provided.
     *
     * @return
     * @throws FedoraException
     */
    String getFilename() throws FedoraException;

    /**
     * Get the mimetype of the store binary content
     *
     * @return
     * @throws FedoraException
     */
    String getMimetype() throws FedoraException;

    /**
     * Get the SHA-1 checksum for the stored binary content
     * @return
     * @throws FedoraException
     */
    String getSha1Checksum() throws FedoraException;

    /**
     * Get the MD5 checksum for the stored binary content
     * @return
     * @throws FedoraException
     */
    String getMd5Checksum() throws FedoraException;

    /**
     * Get the filesize of the stored binary content in bytes
     *
     * @return
     * @throws FedoraException
     */
    Long getFilesize() throws FedoraException;
}
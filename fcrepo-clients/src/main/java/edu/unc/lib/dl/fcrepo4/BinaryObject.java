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
package edu.unc.lib.dl.fcrepo4;

import java.io.InputStream;
import java.net.URI;

import org.apache.jena.rdf.model.Statement;

import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.ObjectTypeMismatchException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Ebucore;
import edu.unc.lib.dl.rdf.Fcrepo4Repository;
import edu.unc.lib.dl.rdf.Premis;

/**
 * A binary resource object in the repository. Represents a single binary file
 * and its properties.
 *
 * @author bbpennel
 *
 */
public class BinaryObject extends RepositoryObject {

    private String filename;
    private String mimetype;
    private String checksum;
    private Long filesize;

    private URI metadataUri;
    private RepositoryPaths repoPaths;

    protected BinaryObject(PID pid, RepositoryObjectLoader repoObjLoader, RepositoryObjectDataLoader dataLoader,
            RepositoryObjectFactory repoObjFactory) {
        super(pid, repoObjLoader, dataLoader, repoObjFactory);

        metadataUri = repoPaths.getMetadataUri(pid);
    }

    @Override
    public BinaryObject validateType() throws FedoraException {
        if (!isType(Fcrepo4Repository.Binary.getURI())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a binary.");
        }
        return this;
    }

    @Override
    public RepositoryObject getParent() {
        return dataLoader.getParentObject(this);
    }

    /**
     * Get an inputstream of the binary content
     *
     * @return
     * @throws FedoraException
     */
    public InputStream getBinaryStream() throws FedoraException {
        return dataLoader.getBinaryStream(this);
    }

    /**
     * Non-RDF resources, like binaries, have to retrieve metadata from a different path
     */
    @Override
    public URI getMetadataUri() {
        return metadataUri;
    }

    /**
     * Get the filename of the binary content if it was provided.
     *
     * @return
     * @throws FedoraException
     */
    public String getFilename() throws FedoraException {
        if (filename == null) {
            Statement property = getResource().getProperty(Ebucore.filename);
            if (property != null) {
                filename = property.getString();
            }
        }
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    /**
     * Get the mimetype of the store binary content
     *
     * @return
     * @throws FedoraException
     */
    public String getMimetype() throws FedoraException {
        if (mimetype == null) {
            mimetype = getResource().getProperty(Ebucore.hasMimeType).getString();
        }
        return mimetype;
    }

    public void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    /**
     * Get the SHA-1 checksum for the stored binary content
     * @return
     * @throws FedoraException
     */
    public String getChecksum() throws FedoraException {
        if (checksum == null) {
            checksum = getResource().getProperty(Premis.hasMessageDigest)
                    .getObject().toString();
        }
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    /**
     * Get the filesize of the stored binary content in bytes
     *
     * @return
     * @throws FedoraException
     */
    public Long getFilesize() throws FedoraException {
        if (filesize == null) {
            filesize = getResource().getProperty(Premis.hasSize).getLong();
        }
        return filesize;
    }

    public void setFilesize(Long filesize) {
        this.filesize = filesize;
    }
}

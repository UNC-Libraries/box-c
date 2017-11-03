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
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

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

    private static final String SHA1_PREFIX = "urn:sha1:";
    private static final String MD5_PREFIX = "urn:md5:";

    private String filename;
    private String mimetype;
    private String sha1Checksum;
    private String md5Checksum;
    private Long filesize;

    private URI metadataUri;

    protected BinaryObject(PID pid, RepositoryObjectDriver driver,
            RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);

        metadataUri = RepositoryPaths.getMetadataUri(pid);
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
        return driver.getParentObject(this);
    }

    /**
     * Get an inputstream of the binary content
     *
     * @return
     * @throws FedoraException
     */
    public InputStream getBinaryStream() throws FedoraException {
        return driver.getBinaryStream(this);
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
     * Get the list of checksums for the stored binary content
     * @return
     * @throws FedoraException
     */
    private List<String> getChecksums() throws FedoraException {
        StmtIterator it = getResource().listProperties(Premis.hasMessageDigest);
        ArrayList<String> checksums = new ArrayList<>();
        while (it.hasNext()) {
            checksums.add(it.next().getObject().toString());
        }
        return checksums;
    }

    /**
     * Get the SHA-1 checksum for the stored binary content
     * @return
     * @throws FedoraException
     */
    public String getSha1Checksum() throws FedoraException {
        if (sha1Checksum == null) {
            List<String> checksums = getChecksums();
            for (String checksum : checksums) {
                if (checksum.startsWith(SHA1_PREFIX)) {
                    sha1Checksum = checksum;
                    break;
                }
            }
       }
        return sha1Checksum;
    }

    /**
     * Get the MD5 checksum for the stored binary content
     * @return
     * @throws FedoraException
     */
    public String getMd5Checksum() throws FedoraException {
        if (md5Checksum == null) {
            List<String> checksums = getChecksums();
            for (String checksum : checksums) {
                if (checksum.startsWith(MD5_PREFIX)) {
                    md5Checksum = checksum;
                    break;
                }
            }
        }
        return md5Checksum;
    }

    public void setSha1Checksum(String sha1) {
        this.sha1Checksum = sha1;
    }

    public void setMd5Checksum(String md5) {
        this.md5Checksum = md5;
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

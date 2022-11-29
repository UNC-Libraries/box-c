package edu.unc.lib.boxc.model.fcrepo.objects;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.ObjectTypeMismatchException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectDriver;

/**
 * A binary resource object in the repository. Represents a single binary file
 * and its properties.
 *
 * @author bbpennel
 *
 */
public class BinaryObjectImpl extends AbstractRepositoryObject implements BinaryObject {

    private static final String SHA1_PREFIX = "urn:sha1:";
    private static final String MD5_PREFIX = "urn:md5:";

    private String filename;
    private String mimetype;
    private String sha1Checksum;
    private String md5Checksum;
    private Long filesize;

    private URI metadataUri;
    private URI contentUri;

    /**
     * Constructor for internal binary
     *
     * @param pid
     * @param driver
     * @param repoObjFactory
     */
    protected BinaryObjectImpl(PID pid, RepositoryObjectDriver driver,
            RepositoryObjectFactory repoObjFactory) {
        this(pid, null, driver, repoObjFactory);
    }

    /**
     * Constructor for external binary
     *
     * @param pid
     * @param contentUri
     * @param driver
     * @param repoObjFactory
     */
    protected BinaryObjectImpl(PID pid, URI contentUri, RepositoryObjectDriver driver,
            RepositoryObjectFactory repoObjFactory) {
        super(pid, driver, repoObjFactory);

        metadataUri = RepositoryPaths.getMetadataUri(pid);
        this.contentUri = contentUri;
    }

    @Override
    public BinaryObjectImpl validateType() throws FedoraException {
        if (!isType(Fcrepo4Repository.Binary.getURI())) {
            throw new ObjectTypeMismatchException("Object " + pid + " is not a binary.");
        }
        return this;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.Binary;
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
    @Override
    public InputStream getBinaryStream() throws FedoraException {
        return driver.getBinaryStream(this);
    }

    /**
     * @return the URI where the content for this binary is located
     */
    @Override
    public URI getContentUri() {
        return contentUri;
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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

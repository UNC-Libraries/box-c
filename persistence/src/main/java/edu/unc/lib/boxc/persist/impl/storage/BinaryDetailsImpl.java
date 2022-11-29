package edu.unc.lib.boxc.persist.impl.storage;

import java.net.URI;
import java.util.Date;

import edu.unc.lib.boxc.persist.api.storage.BinaryDetails;

/**
 * Implementation of generic details of a binary file
 *
 * @author bbpennel
 */
public class BinaryDetailsImpl implements BinaryDetails {

    private URI uri;
    private Date lastModified;
    private long size;
    private String digest;

    /**
     * @param lastModified
     * @param size
     */
    public BinaryDetailsImpl(URI uri, Date lastModified, long size, String digest) {
        this.uri = uri;
        this.lastModified = lastModified;
        this.size = size;
        this.digest = digest;
    }

    @Override
    public Date getLastModified() {
        return lastModified;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public URI getDestinationUri() {
        return uri;
    }

    @Override
    public String getDigest() {
        return digest;
    }

}

package edu.unc.lib.boxc.persist.api.storage;

import java.net.URI;
import java.util.Date;

/**
 * Details of a binary file
 *
 * @author bbpennel
 */
public interface BinaryDetails {

    /**
     * @return the URI of the stored binary
     */
    URI getDestinationUri();

    /**
     * @return last modified timestamp of the binary
     */
    Date getLastModified();

    /**
     * @return file size of the binary
     */
    long getSize();

    /**
     * @return the digest for the binary
     */
    String getDigest();
}

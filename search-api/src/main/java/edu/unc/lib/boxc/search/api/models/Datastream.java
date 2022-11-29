package edu.unc.lib.boxc.search.api.models;

/**
 * A record representing a datastream held by a repository object
 *
 * @author bbpennel
 */
public interface Datastream {

    /**
     * @return identifier of this specific instance of a datastream, including the owner id and datastream name
     */
    String getDatastreamIdentifier();

    /**
     * @return Name of this datastream, generally the identifier of the datastream independent of the object holding it
     */
    String getName();

    /**
     * @return identifier of the object to which this datastream belongs
     */
    String getOwner();

    /**
     * @return filesize in byte for this datastream
     */
    Long getFilesize();

    /**
     * @return mimetype of this datastream
     */
    String getMimetype();

    /**
     * @return file extension for this datastream
     */
    String getExtension();

    /**
     * @return Checksum/digest for this datastream
     */
    String getChecksum();

    /**
     * @return filename of the datastream
     */
    String getFilename();

    /**
     * @return technical details of the datastream, such as dimensions
     */
    String getExtent();

}
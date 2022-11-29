package edu.unc.lib.boxc.model.api.objects;

import java.io.InputStream;
import java.util.List;

import edu.unc.lib.boxc.model.api.exceptions.InvalidRelationshipException;

/**
 * Represents a generic repository object within the main content tree.
 * @author bbpennel
 */
public interface ContentObject extends RepositoryObject {

    /**
     * Adds source metadata file to this object
     *
     * @param sourceMdStream
     * @param sourceProfile
     *            identifies the encoding, profile, and/or origins of the
     *            sourceMdStream using an identifier defined in
     *            edu.unc.lib.dl.util.MetadataProfileConstants
     * @return BinaryObjects for source metadata
     * @throws InvalidRelationshipException
     *             in case no source profile was provided
     */
    BinaryObject addSourceMetadata(InputStream sourceMdStream, String sourceProfile)
            throws InvalidRelationshipException;

    /**
     * Gets a list of BinaryObjects for the metadata binaries associated with
     * this object.
     *
     * @return List of metadata BinaryObjects for this object
     */
    List<BinaryObject> listMetadata();

    /**
     * Gets the BinaryObject with the MODS for this object
     * @return the BinaryObject
     */
    BinaryObject getDescription();

}
package edu.unc.lib.boxc.model.api.rdf;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definitions from /Users/bbpennel/Desktop/cdr-schemas/cdrDeposit.rdf
 * @author Auto-generated by schemagen on 05 May 2016 10:57
 */
public class CdrDeposit {
    private CdrDeposit() {
    }

    /** The namespace of the vocabulary as a string */
    public static final String NS = "http://cdr.unc.edu/definitions/deposit#";

    /** The namespace of the vocabulary as a string
     *  @see #NS */
    public static String getURI() {
        return NS; }

    /** The namespace of the vocabulary as a resource */
    public static final Resource NAMESPACE = createResource( NS );

    /** File path suggested for cleanup after the deposit has been ingested */
    public static final Property cleanupLocation = createProperty(
            "http://cdr.unc.edu/definitions/deposit#cleanupLocation" );

    /** Created timestamp of this file on its original storage */
    public static final Property createTime = createProperty(
            "http://cdr.unc.edu/definitions/deposit#createTime" );

    /** Last modified timestamp of this file on its original storage */
    public static final Property lastModifiedTime = createProperty(
            "http://cdr.unc.edu/definitions/deposit#lastModifiedTime");

    /** Filename to provide for this object in the repository */
    public static final Property label = createProperty(
            "http://cdr.unc.edu/definitions/deposit#label" );

    /** MD5 checksum for this binary object */
    public static final Property md5sum = createProperty(
            "http://cdr.unc.edu/definitions/deposit#md5sum" );

    /** Provided mimetype for this binary object */
    public static final Property mimetype = createProperty(
            "http://cdr.unc.edu/definitions/deposit#mimetype" );

    /** RDF resource type for this object */
    public static final Property objectType = createProperty(
            "http://cdr.unc.edu/definitions/deposit#objectType" );

    /** Location URI for this object in its original storage location */
    public static final Property originalLocation = createProperty(
            "http://cdr.unc.edu/definitions/deposit#originalLocation" );

    /** URI referencing the resource which represents the original deposit for an object */
    public static final Property originalDeposit = createProperty(
            "http://cdr.unc.edu/definitions/deposit#originalDeposit" );

    /** SHA1 checksum for this binary object */
    public static final Property sha1sum = createProperty(
            "http://cdr.unc.edu/definitions/deposit#sha1sum" );

    /** Filesize in bytes for this binary object */
    public static final Property size = createProperty(
            "http://cdr.unc.edu/definitions/deposit#size" );

    /** Path to the staged content for ingest. */
    public static final Property stagingLocation = createProperty(
            "http://cdr.unc.edu/definitions/deposit#stagingLocation" );

    /** URI of binary content within a storage location. */
    public static final Property storageUri = createProperty(
            "http://cdr.unc.edu/definitions/deposit#storageUri" );

    /** Link to original binary resource */
    public static final Property hasDatastreamOriginal = createProperty(
            "http://cdr.unc.edu/definitions/deposit#hasDatastreamOriginal" );

    /** Link to access copy binary resource */
    public static final Property hasDatastreamAccessSurrogate = createProperty(
            "http://cdr.unc.edu/definitions/deposit#hasDatastreamAccessSurrogate" );

    /** Link to FITS binary resource */
    public static final Property hasDatastreamFits = createProperty(
            "http://cdr.unc.edu/definitions/deposit#hasDatastreamFits" );

    public static final Property hasDatastreamFitsHistory = createProperty(
            "http://cdr.unc.edu/definitions/deposit#hasDatastreamFitsHistory" );

    /** Link to description history binary resource */
    public static final Property hasDatastreamDescriptiveHistory = createProperty(
            "http://cdr.unc.edu/definitions/deposit#hasDatastreamDescriptiveHistory" );

    public static final Property hasDatastreamManifest = createProperty(
            "http://cdr.unc.edu/definitions/deposit#hasDatastreamManifest" );
}

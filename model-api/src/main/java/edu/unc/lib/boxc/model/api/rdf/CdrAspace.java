package edu.unc.lib.boxc.model.api.rdf;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

public class CdrAspace {
    private CdrAspace() {
    }

    /** The namespace of the vocabulary as a string */
    public static final String NS = "http://cdr.unc.edu/definitions/aspace#";

    /** The namespace of the vocabulary as a string
     *  @see #NS */
    public static String getURI() {
        return NS;
    }

    /** The namespace of the vocabulary as a resource */
    public static final Resource NAMESPACE = createResource( NS );

    /**
     * Property which holds the ArchivesSpace Ref ID
     */
    public static final Property refId = createProperty(
            "http://cdr.unc.edu/definitions/aspace#refId"
    );
}

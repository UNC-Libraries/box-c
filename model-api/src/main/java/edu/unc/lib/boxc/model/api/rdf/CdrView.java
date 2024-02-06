package edu.unc.lib.boxc.model.api.rdf;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;

/**
 * @author snluong
 */
public class CdrView {
    private CdrView() {
    }

    /** The namespace of the vocabulary as a string */
    public static final String NS = "http://cdr.unc.edu/definitions/view#";

    /** The namespace of the vocabulary as a string
     *  @see #NS */
    public static String getURI() {
        return NS; }

    /** The namespace of the vocabulary as a resource */
    public static final Resource NAMESPACE = createResource( NS );

    /** Used to define the IIIFv3 view "behavior" property for works.
     * Valid values: https://iiif.io/api/presentation/3.0/#behavior */
    public static final Property viewBehavior = createProperty(
            "http://cdr.unc.edu/definitions/view#behavior");
}

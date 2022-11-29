package edu.unc.lib.boxc.model.api.rdf;

import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

import org.apache.jena.rdf.model.Property;

/**
 * PROV namespace properties
 *
 * @author bbpennel
 */
public class Prov {
    private Prov() {
    }

    public static final String NS = "http://w3.org/ns/prov#";

    /** The namespace of the vocabulary as a string
     *  @see #NS */
    public static String getURI() {
        return NS;
    }

    public static final Property generated = createProperty(NS + "generated");
    public static final Property used = createProperty(NS + "used");
}

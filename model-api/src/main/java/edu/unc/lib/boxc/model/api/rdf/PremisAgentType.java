package edu.unc.lib.boxc.model.api.rdf;

import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definitions from rdf-schemas/premis.rdf
 * @author harring
 */
public class PremisAgentType {

    /** The namespace of the vocabulary as a string */
    public static final String NS = "http://www.loc.gov/premis/rdf/v3/";

    private PremisAgentType() {
    }

    /** The namespace of the vocabulary as a string
     *  @see #NS */
    public static String getURI() {
        return NS;
    }

    /** The namespace of the vocabulary as a resource */
    public static final Resource NAMESPACE = createResource( NS );

    /**
     * A high-level characterization of the type of agent.
     * See http://id.loc.gov/ontologies/premis-3-0-0.html#c_Agent
     */

    public static final Resource Hardware = createResource("http://www.loc.gov/premis/rdf/v3/HardwareAgent");

    public static final Resource Organization = createResource(
            "http://www.loc.gov/premis/rdf/v3/Organization");

    public static final Resource Person = createResource("http://www.loc.gov/premis/rdf/v3/Person");

    public static final Resource Software = createResource("http://www.loc.gov/premis/rdf/v3/SoftwareAgent");
}

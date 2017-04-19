package edu.unc.lib.dl.rdf; 

import static org.apache.jena.rdf.model.ResourceFactory.createResource;

import org.apache.jena.rdf.model.Resource;
 
/**
 * Vocabulary definitions from rdf-schemas/premis.rdf 
 * @author harring
 */
public class PremisAgentType {
    
    /** The namespace of the vocabulary as a string */
    public static final String NS = "http://id.loc.gov/vocabulary/preservation/agentType";
    
    /** The namespace of the vocabulary as a string
     *  @see #NS */
    public static String getURI() {return NS;}
    
    /** The namespace of the vocabulary as a resource */
    public static final Resource NAMESPACE = createResource( NS );
    
    /**
     * A high-level characterization of the type of agent. See http://id.loc.gov/vocabulary/preservation/agentType/collection_PREMIS
     */
    
    public static final Resource Hardware = createResource("http://id.loc.gov/vocabulary/preservation/agentType/har");
    
    public static final Resource Organization = createResource("http://id.loc.gov/vocabulary/preservation/agentType/org");
    
    public static final Resource Person = createResource("http://id.loc.gov/vocabulary/preservation/agentType/per");
    
    public static final Resource Software = createResource("http://id.loc.gov/vocabulary/preservation/agentType/sof");
}

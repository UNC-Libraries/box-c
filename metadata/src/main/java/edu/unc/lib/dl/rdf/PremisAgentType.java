/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
     * See http://id.loc.gov/vocabulary/preservation/agentType/collection_PREMIS
     */

    public static final Resource Hardware = createResource("http://id.loc.gov/vocabulary/preservation/agentType/har");

    public static final Resource Organization = createResource(
            "http://id.loc.gov/vocabulary/preservation/agentType/org");

    public static final Resource Person = createResource("http://id.loc.gov/vocabulary/preservation/agentType/per");

    public static final Resource Software = createResource("http://id.loc.gov/vocabulary/preservation/agentType/sof");
}

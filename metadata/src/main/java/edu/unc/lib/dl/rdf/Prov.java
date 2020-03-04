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

    public static final Property wasGeneratedBy = createProperty(NS + "wasGeneratedBy");
    public static final Property wasUsedBy = createProperty(NS + "wasUsedBy");
}

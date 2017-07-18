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
package edu.unc.lib.dl.util;

import java.util.List;

import org.jdom2.Element;

import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * 
 * @author bbpennel
 *
 */
public abstract class RDFUtil {

    /**
     * Adds all elements from an incoming RDF element into a base RDF element
     * without overwriting any existing data
     *
     * @param baseRDF
     * @param incomingRDF
     * @return
     */
    public static Element mergeRDF(Element baseRDF, Element incomingRDF) {
        if (baseRDF == null) {
            return incomingRDF;
        }
        if (incomingRDF == null) {
            return baseRDF;
        }
        List<?> incomingChildren = incomingRDF.getChildren("Description", JDOMNamespaceUtil.RDF_NS);
        // If there is no rdf:Description tag in the incoming data, then there is nothing to add.
        if (incomingChildren.size() == 0) {
            return baseRDF;
        }
        Element incomingDescription = (Element) incomingChildren.get(0);
        Element newDescription = null;

        newDescription = baseRDF.getChild("Description", JDOMNamespaceUtil.RDF_NS);
        // If the previous rels-ext didn't have a description, then use the new one
        if (newDescription == null) {
            baseRDF.addContent((Element) incomingDescription.clone());
            return baseRDF;
        }

        // Clone all the child elements of the incoming rdf:Description tag
        List<?> incomingElements = incomingDescription.getChildren();
        // Add all the incoming element children to the base modified object
        for (Object incomingObject : incomingElements) {
            if (incomingObject instanceof Element) {
                newDescription.addContent((Element) ((Element) incomingObject).clone());
            }
        }

        return baseRDF;
    }

    /**
     * Adds all elements from an incoming RDF element into a base RDF element,
     * where all relations in the incoming element will overwrite those in the
     * base element when they match on both subject and predicate.
     *
     * @param baseRDF
     * @param incomingRDF
     * @return
     */
    public static Element updateRDF(Element baseRDF, Element incomingRDF) {
        if (baseRDF == null) {
            return incomingRDF;
        }
        if (incomingRDF == null) {
            return baseRDF;
        }
        // If there is no rdf:Description tag in the incoming data, then there is nothing to add.
        if (incomingRDF.getChildren().size() == 0) {
            return baseRDF;
        }
        Element incomingDescription = (Element) incomingRDF.getChildren("Description",
                JDOMNamespaceUtil.RDF_NS).get(0);
        Element newDescription = null;

        // If the previous rels-ext didn't have a description, then use the new one
        if (baseRDF.getChildren().size() == 0) {
            baseRDF.addContent((Element) incomingDescription.clone());
            return baseRDF;
        } else {
            newDescription = (Element) baseRDF.getChildren().get(0);
        }

        // Clone all the child elements of the incoming rdf:Description tag
        List<Element> incomingElements = (List<Element>) incomingDescription.getChildren();
        // Add all the incoming element children to the base modified object
        for (Element incomingElement : incomingElements) {
            // Remove the preexisting relations of the same type before adding the new entries
            newDescription.removeChildren(incomingElement.getName(), incomingElement.getNamespace());
            newDescription.addContent((Element) incomingElement.clone());
        }

        return baseRDF;
    }
}

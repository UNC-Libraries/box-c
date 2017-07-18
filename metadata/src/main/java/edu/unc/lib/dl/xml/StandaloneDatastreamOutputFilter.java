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
package edu.unc.lib.dl.xml;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Filters FOXML SAX Events before serialization, adding redundant namespace
 * declarations to XML content and RELS-EXT datastreams, so that Fedora will be able to
 * process them after they have been extracted.
 * effectively makes XML datastreams "standalone" documents.
 *
 * @author count0
 */
public class StandaloneDatastreamOutputFilter extends XMLFilterImpl {

//    String xsiPrefix = "xsi";
//    String rdfPrefix = null;

    // map containing the namespace declarations that are currently in scope.
    Map<String, String> scopeNamespaces = new HashMap<String, String>();

    boolean inRDF = false;

    @Override
    public void startPrefixMapping(final String prefix, final String uri) throws SAXException {
//        if (JDOMNamespaceUtil.XSI_NS.getURI().equals(uri)) {
//            xsiPrefix = prefix;
//        }
//        if (JDOMNamespaceUtil.RDF_NS.getURI().equals(uri)) {
//            rdfPrefix = prefix;
//        }
        scopeNamespaces.put(uri, prefix);
        super.startPrefixMapping(prefix, uri);
    }


    @Override
    public void endPrefixMapping(final String prefix) throws SAXException {
//        if (inRDF && rdfPrefix != null && rdfPrefix.equals(prefix)) {
//            rdfPrefix = null;
//        }
        //scopeNamespaces.remove(prefix);
        super.endPrefixMapping(prefix);
    }


    @Override
    public void startElement(final String uri, final String localName, final String qName,
            Attributes atts) throws SAXException {
        if ("RDF".equals(localName) && JDOMNamespaceUtil.RDF_NS.getURI().equals(uri)) {
            // manually start a prefix mapping for the "CDR" namespace, since it's
            // reasonably likely we're going
            // to have an element from this namespace in the RELS-EXT; due to an
            // apparent bug in Fedora 3.0b2, namespaces not explicitly declared
            // on the rdf:RDF element
            // will not be output when the RELS-EXT datastream is extracted. --AC
            // add all expected RDF namespaces with the prefixes detected earlier.
            inRDF = true;
            getContentHandler().startPrefixMapping(
                    this.scopeNamespaces.get(JDOMNamespaceUtil.CDR_NS.getURI()),
                    JDOMNamespaceUtil.CDR_NS.getURI());
        }

        if (this.inRDF) {
            AttributesImpl newAtts = new AttributesImpl();
            for (int i = 0; i < atts.getLength(); i++) {
                if ("about".equals(atts.getLocalName(i)) || "resource".equals(atts.getLocalName(i))) {
                    newAtts.addAttribute(
                        JDOMNamespaceUtil.RDF_NS.getURI(),
                        atts.getLocalName(i),
                        this.scopeNamespaces.get(JDOMNamespaceUtil.RDF_NS.getURI()) +
                          ":" + atts.getLocalName(i),
                        atts.getType(i),
                        atts.getValue(i));
                } else { // just copy
                    newAtts.addAttribute(atts.getURI(i), atts.getLocalName(i), atts.getQName(i),
                            atts.getType(i), atts.getValue(i));
                }
            }
            atts = newAtts;
        }
        super.startElement(uri, localName, qName, atts);
        if ("xmlContent".equals(localName)) {
            this.getContentHandler().startPrefixMapping(
                this.scopeNamespaces.get(JDOMNamespaceUtil.XSI_NS.getURI()),
                JDOMNamespaceUtil.XSI_NS.getURI());
        }
    }


    @Override
    public void endElement(final String uri, final String localName, final String name) throws SAXException {
    super.endElement(uri, localName, name);
        if ("RDF".equals(localName) && JDOMNamespaceUtil.RDF_NS.getURI().equals(uri)) {
            //this.getContentHandler().endPrefixMapping(this.scopeNamespaces.get(JDOMNamespaceUtil.RDF_NS.getURI()));
            inRDF = false;
        }
    }



}

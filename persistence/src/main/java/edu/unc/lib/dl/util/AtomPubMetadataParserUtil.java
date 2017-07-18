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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.log4j.Logger;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import edu.unc.lib.dl.fedora.DatastreamPID;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * 
 * @author bbpennel
 *
 */
public abstract class AtomPubMetadataParserUtil {
    private static Logger log = Logger
            .getLogger(AtomPubMetadataParserUtil.class);

    public static final String ATOM_DC_DATASTREAM = "ATOM_DC";
    private static final QName datastreamQName = new QName(
            "http://cdr.lib.unc.edu/", "datastream");
    private static final QName modsQName = new QName(
            "http://www.loc.gov/mods/v3", "mods");
    private static final String dcNamespace = "http://purl.org/dc/terms/";
    private static final String atomPubNamespace = "http://www.w3.org/2005/Atom";

    public static Map<String, org.jdom2.Element> extractDatastreams(Entry entry)
            throws IOException, JDOMException {
        return extractDatastreams(entry, (String) null);
    }

    public static Map<String, org.jdom2.Element> extractDatastreams(
            Entry entry, PID pid) throws IOException, JDOMException {
        String defaultDatastream = null;
        // If the request was for a specific datastream, add it in
        if (pid != null && pid instanceof DatastreamPID) {
            defaultDatastream = ((DatastreamPID) pid).getDatastream();
        }
        return extractDatastreams(entry, defaultDatastream);
    }

    /**
     * Returns a map containing the metadata content as jdom elements associated
     * with their datastream id. The content is extracted from an Atom Pub
     * abdera entry. If the
     *
     * root level qualified dublin core tags or a MODS entry, as well as any
     * number of cdr:datastream tags containing specific metadata streams to
     * extract.
     *
     * If a datastream tag contains more than one root element, only the first
     * element will be retained
     *
     * @param entry
     *            abdera Atom Pub entry containing metadata for extraction.
     * @return
     * @throws IOException
     * @throws JDOMException
     */
    public static Map<String, org.jdom2.Element> extractDatastreams(
            Entry entry, String defaultDatastream) throws IOException,
            JDOMException {
        if (entry == null || entry.getElements().size() == 0) {
            return null;
        }

        SAXBuilder saxBuilder = new SAXBuilder();

        Map<String, org.jdom2.Element> datastreamMap = new HashMap<String, org.jdom2.Element>();
        // Outstream containing the compiled default dublin core tags
        ByteArrayOutputStream dcOutStream = null;

        Element defaultDatastreamElement = null;

        boolean multiDocumentMode = defaultDatastream == null;
        boolean rootDublinCoreElements = false;

        try {
            for (Element element : entry.getElements()) {
                if (dcNamespace.equals(element.getQName().getNamespaceURI())) {
                    // Populate dublin core properties from the default entry
                    // metadata
                    if (dcOutStream == null) {
                        dcOutStream = new ByteArrayOutputStream();
                        dcOutStream
                                .write("<dcterms:dc xmlns:dcterms=\"http://purl.org/dc/terms/\">"
                                        .getBytes("UTF-8"));
                        rootDublinCoreElements = true;
                    }
                    element.writeTo(dcOutStream);
                } else if (multiDocumentMode) { // Multi document mode
                    // Datastream wrapper tag
                    if (datastreamQName.equals(element.getQName())) {
                        // Create new datastream entry
                        String id = element.getAttributeValue("id");
                        if (id != null) {
                            org.jdom2.Element jdomElement = abderaToJDOM(
                                    element, saxBuilder);
                            org.jdom2.Element dsContentElement = null;
                            // Store the first child of the datastream tag as
                            // the content for this DS
                            if (jdomElement.getChildren().size() > 0) {
                                dsContentElement = ((org.jdom2.Element) jdomElement
                                        .getChildren().get(0));
                                datastreamMap.put(id,
                                        (org.jdom2.Element) dsContentElement
                                                .detach());
                            }
                        }
                        // MODS root tag
                    } else if (modsQName.equals(element.getQName())) {
                        // Create the default mods datastream, taking precedence
                        // over the stub from DC terms
                        org.jdom2.Element modsElement = abderaToJDOM(element,
                                saxBuilder);
                        datastreamMap.put(
                                ContentModelHelper.Datastream.MD_DESCRIPTIVE
                                        .getName(), modsElement);
                    } else if (JDOMNamespaceUtil.CDR_ACL_NS.getURI().equals(
                            element.getQName().getNamespaceURI())) {
                        log.debug("Extracting access control virtual datastream info");
                        org.jdom2.Element aclElement = abderaToJDOM(element,
                                saxBuilder);
                        datastreamMap.put("ACL", aclElement);
                    }
                } else {
                    // Specific datastream mode, use the first non-atompub tag
                    // since we can't have multiple roots
                    // Can't have one of these if we're already in dublin core
                    // mode
                    if (!rootDublinCoreElements
                            && !atomPubNamespace.equals(element.getQName()
                                    .getNamespaceURI())) {
                        defaultDatastreamElement = element;
                        break;
                    }
                }
            }

            // Create the atom dublin core default datastream if it's populated
            if (dcOutStream != null) {
                dcOutStream.write("</dcterms:dc>".getBytes("UTF-8"));
                try (ByteArrayInputStream inStream = new ByteArrayInputStream(
                        dcOutStream.toByteArray())) {
                    org.jdom2.Document jdomDocument = saxBuilder
                            .build(inStream);
                    org.jdom2.Element rootNode = jdomDocument.getRootElement();

                    if (defaultDatastream == null) {
                        datastreamMap.put(ATOM_DC_DATASTREAM, rootNode);
                    } else {
                        datastreamMap.put(defaultDatastream, rootNode);
                    }
                }
            } else if (!multiDocumentMode) {
                // Add in the targeted datastream
                org.jdom2.Element jdomElement = abderaToJDOM(
                        defaultDatastreamElement, saxBuilder);
                datastreamMap.put(defaultDatastream, jdomElement);
            }

            // Implied datastreams
            // Add RELS-EXT datastream stub if the ACL datastream is specified
            // and there isn't currently a RELS-EXT
            if (datastreamMap.containsKey("ACL")
                    && !datastreamMap
                            .containsKey(ContentModelHelper.Datastream.RELS_EXT
                                    .getName())) {
                datastreamMap.put(
                        ContentModelHelper.Datastream.RELS_EXT.getName(), null);
            }

            // Add in a stub for MD_DESCRIPTIVE if a root dc entry was generated
            // and no MODS have been added yet.
            if (multiDocumentMode
                    && datastreamMap.containsKey(ATOM_DC_DATASTREAM)
                    && !datastreamMap
                            .containsKey(ContentModelHelper.Datastream.MD_DESCRIPTIVE
                                    .getName())) {
                datastreamMap.put(
                        ContentModelHelper.Datastream.MD_DESCRIPTIVE.getName(),
                        null);
            }
        } finally {
            if (dcOutStream != null) {
                try {
                    dcOutStream.close();
                } catch (IOException e) {
                    log.error("Failed to close DC", e);
                }
            }
        }
        return datastreamMap;
    }

    /**
     * Converts an abdera element to a jdom element by converting it back to raw
     * xml.
     *
     * @param element
     * @return
     * @throws JDOMException
     * @throws IOException
     */
    public static org.jdom2.Element abderaToJDOM(Element element,
            SAXBuilder saxBuilder) throws JDOMException, IOException {
        if (element == null) {
            return null;
        }
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            element.writeTo(outStream);
            try (ByteArrayInputStream inStream = new ByteArrayInputStream(
                    outStream.toByteArray())) {
                org.jdom2.Document jdomDocument = saxBuilder.build(inStream);
                return jdomDocument.detachRootElement();
            }
        }
    }
}

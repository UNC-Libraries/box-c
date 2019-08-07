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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.xpath.XPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.util.ContentModelHelper;

/**
 * Convenience methods for working with FoxML JDOM
 * @author count0
 *
 */
public class FOXMLJDOMUtil {
    private FOXMLJDOMUtil() {
    }

    public static enum ObjectProperty {
        createdDate(JDOMNamespaceUtil.FEDORA_MODEL_NS, "createdDate"), label(
                JDOMNamespaceUtil.FEDORA_MODEL_NS, "label"), ownerId(
                JDOMNamespaceUtil.FEDORA_MODEL_NS, "ownerId"), state(
                JDOMNamespaceUtil.FEDORA_MODEL_NS, "state");
        private URI uri;

        ObjectProperty(final Namespace ns, final String suffix) {
            try {
                this.uri = new URI(ns.getURI() + suffix);
            } catch (URISyntaxException e) {
                Error x = new ExceptionInInitializerError(
                        "Cannot initialize ContentModelHelper");
                x.initCause(e);
                throw x;
            }
        }

        public URI getURI() {
            return this.uri;
        }

        @Override
        public String toString() {
            return this.uri.toString();
        }
    }

    private static final String __foxmlXpathPrefix = "f";
    private static XPath _fileLocatorXpath;
    private static final Namespace _foxmlXpathNamespace = Namespace
            .getNamespace(__foxmlXpathPrefix,
                    JDOMNamespaceUtil.FOXML_NS.getURI());
    private static XPath _getAllDatastreamsXpath;
    private static XPath _labelXPath;
    private static XPath _pidXPath;
    private static final String datastreamXmlContentXpath =
            "/f:digitalObject/f:datastream[@ID='%DSID%']/f:datastreamVersion/f:xmlContent";
    private static final String fileLocatorXpath = "//f:contentLocation";
    private static final String getAllDatastreamsXpath = "/f:digitalObject/f:datastream";
    private static final String getDatastreamXpath = "/f:digitalObject/f:datastream[@ID='%DSID%']";
    private static final String labelXpath =
            "/f:digitalObject/f:objectProperties/f:property[@NAME='info:fedora/fedora-system:def/model#label']/@VALUE";
    private static final Logger log = LoggerFactory.getLogger(FOXMLJDOMUtil.class);

    private static final String pidXpath = "/f:digitalObject/@PID";

    static {
        try {
            _pidXPath = XPath.newInstance(pidXpath);
            _pidXPath.addNamespace(_foxmlXpathNamespace);
            _labelXPath = XPath.newInstance(labelXpath);
            _labelXPath.addNamespace(_foxmlXpathNamespace);
            _fileLocatorXpath = XPath.newInstance(fileLocatorXpath);
            _fileLocatorXpath.addNamespace(_foxmlXpathNamespace);
            _getAllDatastreamsXpath = XPath.newInstance(getAllDatastreamsXpath);
            _getAllDatastreamsXpath.addNamespace(_foxmlXpathNamespace);
        } catch (JDOMException e) {
            log.error("Bad Configuration for FOXMLJDOMUtil", e);
            throw new IllegalArgumentException(
                    "Bad Configuration for FOXMLJDOMUtil", e);
        }
    }

    public static List<Element> getAllDatastreams(final Document foxml) {
        List<Element> result = null;
        try {
            result = extracted(_getAllDatastreamsXpath.selectNodes(foxml));
        } catch (JDOMException e) {
            log.warn("JDOMException trying to setDatastreamXmlContent", e);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<Element> extracted(
            @SuppressWarnings("rawtypes") final List selectNodes) {
        return selectNodes;
    }

    public static Element getDatastream(final Document foxml, final String id) {
        Element result = null;
        try {
            XPath _getDatastreamXpath = XPath.newInstance(getDatastreamXpath
                    .replaceFirst("%DSID%", id));
            _getDatastreamXpath.addNamespace(_foxmlXpathNamespace);
            Object o = _getDatastreamXpath.selectSingleNode(foxml);
            if (o != null && o instanceof Element) {
                result = (Element) o;
            }
        } catch (JDOMException e) {
            log.warn("JDOMException trying to setDatastreamXmlContent", e);
        }
        return result;
    }

    public static List<Element> getFileLocators(final Document foxml) {
        List<Element> result = new ArrayList<>();
        try {
            result = extracted(_fileLocatorXpath.selectNodes(foxml));
        } catch (JDOMException e) {
            log.warn("JDOMException trying to get label property from foxml", e);
        } catch (ClassCastException e) {
            log.error("programmer error", e);
            throw new Error("programmer error", e);
        }
        return result;
    }

    public static String getLabel(final Document foxml) {
        String result = null;
        try {
            result = _labelXPath.valueOf(foxml);
        } catch (JDOMException e) {
            log.warn("JDOMException trying to get label property from foxml", e);
        }
        if (result != null && "".equals(result.trim())) {
            result = null;
        }
        return result;
    }

    public static String getPID(final Document foxml) {
        String result = null;
        try {
            result = _pidXPath.valueOf(foxml).trim();
        } catch (JDOMException e) {
            log.warn(
                    "JDOMException trying to get path in collection from foxml",
                    e);
        }
        if ("".equals(result)) {
            result = null;
        }
        return result;
    }

    public static Document makeFOXMLDocument(final String pid) {
        Document result = new Document();
        Element rootElement = new Element("digitalObject",
                JDOMNamespaceUtil.FOXML_NS);
        rootElement.addNamespaceDeclaration(JDOMNamespaceUtil.XSI_NS);
        rootElement.setAttribute("VERSION", "1.1");
        rootElement
                .setAttribute(
                        "schemaLocation",
                        JDOMNamespaceUtil.FOXML_NS.getURI()
                                + " http://www.fedora.info/definitions/1/0/foxml1-1.xsd",
                        JDOMNamespaceUtil.XSI_NS);
        if (pid != null) {
            rootElement.setAttribute("PID", pid);
        }
        rootElement.addContent(new Element("objectProperties",
                JDOMNamespaceUtil.FOXML_NS));
        result.setRootElement(rootElement);
        return result;
    }

    /**
     * Creates a FOXML locator element.
     *
     * @param id
     * @param ctrlGroup
     * @param locator
     * @param mimeType
     * @param locatorType
     * @param label
     * @return
     */
    // public static Element makeLocatorDatastream(String id, String ctrlGroup,
    // String locator, String mimeType,
    // String locatorType, String label, boolean versionable) {
    // Element dcDS = new Element("datastream", JDOMNamespaceUtil.FOXML_NS);
    // dcDS.setAttribute("ID", id);
    // dcDS.setAttribute("CONTROL_GROUP", ctrlGroup);
    // dcDS.setAttribute("STATE", "A");
    // dcDS.setAttribute("VERSIONABLE", versionable ? "true" : "false" );
    // Element dcDSV = new Element("datastreamVersion",
    // JDOMNamespaceUtil.FOXML_NS);
    // dcDS.addContent(dcDSV);
    // dcDSV.setAttribute("ID", id + ".0");
    // dcDSV.setAttribute("MIMETYPE", mimeType);
    // dcDSV.setAttribute("LABEL", label);
    // Element contentDigest = new Element("contentDigest",
    // JDOMNamespaceUtil.FOXML_NS);
    // contentDigest.setAttribute("TYPE", "MD5");
    // contentDigest.setAttribute("DIGEST", "none");
    // dcDSV.addContent(contentDigest);
    // Element contentLocation = new Element("contentLocation",
    // JDOMNamespaceUtil.FOXML_NS);
    // contentLocation.setAttribute("REF", locator);
    // contentLocation.setAttribute("TYPE", locatorType);
    // dcDSV.addContent(contentLocation);
    // return dcDS;
    // }

    public static Element makeLocatorDatastream(final String id, final String ctrlGroup,
            final String locator, final String mimeType, final String locatorType, final String label,
            final boolean versionable, final String md5checksum) {
        Element dcDS = new Element("datastream", JDOMNamespaceUtil.FOXML_NS);
        dcDS.setAttribute("ID", id);
        dcDS.setAttribute("CONTROL_GROUP", ctrlGroup);
        dcDS.setAttribute("STATE", "A");
        dcDS.setAttribute("VERSIONABLE", versionable ? "true" : "false");
        Element dcDSV = new Element("datastreamVersion",
                JDOMNamespaceUtil.FOXML_NS);
        dcDS.addContent(dcDSV);
        dcDSV.setAttribute("ID", id + ".0");

        if (mimeType != null) {
            dcDSV.setAttribute("MIMETYPE", mimeType);
        }

        if (label != null) {
            dcDSV.setAttribute("LABEL", label);
        }

        Element contentDigest = new Element("contentDigest",
                JDOMNamespaceUtil.FOXML_NS);
        contentDigest.setAttribute("TYPE", "MD5");
        if (md5checksum != null) {
            contentDigest.setAttribute("DIGEST", md5checksum);
        } else {
            contentDigest.setAttribute("DIGEST", "none");
        }
        dcDSV.addContent(contentDigest);
        Element contentLocation = new Element("contentLocation",
                JDOMNamespaceUtil.FOXML_NS);
        contentLocation.setAttribute("REF", locator);
        contentLocation.setAttribute("TYPE", locatorType);
        dcDSV.addContent(contentLocation);
        return dcDS;
    }

    public static Element makeInlineXMLDatastreamElement(final String id,
            final String label, final Element xmlData, final boolean versioned) {
        return makeInlineXMLDatastreamElement(id, label, id + "1.0", xmlData,
                versioned);
    }

    public static Element makeInlineXMLDatastreamElement(final String id,
            final String label, final String versionId, final Element xmlData, final boolean versioned) {
        Element dcDS = new Element("datastream", JDOMNamespaceUtil.FOXML_NS);
        dcDS.setAttribute("ID", id);
        dcDS.setAttribute("CONTROL_GROUP", "X");
        dcDS.setAttribute("STATE", "A");
        dcDS.setAttribute("VERSIONABLE", versioned ? "true" : "false");
        Element dcDSV = new Element("datastreamVersion",
                JDOMNamespaceUtil.FOXML_NS);
        dcDS.addContent(dcDSV);
        dcDSV.setAttribute("ID", versionId);
        dcDSV.setAttribute("MIMETYPE", "text/xml");
        dcDSV.setAttribute("LABEL", label);
        Element contentDigest = new Element("contentDigest",
                JDOMNamespaceUtil.FOXML_NS);
        contentDigest.setAttribute("TYPE", "MD5");
        contentDigest.setAttribute("DIGEST", "none");
        dcDSV.addContent(contentDigest);
        Element xmlContent = new Element("xmlContent",
                JDOMNamespaceUtil.FOXML_NS);
        dcDSV.addContent(xmlContent);
        xmlContent.addContent(xmlData);
        return dcDS;
    }

    /**
     * Sets the content of the datastream, creating it if it doesn't exist.
     *
     * @param foxml
     *            the FOXML Document
     * @param datastreamId
     *            the ID of the datastream
     * @param label
     *            the label for the datastreamVersion (ignored if datastream
     *            exists)
     * @param newContent
     *            the new Element content of the datastream (within xmlContent)
     */
    public static void setInlineXMLDatastreamContent(final Document foxml,
            final String datastreamId, final String label, final Element newContent,
            final boolean versioned) {
        try {
            XPath _datastreamXmlContentXpath = XPath
                    .newInstance(datastreamXmlContentXpath.replaceFirst(
                            "%DSID%", datastreamId));
            _datastreamXmlContentXpath.addNamespace(_foxmlXpathNamespace);
            Object o = _datastreamXmlContentXpath.selectSingleNode(foxml);
            Element xmlContent = null;
            if (o == null) { // there's no datastream w/ID, so make one and
                // add it!
                Element dsContent = makeInlineXMLDatastreamElement(
                        datastreamId, label, newContent, versioned);
                foxml.getRootElement().addContent(dsContent);
            } else if (o instanceof Element) {
                xmlContent = (Element) o;
                xmlContent.setContent(newContent);
            }
        } catch (JDOMException e) {
            log.warn("JDOMException trying to setDatastreamXmlContent", e);
        }
    }

    public static void setProperty(final Document doc, final ObjectProperty prop,
            final String value) {
        Element props = doc.getRootElement().getChild("objectProperties",
                JDOMNamespaceUtil.FOXML_NS);
        if (props == null) {
            props = new Element("objectProperties", JDOMNamespaceUtil.FOXML_NS);
            doc.getRootElement().addContent(1, props);
        } else {
            Iterator<Element> childIt = props.getChildren().iterator();
            while (childIt.hasNext()) {
                Element el = childIt.next();
                if (prop.toString().equals(el.getAttributeValue("NAME"))) {
                    childIt.remove();
                }
            }
        }
        Element newProp = new Element("property", JDOMNamespaceUtil.FOXML_NS);
        newProp.setAttribute("NAME", prop.toString());
        newProp.setAttribute("VALUE", value);
        props.addContent(newProp);
        return;
    }

    public static Element getDatastreamContent(
            final ContentModelHelper.Datastream datastream, final Document foxml) {
        return getDatastreamContent(datastream, foxml.getRootElement());
    }

    /**
     * Returns the content of an internal datastream from the given foxml. If
     * the datastream is versionable, then the most recent version of the
     * datastream is returned.
     *
     * @param datastream
     * @param foxml
     * @return
     */
    public static Element getDatastreamContent(
            final ContentModelHelper.Datastream datastream, final Element foxml) {
        Element dsVersion;
        Element datastreamEl = JDOMQueryUtil.getChildByAttribute(foxml,
                "datastream", JDOMNamespaceUtil.FOXML_NS, "ID",
                datastream.getName());

        if (datastreamEl == null) {
            return null;
        }

        if (datastream.isVersionable()) {
            dsVersion = FOXMLJDOMUtil
                    .getMostRecentDatastreamVersion(datastreamEl.getChildren(
                            "datastreamVersion", JDOMNamespaceUtil.FOXML_NS));
        } else {
            dsVersion = datastreamEl.getChild("datastreamVersion",
                    JDOMNamespaceUtil.FOXML_NS);
        }

        return dsVersion.getChild("xmlContent", JDOMNamespaceUtil.FOXML_NS)
                .getChildren().get(0);
    }

    /**
     * Returns a map of all of the most recent versions of datastreams listed in
     * the given FOXML document
     *
     * @param foxml
     * @return
     */
    public static Map<String, Element> getMostRecentDatastreamMap(final Document foxml) {
        Map<String, Element> datastreams = new HashMap<>();
        List<?> datastreamList = foxml.getRootElement().getChildren(
                "datastream", JDOMNamespaceUtil.FOXML_NS);
        for (Object datastreamObject : datastreamList) {
            Element datastreamEl = (Element) datastreamObject;
            String datastreamName = datastreamEl.getAttributeValue("ID");
            if (datastreamName != null) {
                ContentModelHelper.Datastream datastreamClass = ContentModelHelper.Datastream
                        .getDatastream(datastreamName);
                if (datastreamClass == null) {
                    continue;
                }

                Element dsVersion;
                if (datastreamClass.isVersionable()) {
                    dsVersion = FOXMLJDOMUtil
                            .getMostRecentDatastreamVersion(datastreamEl
                                    .getChildren("datastreamVersion",
                                            JDOMNamespaceUtil.FOXML_NS));
                } else {
                    dsVersion = datastreamEl.getChild("datastreamVersion",
                            JDOMNamespaceUtil.FOXML_NS);
                }
                datastreams.put(datastreamName, dsVersion);
            }
        }

        return datastreams;
    }

    public static Element getMostRecentDatastream(
            final ContentModelHelper.Datastream datastream, final Document foxml) {
        return getMostRecentDatastream(datastream, foxml.getRootElement());
    }

    public static Element getMostRecentDatastream(
            final ContentModelHelper.Datastream datastream, final Element foxml) {
        List<?> datastreamList = foxml.getChildren("datastream",
                JDOMNamespaceUtil.FOXML_NS);

        for (Object datastreamObject : datastreamList) {
            Element datastreamEl = (Element) datastreamObject;
            String datastreamName = datastreamEl.getAttributeValue("ID");
            if (datastreamName != null
                    && datastreamName.equals(datastream.getName())) {
                Element dsVersion;
                if (datastream.isVersionable()) {
                    dsVersion = FOXMLJDOMUtil
                            .getMostRecentDatastreamVersion(datastreamEl
                                    .getChildren("datastreamVersion",
                                            JDOMNamespaceUtil.FOXML_NS));
                } else {
                    dsVersion = datastreamEl.getChild("datastreamVersion",
                            JDOMNamespaceUtil.FOXML_NS);
                }
                return dsVersion;
            }
        }

        return null;
    }

    /**
     * Returns the most recent version of a datastream from the given set of
     * datastream elements
     *
     * @param elements
     * @return
     */
    public static Element getMostRecentDatastreamVersion(final List<?> elements) {
        if (elements == null || elements.size() == 0) {
            return null;
        }

        if (elements.size() == 1) {
            return (Element) elements.get(0);
        }

        String mostRecentDate = "";
        Element mostRecent = null;
        for (Object datastreamVersionObj : elements) {
            Element datastreamVersion = (Element) datastreamVersionObj;
            String created = datastreamVersion.getAttributeValue("CREATED");
            if (mostRecentDate.compareTo(created) < 0) {
                mostRecentDate = created;
                mostRecent = datastreamVersion;
            }
        }
        if (mostRecent != null) {
            return mostRecent;
        }

        return (Element) elements.get(0);
    }

    /**
     * Returns the object of the relationship specified from the provided
     * RELS-EXT datastream. Only returns the first value if the relation occurs
     * multiple times
     *
     * @param relationName
     * @param relationNS
     * @param relsExt
     * @return
     */
    public static String getRelationValue(final String relationName,
            final Namespace relationNS, final Element relsExt) {
        Element relationEl = relsExt.getChild(relationName, relationNS);
        if (relationEl != null) {
            String value = relationEl.getAttributeValue("resource",
                    JDOMNamespaceUtil.RDF_NS);
            if (value == null) {
                value = relationEl.getText();
            }
            return value;
        }
        return null;
    }

    public static List<String> getRelationValues(final String relationName,
            final Namespace relationNS, final Element relsExt) {
        List<?> relationEls = relsExt.getChildren(relationName, relationNS);
        if (relationEls != null) {
            List<String> values = new ArrayList<>(relationEls.size());
            for (Object relationObj : relationEls) {
                values.add(((Element) relationObj).getAttributeValue(
                        "resource", JDOMNamespaceUtil.RDF_NS));
            }
            return values;
        }
        return null;
    }

    public static Element getObjectProperties(final Document foxml) {
        // /foxml:digitalObject/foxml:objectProperties
        return foxml.getRootElement().getChild("objectProperties",
                JDOMNamespaceUtil.FOXML_NS);
    }

    public static Element getRelsExt(final Document foxml) {
        Element relsExt = getDatastreamContent(
                ContentModelHelper.Datastream.RELS_EXT, foxml);
        if ("RDF".equals(relsExt.getName())
                && JDOMNamespaceUtil.RDF_NS.equals(relsExt.getNamespace())) {
            return relsExt.getChildren().get(0);
        }
        return relsExt;
    }
}

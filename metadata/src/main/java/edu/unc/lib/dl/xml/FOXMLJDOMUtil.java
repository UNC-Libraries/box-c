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
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

public class FOXMLJDOMUtil {
	public static enum ObjectProperty {
		createdDate(JDOMNamespaceUtil.FEDORA_MODEL_NS, "createdDate"), label(JDOMNamespaceUtil.FEDORA_MODEL_NS, "label"), ownerId(
				JDOMNamespaceUtil.FEDORA_MODEL_NS, "ownerId"), state(JDOMNamespaceUtil.FEDORA_MODEL_NS, "state");
		private URI uri;

		ObjectProperty(Namespace ns, String suffix) {
			try {
				this.uri = new URI(ns.getURI() + suffix);
			} catch (URISyntaxException e) {
				Error x = new ExceptionInInitializerError("Cannot initialize ContentModelHelper");
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
	private static final Namespace _foxmlXpathNamespace = Namespace.getNamespace(__foxmlXpathPrefix,
			JDOMNamespaceUtil.FOXML_NS.getURI());
	private static XPath _getAllDatastreamsXpath;
	private static XPath _labelXPath;
	private static XPath _pidXPath;
	private static final String datastreamXmlContentXpath = "/f:digitalObject/f:datastream[@ID='%DSID%']/f:datastreamVersion/f:xmlContent";
	private static final String fileLocatorXpath = "//f:contentLocation";
	private static final String getAllDatastreamsXpath = "/f:digitalObject/f:datastream";
	private static final String getDatastreamXpath = "/f:digitalObject/f:datastream[@ID='%DSID%']";
	private static final String labelXpath = "/f:digitalObject/f:objectProperties/f:property[@NAME='info:fedora/fedora-system:def/model#label']/@VALUE";
	private static Logger log = Logger.getLogger(FOXMLJDOMUtil.class);

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
			throw new IllegalArgumentException("Bad Configuration for FOXMLJDOMUtil", e);
		}
	}

	public static List<Element> getAllDatastreams(Document foxml) {
		List<Element> result = null;
		try {
			result = extracted(_getAllDatastreamsXpath.selectNodes(foxml));
		} catch (JDOMException e) {
			log.warn("JDOMException trying to setDatastreamXmlContent", e);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static List<Element> extracted(List selectNodes) {
		return selectNodes;
	}

	public static Element getDatastream(Document foxml, String id) {
		Element result = null;
		try {
			XPath _getDatastreamXpath = XPath.newInstance(getDatastreamXpath.replaceFirst("%DSID%", id));
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

	public static List<Element> getFileLocators(Document foxml) {
		List<Element> result = new ArrayList<Element>();
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

	public static String getLabel(Document foxml) {
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

	public static String getPID(Document foxml) {
		String result = null;
		try {
			result = _pidXPath.valueOf(foxml).trim();
		} catch (JDOMException e) {
			log.warn("JDOMException trying to get path in collection from foxml", e);
		}
		if ("".equals(result)) {
			result = null;
		}
		return result;
	}

	public static Document makeFOXMLDocument(String pid) {
		Document result = new Document();
		Element rootElement = new Element("digitalObject", JDOMNamespaceUtil.FOXML_NS);
		rootElement.addNamespaceDeclaration(JDOMNamespaceUtil.XSI_NS);
		rootElement.setAttribute("VERSION", "1.1");
		rootElement.setAttribute("schemaLocation", JDOMNamespaceUtil.FOXML_NS.getURI()
				+ " http://www.fedora.info/definitions/1/0/foxml1-1.xsd", JDOMNamespaceUtil.XSI_NS);
		if (pid != null) {
			rootElement.setAttribute("PID", pid);
		}
		rootElement.addContent(new Element("objectProperties", JDOMNamespaceUtil.FOXML_NS));
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
	// public static Element makeLocatorDatastream(String id, String ctrlGroup, String locator, String mimeType,
	// String locatorType, String label, boolean versionable) {
	// Element dcDS = new Element("datastream", JDOMNamespaceUtil.FOXML_NS);
	// dcDS.setAttribute("ID", id);
	// dcDS.setAttribute("CONTROL_GROUP", ctrlGroup);
	// dcDS.setAttribute("STATE", "A");
	// dcDS.setAttribute("VERSIONABLE", versionable ? "true" : "false" );
	// Element dcDSV = new Element("datastreamVersion", JDOMNamespaceUtil.FOXML_NS);
	// dcDS.addContent(dcDSV);
	// dcDSV.setAttribute("ID", id + ".0");
	// dcDSV.setAttribute("MIMETYPE", mimeType);
	// dcDSV.setAttribute("LABEL", label);
	// Element contentDigest = new Element("contentDigest", JDOMNamespaceUtil.FOXML_NS);
	// contentDigest.setAttribute("TYPE", "MD5");
	// contentDigest.setAttribute("DIGEST", "none");
	// dcDSV.addContent(contentDigest);
	// Element contentLocation = new Element("contentLocation", JDOMNamespaceUtil.FOXML_NS);
	// contentLocation.setAttribute("REF", locator);
	// contentLocation.setAttribute("TYPE", locatorType);
	// dcDSV.addContent(contentLocation);
	// return dcDS;
	// }

	public static Element makeLocatorDatastream(String id, String ctrlGroup, String locator, String mimeType,
			String locatorType, String label, boolean versionable, String md5checksum) {
		Element dcDS = new Element("datastream", JDOMNamespaceUtil.FOXML_NS);
		dcDS.setAttribute("ID", id);
		dcDS.setAttribute("CONTROL_GROUP", ctrlGroup);
		dcDS.setAttribute("STATE", "A");
		dcDS.setAttribute("VERSIONABLE", versionable ? "true" : "false");
		Element dcDSV = new Element("datastreamVersion", JDOMNamespaceUtil.FOXML_NS);
		dcDS.addContent(dcDSV);
		dcDSV.setAttribute("ID", id + ".0");
		dcDSV.setAttribute("MIMETYPE", mimeType);
		dcDSV.setAttribute("LABEL", label);
		Element contentDigest = new Element("contentDigest", JDOMNamespaceUtil.FOXML_NS);
		contentDigest.setAttribute("TYPE", "MD5");
		if (md5checksum != null) {
			contentDigest.setAttribute("DIGEST", md5checksum);
		} else {
			contentDigest.setAttribute("DIGEST", "none");
		}
		dcDSV.addContent(contentDigest);
		Element contentLocation = new Element("contentLocation", JDOMNamespaceUtil.FOXML_NS);
		contentLocation.setAttribute("REF", locator);
		contentLocation.setAttribute("TYPE", locatorType);
		dcDSV.addContent(contentLocation);
		return dcDS;
	}

	public static Element makeInlineXMLDatastreamElement(String id, String label, Element xmlData, boolean versioned) {
		return makeInlineXMLDatastreamElement(id, label, id + "1.0", xmlData, versioned);
	}

	public static Element makeInlineXMLDatastreamElement(String id, String label, String versionId, Element xmlData,
			boolean versioned) {
		Element dcDS = new Element("datastream", JDOMNamespaceUtil.FOXML_NS);
		dcDS.setAttribute("ID", id);
		dcDS.setAttribute("CONTROL_GROUP", "X");
		dcDS.setAttribute("STATE", "A");
		dcDS.setAttribute("VERSIONABLE", versioned ? "true" : "false");
		Element dcDSV = new Element("datastreamVersion", JDOMNamespaceUtil.FOXML_NS);
		dcDS.addContent(dcDSV);
		dcDSV.setAttribute("ID", versionId);
		dcDSV.setAttribute("MIMETYPE", "text/xml");
		dcDSV.setAttribute("LABEL", label);
		Element contentDigest = new Element("contentDigest", JDOMNamespaceUtil.FOXML_NS);
		contentDigest.setAttribute("TYPE", "MD5");
		contentDigest.setAttribute("DIGEST", "none");
		dcDSV.addContent(contentDigest);
		Element xmlContent = new Element("xmlContent", JDOMNamespaceUtil.FOXML_NS);
		dcDSV.addContent(xmlContent);
		xmlContent.addContent(xmlData);
		return dcDS;
	}

	public static Element makeXMLManagedDatastreamElement(String id, String label, String versionId, Element xmlData,
			boolean versioned) {
		Element dcDS = new Element("datastream", JDOMNamespaceUtil.FOXML_NS);
		dcDS.setAttribute("ID", id);
		dcDS.setAttribute("CONTROL_GROUP", "M");
		dcDS.setAttribute("STATE", "A");
		dcDS.setAttribute("VERSIONABLE", versioned ? "true" : "false");
		Element dcDSV = new Element("datastreamVersion", JDOMNamespaceUtil.FOXML_NS);
		dcDS.addContent(dcDSV);
		dcDSV.setAttribute("ID", versionId);
		dcDSV.setAttribute("MIMETYPE", "text/xml");
		dcDSV.setAttribute("LABEL", label);
		Element contentDigest = new Element("contentDigest", JDOMNamespaceUtil.FOXML_NS);
		contentDigest.setAttribute("TYPE", "MD5");
		contentDigest.setAttribute("DIGEST", "none");
		dcDSV.addContent(contentDigest);
		Element xmlContent = new Element("xmlContent", JDOMNamespaceUtil.FOXML_NS);
		dcDSV.addContent(xmlContent);
		xmlContent.addContent(xmlData);
		return dcDS;
	}

	/**
	 * Sets the content of the datastream, creating it if it doesn't exist.
	 *
	 * @param foxml
	 *           the FOXML Document
	 * @param datastreamId
	 *           the ID of the datastream
	 * @param label
	 *           the label for the datastreamVersion (ignored if datastream exists)
	 * @param newContent
	 *           the new Element content of the datastream (within xmlContent)
	 */
	public static void setInlineXMLDatastreamContent(Document foxml, String datastreamId, String label, Element newContent,
			boolean versioned) {
		try {
			XPath _datastreamXmlContentXpath = XPath.newInstance(datastreamXmlContentXpath.replaceFirst("%DSID%",
					datastreamId));
			_datastreamXmlContentXpath.addNamespace(_foxmlXpathNamespace);
			Object o = _datastreamXmlContentXpath.selectSingleNode(foxml);
			Element xmlContent = null;
			if (o == null) { // there's no datastream w/ID, so make one and
				// add it!
				Element dsContent = makeInlineXMLDatastreamElement(datastreamId, label, newContent, versioned);
				foxml.getRootElement().addContent(dsContent);
			} else if (o instanceof Element) {
				xmlContent = (Element) o;
				xmlContent.setContent(newContent);
			}
		} catch (JDOMException e) {
			log.warn("JDOMException trying to setDatastreamXmlContent", e);
		}
	}

	public static void setProperty(Document doc, ObjectProperty prop, String value) {
		Element props = doc.getRootElement().getChild("objectProperties", JDOMNamespaceUtil.FOXML_NS);
		if (props == null) {
			props = new Element("objectProperties", JDOMNamespaceUtil.FOXML_NS);
			doc.getRootElement().addContent(1, props);
		} else {
			@SuppressWarnings("unchecked")
			Iterator<Element> childIt = props.getChildren().iterator();
			while (childIt.hasNext()){
				Element el = childIt.next();
				if (prop.toString().equals(el.getAttributeValue("NAME"))){
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
}

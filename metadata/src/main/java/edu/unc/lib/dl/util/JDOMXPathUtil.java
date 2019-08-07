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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.xpath.XPath;

import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 * A utility class that is configured to retrieve collection identifiers
 * 
 * @author count0
 * 
 */
public class JDOMXPathUtil {
    private static final String __p = JDOMNamespaceUtil.PREMIS_V2_NS
            .getPrefix();

    private static XPath _dcFieldsXpath;
    private static XPath _guidXpath;
    private static XPath _isMemberOfCollectionXpath;
    private static XPath _isPartOfIDTYPEXpath;
    private static XPath _isPartOfIDVALUEXpath;
    private static final String _isPartOfRelationshipXpath = "//" + __p
            + ":relationship" + "[" + __p + ":relationshipType = '"
            + JDOMNamespaceUtil.RELSEXT_NS.getURI() + "isPartOf']";
    private static XPath _pathInCollectionXpath;
    private static XPath _repoPathXpath;
    private static final String dcFieldsXpath = "//"
            + JDOMNamespaceUtil.DC_NS.getPrefix() + ":*";

    private static final String guidXpath = "//"
            + JDOMNamespaceUtil.CDR_NS.getPrefix() + ":uid";
    private static final String isMemberOfCollectionXpath = "//"
            + JDOMNamespaceUtil.RELSEXT_NS.getPrefix()
            + ":isMemberOfCollection/@resource";
    private static final String isPartOfIDTYPEXpath = _isPartOfRelationshipXpath
            + "/"
            + __p
            + ":relatedObjectIdentification/"
            + __p
            + ":relatedObjectIdentifierType";
    private static final String isPartOfIDVALUEXpath = _isPartOfRelationshipXpath
            + "/"
            + __p
            + ":relatedObjectIdentification/"
            + __p
            + ":relatedObjectIdentifierValue";
    private static final Log log = LogFactory.getLog(JDOMXPathUtil.class);
    private static final String pathInCollectionXpath = "//"
            + JDOMNamespaceUtil.CDR_NS.getPrefix() + ":pathInCollection";
    private static final String repoPathXpath = "//"
            + JDOMNamespaceUtil.CDR_NS.getPrefix() + ":repositoryPath";

    private JDOMXPathUtil() {
    }

    static {
        try {
            _isMemberOfCollectionXpath = XPath
                    .newInstance(isMemberOfCollectionXpath);
            _isMemberOfCollectionXpath
                    .addNamespace(JDOMNamespaceUtil.RELSEXT_NS);
            _isMemberOfCollectionXpath.addNamespace(JDOMNamespaceUtil.RDF_NS);
            _isPartOfIDTYPEXpath = XPath.newInstance(isPartOfIDTYPEXpath);
            _isPartOfIDTYPEXpath.addNamespace(JDOMNamespaceUtil.PREMIS_V2_NS);
            _isPartOfIDVALUEXpath = XPath.newInstance(isPartOfIDVALUEXpath);
            _isPartOfIDVALUEXpath.addNamespace(JDOMNamespaceUtil.PREMIS_V2_NS);
            _pathInCollectionXpath = XPath.newInstance(pathInCollectionXpath);
            _pathInCollectionXpath.addNamespace(JDOMNamespaceUtil.CDR_NS);
            _guidXpath = XPath.newInstance(guidXpath);
            _guidXpath.addNamespace(JDOMNamespaceUtil.CDR_NS);
            _repoPathXpath = XPath.newInstance(repoPathXpath);
            _repoPathXpath.addNamespace(JDOMNamespaceUtil.CDR_NS);
            _dcFieldsXpath = XPath.newInstance(dcFieldsXpath);
            _dcFieldsXpath.addNamespace(JDOMNamespaceUtil.DC_NS);
            _dcFieldsXpath.addNamespace(JDOMNamespaceUtil.CDR_NS);
        } catch (JDOMException e) {
            log.error("Bad Configuration for JDOMXPathUtil", e);
            throw new IllegalArgumentException(
                    "Bad Configuration for JDOMXPathUtil", e);
        }
    }

    public static Map<String, String> getDCFields(Document doc) {
        HashMap<String, String> result = new HashMap<String, String>();
        try {
            for (Object con : _dcFieldsXpath.selectNodes(doc)) {
                if (con instanceof Element) {
                    Element el = (Element) con;
                    String value = el.getTextTrim();
                    String name = el.getName();
                    result.put(name, value);
                }
            }
        } catch (JDOMException e) {
            throw new Error("Programming error, XPath should be valid", e);
        }
        return result;
    }

    /**
     * Attempt to find the globally unique identifier in the FOXML Document.
     * 
     * @param foxml
     * @return either an identifier or null
     */
    public static String getGUID(Document foxml) {
        String guid = null;
        try {
            guid = _guidXpath.valueOf(foxml).trim();
            if ("".equals(guid)) {
                guid = null;
                log.info("Object is missing it's GUID: "
                        + _guidXpath.getXPath());
            }
        } catch (JDOMException e) {
            log.info("Problem finding collection identifier XPaths", e);
        }
        return guid;
    }

    /**
     * Attempt to find a collection-based repository path in the FOXML Document.
     * 
     * @param foxml
     * @return either an identifier or null
     */
    /*
     * public static String getRepositoryPath(Document foxml) { String path =
     * null; try { path = _repoPathXpath.valueOf(foxml).trim(); if
     * ("".equals(path)) { path = null; log.info("Object is missing it's
     * repository path: " + _repoPathXpath.getXPath()); } } catch (JDOMException
     * e) { log.info("Problem finding repository path in FOXML via XPath.", e);
     * } return path; }
     */

    public static String getIsMemberOfCollectionPID(Document foxml) {
        String result = null;
        try {
            result = _isMemberOfCollectionXpath.valueOf(foxml).trim();
        } catch (JDOMException e) {
            log.warn("JDOMException trying to get collection from foxml", e);
        }
        if ("".equals(result)) {
            result = null;
        }
        return result;
    }

    /**
     * @param foxml
     * @return a two string array containing the identifier type and the
     *         identifier value This functions returns an identifier type and
     *         value for the isPartOf relationship defined in PREMIS. The PREMIS
     *         supplied isPartOf information is used to link the top object in a
     *         SIP to it's containing object in the repository.
     */
    public static String[] getIsPartOfRelatedObject(Document foxml) {
        String[] result = new String[2];
        String type = null;
        String value = null;
        try {
            type = _isPartOfIDTYPEXpath.valueOf(foxml).trim();
            value = _isPartOfIDVALUEXpath.valueOf(foxml).trim();
            result[0] = type;
            result[1] = value;
        } catch (JDOMException e) {
            log.warn("JDOMException trying to get collection from foxml", e);
        }
        if ("".equals(type) || "".equals(value)) {
            result = null;
        }
        return result;
    }

    public static String getPathInCollection(Document foxml) {
        String result = null;
        try {
            result = _pathInCollectionXpath.valueOf(foxml).trim();
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
}

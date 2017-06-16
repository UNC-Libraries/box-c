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

import static edu.unc.lib.dl.xml.NamespaceConstants.ATOM_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.ATOM_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.CDR_MESSAGE_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.CDR_MESSAGE_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.CDR_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.CDR_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.DCTERMS_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.DCTERMS_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.DC_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.DC_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.FEDORA_MODEL_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.FEDORA_MODEL_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.FEDORA_VIEW_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.FEDORA_VIEW_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.FITS_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.FITS_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.FOXML_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.LOCAL_RELS_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.LOCAL_RELS_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.METS_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.METS_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.MODS_V3_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.MODS_V3_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.MULGARA_TQL_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.OAI_DC_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.OAI_DC_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.PREMIS_V2_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.PREMIS_V2_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.PREMIS_V3_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.PREMIS_V3_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.RDF_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.RDF_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.RELSEXT_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.RELSEXT_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.SCHEMATRON_VALIDATION_REPORT_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.SCHEMATRON_VALIDATION_REPORT_URI;
import static edu.unc.lib.dl.xml.NamespaceConstants.XLINK_PREFIX;
import static edu.unc.lib.dl.xml.NamespaceConstants.XLINK_URI;

import javax.xml.XMLConstants;

import org.jdom2.Namespace;

/**
 * <strong>* EXPERIMENTAL *</strong> Utility class that contains JDOM
 * <code>Namepsace</code> objects for namespaces that are commonly encountered
 * when processing XML in the digital library platform. The defined namespaces
 * contain 'typical' prefixes for what's expected to be the most common use
 * cases.
 * <p>
 *
 * @author adamc, $LastChangedBy$
 * @version $LastChangedRevision$
 * @see NamespaceConstants
 * @see org.jdom2.Namespace
 */
public class JDOMNamespaceUtil {

    /**
     * CDR namespace with "cdr" prefix.
     */
    // public static final Namespace CDR_BASEMODEL_NS =
    // Namespace.getNamespace(CDR_BASEMODEL_PREFIX, CDR_BASEMODEL_URI);

    /**
     * CDR namespace with "cdr" prefix
     */
    public static final Namespace CDR_NS = Namespace.getNamespace(CDR_PREFIX,
            CDR_URI);

    /**
     * CDR Messages namespace with "cdrmsg" prefix
     */
    public static final Namespace CDR_MESSAGE_NS = Namespace.getNamespace(
            CDR_MESSAGE_PREFIX, CDR_MESSAGE_URI);

    /**
     * DCMI namespace with standard prefix.
     */
    public static final Namespace DC_NS = Namespace.getNamespace(DC_PREFIX,
            DC_URI);

    /**
     * DCMI Metadata Terms namespace with standard prefix.
     */
    public static final Namespace DCTERMS_NS = Namespace.getNamespace(
            DCTERMS_PREFIX, DCTERMS_URI);

    /**
     * Fedora content model namespace with standard prefix.
     */
    public static final Namespace FEDORA_MODEL_NS = Namespace.getNamespace(
            FEDORA_MODEL_PREFIX, FEDORA_MODEL_URI);

    /**
     * Fedora content view namespace with standard prefix.
     */
    public static final Namespace FEDORA_VIEW_NS = Namespace.getNamespace(
            FEDORA_VIEW_PREFIX, FEDORA_VIEW_URI);

    /**
     * Fedora Object XML namespace (no prefix).
     */
    public static final Namespace FOXML_NS = Namespace.getNamespace(FOXML_URI);

    /**
     * Local RELS-EXT custom namespace with standard prefix.
     */
    public static final Namespace LOCAL_RELS_NS = Namespace.getNamespace(
            LOCAL_RELS_PREFIX, LOCAL_RELS_URI);

    /**
     * METS v3 namespace (no prefix).
     */
    public static final Namespace METS_NS = Namespace.getNamespace(METS_PREFIX,
            METS_URI);

    /**
     * MODS V3 namespace with standard prefix.
     */
    public static final Namespace MODS_V3_NS = Namespace.getNamespace(
            MODS_V3_PREFIX, MODS_V3_URI);

    /**
     * MULGARA TQL response namespace (no prefix).
     */
    public static final Namespace MULGARA_TQL_NS = Namespace
            .getNamespace(MULGARA_TQL_URI);

    /**
     * OAI Dublin Core namespace with standard prefix.
     */
    public static final Namespace OAI_DC_NS = Namespace.getNamespace(
            OAI_DC_PREFIX, OAI_DC_URI);

    /**
     * PREMIS v2 namespace with standard prefix.
     */
    public static final Namespace PREMIS_V2_NS = Namespace.getNamespace(
            PREMIS_V2_PREFIX, PREMIS_V2_URI);

    public static final Namespace PREMIS_V3_NS = Namespace.getNamespace(
            PREMIS_V3_PREFIX, PREMIS_V3_URI);

    /**
     * RDF namespace with standard prefix.
     */
    public static final Namespace RDF_NS = Namespace.getNamespace(RDF_PREFIX,
            RDF_URI);

    /**
     * Fedora RELS-EXT namespace with standard prefix.
     */
    public static final Namespace RELSEXT_NS = Namespace.getNamespace(
            RELSEXT_PREFIX, RELSEXT_URI);

    // public static final Namespace SCHEMATRON_NS =
    // Namespace.getNamespace(SCHEMATRON_ISO_PREFIX, SCHEMATRON_ISO_URI);

    public static final Namespace SCHEMATRON_VALIDATION_REPORT_NS = Namespace
            .getNamespace(SCHEMATRON_VALIDATION_REPORT_PREFIX,
                    SCHEMATRON_VALIDATION_REPORT_URI);

    /**
     * XLink namespace with standard prefix.
     */
    public static final Namespace XLINK_NS = Namespace.getNamespace(
            XLINK_PREFIX, XLINK_URI);

    /**
     * Atom namespace with standard prefix.
     */
    public static final Namespace ATOM_NS = Namespace.getNamespace(ATOM_PREFIX,
            ATOM_URI);

    /**
     * FITS output namespace with standard prefix.
     */
    public static final Namespace FITS_NS = Namespace.getNamespace(FITS_PREFIX,
            FITS_URI);

    /**
     * W3C XML Schema namespace with "xsd" prefix.
     *
     * @see javax.xml.XMLConstants#W3C_XML_SCHEMA_NS_URI
     */
    public static final Namespace XSD_NS = Namespace.getNamespace("xsd",
            XMLConstants.W3C_XML_SCHEMA_NS_URI);

    /**
     * W3C XML Schema Instance namespace with "xsi" prefix.
     *
     * @see javax.xml.XMLConstants#W3C_XML_SCHEMA_INSTANCE_NS_URI
     */
    public static final Namespace XSI_NS = Namespace.getNamespace("xsi",
            XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);

    /**
     * CDR user roles namespace.
     */
    public static final Namespace CDR_ROLE_NS = Namespace.getNamespace(
            NamespaceConstants.CDR_ROLES_PREFIX,
            NamespaceConstants.CDR_ROLE_NS_URI);

    public static final Namespace CDR_ACL_NS = Namespace.getNamespace(
            NamespaceConstants.CDR_ACL_PREFIX,
            NamespaceConstants.CDR_ACL_NS_URI);

    public static final Namespace EPDCX_NS = Namespace.getNamespace(
            NamespaceConstants.EPDCX_PREFIX, NamespaceConstants.EPDCX_URI);

    public static final Namespace DEPOSIT_NS = Namespace.getNamespace(
            NamespaceConstants.DEPOSIT_PREFIX, NamespaceConstants.DEPOSIT_URI);

    public static final Namespace SKOS_NS = Namespace.getNamespace(
            NamespaceConstants.SKOS_PREFIX, NamespaceConstants.SKOS_URI);

    public static final Namespace SIMPLE_METS_PROFILE_NS = Namespace
            .getNamespace(NamespaceConstants.SIMPLE_METS_PROFILE_PREFIX,
                    NamespaceConstants.SIMPLE_METS_PROFILE_URI);

    // private constructor to prevent instantiation.
    private JDOMNamespaceUtil() {
    }

}

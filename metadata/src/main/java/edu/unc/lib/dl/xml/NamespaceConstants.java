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

/**
 * Utility class to contain various XML namespace values for the digital library
 * as constants. These relate to various metadata standards, near-standards, and
 * local prefixes. In general, namepsace constants come in pairs,
 * <code>[namespace identifier]_PREFIX</code> and
 * <code>[namespace identifier]_URI</code>. Where a <code>_URI</code> is
 * defined without a corresponding namespace, it suggests that the namespace is
 * typically used as the default in documents where it occurs.
 * <p>
 * Classes that generate XML output should use the prefixes specified here where
 * possible.
 * </p>
 *
 * @author adamc, $LastChangedBy$
 * @version $LastChangedRevision$
 * @see javax.xml.XMLConstants
 */
public final class NamespaceConstants {

    /**
     * The namespace URI for CDR Triples in RELS-EXT
     */
    // GJ: changed namespace to make it possible for model URIs to become PIDs later
    //public static final String CDR_BASEMODEL_URI = "info:cdr/xmlns/content-model/Base-1.0#";
    public static final String CDR_BASEMODEL_URI = "info:fedora/cdr-model:";

    /**
     * The CDR (Carolina Digital Repository) namespace prefix.
     */
    public static final String CDR_PREFIX = "cdr";

    /**
     * The namespace URI for CDR Triples in RELS-EXT
     */
    public static final String CDR_URI = "http://cdr.unc.edu/definitions/1.0/base-model.xml#";

    /**
     * The namespace URI for CDR Message Namespace
     */
    public static final String CDR_MESSAGE_URI = "http://cdr.unc.edu/schema/message";

    public static final String CDR_MESSAGE_PREFIX = "cdrmsg";

    /**
     * The Dublin Core namespace prefix.
     */
    public static final String DC_PREFIX = "dc";

    /**
     * The Dublin Core namespace URI.
     */
    public static final String DC_URI = "http://purl.org/dc/elements/1.1/";

    /**
     * The usual prefix for the Dublin Core terms namespace.
     */
    public static final String DCTERMS_PREFIX = "dcterms";

    /**
     * The Dublin Core Metadata Initiative Terms namespace URI.
     */
    public static final String DCTERMS_URI = "http://purl.org/dc/terms/";

    /**
     * The Fedora content model URI.
     */
    public static final String FEDORA_MODEL_URI = "info:fedora/fedora-system:def/model#";

    /**
     * The Fedora content model namespace prefix.
     */
    public static final String FEDORA_MODEL_PREFIX = "fedModel";

    /**
     * The Fedora content model URI.
     */
    public static final String FEDORA_VIEW_URI = "info:fedora/fedora-system:def/view#";

    /**
     * The Fedora content model namespace prefix.
     */
    public static final String FEDORA_VIEW_PREFIX = "fedView";

    /**
     * The URI for Fedora's FOXML namespace (note: no standard prefix).
     */
    public static final String FOXML_URI = "info:fedora/fedora-system:def/foxml#";

    /**
     * The locally added relations namespace prefix. This namespace is intended
     * to allow adding custom relations to Fedora RELS-EXT datastreams.
     */
    public static final String LOCAL_RELS_PREFIX = "ir";

    /**
     * Local relations namespace URI. This namespace is intended to allow adding
     * custom relations to Fedora RELS-EXT datastreams.
     */
    public static final String LOCAL_RELS_URI = "http://www.lib.unc.edu/ir/definitions/ir-relsext-ontology.rdfs#";

    /**
     * The METS namespace URI (note: no standard prefix).
     */
    public static final String METS_URI = "http://www.loc.gov/METS/";


    public static final String METS_PREFIX = "m";

    /**
     * The MODS v3 namespace prefix.
     */
    public static final String MODS_V3_PREFIX = "mods";

    /**
     * The MODS v3 namespace URI.
     */
    public static final String MODS_V3_URI = "http://www.loc.gov/mods/v3";

    /**
     * The namespace URI for Mulgara TQL responses (note: no standard prefix).
     */
    public static final String MULGARA_TQL_URI = "http://mulgara.org/tql#";

    /**
     * The usual prefix for OAI Dublin Core namespace
     */
    public static final String OAI_DC_PREFIX = "oai_dc";

    /**
     * URI for OAI-Dublin Core namespace.
     */
    public static final String OAI_DC_URI = "http://www.openarchives.org/OAI/2.0/oai_dc/";

    /**
     * The usual PREMIS (v2) namespace prefix.
     */
    public static final String PREMIS_V2_PREFIX = "premis";

    /**
     * The PREMIS v2 namespace URI.
     */
    public static final String PREMIS_V2_URI = "info:lc/xmlns/premis-v2";

    /**
     * The usual PREMIS (v3) namespace prefix.
     */
    public static final String PREMIS_V3_PREFIX = "premis3";

    /**
     * The PREMIS v3 namespace URI.
     */
    public static final String PREMIS_V3_URI = "http://www.loc.gov/premis/v3";

    /**
     * The RDF namespace prefix.
     */
    public static final String RDF_PREFIX = "rdf";

    /**
     * The RDF namespace URI.
     */
    public static final String RDF_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    /**
     * Fedora RELS-EXT prefix.
     */
    public static final String RELSEXT_PREFIX = "fedRelsExt";

    /**
     * The Fedora RELS-EXT namespace URI.
     */
    public static final String RELSEXT_URI = "info:fedora/fedora-system:def/relations-external#";

    public static final String SCHEMATRON_ISO_PREFIX = "schematron";

    /**
     * ISO Schematron namespace as defined in ISO/IEC FDIS 19757-3
     */
    public static final String SCHEMATRON_ISO_URI = "http://purl.oclc.org/dsdl/schematron";

    public static final String SCHEMATRON_VALIDATION_REPORT_PREFIX = "svrl";

    /**
     * ISO Schematron Validation Report Language as defined in ISO/IEC FDIS
     * 19757-3, Annex D.
     */
    public static final String SCHEMATRON_VALIDATION_REPORT_URI = "http://purl.oclc.org/dsdl/svrl";

    /**
     * The usual XLink namespace prefix
     */
    public static final String XLINK_PREFIX = "xlink";

    /**
     * The XLink namespace URI.
     */
    public static final String XLINK_URI = "http://www.w3.org/1999/xlink";

    /**
     * The usual Atom namespace prefix
     */
    public static final String ATOM_PREFIX = "atom";

    /**
     * The Atom namespace URI.
     */
    public static final String ATOM_URI = "http://www.w3.org/2005/Atom";

    /**
     * The usual File Information Tool Set (FITS) output namespace prefix
     */
    public static final String FITS_PREFIX = "fits";

    /**
     * The File Information Tool Set (FITS) output namespace URI.
     */
    public static final String FITS_URI = "http://hul.harvard.edu/ois/xml/ns/fits/fits_output";


    public static final String CDR_MESSAGE_AUTHOR_URI = "http://cdr.lib.unc.edu/schema/message#author";

    public static final String CDR_ROLES_PREFIX = "cdr-role";

    public static final String CDR_ROLE_NS_URI = "http://cdr.unc.edu/definitions/roles#";

    public static final String CDR_ACL_PREFIX = "cdr-acl";

    public static final String CDR_ACL_NS_URI = "http://cdr.unc.edu/definitions/acl#";

    public static final String EPDCX_PREFIX = "epdcx";

    public static final String EPDCX_URI = "http://purl.org/eprint/epdcx/2006-11-16/";

    public static final String DEPOSIT_PREFIX = "deposit";

    public static final String DEPOSIT_URI = "http://cdr.unc.edu/definitions/deposit/";

    public static final String SKOS_PREFIX = "skos";

    public static final String SKOS_URI = "http://www.w3.org/2004/02/skos/core#";

    public static final String SIMPLE_METS_PROFILE_URI = "http://cdr.unc.edu/METS/profiles/Simple";

    public static final String SIMPLE_METS_PROFILE_PREFIX = "simple";

    // Prevent instantiation.
    private NamespaceConstants() {
    }

}

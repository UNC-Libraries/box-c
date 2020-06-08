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
package edu.unc.lib.dcr.migration.fcrepo3;

import static edu.unc.lib.dl.util.RDFModelUtil.createModel;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.FOXML_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.RDF_NS;
import static java.util.stream.Collectors.toSet;
import static org.apache.jena.rdf.model.ResourceFactory.createProperty;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

import edu.unc.lib.dl.exceptions.RepositoryException;

/**
 * Utility methods for pulling data from foxml documents
 *
 * @author bbpennel
 */
public class FoxmlDocumentHelpers {
    public static final String RELS_EXT_DS = "RELS-EXT";
    public static final String MODS_DS = "MD_DESCRIPTIVE";
    public static final String FITS_DS = "MD_TECHNICAL";
    public static final String DC_DS = "DC";
    public static final String PREMIS_DS = "MD_EVENTS";
    public static final String MANIFEST_DS = "DATA_MANIFEST";
    public static final String ORIGINAL_DS = "DATA_FILE";

    private FoxmlDocumentHelpers() {
    }

    /**
     * List the names of all datastreams present in the foxml
     *
     * @param foxml
     * @return list of datastream names
     */
    public static Set<String> listDatastreams(Document foxml) {
        return foxml.getRootElement().getChildren("datastream", FOXML_NS).stream()
            .map(ds -> ds.getAttributeValue("ID"))
            .collect(toSet());
    }

    /**
     * Retrieve the properties and relationships for the object represented by
     * the provided foxml as a model.
     *
     * @param foxml foxml document
     * @return model containing properties, with the repository URI of the object as the resource.
     */
    public static Model getObjectModel(Document foxml) {
        // Load the relations from RELS-EXT into the model
        Element relsContentEl = getDatastreamElByName(foxml, RELS_EXT_DS)
            .getChild("datastreamVersion", FOXML_NS)
            .getChild("xmlContent", FOXML_NS)
            .getChild("RDF", RDF_NS);

        Model model;
        try {
            model = createModel(convertRelsExtToStream(relsContentEl), "RDF/XML");
        } catch (IOException e) {
            throw new RepositoryException("Failed to serialize RELS-EXT", e);
        }

        // Swap out the fedora3 uri for the current form
        String foxmlPid = getFoxmlPid(foxml);
        String relsRescUri = "info:fedora/" + foxmlPid.toLowerCase();
        Resource resc = model.getResource(relsRescUri);

        // Add the object properties to the model
        for (Element propEl : foxml.getRootElement().getChild("objectProperties", FOXML_NS).getChildren()) {
            String name = propEl.getAttributeValue("NAME");
            String value = propEl.getAttributeValue("VALUE");
            resc.addLiteral(createProperty(name), value);
        }

        return model;
    }

    /**
     * Get a datastream element by name field from the provided foxml
     *
     * @param foxml
     * @param name
     * @return
     */
    public static Element getDatastreamElByName(Document foxml, String name) {
        return foxml.getRootElement().getChildren("datastream", FOXML_NS).stream()
                .filter(ds -> name.equals(ds.getAttributeValue("ID")))
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieve the id of the object in this foxml.
     *
     * @param foxml
     * @return
     */
    public static String getFoxmlPid(Document foxml) {
        return foxml.getRootElement().getAttributeValue("PID");
    }

    public static InputStream convertRelsExtToStream(Element el) throws IOException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        new XMLOutputter().output(el, outStream);
        String foxmlText = outStream.toString();

        // Make sure UUID is lowercase
        int lastIndex = 0;
        StringBuilder output = new StringBuilder();
        Matcher matcher = Pattern.compile("(uuid.*?\")").matcher(foxmlText);

        while (matcher.find()) {
            output.append(foxmlText, lastIndex, matcher.start())
                    .append(matcher.group(1).toLowerCase());

            lastIndex = matcher.end();
        }

        if (lastIndex < foxmlText.length()) {
            output.append(foxmlText, lastIndex, foxmlText.length());
        }

        return new ByteArrayInputStream(output.toString().getBytes());
    }

    /**
     * Generate a list of datastream versions for the specified datastream,
     * ordered from oldest to newest.
     *
     * @param foxml
     * @param name
     * @return
     */
    public static List<DatastreamVersion> listDatastreamVersions(Document foxml, String name) {
        Element dsEl = getDatastreamElByName(foxml, name);
        if (dsEl == null) {
            return null;
        }

        List<DatastreamVersion> result = new ArrayList<>();
        for (Element dsvEl: dsEl.getChildren("datastreamVersion", FOXML_NS)) {
            String versionName = dsvEl.getAttributeValue("ID");
            String mimeType = dsvEl.getAttributeValue("MIMETYPE");
            String size = dsvEl.getAttributeValue("SIZE");
            String created = dsvEl.getAttributeValue("CREATED");
            Element digestEl = dsvEl.getChild("contentDigest", FOXML_NS);
            String md5 = digestEl == null ? null : digestEl.getAttributeValue("DIGEST");
            String altIds = dsvEl.getAttributeValue("ALT_IDS");

            DatastreamVersion dsVersion = new DatastreamVersion(md5, name, versionName,
                    created, size, mimeType, altIds);

            Element bodyEl = dsvEl.getChild("xmlContent", FOXML_NS);
            if (bodyEl != null) {
                dsVersion.setBodyEl(bodyEl.getChildren().get(0));
            }

            result.add(dsVersion);
        }

        result.sort((dsV1, dsV2) -> dsV1.getCreated().compareTo(dsV2.getCreated()));

        return result;
    }
}

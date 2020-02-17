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

import static edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentHelpers.DC_DS;
import static edu.unc.lib.dcr.migration.fcrepo3.FoxmlDocumentHelpers.RELS_EXT_DS;
import static edu.unc.lib.dl.util.RDFModelUtil.streamModel;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.DC_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.FOXML_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.OAI_DC_NS;
import static edu.unc.lib.dl.xml.SecureXMLFactory.createSAXBuilder;
import static java.util.Arrays.asList;
import static org.apache.jena.riot.RDFFormat.RDFXML_PRETTY;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.rdf.model.Model;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import edu.unc.lib.dcr.migration.fcrepo3.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.exceptions.RepositoryException;
import edu.unc.lib.dl.fedora.PID;

/**
 * @author bbpennel
 */
public class FoxmlDocumentBuilder {
    public static final String DEFAULT_CREATED_DATE = "2011-10-04T20:36:44.902Z";
    public static final String DEFAULT_LAST_MODIFIED = "2013-10-06T10:16:44.111Z";

    private PID pid;
    private String label;
    private String dcTitle;
    private String createdDate;
    private String lastModifiedDate;
    private String state;

    private Model relsExtModel;
    Map<String, List<DatastreamVersion>> datastreamVersions;

    public FoxmlDocumentBuilder(PID pid, String label) {
        this.pid = pid;
        this.label = label;
        this.dcTitle = label;
        this.state = "Active";
        this.createdDate = DEFAULT_CREATED_DATE;
        this.lastModifiedDate = DEFAULT_LAST_MODIFIED;
        this.datastreamVersions = new HashMap<>();
        withInternalXmlDatastream("AUDIT", new Element("xmlContent", FOXML_NS));
    }

    public FoxmlDocumentBuilder createdDate(String created) {
        this.createdDate = created;
        return this;
    }

    public FoxmlDocumentBuilder lastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
        return this;
    }

    public FoxmlDocumentBuilder state(String state) {
        this.state = state;
        return this;
    }

    public FoxmlDocumentBuilder label(String label) {
        this.label = label;
        return this;
    }

    public FoxmlDocumentBuilder withInternalXmlDatastream(String dsName, Element bodyEl) {
        DatastreamVersion ds = new DatastreamVersion(null, dsName, dsName + ".0", DEFAULT_CREATED_DATE, "55",
                "text/xml", null);
        ds.setBodyEl(bodyEl);
        datastreamVersions.put(dsName, asList(ds));

        return this;
    }

    public FoxmlDocumentBuilder withDatastreamVersions(String dsName, List<DatastreamVersion> dsVersions) {
        datastreamVersions.put(dsName, dsVersions);
        return this;
    }

    public FoxmlDocumentBuilder withDatastreamVersion(DatastreamVersion dsVersion) {
        datastreamVersions.put(dsVersion.getDsName(), asList(dsVersion));
        return this;
    }

    public FoxmlDocumentBuilder relsExtModel(Model relsExtModel) {
        this.relsExtModel = relsExtModel;
        return this;
    }

    public Document build() {
        Document foxml = new Document();
        Element foxmlEl = new Element("digitalObject", FOXML_NS)
                .setAttribute("PID", "uuid:" + pid.getId());
        foxml.addContent(foxmlEl);

        // Populate all the object properties
        foxmlEl.addContent(new Element("objectProperties", FOXML_NS)
                .addContent(addObjectProperty(FedoraProperty.state, state))
                .addContent(addObjectProperty(FedoraProperty.label, label))
                .addContent(addObjectProperty(FedoraProperty.ownerId, ""))
                .addContent(addObjectProperty(FedoraProperty.createdDate, createdDate))
                .addContent(addObjectProperty(FedoraProperty.lastModifiedDate, lastModifiedDate)));

        if (relsExtModel != null) {
            try {
                InputStream modelStream = streamModel(relsExtModel, RDFXML_PRETTY);
                Document relsDoc = createSAXBuilder().build(modelStream);
                withInternalXmlDatastream(RELS_EXT_DS, relsDoc.detachRootElement());
            } catch (JDOMException | IOException e) {
                throw new RepositoryException("Failed to stream model", e);
            }
        }

        if (!datastreamVersions.containsKey(DC_DS)) {
            Document dcDoc = new Document()
                    .addContent(new Element("dc", OAI_DC_NS)
                            .addContent(new Element("title", DC_NS).setText(dcTitle)));
            withInternalXmlDatastream(DC_DS, dcDoc.detachRootElement());
        }

        for (Entry<String, List<DatastreamVersion>> dsEntry: datastreamVersions.entrySet()) {
            if (dsEntry.getValue() == null) {
                continue;
            }

            Element dsEl = new Element("datastream", FOXML_NS)
                    .setAttribute("ID", dsEntry.getKey())
                    .setAttribute("STATE", "A")
                    .setAttribute("CONTROL_GROUP", "X")
                    .setAttribute("VERSIONABLE", "true");
            foxmlEl.addContent(dsEl);

            for (DatastreamVersion dsVersion: dsEntry.getValue()) {
                Element dsvEl = new Element("datastreamVersion", FOXML_NS)
                        .setAttribute("ID", dsVersion.getVersionName())
                        .setAttribute("CREATED", dsVersion.getCreated())
                        .setAttribute("MIMETYPE", dsVersion.getMimeType())
                        .setAttribute("SIZE", dsVersion.getSize());
                if (dsVersion.getAltIds() != null) {
                    dsvEl.setAttribute("ALT_IDS", dsVersion.getAltIds());
                }
                dsEl.addContent(dsvEl);

                if (dsVersion.getMd5() != null) {
                    dsvEl.addContent(new Element("contentDigest", FOXML_NS)
                            .setAttribute("TYPE", "MD5")
                            .setAttribute("DIGEST", dsVersion.getMd5()));
                }

                if (dsVersion.getBodyEl() != null) {
                    dsvEl.addContent(new Element("xmlContent", FOXML_NS)
                            .addContent(dsVersion.getBodyEl().detach()));
                } else {
                    String ref = "uuid:" + pid.getId() + "+" + dsVersion.getDsName()
                            + "+" + dsVersion.getVersionName();
                    dsvEl.addContent(new Element("contentLocation", FOXML_NS)
                            .setAttribute("TYPE", "INTERNAL_ID")
                            .setAttribute("REF", ref));
                }
            }
        }

        return foxml;
    }

    private Element addObjectProperty(FedoraProperty prop, String value) {
        return new Element("property", FOXML_NS)
                .setAttribute("NAME", prop.getProperty().toString())
                .setAttribute("VALUE", value);
    }
}

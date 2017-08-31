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
package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.METS_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.MODS_V3_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.XLINK_NS;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrAcl;
import edu.unc.lib.dl.rdf.CdrDeposit;

/**
 *
 * @author bbpennel
 *
 */
public class CDRMETSGraphExtractor {
    public static final Logger LOG = LoggerFactory.getLogger(CDRMETSGraphExtractor.class);
    public static final Namespace METS_ACL_NS = Namespace.getNamespace("acl", "http://cdr.unc.edu/definitions/acl");

    private static Map<String, URI> containerTypes = new HashMap<>();
    static {
        containerTypes.put("Folder", URI.create(Cdr.Folder.getURI()));
        containerTypes.put("AdminUnit", URI.create(Cdr.AdminUnit.getURI()));
        containerTypes.put("Collection", URI.create(Cdr.Collection.getURI()));
        containerTypes.put("Aggregate Work", URI.create(Cdr.Work.getURI()));
        containerTypes.put("Work", URI.create(Cdr.Work.getURI()));
        containerTypes.put("SWORD Object", URI.create(Cdr.Work.getURI()));
    }

    private PID depositId = null;
    METSHelper helper = null;
    private Document mets = null;

    public CDRMETSGraphExtractor(Document mets, PID depositId) {
        this.depositId = depositId;
        this.mets = mets;
        this.helper = new METSHelper(mets);
    }

    public void addArrangement(Model m) {
        addDivProperties(m);
        LOG.info("Added DIV properties");
        addStructLinkProperties(m);
        LOG.info("Added struct link properties");
        addContainerTriples(m);
    }

    private void addDivProperties(Model m) {
        Iterator<Element> divs = helper.getDivs();
        while (divs.hasNext()) {
            Element div = divs.next();
            String pid = METSHelper.getPIDURI(div);
            Resource o = m.createResource(pid);
            if (div.getAttributeValue("LABEL") != null) {
                m.add(o, CdrDeposit.label, div.getAttributeValue("LABEL"));
            }
            String orig = METSHelper.getOriginalURI(div);
            if (orig != null) {
                m.add(o, CdrDeposit.originalLocation, m.getResource(orig));
            }
        }
    }

    private void addStructLinkProperties(Model m) {
        if (mets.getRootElement().getChild("structLink", METS_NS) == null) {
            return;
        }
        for (Object e : mets.getRootElement().getChild("structLink", METS_NS)
                .getChildren()) {
            if (!(e instanceof Element)) {
                continue;
            }
            Element link = (Element) e;
            String from = link.getAttributeValue("from", XLINK_NS);
            String arcrole = link.getAttributeValue("arcrole", XLINK_NS);
            String to = link.getAttributeValue("to", XLINK_NS);
            if ("http://cdr.unc.edu/definitions/1.0/base-model.xml#hasAlphabeticalOrder"
                    .equals(arcrole)) {
                // TODO: handle alphabetic sorting
            } else {
                Resource fromR = m.createResource(helper.getPIDURIForDIVID(from));
                Resource toR = m.createResource(helper.getPIDURIForDIVID(to));
                Property role = m.createProperty(arcrole);
                m.add(fromR, role, toR);
            }
        }
    }

    public void saveDescriptions(FilePathFunction f) {
        Iterator<Element> divs = helper.getDivs();
        while (divs.hasNext()) {
            Element div = divs.next();
            String dmdid = div.getAttributeValue("DMDID");
            if (dmdid == null) {
                continue;
            }
            Element dmdSecEl = helper.getElement(dmdid);
            if (dmdSecEl == null) {
                continue;
            }
            Element modsEl = dmdSecEl.getChild("mdWrap", METS_NS)
                    .getChild("xmlData", METS_NS).getChild("mods", MODS_V3_NS);
            String pid = METSHelper.getPIDURI(div);
            String path = f.getPath(pid);

            try (OutputStream fos = new FileOutputStream(path)) {
                Document mods = new Document();
                mods.setRootElement(modsEl.detach());
                new XMLOutputter(Format.getPrettyFormat()).output(mods, fos);
            } catch (IOException e) {
                throw new Error("unexpected exception", e);
            }
        }
    }

    private void addContainerTriples(Model m) {
        // add deposit-level parent (represented as structMap or bag div)
        Element topContainer = mets.getRootElement().getChild(
                "structMap", METS_NS);
        Element firstdiv = topContainer.getChild("div", METS_NS);
        if (firstdiv != null
                && "bag".equals(firstdiv.getAttributeValue("TYPE")
                        .toLowerCase())) {
            topContainer = firstdiv;
        }
        Bag top = m.createBag(depositId.getURI());
        List<Element> topchildren = topContainer.getChildren(
                "div", METS_NS);
        for (Element childEl : topchildren) {
            Resource child = m.createResource(METSHelper.getPIDURI(childEl));
            top.add(child);
        }

        Iterator<Element> divs = helper.getDivs();
        while (divs.hasNext()) {
            // FIXME detect Baq or Seq from model
            Element div = divs.next();
            String type = div.getAttributeValue("TYPE");
            if (type != null && "bag".equals(type.toLowerCase())) {
                continue;
            }
            List<Element> children = div.getChildren("div",
                    METS_NS);

            if (type == null) {
                if (children.size() > 0) {
                    type = "Folder";
                } else {
                    type = "File";
                }
            }

            if (containerTypes.keySet().contains(type)) {
                // add any children
                Bag parent = m.createBag(METSHelper.getPIDURI(div));
                for (Element childEl : children) {
                    Resource child = m.createResource(METSHelper
                            .getPIDURI(childEl));
                    parent.add(child);
                }
                // Set container type
                m.add(parent, RDF.type, m.createResource(containerTypes
                        .get(type).toString()));
            } else if (type.equals("File")) {
                // Type was file, so store FileObject type
                Resource fileResc = m.getResource(METSHelper.getPIDURI(div));
                m.add(fileResc, RDF.type, Cdr.FileObject);
            }
        }
    }

    public void addAccessControls(Model m) {
        Iterator<Element> divs = helper.getDivs();
        while (divs.hasNext()) {
            Element div = divs.next();
            Resource object = m.createResource(METSHelper.getPIDURI(div));
            if (div.getAttributeValue("ADMID") != null) {
                Element rightsMdEl = helper.getElement(div
                        .getAttributeValue("ADMID"));
                Element aclEl = rightsMdEl.getChild("mdWrap", METS_NS)
                        .getChild("xmlData", METS_NS)
                        .getChild("accessControl", METS_ACL_NS);

                // TODO: need to revisit this; isPublished, when "false" record "no"
                /**String publishedVal = aclEl.getAttributeValue("published",
                        METS_ACL_NS);
                if ("false".equals(publishedVal)) {
                    Property published = m
                            .createProperty(ContentModelHelper.CDRProperty.isPublished
                                    .getURI().toString());
                    m.add(object, published, "no");
                } */

                // embargo, converts date to dateTime
                String embargoUntilVal = aclEl.getAttributeValue(
                        "embargo-until", METS_ACL_NS);
                if (embargoUntilVal != null) {
                    m.add(object, CdrAcl.embargoUntil, embargoUntilVal + "T00:00:00",
                            XSDDatatype.XSDdateTime);
                }

                String patronAccessVal = aclEl.getAttributeValue(
                        "patronAccess", METS_ACL_NS);
                if (patronAccessVal != null) {
                    m.add(object, CdrAcl.patronAccess, patronAccessVal);
                }

                // add grants to groups
                for (Object o : aclEl.getChildren("grant", METS_ACL_NS)) {
                    Element grant = (Element) o;
                    String role = grant.getAttributeValue("role", METS_ACL_NS);
                    String group = grant.getAttributeValue("group", METS_ACL_NS);
                    String roleURI = CdrAcl.NS + role;
                    LOG.debug("Found grant of role {} with group {}", roleURI, group);
                    Property roleProp = m.createProperty(roleURI);
                    m.add(object, roleProp, group);
                }

            }
        }
    }
}

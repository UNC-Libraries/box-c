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
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.XLINK_NS;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;

import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

/**
 *
 * @author bbpennel
 *
 */
public class METSHelper {

    protected Document mets;

    public METSHelper(Document mets) {
        this.mets = mets;
        initIdMap();
    }

    protected Map<String, Element> elementsById = null;

    protected static String getPIDURI(Element div) {
        String cids = div.getAttributeValue("CONTENTIDS");
        for (String s : cids.split("\\s")) {
            if (PIDs.get(s) != null) {
                return s;
            }
        }
        return null;
    }

    protected static String getOriginalURI(Element div) {
        String cids = div.getAttributeValue("CONTENTIDS");
        for (String s : cids.split("\\s")) {
            if (PIDs.get(s) != null) {
                return s;
            }
        }
        return null;
    }

    protected void initIdMap() {
        elementsById = new HashMap<>();
        Iterator<Element> els = mets.getRootElement()
                .getDescendants(new ElementFilter());
        while (els.hasNext()) {
            Element el = els.next();
            String id = el.getAttributeValue("ID");
            if (id != null) {
                elementsById.put(id, el);
            }
        }
    }

    protected Element getElement(String id) {
        return elementsById.get(id);
    }

    protected Iterator<Element> getDivs() {
        // add deposit-level parent (represented as structMap or bag div)
        Element topContainer = mets.getRootElement().getChild(
                "structMap", METS_NS);
        Element firstdiv = topContainer.getChild("div", METS_NS);
        if (firstdiv != null
                && "Bag".equals(firstdiv.getAttributeValue("TYPE"))) {
            topContainer = firstdiv;
        }
        Iterator<Element> divs = topContainer
                .getDescendants(new ElementFilter("div", JDOMNamespaceUtil.METS_NS));
        return divs;
    }

    protected Iterator<Element> getFptrs() {
        Iterator<Element> fptrs = mets.getRootElement()
                .getChild("structMap", METS_NS)
                .getDescendants(new ElementFilter("fptr", JDOMNamespaceUtil.METS_NS));
        return fptrs;
    }

    protected String getPIDURI(String id) {
        return getPIDURI(elementsById.get(id));
    }

    public void addFileAssociations(Model m, boolean prependDataPath) {
        // for every fptr
        Iterator<Element> fptrs = getFptrs();
        while (fptrs.hasNext()) {
            Element fptr = fptrs.next();
            String fileId = fptr.getAttributeValue("FILEID");
            Element div = fptr.getParentElement();
            String pid = METSHelper.getPIDURI(div);
            Element fileEl = getElement(fileId);
            @SuppressWarnings("unused") // only 1 USE supported
            String use = fileEl.getAttributeValue("USE"); // may be null
            Element flocat = fileEl.getChild("FLocat", METS_NS);
            String href = flocat.getAttributeValue("href", XLINK_NS);
            if (prependDataPath && !href.contains(":")) {
                href = "data/" + href;
            }
            Resource object = m.createResource(pid);

            // record object source data file
            // only supporting one USE in fileSec, i.e. source data

            // record file location
            m.add(object, CdrDeposit.stagingLocation, href);

            m.add(object, RDF.type, Cdr.FileObject);

            // record mimetype
            if (fileEl.getAttributeValue("MIMETYPE") != null) {
                m.add(object, CdrDeposit.mimetype, fileEl.getAttributeValue("MIMETYPE"));
            }

            // record File checksum if supplied, we only support MD5 in Simple profile
            if (fileEl.getAttributeValue("CHECKSUM") != null) {
                m.add(object, CdrDeposit.md5sum, fileEl.getAttributeValue("CHECKSUM"));
            }

            // record SIZE (bytes/octets)
            if (fileEl.getAttributeValue("SIZE") != null) {
                m.add(object, CdrDeposit.size, fileEl.getAttributeValue("SIZE"));
            }

            // record CREATED (iso8601)
            if (fileEl.getAttributeValue("CREATED") != null) {
                m.add(object, CdrDeposit.createTime, fileEl.getAttributeValue("CREATED"), XSDDatatype.XSDdateTime);
            }

        }
    }

    public String getPIDURIForDIVID(String ref) {
        if (ref.startsWith("#")) {
            ref = ref.substring(1);
        }
        Iterator<Element> i = getDivs();
        while (i.hasNext()) {
            Element div = i.next();
            if (ref.equals(div.getAttributeValue("ID"))) {
                return getPIDURI(div);
            }
        }
        return null;
    }

}
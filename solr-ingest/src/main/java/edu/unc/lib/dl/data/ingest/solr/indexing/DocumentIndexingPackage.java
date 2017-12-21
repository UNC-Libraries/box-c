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
package edu.unc.lib.dl.data.ingest.solr.indexing;

import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;

import edu.unc.lib.dl.acl.util.ObjectAccessControlsBean;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.util.ContentModelHelper.FedoraProperty;
import edu.unc.lib.dl.util.ResourceType;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;
import edu.unc.lib.dl.xml.JDOMQueryUtil;

/**
 *
 * @author bbpennel
 *
 */
public class DocumentIndexingPackage {
    private DocumentIndexingPackageDataLoader loader;

    private PID pid;
    private PID parentPid;
    private DocumentIndexingPackage parentDocument;
    private boolean attemptedToRetrieveDefaultWebObject;
    private DocumentIndexingPackage defaultWebObject;
    private String defaultWebData;
    private Document foxml;
    private Element mods;
    private Element mdContents;
    private Boolean isPublished;
    private Boolean isDeleted;
    private IndexDocumentBean document;
    private String label;
    private ResourceType resourceType;
    private List<PID> children;
    private Map<String, List<String>> triples;
    private ObjectAccessControlsBean aclBean;

    public DocumentIndexingPackage(PID pid, DocumentIndexingPackage parentDip,
            DocumentIndexingPackageDataLoader loader) {
        document = new IndexDocumentBean();
        this.pid = pid;
        document.setId(pid.getId());
        this.attemptedToRetrieveDefaultWebObject = false;
        this.parentDocument = parentDip;
        if (parentDip != null) {
            this.parentPid = parentDip.parentPid;
        }
        this.loader = loader;
    }

    public PID getPid() {
        return pid;
    }

    public void setPid(PID pid) {
        this.pid = pid;
    }

    public ContentObject getContentObject() throws IndexingException {
        return loader.getContentObject(this);
    }

    public DocumentIndexingPackage getParentDocument() throws IndexingException {
        if (parentDocument == null) {
            parentDocument = loader.loadParentDip(this);
        }

        return parentDocument;
    }

    public boolean hasParentDocument() {
        return parentDocument != null;
    }

    public void setParentDocument(DocumentIndexingPackage parentDocument) {
        this.parentDocument = parentDocument;
    }

    public PID getParentPid() throws IndexingException {
        if (parentPid == null) {
            parentPid = loader.loadParentPid(this);
        }
        return parentPid;
    }

    public boolean isAttemptedToRetrieveDefaultWebObject() {
        return attemptedToRetrieveDefaultWebObject;
    }

    public void setAttemptedToRetrieveDefaultWebObject(boolean attemptedToRetrieveDefaultWebObject) {
        this.attemptedToRetrieveDefaultWebObject = attemptedToRetrieveDefaultWebObject;
    }

    public DocumentIndexingPackage getDefaultWebObject() throws IndexingException {
        if (defaultWebObject == null && !attemptedToRetrieveDefaultWebObject) {
            attemptedToRetrieveDefaultWebObject = true;
            defaultWebObject = loader.loadDefaultWebObject(this);
        }
        return defaultWebObject;
    }

    public void setDefaultWebObject(DocumentIndexingPackage defaultWebObject) {
        this.defaultWebObject = defaultWebObject;
    }

    public String getDefaultWebData() throws IndexingException {
        if (defaultWebData == null) {
            defaultWebData = loader.loadDefaultWebData(this);
        }

        return defaultWebData;
    }

    public void setDefaultWebData(String defaultWebData) {
        this.defaultWebData = defaultWebData;
    }
    @Deprecated
    public Document getFoxml() throws IndexingException {
        if (foxml == null) {
            foxml = loader.loadFOXML(this);
        }
        return foxml;
    }
    @Deprecated
    public void setFoxml(Document foxml) {
        this.foxml = foxml;
    }
    @Deprecated
    public boolean hasFoxml() {
        return foxml != null;
    }

    public IndexDocumentBean getDocument() {
        return document;
    }

    public void setDocument(IndexDocumentBean document) {
        this.document = document;
    }

    public Element getMods() throws IndexingException {
        if (mods == null) {
            mods = loader.loadMods(this);
        }
        return mods;
    }

    public void setMods(Element mods) {
        this.mods = mods;
    }

    public Element getMdContents() throws IndexingException {
        if (mdContents == null) {
            mdContents = loader.loadMDContents(this);
        }
        return mdContents;
    }

    public void setMdContents(Element mdContents) {
        this.mdContents = mdContents;
    }

    public Long getDisplayOrder(String pid) throws NumberFormatException, IndexingException {
        Element mdContents = getMdContents();
        if (mdContents == null) {
            return null;
        }
        Element containerDiv = mdContents.getChild("div", JDOMNamespaceUtil.METS_NS);
        Element orderDiv = JDOMQueryUtil.getChildByAttribute(
                containerDiv, "div", JDOMNamespaceUtil.METS_NS, "ID", pid);
        if (orderDiv != null) {
            Long order = new Long(orderDiv.getAttributeValue("ORDER"));
            return order;
        }
        return null;
    }

    public String getLabel() throws IndexingException {
        if (label == null) {
            Map<String, List<String>> triples = getTriples();
            if (triples != null) {
                this.label = triples.get(FedoraProperty.label.toString()).get(0);
            }
        }
        return label;
    }

    public List<PID> getChildren() throws IndexingException {
        if (children == null) {
            children = loader.loadChildren(this);
        }
        return children;
    }

    public void setChildren(List<PID> children) {
        this.children = children;
    }

    public Map<String, List<String>> getTriples() throws IndexingException {
        if (triples == null) {
            triples = loader.loadTriples(this);
        }
        return triples;
    }

    public String getFirstTriple(String uri) throws IndexingException {
        Map<String, List<String>> triples = getTriples();

        List<String> tripleList = triples.get(uri);

        if (tripleList == null || tripleList.size() == 0) {
            return null;
        }

        return tripleList.get(0);
    }

    public void setTriples(Map<String, List<String>> triples) {
        this.triples = triples;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Deprecated
    public ResourceType getResourceType() {
        return resourceType;
    }

    @Deprecated
    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public ObjectAccessControlsBean getAclBean() throws IndexingException {
        if (aclBean == null) {
            aclBean = loader.loadAccessControlBean(this);
        }
        return aclBean;
    }

    public boolean hasAclBean() {
        return aclBean != null;
    }

    public void setAclBean(ObjectAccessControlsBean aclBean) {
        this.aclBean = aclBean;
    }

    public void setDataLoader(DocumentIndexingPackageDataLoader dataLoader) {
        loader = dataLoader;
    }
}

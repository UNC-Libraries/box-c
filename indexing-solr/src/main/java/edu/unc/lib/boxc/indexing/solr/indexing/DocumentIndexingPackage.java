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
package edu.unc.lib.boxc.indexing.solr.indexing;

import org.jdom2.Element;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;

/**
 *
 * @author bbpennel
 *
 */
public class DocumentIndexingPackage {
    private DocumentIndexingPackageDataLoader loader;

    private PID pid;
    private PID parentPid;
    private Element mods;
    private IndexDocumentBean document;
    private ContentObject contentObject;

    public DocumentIndexingPackage(PID pid, DocumentIndexingPackage parentDip,
            DocumentIndexingPackageDataLoader loader) {
        document = new IndexDocumentBean();
        this.pid = pid;
        document.setId(pid.getId());
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
        if (contentObject == null) {
            contentObject = loader.getContentObject(this);
        }
        return contentObject;
    }

    public void setContentObject(ContentObject contentObject) {
        this.contentObject = contentObject;
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
}

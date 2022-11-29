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

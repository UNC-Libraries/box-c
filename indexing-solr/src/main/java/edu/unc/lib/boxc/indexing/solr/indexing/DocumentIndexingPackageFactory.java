package edu.unc.lib.boxc.indexing.solr.indexing;

import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

/**
 * @author bbpennel
 * @date Jun 24, 2015
 */
public class DocumentIndexingPackageFactory {

    @Autowired
    private DocumentIndexingPackageDataLoader dataLoader;

    public DocumentIndexingPackage createDip(String pid) {
        return new DocumentIndexingPackage(PIDs.get(pid), null, dataLoader);
    }

    public DocumentIndexingPackage createDip(PID pid) {
        return new DocumentIndexingPackage(pid, null, dataLoader);
    }

    public DocumentIndexingPackage createDip(PID pid, DocumentIndexingPackage parentDip) {
        return new DocumentIndexingPackage(pid, parentDip, dataLoader);
    }

    public void setDataLoader(DocumentIndexingPackageDataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

}

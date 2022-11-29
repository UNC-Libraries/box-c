package edu.unc.lib.boxc.indexing.solr.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;

/**
 * Updates or adds the metadata for a single object
 *
 * @author bbpennel
 *
 */
public class UpdateObjectAction extends AbstractIndexingAction {
    final Logger log = LoggerFactory.getLogger(UpdateObjectAction.class);

    @Override
    public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
        log.debug("Indexing object {}", updateRequest.getPid());
        // Retrieve object metadata from Fedora and add to update document list
        DocumentIndexingPackage dip = updateRequest.getDocumentIndexingPackage();
        if (dip == null) {
            dip = factory.createDip(updateRequest.getPid());
            updateRequest.setDocumentIndexingPackage(dip);
        }

        pipeline.process(dip);
        if (this.addDocumentMode) {
            solrUpdateDriver.addDocument(dip.getDocument());
        } else {
            solrUpdateDriver.updateDocument(dip.getDocument());
        }

    }
}

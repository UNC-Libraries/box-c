package edu.unc.lib.dl.data.ingest.solr.action;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;

/**
 * Performs depth first indexing of a tree of repository objects, starting at the PID of the provided update request.
 * 
 * @author bbpennel
 * 
 */
public class RecursiveTreeIndexer {
	private static final Logger log = LoggerFactory.getLogger(RecursiveTreeIndexer.class);
	
	private UpdateTreeAction action;
	private SolrUpdateRequest updateRequest;
	private boolean addDocumentMode = true;

	public RecursiveTreeIndexer(SolrUpdateRequest updateRequest, UpdateTreeAction action) {
		this.updateRequest = updateRequest;
		this.action = action;
	}
	
	public RecursiveTreeIndexer(SolrUpdateRequest updateRequest, UpdateTreeAction action, boolean addDocumentMode) {
		this.updateRequest = updateRequest;
		this.action = action;
		this.addDocumentMode = addDocumentMode;
	}

	public void index(PID pid, DocumentIndexingPackage parent) {
		try {
			DocumentIndexingPackage dip = this.action.getDocumentIndexingPackage(pid, parent);

			if (dip != null) {
				// Update the current target in solr
				this.action.getPipeline().process(dip);
				if (addDocumentMode)
					this.action.getSolrUpdateDriver().addDocument(dip.getDocument());
				else this.action.getSolrUpdateDriver().updateDocument("set", dip.getDocument());
				
				// Clear parent bond to allow memory cleanup
				dip.setParentDocument(null);

				// Update the number of objects processed in this action
				this.updateRequest.incrementChildrenProcessed();

				// Start indexing the children
				this.indexChildren(dip, dip.getChildren());
			}
		} catch (IndexingException e) {
			log.warn("Failed to index {}", pid.getPid(), e);
		}
	}
	
	public void indexChildren(DocumentIndexingPackage parent, List<PID> children) {
		if (children != null) {
			for (PID child : children) {
				this.index(child, parent);
			}
		}
	}
}
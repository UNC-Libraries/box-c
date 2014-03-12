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

	private final UpdateTreeAction action;
	private final SolrUpdateRequest updateRequest;
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
		DocumentIndexingPackage dip = null;
		try {
			dip = this.action.getDocumentIndexingPackage(pid, parent);
			if (dip == null)
				throw new IndexingException("No document indexing package was retrieved for " + pid.getPid());

			// Perform document populating pipeline
			this.action.getPipeline().process(dip);

			// Update the current target in solr
			if (addDocumentMode)
				this.action.getSolrUpdateDriver().addDocument(dip.getDocument());
			else
				this.action.getSolrUpdateDriver().updateDocument("set", dip.getDocument());

			// Update the number of objects processed in this action
			this.updateRequest.incrementChildrenProcessed();

		} catch (IndexingException e) {
			log.warn("Failed to index {} and its children", pid.getPid(), e);
			return;
		} finally {
			// Clear parent bond to allow memory cleanup
			if (dip != null)
				dip.setParentDocument(null);
		}

		// Start indexing the children
		this.indexChildren(dip, dip.getChildren());
	}

	public void indexChildren(DocumentIndexingPackage parent, List<PID> children) {
		if (children != null) {
			for (PID child : children) {
				this.index(child, parent);
			}
		}
	}
}
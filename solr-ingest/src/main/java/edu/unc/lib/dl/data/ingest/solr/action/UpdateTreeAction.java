package edu.unc.lib.dl.data.ingest.solr.action;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.data.ingest.solr.ProcessingStatus;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.util.FileUtils;
import edu.unc.lib.dl.util.TripleStoreQueryService;

public class UpdateTreeAction extends AbstractIndexingAction {
	private static final Logger log = LoggerFactory.getLogger(UpdateTreeAction.class);

	@Autowired
	protected TripleStoreQueryService tsqs;
	private String descendantsQuery;

	public void init() {
		try {
			descendantsQuery = FileUtils.inputStreamToString(this.getClass().getResourceAsStream("countDescendants.itql"));
		} catch (IOException e) {
			log.error("Failed to load queries", e);
		}
	}

	@Override
	public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
		// Perform updates
		index(updateRequest);
		updateRequest.setStatus(ProcessingStatus.FINISHED);
	}
	
	public void setTsqs(TripleStoreQueryService tsqs) {
		this.tsqs = tsqs;
	}
	
	protected void index(SolrUpdateRequest updateRequest) {
		int totalObjects = countDescendants(updateRequest.getPid()) + 1;
		updateRequest.setChildrenPending(totalObjects);

		// Start indexing
		RecursiveTreeIndexer treeIndexer = new RecursiveTreeIndexer(updateRequest);
		treeIndexer.index(updateRequest.getPid(), null);
	}

	protected int countDescendants(PID pid) {
		List<List<String>> results = tsqs.queryResourceIndex(String.format(descendantsQuery,
				this.tsqs.getResourceIndexModelUri(), pid.getURI()));
		if (results == null || results.size() == 0 || results.get(0).size() == 0)
			return 0;
		return Integer.parseInt(results.get(0).get(0));
	}

	protected DocumentIndexingPackage getDocumentIndexingPackage(PID pid, DocumentIndexingPackage parent) {
		DocumentIndexingPackage dip = dipFactory.createDocumentIndexingPackage(pid);
		dip.setParentDocument(parent);
		return dip;
	}

	/**
	 * Performs depth first indexing of a tree of repository objects, starting at the PID of the provided update request.
	 * 
	 * @author bbpennel
	 * 
	 */
	protected class RecursiveTreeIndexer {
		private SolrUpdateRequest updateRequest;

		public RecursiveTreeIndexer(SolrUpdateRequest updateRequest) {
			this.updateRequest = updateRequest;
		}

		public void index(PID pid, DocumentIndexingPackage parent) {
			DocumentIndexingPackage dip = getDocumentIndexingPackage(pid, parent);

			if (dip != null) {
				// Update the current target in solr
				pipeline.process(dip);
				solrUpdateDriver.addDocument(dip.getDocument());
				
				// Clear parent bond to allow memory cleanup
				dip.setParentDocument(null);

				// Update the number of objects processed in this action
				this.updateRequest.incrementChildrenProcessed();

				// Start indexing the children
				List<PID> children = dip.getChildren();
				if (children != null) {
					for (PID child : children) {
						this.index(child, dip);
					}
				}
			}
		}
	}
}
package edu.unc.lib.dl.data.ingest.solr.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.util.IndexingActionType;

public class IndexTreeCleanAction extends UpdateTreeAction {
	private static final Logger log = LoggerFactory.getLogger(IndexTreeCleanAction.class);

	private DeleteSolrTreeAction deleteAction;

	public IndexTreeCleanAction() {
		// Clean index doesn't make sense with update mode
		addDocumentMode = true;
	}

	@Override
	public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
		log.debug("Starting clean indexing of {}", updateRequest.getPid().getPid());

		SolrUpdateRequest deleteRequest = new SolrUpdateRequest(updateRequest.getPid().getPid(),
				IndexingActionType.DELETE_SOLR_TREE);
		deleteAction.performAction(deleteRequest);

		// Force commit to ensure delete finishes before we start repopulating
		solrUpdateDriver.commit();

		// Perform normal recursive update
		super.performAction(updateRequest);

		if (log.isDebugEnabled())
			log.debug(String.format("Finished clean indexing of {}.  {} objects updated in {}ms", updateRequest.getPid()
					.getPid(), updateRequest.getChildrenPending(),
					System.currentTimeMillis() - updateRequest.getTimeStarted()));
	}

	public void setDeleteAction(DeleteSolrTreeAction deleteAction) {
		this.deleteAction = deleteAction;
	}

	@Override
	public void setAddDocumentMode(boolean addDocumentMode) {
		// Do nothing, clean index should always be in add mode
	}
}

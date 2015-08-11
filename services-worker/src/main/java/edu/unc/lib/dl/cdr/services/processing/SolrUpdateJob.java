package edu.unc.lib.dl.cdr.services.processing;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.ChildSetRequest;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.action.IndexingAction;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.util.IndexingActionType;

public class SolrUpdateJob implements Runnable {

	private static final Logger LOG = LoggerFactory.getLogger(SolrUpdateJob.class);
	
	private SolrUpdateRequest updateRequest;
	private Map<IndexingActionType, IndexingAction> solrIndexingActionMap;
	
	public SolrUpdateJob(String pid, String action, List<String> children) {
		// we should just have two constructors -- fix SpringJobFactory?
		if (children == null) {
			this.updateRequest = new SolrUpdateRequest(pid, IndexingActionType.getAction(action));
		} else {
			this.updateRequest = new ChildSetRequest(pid, children, IndexingActionType.getAction(action));
		}
	}

	public void setSolrIndexingActionMap(Map<IndexingActionType, IndexingAction> solrIndexingActionMap) {
		this.solrIndexingActionMap = solrIndexingActionMap;
	}

	@Override
	public void run() {
		try {
			IndexingAction indexingAction = this.solrIndexingActionMap.get(updateRequest.getUpdateAction());
			if (indexingAction != null) {
				LOG.info("Performing action {} on object {}", updateRequest.getUpdateAction(), updateRequest.getTargetID());
				indexingAction.performAction(updateRequest);
			}
		} catch (IndexingException e) {
			LOG.error("Error attempting to perform action " + updateRequest.getAction() + " on object " + updateRequest.getTargetID(), e);
		}
	}

}

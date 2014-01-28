package edu.unc.lib.dl.data.ingest.solr.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

/**
 * Performs an update of an object and all of its descendants. After they have all updated, any descendants which were
 * not updated (thereby indicating they have been deleted or removed) will be removed from the index.
 * 
 * @author bbpennel
 * 
 */
public class IndexTreeInplaceAction extends UpdateTreeAction {
	private static final Logger log = LoggerFactory.getLogger(IndexTreeInplaceAction.class);

	@Autowired
	private SolrSettings solrSettings;

	@Override
	public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
		log.debug("Starting inplace indexing of {}", updateRequest.getPid().getPid());

		super.performAction(updateRequest);

		// Force commit the updates currently staged
		solrUpdateDriver.commit();
		// Cleanup any objects in the tree that were no updated
		this.deleteStaleChildren(updateRequest);

		if (log.isDebugEnabled())
			log.debug(String.format("Finished inplace indexing of {}.  {} objects updated in {}ms", updateRequest.getPid()
					.getPid(), updateRequest.getChildrenPending(),
					System.currentTimeMillis() - updateRequest.getTimeStarted()));
	}

	public void deleteStaleChildren(SolrUpdateRequest updateRequest) throws IndexingException {
		try {
			long startTime = updateRequest.getTimeStarted();

			StringBuilder query = new StringBuilder();

			// If the root is not the all target then restrict the delete query to its path.
			if (!TARGET_ALL.equals(updateRequest.getTargetID())) {
				// Get the path facet value for the starting point, since we need the hierarchy tier.
				BriefObjectMetadataBean ancestorPathBean = getRootAncestorPath(updateRequest);
				// If no ancestor path was returned, then this item either doesn't exist or can't have children, so exit
				if (ancestorPathBean == null) {
					log.debug("Canceling deleteChildrenPriorToTimestamp, the root object was not found.");
					return;
				}

				// Limit cleanup scope to root pid
				query.append(solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH.name())).append(':')
						.append(SolrSettings.sanitize(ancestorPathBean.getPath().getSearchValue())).append(",*");
				// Target any children with timestamp older than start time.
				query.append(" AND ").append(solrSettings.getFieldName(SearchFieldKeys.TIMESTAMP.name())).append(":[* TO ")
						.append(org.apache.solr.common.util.DateUtil.getThreadLocalDateFormat().format(startTime))
						.append("]");
			}

			solrUpdateDriver.deleteByQuery(query.toString());
		} catch (Exception e) {
			throw new IndexingException("Error encountered in deleteChildrenPriorToTimestampRequest for "
					+ updateRequest.getTargetID(), e);
		}
	}

	public void setSolrSettings(SolrSettings solrSettings) {
		this.solrSettings = solrSettings;
	}
}

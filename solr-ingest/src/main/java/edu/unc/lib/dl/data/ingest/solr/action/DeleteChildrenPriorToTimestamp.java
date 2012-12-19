package edu.unc.lib.dl.data.ingest.solr.action;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.DeleteChildrenPriorToTimestampRequest;
import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateAction;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;

public class DeleteChildrenPriorToTimestamp extends AbstractIndexingAction {
	private static final Logger LOG = LoggerFactory.getLogger(DeleteChildrenPriorToTimestamp.class);
	
	private SearchStateFactory searchStateFactory;

	@Override
	public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
		try {
			if (!(updateRequest instanceof DeleteChildrenPriorToTimestampRequest)) {
				throw new IndexingException(
						"Improper request issued to deleteChildrenPriorToTimestamp.  The request must be of type DeleteChildrenPriorToTimestampRequest");
			}
			DeleteChildrenPriorToTimestampRequest cleanupRequest = (DeleteChildrenPriorToTimestampRequest) updateRequest;

			// Query Solr for the full list of items that will be deleted
			SearchState searchState = searchStateFactory.createIDSearchState();

			// If the root is not the all target then restrict the delete query to its path.
			if (!TARGET_ALL.equals(updateRequest.getTargetID())) {
				// Get the path facet value for the starting point, since we need the hierarchy tier.
				BriefObjectMetadataBean ancestorPathBean = getRootAncestorPath(updateRequest);
				// If no ancestor path was returned, then this item either doesn't exist or can't have children, so exit
				if (ancestorPathBean == null) {
					LOG.debug("Canceling deleteChildrenPriorToTimestamp, the root object was not found.");
					return;
				}

				HashMap<String, Object> facets = new HashMap<String, Object>();
				facets.put(SearchFieldKeys.ANCESTOR_PATH, ancestorPathBean.getPath());
				searchState.setFacets(facets);
			}

			// Get as many results as possible
			searchState.setRowsPerPage(Integer.MAX_VALUE);

			// Override default resource types to include folder as well.
			searchState.setResourceTypes(searchSettings.getResourceTypes());

			// Filter the results to only contain children with timestamps from before the requests timestamp.
			HashMap<String, SearchState.RangePair> rangePairs = new HashMap<String, SearchState.RangePair>();
			rangePairs
					.put(SearchFieldKeys.TIMESTAMP, new SearchState.RangePair(null, cleanupRequest.getTimestampString()));
			searchState.setRangeFields(rangePairs);

			SearchRequest searchRequest = new SearchRequest(searchState, accessGroups);
			SearchResultResponse orphanedChildResults = solrSearchService.getSearchResults(
					searchRequest);
			LOG.debug("Orphaned children found: " + orphanedChildResults.getResultList().size());

			// Queue up the children for processing, with a delete tree command for containers and a regular delete
			// otherwise
			for (BriefObjectMetadata child : orphanedChildResults.getResultList()) {
				solrUpdateService.offer(child.getId(), SolrUpdateAction.DELETE);
			}
		} catch (Exception e) {
			throw new IndexingException("Error encountered in deleteChildrenPriorToTimestampRequest for "
					+ updateRequest.getTargetID(), e);
		}
	}

	public void setSearchStateFactory(SearchStateFactory searchStateFactory) {
		this.searchStateFactory = searchStateFactory;
	}

}

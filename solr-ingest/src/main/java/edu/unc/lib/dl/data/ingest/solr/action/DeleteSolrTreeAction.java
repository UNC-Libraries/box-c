package edu.unc.lib.dl.data.ingest.solr.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.IndexingException;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

public class DeleteSolrTreeAction extends AbstractIndexingAction {
	private static final Logger LOG = LoggerFactory.getLogger(DeleteSolrTreeAction.class);

	@Override
	public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
		// If the all target is being deleted, then delete everything
		if (TARGET_ALL.equals(updateRequest.getTargetID())) {
			LOG.debug("Delete Solr Tree, targeting all object.");
			solrUpdateDriver.deleteByQuery("*:*");
			return;
		}

		BriefObjectMetadataBean ancestorPathBean = getRootAncestorPath(updateRequest);
		if (ancestorPathBean == null) {
			LOG.debug("Root object " + updateRequest.getTargetID() + " was not found while attempting to delete tree.");
			return;
		}

		// Determine if the starting node is a container.
		if (ancestorPathBean.getResourceType().equals(searchSettings.getResourceTypeCollection())
				|| ancestorPathBean.getResourceType().equals(searchSettings.getResourceTypeFolder())) {
			// Deleting a folder or collection, so perform a full path delete.

			solrUpdateDriver.deleteByQuery(
					solrSearchService.getSolrSettings().getFieldName(SearchFieldKeys.ID.name()) + ":"
							+ SolrSettings.sanitize(updateRequest.getTargetID()));

			solrUpdateDriver.deleteByQuery(
					solrSearchService.getSolrSettings().getFieldName(SearchFieldKeys.ANCESTOR_PATH.name())
							+ ":" + SolrSettings.sanitize(ancestorPathBean.getPath().getSearchValue())
							+ searchSettings.getFacetSubfieldDelimiter() + "*");
		} else {
			// Targeting an individual file, just delete it.
			solrUpdateDriver.delete(updateRequest.getTargetID());
		}
	}

}

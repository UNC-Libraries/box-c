package edu.unc.lib.boxc.indexing.solr.action;

import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;

import java.util.Map;

import org.springframework.util.Assert;

import edu.unc.lib.boxc.indexing.solr.SolrUpdateRequest;
import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;

/**
 * Deletes solr records for all objects which are children of the specified
 * object if they have not been updated since the time specified by
 * staleTimestamp
 *
 * @author bbpennel
 *
 */
public class DeleteStaleChildren extends AbstractIndexingAction {

    public static final String STALE_TIMESTAMP = "staleTimestamp";

    public DeleteStaleChildren() {
    }

    @Override
    public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
        Assert.notEmpty(updateRequest.getParams());

        Map<String, String> params = updateRequest.getParams();
        String staleTimestamp = params.get(STALE_TIMESTAMP);
        Assert.hasText(staleTimestamp, "Cannot cleanup stale children, no staleTimestamp provided");

        try {
            // Force commit the updates currently staged
            solrUpdateDriver.commit();

            StringBuilder query = new StringBuilder();

            // If targeting the content root object, clean out all records
            if (getContentRootPid().equals(updateRequest.getPid())) {
                query.append("*:*");
            } else {
                // Get the path facet value for the starting point, since we need the hierarchy tier.
                ContentObjectRecord ancestorPathBean = getRootAncestorPath(updateRequest);

                // Limit cleanup scope to root pid
                query.append(SearchFieldKey.ANCESTOR_PATH.getSolrField()).append(':')
                        .append(SolrSettings.sanitize(ancestorPathBean.getPath().getSearchValue()));
            }

            // Target any children with timestamp older than start time.
            query.append(" AND ").append(SearchFieldKey.TIMESTAMP.getSolrField())
                    .append(":{* TO ").append(staleTimestamp).append("}");

            solrUpdateDriver.deleteByQuery(query.toString());
        } catch (Exception e) {
            throw new IndexingException("Error encountered in deleteStaleChildren for "
                    + updateRequest.getTargetID(), e);
        }
    }

}

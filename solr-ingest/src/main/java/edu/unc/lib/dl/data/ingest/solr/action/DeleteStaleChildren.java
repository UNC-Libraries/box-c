/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.data.ingest.solr.action;

import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;

import java.util.Map;

import org.springframework.util.Assert;

import edu.unc.lib.boxc.search.api.SearchFieldKeys;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

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
                query.append(solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH.name())).append(':')
                        .append(SolrSettings.sanitize(ancestorPathBean.getPath().getSearchValue()));
            }

            // Target any children with timestamp older than start time.
            query.append(" AND ").append(solrSettings.getFieldName(SearchFieldKeys.TIMESTAMP.name()))
                    .append(":{* TO ").append(staleTimestamp).append("}");

            solrUpdateDriver.deleteByQuery(query.toString());
        } catch (Exception e) {
            throw new IndexingException("Error encountered in deleteStaleChildren for "
                    + updateRequest.getTargetID(), e);
        }
    }

}

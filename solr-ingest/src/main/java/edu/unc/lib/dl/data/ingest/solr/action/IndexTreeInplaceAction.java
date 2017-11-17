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

import static edu.unc.lib.dl.fcrepo4.RepositoryPaths.getContentRootPid;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.util.DateFormatUtil;
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

    @Override
    public void performAction(SolrUpdateRequest updateRequest) throws IndexingException {
        log.info("Starting inplace indexing of {}", updateRequest.getPid());

        super.performAction(updateRequest);

        // Force commit the updates currently staged
        solrUpdateDriver.commit();
        // Cleanup any objects in the tree that were no updated

        this.deleteStaleChildren(updateRequest);

        if (log.isDebugEnabled()) {
            log.debug(String.format("Finished inplace indexing of {}.  {} objects updated in {}ms",
                    updateRequest.getPid().getRepositoryPath(), updateRequest.getChildrenPending(),
                    System.currentTimeMillis() - updateRequest.getTimeStarted()));
        }
    }

    public void deleteStaleChildren(SolrUpdateRequest updateRequest) throws IndexingException {
        try {
            long startTime = updateRequest.getTimeStarted();
            Date startDate = new Date(startTime);
            String isoDate = DateFormatUtil.formatter.format(startDate);

            StringBuilder query = new StringBuilder();

            // If targeting the content root object, clean out all records
            if (getContentRootPid().equals(updateRequest.getPid())) {
                query.append("*:*");
            } else {
                // Get the path facet value for the starting point, since we need the hierarchy tier.
                BriefObjectMetadata ancestorPathBean = getRootAncestorPath(updateRequest);

                // Limit cleanup scope to root pid
                query.append(solrSettings.getFieldName(SearchFieldKeys.ANCESTOR_PATH.name())).append(':')
                        .append(SolrSettings.sanitize(ancestorPathBean.getPath().getSearchValue()));
            }

            // Target any children with timestamp older than start time.
            query.append(" AND ").append(solrSettings.getFieldName(SearchFieldKeys.TIMESTAMP.name()))
                    .append(":{* TO ").append(isoDate).append("}");

            solrUpdateDriver.deleteByQuery(query.toString());
        } catch (Exception e) {
            throw new IndexingException("Error encountered in deleteStaleChildren for "
                    + updateRequest.getTargetID(), e);
        }
    }
}

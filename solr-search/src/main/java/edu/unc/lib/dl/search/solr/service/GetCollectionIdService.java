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
package edu.unc.lib.dl.search.solr.service;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SolrSettings;
import edu.unc.lib.dl.util.ContentPathConstants;

/**
 * Service for retrieving the collection ID for objects
 *
 * @author bbpennel
 */
public class GetCollectionIdService extends AbstractQueryService {
    private static final Logger log = getLogger(GetCollectionIdService.class);

    private final static List<String> RESULT_FIELDS = Collections.singletonList(
            SearchFieldKeys.COLLECTION_ID.name());

    /**
     *
     * @param mdObj Metadata record of object to retriev
     * @return
     */
    public String getCollectionId(BriefObjectMetadata mdObj) {
        if (mdObj.getCollectionId() != null) {
            return mdObj.getCollectionId();
        }

        List<String> ancestors = mdObj.getAncestorPath();
        int index = ancestors.size();

        String idFieldName = solrSettings.getFieldName(SearchFieldKeys.ID.name());
        String collectionIdName = solrSettings.getFieldName(SearchFieldKeys.COLLECTION_ID.name());

        String currentVal = mdObj.getCollectionId();
        while (index >= ContentPathConstants.COLLECTION_DEPTH) {
            if (currentVal != null) {
                return currentVal;
            }


            String nextId = StringUtils.substringBefore(ancestors.get(index), ",");

            QueryResponse queryResponse = null;
            StringBuilder query = new StringBuilder();
            query.append(solrSettings.getFieldName(SearchFieldKeys.ID.name())).append(':')
                    .append(SolrSettings.sanitize(nextId));

            SolrQuery solrQuery = new SolrQuery(query.toString());
            addResultFields(RESULT_FIELDS, solrQuery);
            solrQuery.setRows(1);

            log.debug("getObjectById query: " + solrQuery.toString());
            try {
                queryResponse = executeQuery(solrQuery);

                currentVal = (String) queryResponse.getResults().get(0).getFieldValue(collectionIdName);
            } catch (SolrServerException e) {
                log.error("Error retrieving Solr object request", e);
                return null;
            }

            --index;
        }
        return null;
//        do {
//            if (currentVal != null) {
//                return currentVal;
//            }
//            if (index <= ContentPathConstants.COLLECTION_DEPTH) {
//                return null;
//            }
//
//            --index;
//
//        } while (true);
    }
}

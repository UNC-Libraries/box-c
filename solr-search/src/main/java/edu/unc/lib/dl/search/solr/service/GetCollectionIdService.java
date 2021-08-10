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
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;

import edu.unc.lib.boxc.model.api.ids.ContentPathConstants;
import edu.unc.lib.boxc.search.api.exceptions.SolrRuntimeException;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

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
     * Get the collection id which applies to the given metadata object.
     * Note, the metadata object must have been retrieved with the ID, ancestorPath, and collectionId fields.
     *
     * @param mdObj metadata object
     * @return collection id or null if none applies.
     */
    public String getCollectionId(ContentObjectRecord mdObj) {
        long start = System.nanoTime();
        try {
            return findFirstCollectionId(mdObj);
        } finally {
            log.debug("Finished retrieving collection id for {} with {} ancestors in {}ns",
                    mdObj.getId(), mdObj.getAncestorPath().size(), (System.nanoTime() - start) / 1e6);
        }
    }

    private String findFirstCollectionId(ContentObjectRecord mdObj) {
        if (mdObj.getAncestorPath() == null || mdObj.getId() == null) {
            throw new IllegalArgumentException("Provided metadata object is missing required fields");
        }
        if (mdObj.getCollectionId() != null) {
            return mdObj.getCollectionId();
        }

        List<String> ancestors = mdObj.getAncestorPath();
        int index = ancestors.size();

        String idFieldName = solrSettings.getFieldName(SearchFieldKeys.ID.name());
        String collectionIdName = solrSettings.getFieldName(SearchFieldKeys.COLLECTION_ID.name());

        while (--index >= ContentPathConstants.COLLECTION_DEPTH) {
            String nextId = StringUtils.substringAfter(ancestors.get(index), ",");

            QueryResponse queryResponse = null;
            StringBuilder query = new StringBuilder();
            query.append(idFieldName).append(':').append(SolrSettings.sanitize(nextId));

            SolrQuery solrQuery = new SolrQuery(query.toString());
            addResultFields(RESULT_FIELDS, solrQuery);
            solrQuery.setRows(1);

            try {
                queryResponse = executeQuery(solrQuery);
                SolrDocumentList results = queryResponse.getResults();
                if (results.size() == 0) {
                    log.warn("Ancestor {} for {} was not found, cannot determine collection Id",
                            nextId, mdObj.getId());
                    return null;
                }

                String currentVal = (String) queryResponse.getResults().get(0).getFieldValue(collectionIdName);
                if (currentVal != null) {
                    return currentVal;
                }
            } catch (SolrServerException e) {
                throw new SolrRuntimeException(e);
            }
        }
        return null;
    }
}

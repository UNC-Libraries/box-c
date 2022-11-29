package edu.unc.lib.boxc.search.solr.services;

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
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.exceptions.SolrRuntimeException;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;

/**
 * Service for retrieving the collection ID for objects
 *
 * @author bbpennel
 */
public class GetCollectionIdService extends AbstractQueryService {
    private static final Logger log = getLogger(GetCollectionIdService.class);

    private final static List<String> RESULT_FIELDS = Collections.singletonList(
            SearchFieldKey.COLLECTION_ID.name());

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

        String idFieldName = SearchFieldKey.ID.getSolrField();
        String collectionIdName = SearchFieldKey.COLLECTION_ID.getSolrField();

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

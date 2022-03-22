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
package edu.unc.lib.boxc.search.solr.services;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;
import edu.unc.lib.boxc.search.solr.utils.AccessRestrictionUtil;

/**
 * Abstract service for performing solr queries.
 *
 * @author bbpennel
 *
 */
public abstract class AbstractQueryService {
    private static final Logger log = getLogger(AbstractQueryService.class);

    private final static List<String> DEFAULT_RESULT_FIELDS = Arrays.asList(
            SearchFieldKey.ABSTRACT.name(),
            SearchFieldKey.ADMIN_GROUP.name(),
            SearchFieldKey.ANCESTOR_IDS.name(),
            SearchFieldKey.ANCESTOR_PATH.name(),
            SearchFieldKey.CITATION.name(),
            SearchFieldKey.COLLECTION_ID.name(),
            SearchFieldKey.CONTENT_STATUS.name(),
            SearchFieldKey.CONTENT_TYPE.name(),
            SearchFieldKey.CONTRIBUTOR.name(),
            SearchFieldKey.CREATOR.name(),
            SearchFieldKey.DATASTREAM.name(),
            SearchFieldKey.DATE_ADDED.name(),
            SearchFieldKey.DATE_CREATED.name(),
            SearchFieldKey.DATE_UPDATED.name(),
            SearchFieldKey.FILESIZE.name(),
            SearchFieldKey.FILESIZE_TOTAL.name(),
            SearchFieldKey.ID.name(),
            SearchFieldKey.IDENTIFIER.name(),
            SearchFieldKey.KEYWORD.name(),
            SearchFieldKey.LANGUAGE.name(),
            SearchFieldKey.LAST_INDEXED.name(),
            SearchFieldKey.LOCATION.name(),
            SearchFieldKey.OTHER_TITLES.name(),
            SearchFieldKey.PARENT_COLLECTION.name(),
            SearchFieldKey.PARENT_UNIT.name(),
            SearchFieldKey.PUBLISHER.name(),
            SearchFieldKey.READ_GROUP.name(),
            SearchFieldKey.RESOURCE_TYPE.name(),
            SearchFieldKey.ROLE_GROUP.name(),
            SearchFieldKey.ROLLUP_ID.name(),
            SearchFieldKey.STATUS.name(),
            SearchFieldKey.SUBJECT.name(),
            SearchFieldKey.TIMESTAMP.name(),
            SearchFieldKey.TITLE.name(),
            SearchFieldKey.VERSION.name());

    @Autowired
    protected SearchSettings searchSettings;
    @Autowired
    protected SolrSettings solrSettings;
    protected SolrClient solrClient;

    protected AccessRestrictionUtil restrictionUtil;

    /**
     * Establish the SolrServer object according to the configuration specified in settings.
     */
    protected void initializeSolrServer() {
        solrClient = solrSettings.getSolrClient();
    }

    /**
     * Add a filter for the specified field with the given value.
     *
     * @param query
     * @param fieldKey
     * @param value
     */
    protected void addFilter(StringBuilder query, SearchFieldKey fieldKey, String value) {
        query.append(solrField(fieldKey)).append(':')
                .append(SolrSettings.sanitize(value));
    }

    /**
     * Add a filter to query, restricting a field to match any of the provided
     * values.
     *
     * @param query query the filter will be added to.
     * @param fieldKey
     * @param values collection of values
     */
    protected void addFilter(StringBuilder query, SearchFieldKey fieldKey, Collection<String> values) {
        query.append(makeFilter(fieldKey, values));
    }

    /**
     * Create a filter, restricting a field to match any of the provided values.
     *
     * @param fieldKey
     * @param values
     * @return filter
     */
    protected String makeFilter(SearchFieldKey fieldKey, Collection<String> values) {
        if (values == null || values.size() == 0) {
            return "";
        }
        return solrField(fieldKey) + ":(" + String.join(" OR ", values) + ")";
    }

    /**
     * Adds the sort identified by sortType to the query. If normalOrder is
     * true, then the query will sort in the default order specified by the
     * sort, otherwise it will be reversed
     *
     * @param solrQuery
     * @param sortType
     * @param normalOrder
     */
    protected void addSort(SolrQuery solrQuery, String sortType, boolean normalOrder) {
        if (sortType == null) {
            return;
        }
        var sortFields = SearchSettings.getSortFields(sortType);
        if (sortFields != null) {
            for (int i = 0; i < sortFields.size(); i++) {
                SearchSettings.SortField sortField = sortFields.get(i);
                SolrQuery.ORDER sortOrder = SolrQuery.ORDER.valueOf(sortField.getSortOrder());
                if (!normalOrder) {
                    sortOrder = sortOrder.reverse();
                }
                solrQuery.addSort(SearchFieldKey.valueOf(sortField.getFieldName()).getSolrField(), sortOrder);
            }
        }
    }

    /**
     * Adds the specified result fields to the solrQuery, or if no resultFields
     * are provided, then the default result fields are added.
     * @param resultFields
     * @param solrQuery
     */
    protected void addResultFields(List<String> resultFields, SolrQuery solrQuery) {
        if (resultFields == null) {
            resultFields = DEFAULT_RESULT_FIELDS;
        }
        for (String field : resultFields) {
            var solrField = SearchFieldKey.valueOf(field);
            if (solrField != null) {
                solrQuery.addField(solrField.getSolrField());
            }
        }
    }

    /**
     * Get the name of the solr field for the key value.
     *
     * @param fieldKey
     * @return
     */
    public String solrField(SearchFieldKey fieldKey) {
        return fieldKey.getSolrField();
    }

    /**
     * Wrapper method for executing a solr query.
     *
     * @param query
     * @return
     * @throws SolrServerException
     */
    protected QueryResponse executeQuery(SolrQuery query) throws SolrServerException {
        try {
            log.debug("Executing solr query: {}", query);
            return solrClient.query(query);
        } catch (IOException e) {
            throw new SolrServerException(e);
        }
    }

    /**
     * @param searchSettings the searchSettings to set
     */
    public void setSearchSettings(SearchSettings searchSettings) {
        this.searchSettings = searchSettings;
    }

    /**
     * @param solrSettings the solrSettings to set
     */
    public void setSolrSettings(SolrSettings solrSettings) {
        this.solrSettings = solrSettings;
    }

    /**
     * @param solrClient the solrClient to set
     */
    public void setSolrClient(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    /**
     * @param restrictionUtil the restrictionUtil to set
     */
    public void setAccessRestrictionUtil(AccessRestrictionUtil restrictionUtil) {
        this.restrictionUtil = restrictionUtil;
    }
}

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

import edu.unc.lib.dl.search.solr.util.AccessRestrictionUtil;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

/**
 * Abstract service for performing solr queries.
 *
 * @author bbpennel
 *
 */
public abstract class AbstractQueryService {
    private static final Logger log = getLogger(AbstractQueryService.class);

    private final static List<String> DEFAULT_RESULT_FIELDS = Arrays.asList(
            SearchFieldKeys.ABSTRACT.name(),
            SearchFieldKeys.ADMIN_GROUP.name(),
            SearchFieldKeys.ANCESTOR_IDS.name(),
            SearchFieldKeys.ANCESTOR_PATH.name(),
            SearchFieldKeys.CITATION.name(),
            SearchFieldKeys.COLLECTION_ID.name(),
            SearchFieldKeys.CONTENT_STATUS.name(),
            SearchFieldKeys.CONTENT_TYPE.name(),
            SearchFieldKeys.CONTRIBUTOR.name(),
            SearchFieldKeys.CREATOR.name(),
            SearchFieldKeys.DATASTREAM.name(),
            SearchFieldKeys.DATE_ADDED.name(),
            SearchFieldKeys.DATE_CREATED.name(),
            SearchFieldKeys.DATE_UPDATED.name(),
            SearchFieldKeys.DEPARTMENT.name(),
            SearchFieldKeys.FILESIZE.name(),
            SearchFieldKeys.FILESIZE_TOTAL.name(),
            SearchFieldKeys.ID.name(),
            SearchFieldKeys.IDENTIFIER.name(),
            SearchFieldKeys.IS_PART.name(),
            SearchFieldKeys.KEYWORD.name(),
            SearchFieldKeys.LABEL.name(),
            SearchFieldKeys.LANGUAGE.name(),
            SearchFieldKeys.LAST_INDEXED.name(),
            SearchFieldKeys.OTHER_TITLES.name(),
            SearchFieldKeys.PARENT_COLLECTION.name(),
            SearchFieldKeys.PARENT_UNIT.name(),
            SearchFieldKeys.READ_GROUP.name(),
            SearchFieldKeys.RELATIONS.name(),
            SearchFieldKeys.RESOURCE_TYPE.name(),
            SearchFieldKeys.ROLE_GROUP.name(),
            SearchFieldKeys.ROLLUP_ID.name(),
            SearchFieldKeys.STATUS.name(),
            SearchFieldKeys.SUBJECT.name(),
            SearchFieldKeys.TIMESTAMP.name(),
            SearchFieldKeys.TITLE.name(),
            SearchFieldKeys.VERSION.name());

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
    protected void addFilter(StringBuilder query, SearchFieldKeys fieldKey, String value) {
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
    protected void addFilter(StringBuilder query, SearchFieldKeys fieldKey, Collection<String> values) {
        query.append(makeFilter(fieldKey, values));
    }

    /**
     * Create a filter, restricting a field to match any of the provided values.
     *
     * @param fieldKey
     * @param values
     * @return filter
     */
    protected String makeFilter(SearchFieldKeys fieldKey, Collection<String> values) {
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
        List<SearchSettings.SortField> sortFields = searchSettings.sortTypes.get(sortType);
        if (sortFields != null) {
            for (int i = 0; i < sortFields.size(); i++) {
                SearchSettings.SortField sortField = sortFields.get(i);
                SolrQuery.ORDER sortOrder = SolrQuery.ORDER.valueOf(sortField.getSortOrder());
                if (!normalOrder) {
                    sortOrder = sortOrder.reverse();
                }
                solrQuery.addSort(solrSettings.getFieldName(sortField.getFieldName()), sortOrder);
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
            String solrFieldName = solrSettings.getFieldName(field);
            if (solrFieldName != null) {
                solrQuery.addField(solrFieldName);
            }
        }
    }

    /**
     * Get the name of the solr field for the key value.
     *
     * @param fieldKey
     * @return
     */
    protected String solrField(SearchFieldKeys fieldKey) {
        return solrSettings.getFieldName(fieldKey.name());
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

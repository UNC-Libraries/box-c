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

import java.io.IOException;
import java.util.Collection;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
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

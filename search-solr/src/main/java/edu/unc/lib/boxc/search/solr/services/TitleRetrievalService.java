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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import org.apache.solr.client.solrj.SolrQuery;

import java.util.concurrent.TimeUnit;

/**
 * Service for retrieving titles of objects
 *
 * @author bbpennel
 */
public class TitleRetrievalService {
    private LoadingCache<String, String> titleCache;

    private long cacheTimeToLive = 10 * 60;
    private long cacheMaxSize = 256;

    private SolrSearchService solrSearchService;

    public void init() {
        titleCache = CacheBuilder.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(cacheTimeToLive, TimeUnit.SECONDS)
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(String key) throws Exception {
                        SolrQuery solrQuery = new SolrQuery();
                        solrQuery.addFilterQuery(SearchFieldKey.ID.getSolrField() + ":" + key);
                        solrQuery.setFields(SearchFieldKey.TITLE.getSolrField());
                        var resp = solrSearchService.executeQuery(solrQuery);
                        var results = resp.getResults();
                        if (results.isEmpty()) {
                            throw new NotFoundException("Unable to find solr record for object " + key);
                        } else {
                            return (String) results.get(0).getFieldValue(SearchFieldKey.TITLE.getSolrField());
                        }
                    }
                });
    }

    /**
     * Retrieve the title of the specified object, or throw a NotFoundException if not found.
     * @param pid
     * @return title
     */
    public String retrieveTitle(PID pid) {
        return titleCache.getUnchecked(pid.getId());
    }

    /**
     * Invalidate cache entry for the specified pid
     * @param pid
     */
    public void invalidate(PID pid) {
        titleCache.invalidate(pid.getId());
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }
}

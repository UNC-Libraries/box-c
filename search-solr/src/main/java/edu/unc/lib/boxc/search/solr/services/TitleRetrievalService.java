package edu.unc.lib.boxc.search.solr.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import org.apache.solr.client.solrj.SolrQuery;
import org.slf4j.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service for retrieving titles of objects
 *
 * @author bbpennel
 */
public class TitleRetrievalService {
    private static final Logger log = getLogger(TitleRetrievalService.class);
    private LoadingCache<String, String> titleCache;

    private long cacheTimeToLive = 10 * 60;
    private long cacheMaxSize = 1024;

    private SolrSearchService solrSearchService;

    public void init() {
        titleCache = CacheBuilder.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(cacheTimeToLive, TimeUnit.SECONDS)
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(String key) throws Exception {
                        SolrQuery solrQuery = new SolrQuery();
                        solrQuery.setQuery("*:*");
                        solrQuery.addFilterQuery(SearchFieldKey.ID.getSolrField() + ":" + key);
                        solrQuery.setFields(SearchFieldKey.TITLE.getSolrField());
                        log.debug("Retrieve title query {}", solrQuery);
                        var resp = solrSearchService.executeQuery(solrQuery);
                        var results = resp.getResults();
                        if (results.isEmpty()) {
                            throw new NotFoundException("Unable to find solr record for object " + key);
                        } else {
                            var title = (String) results.get(0).getFieldValue(SearchFieldKey.TITLE.getSolrField());
                            log.debug("Got title {} for {}", title, key);
                            return title;
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
        try {
            return titleCache.get(pid.getId());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException("Failed to retrieve title for " + pid, e);
        }
    }

    /**
     * Retrieve the title from the cache if already present, otherwise return null
     * @param pid
     * @return
     */
    public String retrieveCachedTitle(PID pid) {
        return titleCache.getIfPresent(pid.getId());
    }

    /**
     * Store a title entry to the cache
     * @param pid
     * @param title
     */
    public void storeTitle(PID pid, String title) {
        titleCache.put(pid.getId(), title);
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

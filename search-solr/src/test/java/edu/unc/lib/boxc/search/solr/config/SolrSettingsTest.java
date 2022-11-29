package edu.unc.lib.boxc.search.solr.config;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.boxc.search.solr.config.SolrSettings;

public class SolrSettingsTest extends Assert {

    @Test
    public void sanitizeTest() {
        String pid = SolrSettings.sanitize("uuid:2dbf9ab9-0c47-42c7-8e7c-5092febf3415");
        assertEquals("uuid\\:2dbf9ab9\\-0c47\\-42c7\\-8e7c\\-5092febf3415", pid);

        assertEquals("*.txt", SolrSettings.sanitize("*.txt"));
        assertEquals("\\/regex\\/*.txt", SolrSettings.sanitize("/regex/*.txt"));

        assertEquals("AND*", SolrSettings.sanitize("AND*"));
        assertEquals("hello\\ 'OR'\\ world", SolrSettings.sanitize("hello OR world"));
        assertEquals("hello\\ *AND*\\ world", SolrSettings.sanitize("hello *AND* world"));
    }

    @Test
    public void sanitizeWildCardTest() {

    }

    @Test
    public void tokenizeSearchTerms() {
        List<String> tokens = SolrSettings.getSearchTermFragments("hello world");
        assertEquals("hello", tokens.get(0));
        assertEquals("world", tokens.get(1));
        assertEquals(2, tokens.size());

        tokens = SolrSettings.getSearchTermFragments("hello:world");
        assertEquals("\"hello\\:world\"", tokens.get(0));
        assertEquals(1, tokens.size());

        tokens = SolrSettings.getSearchTermFragments("\"hello world\"");
        assertEquals("\"hello\\ world\"", tokens.get(0));
        assertEquals(1, tokens.size());

        tokens = SolrSettings.getSearchTermFragments("\"hello world\" welcome");
        assertEquals("\"hello\\ world\"", tokens.get(0));
        assertEquals("welcome", tokens.get(1));
        assertEquals(2, tokens.size());

        tokens = SolrSettings.getSearchTermFragments("\"hello world\" wel*");
        assertEquals("\"hello\\ world\"", tokens.get(0));
        assertEquals("wel*", tokens.get(1));
        assertEquals(2, tokens.size());
    }

    @Test
    public void tokenizeSearchTermsContainingQuote() {
        List<String> tokens = SolrSettings.getSearchTermFragments("\"Level 1, 0-6\\\" BS\"");
        assertEquals("\"Level\\ 1,\\ 0\\-6\\\"\\ BS\"", tokens.get(0));
        assertEquals(1, tokens.size());
    }
}

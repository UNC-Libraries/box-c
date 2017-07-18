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
package edu.unc.lib.dl.search.solr.util;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

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

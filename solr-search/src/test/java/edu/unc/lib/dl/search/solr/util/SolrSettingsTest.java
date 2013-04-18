package edu.unc.lib.dl.search.solr.util;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class SolrSettingsTest extends Assert {

	@Test
	public void sanitizeTest() {
		String pid = SolrSettings.sanitize("uuid:2dbf9ab9-0c47-42c7-8e7c-5092febf3415");
		assertEquals("uuid\\:2dbf9ab9\\-0c47\\-42c7\\-8e7c\\-5092febf3415", pid);
		
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
	}
}

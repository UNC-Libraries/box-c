package edu.unc.lib.dl.search.solr.service;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.search.solr.model.SearchState;

public class SearchStateTest extends Assert {

	@Test
	public void searchTermFragmentationApostrophe() {
		SearchState state = new SearchState();
		HashMap<String,String> searchFields = new HashMap<String,String>();
		searchFields.put("keyword", "Barriers to women's participation in inter-conceptional care: a cross-sectional analysis");
		state.setSearchFields(searchFields);
		
		ArrayList<String> fragments = state.getSearchTermFragments("keyword");
		assertEquals(10, fragments.size());
		
		assertTrue(fragments.contains("Barriers"));
		assertTrue(fragments.contains("to"));
		assertTrue(fragments.contains("women's"));
		assertTrue(fragments.contains("participation"));
		assertTrue(fragments.contains("in"));
		assertTrue(fragments.contains("inter-conceptional"));
		assertTrue(fragments.contains("care"));
		assertTrue(fragments.contains("a"));
		assertTrue(fragments.contains("cross-sectional"));
		assertTrue(fragments.contains("analysis"));
	}
	
	@Test
	public void searchTermFragmentationQuoted() {
		SearchState state = new SearchState();
		HashMap<String,String> searchFields = new HashMap<String,String>();
		searchFields.put("keyword", "Barriers to \"participation in inter-conceptional care\": a cross-sectional analysis");
		state.setSearchFields(searchFields);
		
		ArrayList<String> fragments = state.getSearchTermFragments("keyword");
		assertEquals(6, fragments.size());
		
		assertTrue(fragments.contains("Barriers"));
		assertTrue(fragments.contains("to"));
		assertTrue(fragments.contains("\"participation in inter-conceptional care\""));
		assertTrue(fragments.contains("a"));
		assertTrue(fragments.contains("cross-sectional"));
		assertTrue(fragments.contains("analysis"));
	}
	
	@Test
	public void searchTermFragmentationUnclosedQuote() {
		SearchState state = new SearchState();
		HashMap<String,String> searchFields = new HashMap<String,String>();
		searchFields.put("keyword", "Barriers to \"participation in inter-conceptional care: a cross-sectional analysis");
		state.setSearchFields(searchFields);
		
		ArrayList<String> fragments = state.getSearchTermFragments("keyword");
		assertEquals(9, fragments.size());
		
		assertTrue(fragments.contains("Barriers"));
		assertTrue(fragments.contains("to"));
		assertTrue(fragments.contains("participation"));
		assertTrue(fragments.contains("in"));
		assertTrue(fragments.contains("inter-conceptional"));
		assertTrue(fragments.contains("care"));
		assertTrue(fragments.contains("a"));
		assertTrue(fragments.contains("cross-sectional"));
		assertTrue(fragments.contains("analysis"));
	}
	
	@Test
	public void searchTermFragmentationEmptyQuotes() {
		SearchState state = new SearchState();
		HashMap<String,String> searchFields = new HashMap<String,String>();
		searchFields.put("keyword", "Barriers to \"\" participation in inter-conceptional care: a cross-sectional analysis");
		state.setSearchFields(searchFields);
		
		ArrayList<String> fragments = state.getSearchTermFragments("keyword");
		assertEquals(10, fragments.size());
		
		assertTrue(fragments.contains("Barriers"));
		assertTrue(fragments.contains("to"));
		assertTrue(fragments.contains("\"\""));
		assertTrue(fragments.contains("participation"));
		assertTrue(fragments.contains("in"));
		assertTrue(fragments.contains("inter-conceptional"));
		assertTrue(fragments.contains("care"));
		assertTrue(fragments.contains("a"));
		assertTrue(fragments.contains("cross-sectional"));
		assertTrue(fragments.contains("analysis"));
	}
	
	@Test
	public void searchTermFragmentationSingleQuotes() {
		SearchState state = new SearchState();
		HashMap<String,String> searchFields = new HashMap<String,String>();
		searchFields.put("keyword", "'CFTR-opathies': disease phenotypes associated with 'cystic fibrosis transmembrane' regulator gene ' mutation's");
		state.setSearchFields(searchFields);
		
		ArrayList<String> fragments = state.getSearchTermFragments("keyword");
		assertEquals(12, fragments.size());
		
		assertTrue(fragments.contains("'CFTR-opathies'"));
		assertTrue(fragments.contains("disease"));
		assertTrue(fragments.contains("phenotypes"));
		assertTrue(fragments.contains("associated"));
		assertTrue(fragments.contains("with"));
		assertTrue(fragments.contains("'cystic"));
		assertTrue(fragments.contains("fibrosis"));
		assertTrue(fragments.contains("transmembrane'"));
		assertTrue(fragments.contains("regulator"));
		assertTrue(fragments.contains("'"));
		assertTrue(fragments.contains("gene"));
		assertTrue(fragments.contains("mutation's"));
	}
}

package edu.unc.lib.dl.cdr.services.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.ui.controller.AbstractSolrSearchController;
import edu.unc.lib.dl.ui.util.SerializationUtil;

@Controller
public class SearchRestController extends AbstractSolrSearchController {

	@RequestMapping(value = "/search", method = RequestMethod.GET)
	public @ResponseBody String search(HttpServletRequest request, HttpServletResponse response) {
		return doSearch(null, request, false);
	}
	
	@RequestMapping(value = "/search/{id}", method = RequestMethod.GET)
	public @ResponseBody String search(@PathVariable("id") String id, HttpServletRequest request) {
		return doSearch(id, request, false);
	}
	
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public @ResponseBody String list(HttpServletRequest request, HttpServletResponse response) {
		return doSearch(null, request, true);
	}
	
	@RequestMapping(value = "/list/{id}", method = RequestMethod.GET)
	public @ResponseBody String list(@PathVariable("id") String id, HttpServletRequest request) {
		return doSearch(id, request, true);
	}
	
	private String doSearch(String pid, HttpServletRequest request, boolean applyCutoffs) {
		SearchRequest searchRequest = generateSearchRequest(request);
		searchRequest.setApplyCutoffs(applyCutoffs);
		searchRequest.setRootPid(pid);
		
		SearchState searchState = searchRequest.getSearchState();
		
		String fields = request.getParameter("fields");
		// Allow for retrieving of specific fields
		if (fields != null) {
			String[] fieldNames = fields.split(",");
			List<String> resultFields = new ArrayList<String>();
			for (String fieldName: fieldNames) {
				String fieldKey = searchSettings.searchFieldKey(fieldName);
				if (fieldKey != null)
					resultFields.add(fieldKey);
				else if (searchSettings.isDynamicField(fieldName))
					resultFields.add(fieldName);
			}
			searchState.setResultFields(resultFields);
		} else {
			// Retrieve a predefined set of fields
			String fieldSet = request.getParameter("fieldSet");
			List<String> resultFields = searchSettings.resultFields.get(fieldSet);
			if (resultFields == null)
				resultFields = searchSettings.resultFields.get("brief");
			searchState.setResultFields(resultFields);
		}
		
		// Rollup
		String rollup = request.getParameter("rollup");
		searchState.setRollup(rollup != null && !"false".equalsIgnoreCase(rollup));
		if (searchState.getRollup() && !"true".equalsIgnoreCase(rollup)) {
			String fieldKey = searchSettings.searchFieldKey(rollup);
			if (fieldKey != null)
				searchState.setRollupField(fieldKey);
			else if (searchSettings.isDynamicField(rollup))
				searchState.setRollupField(rollup);
			else
				searchState.setRollup(false);
		}
		
		SearchResultResponse resultResponse = queryLayer.performSearch(searchRequest);
		if (resultResponse == null) 
			return null;
		
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("numFound", resultResponse.getResultCount());
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>(resultResponse.getResultList().size());
		for (BriefObjectMetadata metadata: resultResponse.getResultList()) {
			results.add(SerializationUtil.metadataToMap(metadata, GroupsThreadStore.getGroups()));
		}
		response.put("results", results);
		
		return SerializationUtil.objectToJSON(response);
	}
}

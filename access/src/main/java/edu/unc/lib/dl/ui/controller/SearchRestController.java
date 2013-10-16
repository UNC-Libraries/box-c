package edu.unc.lib.dl.ui.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.ui.controller.AbstractSolrSearchController;
import edu.unc.lib.dl.ui.util.SerializationUtil;

@Controller
public class SearchRestController extends AbstractSolrSearchController {

	@RequestMapping(value = "/api/search", method = RequestMethod.GET)
	public @ResponseBody String search(HttpServletRequest request, HttpServletResponse response) {
		return doSearch(null, request);
	}
	
	@RequestMapping(value = "/api/search/{id}", method = RequestMethod.GET)
	public @ResponseBody String search(@PathVariable("id") String id, HttpServletRequest request) {
		return doSearch(id, request);
	}
	
	// result - Result fields
	// format - Result type, such as json
	// 
	
	private String doSearch(String pid, HttpServletRequest request) {
		SearchRequest searchRequest = generateSearchRequest(request);
		searchRequest.setRootPid(pid);
		
		String fields = request.getParameter("fields");
		List<String> resultFields = searchSettings.resultFields.get(fields);
		if (resultFields == null)
			searchSettings.resultFields.get("brief");
		searchRequest.getSearchState().setResultFields(resultFields);
		
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
		//response.put("results", resultResponse.getResultList());
		
		//String result = SerializationUtil.objectToJSON(response);
		//System.out.println(result);
		
		return SerializationUtil.objectToJSON(response);
	}
}

package edu.unc.lib.dl.cdr.services.rest;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.ui.controller.AbstractSolrSearchController;

@Controller
public class SearchRestController extends AbstractSolrSearchController {

	@RequestMapping(value = "api/search", method = RequestMethod.GET)
	public @ResponseBody Object search(HttpServletRequest request) {
		return doSearch(null, request);
	}
	
	@RequestMapping(value = "api/search/{id}", method = RequestMethod.GET)
	public @ResponseBody Object search(@PathVariable("id") String id, HttpServletRequest request) {
		return doSearch(id, request);
	}
	
	// result - Result fields
	// format - Result type, such as json
	// 
	
	private Object doSearch(String pid, HttpServletRequest request) {
		SearchRequest searchRequest = generateSearchRequest(request);
		searchRequest.setRootPid(pid);
		
		SearchResultResponse resultResponse = queryLayer.performSearch(searchRequest);
		if (resultResponse == null) 
			return null;
		
		Map<String, Object> response = new HashMap<String, Object>();
		response.put("numFound", resultResponse.getResultCount());
		response.put("results", resultResponse.getResultList());
		
		return response;
	}
}

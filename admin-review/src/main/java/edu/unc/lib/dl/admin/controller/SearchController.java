package edu.unc.lib.dl.admin.controller;

import java.net.URLDecoder;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.util.SearchStateUtil;

@Controller
public class SearchController extends AbstractSearchController {
	private static final Logger LOG = LoggerFactory.getLogger(SearchController.class);

	@RequestMapping(value = "doSearch")
	public String searchForm(@RequestParam(value = "query", required = false) String query,
			@RequestParam(value = "queryType", required = false) String queryType,
			@RequestParam(value = "container", required = false) String container,
			@RequestParam(value = "within", required = false) String searchWithin,
			@RequestParam(value = "searchType", required = false) String searchType, Model model,
			HttpServletRequest request) {
		StringBuilder destination = new StringBuilder("redirect:/search");
		if (!"".equals(searchType) && container != null && container.length() > 0)
			destination.append('/').append(container);

		if ("within".equals(searchType) && searchWithin != null) {
			try {
				searchWithin = URLDecoder.decode(searchWithin, "UTF-8");
				HashMap<String, String[]> parameters = SearchStateUtil.getParametersAsHashMap(searchWithin);
				SearchState withinState = searchStateFactory.createSearchState(parameters);
				if (withinState.getSearchFields().size() > 0) {
					String queryKey = searchSettings.searchFieldKey(queryType);
					String typeValue = withinState.getSearchFields().get(queryKey);
					if (queryKey != null) {
						if (typeValue == null)
							withinState.getSearchFields().put(queryKey, query);
						else
							withinState.getSearchFields().put(queryKey, typeValue + " " + query);
						String searchStateUrl = SearchStateUtil.generateStateParameterString(withinState);
						destination.append('?').append(searchStateUrl);
					}
				} else {
					destination.append('?').append(queryType).append('=').append(query);
					destination.append('&').append(searchWithin);
				}
			} catch (Exception e) {
				LOG.error("Failed to decode searchWithin " + searchWithin, e);
			}
		} else {
			destination.append('?').append(queryType).append('=').append(query);
		}
		return destination.toString();
	}

	@RequestMapping(value = "search", method = RequestMethod.GET)
	public String search(Model model, HttpServletRequest request) {
		SearchRequest searchRequest = generateSearchRequest(request);

		SearchResultResponse resultResponse = doSearch(searchRequest, model, request);
		resultResponse.setSelectedContainer(null);
		return "search/resultList";
	}

	@RequestMapping(value = "search/{pid}", method = RequestMethod.GET)
	public String search(@PathVariable("pid") String pid, Model model, HttpServletRequest request) {
		SearchRequest searchRequest = generateSearchRequest(request);
		searchRequest.setRootPid(pid);

		doSearch(searchRequest, model, request);
		return "search/resultList";
	}

	private SearchResultResponse doSearch(SearchRequest searchRequest, Model model, HttpServletRequest request) {
		searchRequest.setApplyCutoffs(false);

		SearchResultResponse resultResponse = getSearchResults(searchRequest);
		
		if (resultResponse != null) {
			queryLayer.populateBreadcrumbs(searchRequest, resultResponse);
		}

		String searchStateUrl = SearchStateUtil.generateStateParameterString(searchRequest.getSearchState());
		model.addAttribute("searchStateUrl", searchStateUrl);
		model.addAttribute("resultResponse", resultResponse);
		model.addAttribute("queryMethod", "search");
		request.getSession().setAttribute("resultOperation", "search");
		return resultResponse;
	}
}

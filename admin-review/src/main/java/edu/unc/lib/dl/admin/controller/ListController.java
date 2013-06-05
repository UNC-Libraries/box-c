package edu.unc.lib.dl.admin.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.util.SearchStateUtil;

@Controller
public class ListController extends AbstractSearchController {
	@RequestMapping(value = "list", method = RequestMethod.GET)
	public String listRootContents(Model model, HttpServletRequest request) {
		SearchRequest searchRequest = generateSearchRequest(request);
		searchRequest.setRootPid(collectionsPid.getPid());

		doList(searchRequest, model, request);
		return "search/resultList";
	}

	@RequestMapping(value = "list/{pid}", method = RequestMethod.GET)
	public String listContainerContents(@PathVariable("pid") String pid, Model model, HttpServletRequest request) {
		SearchRequest searchRequest = generateSearchRequest(request);
		searchRequest.setRootPid(pid);

		doList(searchRequest, model, request);
		return "search/resultList";
	}

	private void doList(SearchRequest searchRequest, Model model, HttpServletRequest request) {
		SearchResultResponse resultResponse = getSearchResults(searchRequest);

		String searchStateUrl = SearchStateUtil.generateStateParameterString(searchRequest.getSearchState());
		model.addAttribute("searchStateUrl", searchStateUrl);
		model.addAttribute("resultResponse", resultResponse);
		model.addAttribute("queryMethod", "list");
		request.getSession().setAttribute("resultOperation", "list");
	}
}
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
package edu.unc.lib.dl.ui.controller;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchStateUtil;
//import edu.unc.lib.dl.ui.model.RecordNavigationState;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseRequest;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse;

/**
 * Handles requests for the heirarchical structure view browse view. The request may either be for an entire stand alone
 * view, or if the ajax option is true then a portion of the tree starting from the room node. The request can specify
 * the max depth in terms of nodes in the tree it will return, where the max depth is limited by the application wide
 * structured depth property.
 * 
 * @author bbpennel
 */
@Controller
public class StructureBrowseController extends AbstractSolrSearchController {
	private static final Logger LOG = LoggerFactory.getLogger(StructureBrowseController.class);

	@RequestMapping(value = "/structure", method = RequestMethod.GET)
	public String getStructure(Model model, HttpServletRequest request, HttpServletResponse response) {
		return getStructure(null, model, request, response);
	}

	@RequestMapping(value = "/structure/{prefix}/{id}", method = RequestMethod.GET)
	public String getStructure(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id, Model model,
			HttpServletRequest request, HttpServletResponse response) {
		return getStructure(idPrefix + ':' + id, model, request, response);
	}

	public String getStructure(String pid, Model model, HttpServletRequest request, HttpServletResponse response) {
		if (LOG.isDebugEnabled())
			LOG.debug("In Structure Browse controller for " + pid);
		String includeFiles = request.getParameter("files");

		if ("only".equals(includeFiles))
			return getItemChildren(pid, model, request, response);
		return getStructureTree(pid, "true".equals(includeFiles), model, request);
	}

	private String getStructureTree(String pid, boolean includeFiles, Model model, HttpServletRequest request) {
		String viewParam = request.getParameter("view");
		String view;
		boolean ajaxRequest;
		if ("ajax".equals(viewParam)) {
			view = "/jsp/structure/structureTree";
			model.addAttribute("template", "ajax");
			ajaxRequest = true;
		} else if ("facet".equals(viewParam)) {
			view = "/jsp/structure/facet";
			ajaxRequest = true;
		} else {
			// full view
			view = "/jsp/structure/search";
			ajaxRequest = false;
		}

		int depth;
		try {
			depth = Integer.parseInt(request.getParameter("depth"));
			if (depth > searchSettings.structuredDepthMax)
				depth = searchSettings.structuredDepthMax;
		} catch (Exception e) {
			depth = searchSettings.structuredDepthDefault;
		}

		// Request object for the search
		HierarchicalBrowseRequest browseRequest = new HierarchicalBrowseRequest(depth);
		if (ajaxRequest) {
			browseRequest.setSearchState(this.searchStateFactory.createStructureBrowseSearchState(request
					.getParameterMap()));
		} else {
			browseRequest.setSearchState(this.searchStateFactory.createHierarchicalBrowseSearchState(request
					.getParameterMap()));
		}
		if (pid != null)
			browseRequest.setRootPid(pid);
		if (includeFiles) {
			browseRequest.getSearchState().setRowsPerPage(searchSettings.defaultPerPage);
		} else {
			browseRequest.getSearchState().setRowsPerPage(0);
		}

		SearchState searchState = browseRequest.getSearchState();

		if (pid == null && !searchState.getFacets().containsKey(SearchFieldKeys.ANCESTOR_PATH.name())) {
			browseRequest.setRetrievalDepth(1);
		}

		HierarchicalBrowseResultResponse resultResponse = null;
		resultResponse = queryLayer.getHierarchicalBrowseResults(browseRequest);

		if (resultResponse != null) {
			// Get the display values for hierarchical facets from the search results.
			if (!ajaxRequest) {
				queryLayer.lookupHierarchicalDisplayValues(searchState, browseRequest.getAccessGroups());

				// Retrieve the facet result set
				SearchResultResponse resultResponseFacets = queryLayer.getFacetList(searchState,
						browseRequest.getAccessGroups(), searchState.getFacetsToRetrieve(), false);
				resultResponse.setFacetFields(resultResponseFacets.getFacetFields());
			}
			// Add the search state to the response.
			resultResponse.setSearchState(searchState);
		}

		String searchParams = SearchStateUtil.generateSearchParameterString(searchState);
		model.addAttribute("searchParams", searchParams);

		if (ajaxRequest) {
			model.addAttribute("template", "ajax");
		} else {
			model.addAttribute("resultType", "structure");
			model.addAttribute("pageSubtitle", "Browse Results");
			/*
			 * RecordNavigationState recordNavigationState = new RecordNavigationState();
			 * recordNavigationState.setSearchState(searchState); recordNavigationState.setSearchStateUrl(searchStateUrl);
			 * 
			 * recordNavigationState.setRecordIdList(resultResponse.getIdList());
			 * recordNavigationState.setTotalResults(resultResponse.getResultCount());
			 * 
			 * request.getSession().setAttribute("recordNavigationState", recordNavigationState);
			 */
		}

		model.addAttribute("resultResponse", resultResponse);
		model.addAttribute("structureResults", resultResponse);

		return view;
	}

	private List<String> tierResultFieldsList = Arrays.asList(SearchFieldKeys.ID.name(),
			SearchFieldKeys.ANCESTOR_PATH.name(), SearchFieldKeys.CONTENT_MODEL.name());

	private String getItemChildren(String pid, Model model, HttpServletRequest request, HttpServletResponse response) {
		BriefObjectMetadataBean selectedContainer = queryLayer.getObjectById(new SimpleIdRequest(pid,
				tierResultFieldsList));
		if (selectedContainer == null) {
			response.setStatus(404);
			return null;
		}

		SearchRequest browseRequest = new SearchRequest();
		generateSearchRequest(request, null, browseRequest);
		browseRequest.getSearchState().getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), selectedContainer.getPath());

		HierarchicalBrowseResultResponse resultResponse = queryLayer.getHierarchicalBrowseItemResult(browseRequest);
		model.addAttribute("structureResults", resultResponse);

		String searchStateUrl = SearchStateUtil.generateStateParameterString(browseRequest.getSearchState());
		model.addAttribute("searchStateUrl", searchStateUrl);

		model.addAttribute("template", "ajax");
		return "/jsp/structure/structureTree";
	}
}

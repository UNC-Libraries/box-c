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
import org.springframework.web.bind.annotation.RequestParam;

import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchStateUtil;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseRequest;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse;
import edu.unc.lib.dl.ui.exception.ResourceNotFoundException;

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

	private List<String> tierResultFieldsList = Arrays.asList(SearchFieldKeys.ID.name(),
			SearchFieldKeys.ANCESTOR_PATH.name(), SearchFieldKeys.CONTENT_MODEL.name());

	/**
	 * Retrieves the contents of the pid specified in a structural view
	 */
	@RequestMapping("/structure")
	public String getStructure(@RequestParam(value = "files", required = false) String includeFiles,
			@RequestParam(value = "view", required = false) String view, Model model, HttpServletRequest request) {
		return getStructureTree(null, "true".equals(includeFiles), view, false, model, request);
	}

	@RequestMapping("/structure/{prefix}/{id}")
	public String getStructure(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id,
			@RequestParam(value = "files", required = false) String includeFiles,
			@RequestParam(value = "view", required = false) String view, Model model, HttpServletRequest request) {
		return getStructureTree(idPrefix + ':' + id, "true".equals(includeFiles), view, false, model, request);
	}

	private String getStructureTree(String pid, boolean includeFiles, String viewParam, boolean collectionMode,
			Model model, HttpServletRequest request) {
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
		if (collectionMode)
			resultResponse = queryLayer.getStructureToParentCollection(browseRequest);
		else
			resultResponse = queryLayer.getHierarchicalBrowseResults(browseRequest);

		if (resultResponse != null) {
			// Get the display values for hierarchical facets from the search results.
			if (!ajaxRequest) {
				LOG.debug("Getting facets and display values");
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

	/**
	 * Retrieves a composite strucuture, containing the normal structure results starting at the specified pid, merged into
	 * a list of all resources in the first tier under the parent collection, linked by any intermediary folders.
	 */
	@RequestMapping("/structure/collection")
	public String getStructureFromParentCollection(@RequestParam(value = "files", required = false) String includeFiles,
			Model model, HttpServletRequest request) {
		return getStructureTree(null, "true".equals(includeFiles), "ajax", true, model, request);
	}
	
	@RequestMapping("/structure/{prefix}/{id}/collection")
	public String getStructureFromParentCollection(@PathVariable("prefix") String idPrefix,
			@PathVariable("id") String id, @RequestParam(value = "files", required = false) String includeFiles,
			Model model, HttpServletRequest request) {
		String pid = idPrefix + ':' + id;
		return getStructureTree(pid, "true".equals(includeFiles), "ajax", true, model, request);
	}

	/**
	 * Retrieves the structure of the contents of the parent of the specified pid.
	 */
	@RequestMapping("/structure/{prefix}/{id}/parent")
	public String getParentChildren(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id,
			@RequestParam(value = "files", required = false) String includeFiles, Model model, HttpServletRequest request) {
		String pid = idPrefix + ':' + id;
		// Get the parent pid for the selected object and get its structure view
		BriefObjectMetadataBean selectedContainer = queryLayer.getObjectById(new SimpleIdRequest(pid,
				tierResultFieldsList));
		if (selectedContainer == null)
			throw new ResourceNotFoundException("Object " + pid + " was not found.");

		return getStructureTree(selectedContainer.getAncestorPathFacet().getSearchKey(), "true".equals(includeFiles),
				"ajax", false, model, request);
	}

	/**
	 * Retrieves the direct children of the pid specified. If no pid is specified, then the root is used
	 */
	@RequestMapping("/structure/{prefix}/{id}/tier")
	public String getSingleTier(@PathVariable("prefix") String idPrefix, @PathVariable("id") String id,
			@RequestParam(value = "files", required = false) String includeFiles, Model model, HttpServletRequest request,
			HttpServletResponse response) {
		return getSingleTier(idPrefix + ':' + id, includeFiles, model, request, response);
	}

	private String getSingleTier(String pid, String includeFiles, Model model, HttpServletRequest request,
			HttpServletResponse response) {
		BriefObjectMetadataBean selectedContainer = queryLayer.getObjectById(new SimpleIdRequest(pid,
				tierResultFieldsList));
		if (selectedContainer == null)
			throw new ResourceNotFoundException("Object " + pid + " was not found.");

		SearchRequest browseRequest = new SearchRequest();
		generateSearchRequest(request, null, browseRequest);
		SearchState searchState = browseRequest.getSearchState();
		searchState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), selectedContainer.getPath());
		if ("only".equals(includeFiles))
			searchState.setResourceTypes(Arrays.asList(searchSettings.resourceTypeFile));
		else if ("true".equals(includeFiles))
			searchState.setResourceTypes(null);
		else
			searchState.setResourceTypes(Arrays.asList("!" + searchSettings.resourceTypeFile));

		HierarchicalBrowseResultResponse resultResponse = queryLayer.getStructureTier(browseRequest);
		model.addAttribute("structureResults", resultResponse);

		String searchStateUrl = SearchStateUtil.generateStateParameterString(browseRequest.getSearchState());
		model.addAttribute("searchStateUrl", searchStateUrl);

		model.addAttribute("template", "ajax");
		return "/jsp/structure/structureTree";
	}
}
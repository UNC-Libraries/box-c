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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.ui.exception.InvalidRecordRequestException;
import edu.unc.lib.dl.ui.model.RecordNavigationState;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseRequest;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.ui.service.FullObjectMetadataFactory;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.ui.view.XSLViewResolver;

import org.jdom.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Controller which retrieves extended metadata and returns a transformed view of it
 * 
 * @author bbpennel
 */
@Controller
@RequestMapping("/record")
public class FullRecordController extends AbstractSolrSearchController {
	private static final Logger LOG = LoggerFactory.getLogger(FullRecordController.class);

	@Autowired(required = true)
	private XSLViewResolver xslViewResolver;
	@Autowired
	private SearchSettings searchSettings;

	@RequestMapping(method = RequestMethod.GET)
	public String handleRequest(Model model, HttpServletRequest request) {
		String id = request.getParameter(searchSettings.searchStateParam(SearchFieldKeys.ID.name()));
		AccessGroupSet accessGroups = GroupsThreadStore.getGroups();
		SimpleIdRequest idRequest = new SimpleIdRequest(id, accessGroups);
		BriefObjectMetadataBean briefObject = queryLayer.getObjectById(idRequest);
		String fullObjectView = null;

		if (briefObject == null) {
			throw new InvalidRecordRequestException();
		} else {
			try {
				Document foxmlView = FullObjectMetadataFactory.getFoxmlViewXML(idRequest);
				if (foxmlView != null) {
					fullObjectView = xslViewResolver.renderView("external.xslView.fullRecord.url", foxmlView);
				}
			} catch(AuthorizationException e) {
				// TODO Go to the list only record page
			} catch (Exception e) {
				LOG.error("Failed to render full record view for " + idRequest.getId(), e);
			}
		}

		if (fullObjectView == null) {
			throw new InvalidRecordRequestException();
		}

		boolean retrieveChildrenCount = briefObject.getResourceType().equals(searchSettings.resourceTypeFolder); 
		boolean retrieveFacets = briefObject.getResourceType().equals(searchSettings.resourceTypeCollection) || briefObject.getResourceType().equals(searchSettings.resourceTypeAggregate);
		boolean retrieveNeighbors = briefObject.getResourceType().equals(searchSettings.resourceTypeFile)
				|| briefObject.getResourceType().equals(searchSettings.resourceTypeAggregate);
		boolean retrieveHierarchicalStructure = briefObject.getResourceType().equals(
				searchSettings.resourceTypeCollection)
				|| briefObject.getResourceType().equals(searchSettings.resourceTypeFolder)
				|| briefObject.getResourceType().equals(searchSettings.resourceTypeAggregate);

		if (retrieveChildrenCount) {
			briefObject.getCountMap().put("child", queryLayer.getChildrenCount(briefObject, accessGroups));
		}
		
		if (retrieveFacets) {
			List<String> facetsToRetrieve = null;
			if (briefObject.getResourceType().equals(searchSettings.resourceTypeCollection)){
				facetsToRetrieve = new ArrayList<String>(searchSettings.collectionBrowseFacetNames);
			} else if (briefObject.getResourceType().equals(searchSettings.resourceTypeAggregate)){
				facetsToRetrieve = new ArrayList<String>();
				facetsToRetrieve.add(SearchFieldKeys.CONTENT_TYPE.name());
			}
			
			LOG.debug("Retrieving supplemental information for container at path " + briefObject.getPath().toString());
			SearchResultResponse resultResponse = queryLayer.getFullRecordSupplementalData(briefObject.getPath(),
					accessGroups, facetsToRetrieve);

			briefObject.getCountMap().put("child", resultResponse.getResultCount());
			String collectionSearchStateUrl = searchSettings.searchStateParams.get("FACET_FIELDS") + "="
					+ searchSettings.searchFieldParams.get(SearchFieldKeys.ANCESTOR_PATH.name()) + ":"
					+ briefObject.getPath().getSearchValue();
			model.addAttribute("facetFields", resultResponse.getFacetFields());
			model.addAttribute("collectionSearchStateUrl", collectionSearchStateUrl);
		}
		
		if (retrieveHierarchicalStructure) {
			LOG.debug("Retrieving hierarchical structure for " + briefObject.getResourceType() + " " + id);

			// Retrieve hierarchical browse results
			SearchState searchState = searchStateFactory.createHierarchicalBrowseSearchState();
			searchState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH.name(), briefObject.getPath());
			searchState.setResourceTypes(null);
			HierarchicalBrowseRequest browseRequest = new HierarchicalBrowseRequest(searchState, 4, accessGroups);
			
			HierarchicalBrowseResultResponse hierarchicalResultResponse = null;

			if (briefObject.getResourceType().equals(searchSettings.resourceTypeAggregate)) {
				searchState.setRowsPerPage(100);
			} else {
				searchState.setRowsPerPage(20);
			}
			hierarchicalResultResponse = queryLayer.getHierarchicalBrowseResults(browseRequest);
			
			hierarchicalResultResponse.setResultCount(hierarchicalResultResponse.getResultList().size());
			
			if (LOG.isDebugEnabled() && hierarchicalResultResponse != null)
				LOG.debug(id + " returned " + hierarchicalResultResponse.getResultCount() + " hierarchical results.");
			
			model.addAttribute("hierarchicalViewResults", hierarchicalResultResponse);
			
			
		}
		
		if (retrieveNeighbors) {
			List<BriefObjectMetadataBean> neighbors = queryLayer.getNeighboringItems(briefObject,
					searchSettings.maxNeighborResults, accessGroups);
			model.addAttribute("neighborList", neighbors);
		}
		
		LOG.debug(briefObject.toString());
		model.addAttribute("briefObject", briefObject);
		model.addAttribute("fullObjectView", fullObjectView);

		RecordNavigationState recordNavigationState = (RecordNavigationState) request.getSession().getAttribute(
				"recordNavigationState");
		if (recordNavigationState != null) {
			int index = recordNavigationState.indexOf(id);
			if (index > -1) {
				recordNavigationState.setCurrentRecordId(id);
				recordNavigationState.setCurrentRecordIndex(index);
				request.getSession().setAttribute("recordNavigationState", recordNavigationState);
			}
		}

		model.addAttribute("pageSubtitle", briefObject.getTitle());
		return "fullRecord";
	}

	@ResponseStatus(value = HttpStatus.FORBIDDEN)
	@ExceptionHandler(InvalidRecordRequestException.class)
	public String handleInvalidRecordRequest(HttpServletRequest request) {
		request.setAttribute("pageSubtitle", "Invalid record");
		return "error/invalidRecord";
	}

	public XSLViewResolver getXslViewResolver() {
		return xslViewResolver;
	}

	public void setXslViewResolver(XSLViewResolver xslViewResolver) {
		this.xslViewResolver = xslViewResolver;
	}
}

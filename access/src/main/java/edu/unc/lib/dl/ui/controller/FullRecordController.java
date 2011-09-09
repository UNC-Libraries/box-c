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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import edu.unc.lib.dl.security.access.AccessGroupSet;
import edu.unc.lib.dl.ui.exception.InvalidRecordRequestException;
import edu.unc.lib.dl.ui.model.RecordNavigationState;
import edu.unc.lib.dl.ui.model.request.HierarchicalBrowseRequest;
import edu.unc.lib.dl.ui.model.response.HierarchicalBrowseResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.ui.service.FullObjectMetadataFactory;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.ui.validator.DatastreamAccessValidator;
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
 * @author bbpennel
 * $Id: FullRecordController.java 2743 2011-08-12 16:56:19Z bbpennel $
 * $URL: https://vcs.lib.unc.edu/cdr/cdr-master/trunk/access/src/main/java/edu/unc/lib/dl/ui/controller/FullRecordController.java $
 */
@Controller
@RequestMapping("/record")
public class FullRecordController extends AbstractSolrSearchController {
	private static final Logger LOG = LoggerFactory.getLogger(FullRecordController.class);
	
	@Autowired(required=true)
	private XSLViewResolver xslViewResolver;
	@Autowired
	private SearchSettings searchSettings;
	
	@RequestMapping(method = RequestMethod.GET)
	public String handleRequest(Model model, HttpServletRequest request){
		String id = request.getParameter(searchSettings.searchStateParam(SearchFieldKeys.ID));
		AccessGroupSet accessGroups = getUserAccessGroups(request);
		SimpleIdRequest idRequest = new SimpleIdRequest(id, accessGroups);
		BriefObjectMetadataBean briefObject = queryLayer.getObjectById(idRequest);
		String fullObjectView = null;
		
		if (briefObject != null){
			//Filter the datastreams in the response according to the users permissions
			DatastreamAccessValidator.filterBriefObject(briefObject, accessGroups);
			try {
				Document foxmlView =  FullObjectMetadataFactory.getFoxmlViewXML(idRequest);
				if (foxmlView.getRootElement().getContent().size() > 0)
					fullObjectView = xslViewResolver.renderView("external.xslView.fullRecord.url", foxmlView);
			} catch (Exception e) {
				LOG.error("Failed to render XSL view", e);
			}
		}

		if (briefObject == null || fullObjectView == null){
			throw new InvalidRecordRequestException();
		}
		
		
		
		//If the retrieved item is a collection then need to get supplemental info
		if (briefObject.getResourceType().equals(searchSettings.resourceTypeCollection) || briefObject.getResourceType().equals(searchSettings.resourceTypeFolder)){
			SearchResultResponse resultResponse = queryLayer.getFullRecordSupplementalData(briefObject.getPath(), accessGroups);
			
			briefObject.setChildCount(resultResponse.getResultCount());
			String collectionSearchStateUrl = searchSettings.searchStateParams.get("FACET_FIELDS") + "=" 
					+ searchSettings.searchFieldParams.get(SearchFieldKeys.ANCESTOR_PATH) + ":" + briefObject.getPath().getSearchValue(); 
			model.addAttribute("facetFields", resultResponse.getFacetFields());
			model.addAttribute("collectionSearchStateUrl", collectionSearchStateUrl);
			
			
			//Retrieve hierarchical browse results
			SearchState searchState = SearchStateFactory.createHierarchicalBrowseSearchState();
			searchState.getFacets().put(SearchFieldKeys.ANCESTOR_PATH, briefObject.getPath());
			searchState.setRowsPerPage(0);
			
			HierarchicalBrowseResultResponse hierarchicalResultResponse = queryLayer.getHierarchicalBrowseResults(new HierarchicalBrowseRequest(searchState, 4, accessGroups));
			
			model.addAttribute("hierarchicalViewResults", hierarchicalResultResponse);
		} else if (briefObject.getResourceType().equals(searchSettings.resourceTypeFile)){
			List<BriefObjectMetadataBean> neighbors = queryLayer.getNeighboringItems(briefObject, searchSettings.maxNeighborResults, accessGroups);
			for (BriefObjectMetadataBean neighbor: neighbors){
				DatastreamAccessValidator.filterBriefObject(neighbor, accessGroups);
			}
			model.addAttribute("neighborList", neighbors);
		}
		LOG.debug(briefObject.toString());
		model.addAttribute("briefObject", briefObject);
		model.addAttribute("fullObjectView", fullObjectView);
		
		RecordNavigationState recordNavigationState = (RecordNavigationState)request.getSession().getAttribute("recordNavigationState");
		if (recordNavigationState != null){
			int index = recordNavigationState.indexOf(id);
			if (index > -1){
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
	public String handleInvalidRecordRequest(HttpServletRequest request){
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

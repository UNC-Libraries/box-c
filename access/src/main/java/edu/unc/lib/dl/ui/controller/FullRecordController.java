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
import edu.unc.lib.dl.fedora.FedoraDataService;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.ui.exception.InvalidRecordRequestException;
import edu.unc.lib.dl.ui.exception.RenderViewException;
import edu.unc.lib.dl.ui.model.RecordNavigationState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.ui.view.XSLViewResolver;
import edu.unc.lib.dl.util.ContentModelHelper;

import org.jdom.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Controller which retrieves data necessary for populating the full record page, retrieving supplemental information
 * according to the specifics of the object being retrieved.
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
	private FedoraDataService fedoraDataService;
	
	private static final int MAX_FOXML_TRIES = 2;

	@RequestMapping(value = "/{pid}", method = RequestMethod.GET)
	public String handleRequest(@PathVariable("pid") String pid, Model model, HttpServletRequest request) {
		return getFullRecord(pid, model, request);
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public String handleOldRequest(@RequestParam("id") String id, Model model, HttpServletRequest request) {
		return getFullRecord(id, model, request);
	}
	
	public String getFullRecord(String pid, Model model, HttpServletRequest request) {
		
		AccessGroupSet accessGroups = GroupsThreadStore.getGroups();
		
		// Retrieve the objects record from Solr
		SimpleIdRequest idRequest = new SimpleIdRequest(pid, accessGroups);
		BriefObjectMetadataBean briefObject = queryLayer.getObjectById(idRequest);
		if (briefObject == null) {
			throw new InvalidRecordRequestException();
		}
		model.addAttribute("briefObject", briefObject);
		
		// Retrieve the objects description from Fedora
		String fullObjectView = null;
		boolean containsContent = false;
		try {
			Document foxmlView;
			int retries = MAX_FOXML_TRIES;
			do {
				foxmlView = fedoraDataService.getFoxmlViewXML(idRequest.getId());
				containsContent = foxmlView.getRootElement().getContent().size() > 0;
			} while (--retries > 0 && !containsContent);
			
			if (containsContent) {
				fullObjectView = xslViewResolver.renderView("external.xslView.fullRecord.url", foxmlView);
			} else {
				throw new InvalidRecordRequestException("Failed to retrieve FOXML for object " + idRequest.getId());
			}
		} catch (AuthorizationException e) {
			LOG.debug("Access to the full record was denied, user has list only access");
			model.addAttribute("listAccess", true);
		} catch (NotFoundException e) {
			throw new InvalidRecordRequestException(e);
		} catch (FedoraException e) {
			LOG.error("Failed to render full record view for " + idRequest.getId(), e);
		} catch (RenderViewException e) {
			LOG.error("Failed to render full record view for " + idRequest.getId(), e);
		}

		// Get additional information depending on the type of object since the user has access
		if (fullObjectView != null) {
			boolean retrieveChildrenCount = briefObject.getResourceType().equals(searchSettings.resourceTypeFolder);
			boolean retrieveFacets = briefObject.getContentModel().contains(ContentModelHelper.Model.CONTAINER.toString());

			if (retrieveChildrenCount) {
				briefObject.getCountMap().put("child", queryLayer.getChildrenCount(briefObject, accessGroups));
			}

			if (retrieveFacets) {
				List<String> facetsToRetrieve = null;
				if (briefObject.getResourceType().equals(searchSettings.resourceTypeCollection)) {
					facetsToRetrieve = new ArrayList<String>(searchSettings.collectionBrowseFacetNames);
				} else if (briefObject.getResourceType().equals(searchSettings.resourceTypeAggregate)) {
					facetsToRetrieve = new ArrayList<String>();
					facetsToRetrieve.add(SearchFieldKeys.CONTENT_TYPE.name());
				}

				LOG.debug("Retrieving supplemental information for container at path " + briefObject.getPath().toString());
				SearchResultResponse resultResponse = queryLayer.getFullRecordSupplementalData(briefObject.getPath(),
						accessGroups, facetsToRetrieve);

				briefObject.getCountMap().put("child", resultResponse.getResultCount());
				model.addAttribute("facetFields", resultResponse.getFacetFields());
			}

			model.addAttribute("fullObjectView", fullObjectView);
		}
		
		if (briefObject.getResourceType().equals(searchSettings.resourceTypeFile) ||
				briefObject.getResourceType().equals(searchSettings.resourceTypeAggregate)) {
			List<BriefObjectMetadataBean> neighbors = queryLayer.getNeighboringItems(briefObject,
					searchSettings.maxNeighborResults, accessGroups);
			model.addAttribute("neighborList", neighbors);
		}

		// Store search state information to the users session to enable page to page navigation
		RecordNavigationState recordNavigationState = (RecordNavigationState) request.getSession().getAttribute(
				"recordNavigationState");
		if (recordNavigationState != null) {
			int index = recordNavigationState.indexOf(pid);
			if (index > -1) {
				recordNavigationState.setCurrentRecordId(pid);
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

	public void setXslViewResolver(XSLViewResolver xslViewResolver) {
		this.xslViewResolver = xslViewResolver;
	}
}

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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;

import org.jdom2.Document;
import org.jdom2.Element;
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

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.fedora.AuthorizationException;
import edu.unc.lib.dl.fedora.FedoraDataService;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.ServiceException;
import edu.unc.lib.dl.model.ContainerSettings;
import edu.unc.lib.dl.model.ContainerSettings.ContainerView;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.FacetFieldObject;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.service.ObjectPathFactory;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.ui.exception.InvalidRecordRequestException;
import edu.unc.lib.dl.ui.exception.RenderViewException;
import edu.unc.lib.dl.ui.util.AccessUtil;
import edu.unc.lib.dl.ui.view.XSLViewResolver;
import edu.unc.lib.dl.util.ContentModelHelper;
import edu.unc.lib.dl.util.ContentModelHelper.Datastream;
import edu.unc.lib.dl.util.ResourceType;
import edu.unc.lib.dl.xml.FOXMLJDOMUtil;
import edu.unc.lib.dl.xml.JDOMNamespaceUtil;

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
	@Autowired
	private ObjectPathFactory pathFactory;
	@Autowired
	SearchStateFactory stateFactory;

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
		// Get path information.
		model.addAttribute("briefObject", briefObject);

		boolean listAccess = AccessUtil.hasListAccessOnly(accessGroups, briefObject);

		Date embargoUntil = briefObject.getActiveEmbargo();
		if (embargoUntil != null) {
			model.addAttribute("embargoDate", embargoUntil);
		}

		// Retrieve the objects description from Fedora
		String fullObjectView = null;
		boolean containsContent = false;
		
		Document foxmlView = null;
		if (!listAccess) {
			try {
				int retries = MAX_FOXML_TRIES;
				do {
					foxmlView = fedoraDataService.getFoxmlViewXML(idRequest.getId());
					containsContent = foxmlView.getRootElement().getContent().size() > 0;
				} while (--retries > 0 && !containsContent);

				if (containsContent) {
					Element foxml = foxmlView.getRootElement().getChild("digitalObject", JDOMNamespaceUtil.FOXML_NS);
					Element mods = FOXMLJDOMUtil.getMostRecentDatastream(Datastream.MD_DESCRIPTIVE, foxml);
					
					if (mods != null) {
						mods = mods.getChild("xmlContent", JDOMNamespaceUtil.FOXML_NS)
								.getChild("mods", JDOMNamespaceUtil.MODS_V3_NS);
						fullObjectView = xslViewResolver.renderView("external.xslView.fullRecord.url", mods);
					}
				} else {
					throw new InvalidRecordRequestException("Failed to retrieve FOXML for object " + idRequest.getId());
				}
			} catch (AuthorizationException e) {
				LOG.debug("Access to the full record was denied, user has list only access");
				listAccess = true;
			} catch (NotFoundException e) {
				throw new InvalidRecordRequestException(e);
			} catch (FedoraException e) {
				LOG.error("Failed to render full record view for " + idRequest.getId(), e);
			} catch (RenderViewException e) {
				LOG.error("Failed to render full record view for " + idRequest.getId(), e);
			} catch (ServiceException e) {
				if (e.getCause() instanceof TimeoutException) {
					LOG.warn("Maximum retrieval time exceeded while retrieving FOXML for full record of {}",
							idRequest.getId());
				} else {
					LOG.error("Failed to retrieve FOXML for object {}" , idRequest.getId(), e);
				}
			}
		}

		// Get additional information depending on the type of object since the user has access
		if (!listAccess) {
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
				
				boolean hasFacets = false;
				for (FacetFieldObject facetField : resultResponse.getFacetFields()) {
					if (facetField.getValues().size() > 0) {
						hasFacets = true;
						break;
					}
				}
				
				model.addAttribute("hasFacetFields", hasFacets);
				model.addAttribute("facetFields", resultResponse.getFacetFields());
			}

			model.addAttribute("fullObjectView", fullObjectView);
		}

		if (briefObject.getResourceType().equals(searchSettings.resourceTypeFile) ||
				briefObject.getResourceType().equals(searchSettings.resourceTypeAggregate)) {
			List<BriefObjectMetadataBean> neighbors = queryLayer.getNeighboringItems(briefObject,
					searchSettings.maxNeighborResults, accessGroups);
			model.addAttribute("neighborList", neighbors);
			
			// Get previous and next record in the same folder if there are any
			List<BriefObjectMetadataBean> previousNext = new ArrayList<>();
			
			int selectedRecord = 0;
			int index = 0;
			
			for (BriefObjectMetadataBean neighbor : neighbors) {
				if (neighbor.getId().equals(briefObject.getId())) {
					selectedRecord = index;
					break;
				}
				index++;
			}
			
			if (selectedRecord - 1 > -1) {
				previousNext.add(neighbors.get(selectedRecord - 1));
			} else {
				previousNext.add(null);
			}
			
			if (selectedRecord + 1 < neighbors.size()) {
				previousNext.add(neighbors.get(selectedRecord + 1));
			} else {
				previousNext.add(null);
			}
			model.addAttribute("previousNext", previousNext);
		}
		
		if (briefObject.getResourceType().equals(searchSettings.resourceTypeCollection)
				|| briefObject.getResourceType().equals(searchSettings.resourceTypeFolder)) {
			applyContainerSettings(pid, foxmlView, model, fullObjectView != null);
		}

		model.addAttribute("listAccess", listAccess);

		model.addAttribute("pageSubtitle", briefObject.getTitle());
		return "fullRecord";
	}
	
	// The default collection tab views which are retrieved if no settings are found
	private static List<String> defaultViews =
			Arrays.asList(ContainerView.STRUCTURE.name(), ContainerView.EXPORTS.name());
	
	private static List<String> defaultViewsDescriptive =
			Arrays.asList(ContainerView.DESCRIPTION.name(), ContainerView.STRUCTURE.name(),
					ContainerView.EXPORTS.name());
	
	private void applyContainerSettings(String pid, Document foxml, Model model, boolean hasDescription) {
		if (foxml == null) {
			return;
		}
		
		ContainerSettings settings = new ContainerSettings(foxml.getRootElement().getChildren().get(0));
		
		if (settings.getViews().size() == 0) {
			// Only include the metadata tab by default if there is a descriptive record
			if (hasDescription) {
				settings.setViews(defaultViewsDescriptive);
			} else {
				settings.setViews(defaultViews);
			}
		}
		
		if (settings.getDefaultView() == null) {
			settings.setDefaultView(ContainerView.STRUCTURE.name());
		}
		
		// Populate department list
		if (settings.getViews().contains(ContainerView.DEPARTMENTS.name())) {
			SearchResultResponse result = queryLayer.getDepartmentList(GroupsThreadStore.getGroups(), pid);
			model.addAttribute("departmentFacets", result.getFacetFields().get(0));
		}
		
		// Populate file list
		if (settings.getViews().contains(ContainerView.LIST_CONTENTS.name())) {
			SearchState searchState = stateFactory.createSearchState();
			searchState.setResourceTypes(
					Arrays.asList(ResourceType.Aggregate.name(), ResourceType.File.name()));
			SearchRequest listContentsRequest = new SearchRequest();
			listContentsRequest.setSearchState(searchState);
			listContentsRequest.setRetrieveFacets(false);
			listContentsRequest.setApplyCutoffs(false);
			listContentsRequest.setRootPid(pid);
			listContentsRequest.getSearchState().setRollup(true);
			
			SearchResultResponse contentListResponse = queryLayer.performSearch(listContentsRequest);
			model.addAttribute("contentListResponse", contentListResponse);
		}

		model.addAttribute("containerSettings", settings);
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

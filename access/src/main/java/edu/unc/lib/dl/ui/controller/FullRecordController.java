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

import static edu.unc.lib.dl.acl.util.GroupsThreadStore.getAgentPrincipals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
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

import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.GroupsThreadStore;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fedora.FedoraException;
import edu.unc.lib.dl.fedora.NotFoundException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.model.ContainerSettings;
import edu.unc.lib.dl.model.ContainerSettings.ContainerView;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.FacetFieldObject;
import edu.unc.lib.dl.search.solr.model.SearchRequest;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchState;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.service.ChildrenCountService;
import edu.unc.lib.dl.search.solr.service.NeighborQueryService;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.ui.exception.InvalidRecordRequestException;
import edu.unc.lib.dl.ui.exception.RenderViewException;
import edu.unc.lib.dl.ui.view.XSLViewResolver;
import edu.unc.lib.dl.util.ResourceType;

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

    @Autowired
    private AccessControlService aclService;
    @Autowired
    private ChildrenCountService childrenCountService;
    @Autowired
    private NeighborQueryService neighborService;

    @Autowired(required = true)
    private XSLViewResolver xslViewResolver;
    @Autowired
    private SearchStateFactory stateFactory;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;

    @RequestMapping(value = "/{pid}", method = RequestMethod.GET)
    public String handleRequest(@PathVariable("pid") String pid, Model model, HttpServletRequest request) {
        return getFullRecord(pid, model, request);
    }

    @RequestMapping(method = RequestMethod.GET)
    public String handleOldRequest(@RequestParam("id") String id, Model model, HttpServletRequest request) {
        return getFullRecord(id, model, request);
    }

    public String getFullRecord(String pidString, Model model, HttpServletRequest request) {
        PID pid = PIDs.get(pidString);

        AccessGroupSet principals = getAgentPrincipals().getPrincipals();

        aclService.assertHasAccess("Insufficient permissions to access full record",
                pid, principals, Permission.viewMetadata);

        // Retrieve the objects record from Solr
        SimpleIdRequest idRequest = new SimpleIdRequest(pidString, principals);
        BriefObjectMetadataBean briefObject = queryLayer.getObjectById(idRequest);
        if (briefObject == null) {
            throw new InvalidRecordRequestException();
        }
        // Get path information.
        model.addAttribute("briefObject", briefObject);

        Date embargoUntil = briefObject.getActiveEmbargo();
        if (embargoUntil != null) {
            model.addAttribute("embargoDate", embargoUntil);
        }

        // Retrieve the objects description from Fedora
        String fullObjectView = null;
        try {
            ContentObject contentObj = (ContentObject) repositoryObjectLoader.getRepositoryObject(pid);

            BinaryObject modsObj = contentObj.getMODS();
            if (modsObj != null) {
                SAXBuilder builder = new SAXBuilder();
                Document modsDoc = builder.build(modsObj.getBinaryStream());

                fullObjectView = xslViewResolver.renderView("external.xslView.fullRecord.url", modsDoc);
            }
        } catch (NotFoundException e) {
            throw new InvalidRecordRequestException(e);
        } catch (FedoraException e) {
            LOG.error("Failed to retrieve object {} from fedora", idRequest.getId(), e);
        } catch (RenderViewException e) {
            LOG.error("Failed to render full record view for {}", idRequest.getId(), e);
        } catch (JDOMException | IOException e) {
            LOG.error("Failed to parse MODS document for {}", idRequest.getId(), e);
        }

        // Get additional information depending on the type of object since the user has access
        String resourceType = briefObject.getResourceType();
        boolean retrieveChildrenCount = resourceType.equals(searchSettings.resourceTypeFolder);
        boolean retrieveFacets = resourceType.equals(searchSettings.resourceTypeFolder)
                || resourceType.equals(searchSettings.resourceTypeCollection);

        if (retrieveChildrenCount) {
            briefObject.getCountMap().put("child", childrenCountService.getChildrenCount(briefObject, principals));
        }

        if (retrieveFacets) {
            List<String> facetsToRetrieve = null;
            if (briefObject.getResourceType().equals(searchSettings.resourceTypeCollection)) {
                facetsToRetrieve = new ArrayList<>(searchSettings.collectionBrowseFacetNames);
            } else if (briefObject.getResourceType().equals(searchSettings.resourceTypeAggregate)) {
                facetsToRetrieve = new ArrayList<>();
                facetsToRetrieve.add(SearchFieldKeys.CONTENT_TYPE.name());
            }

            LOG.debug("Retrieving supplemental information for container at path "
                    + briefObject.getPath().toString());
            SearchResultResponse resultResponse = queryLayer.getFullRecordSupplementalData(briefObject.getPath(),
                    principals, facetsToRetrieve);

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

        if (briefObject.getResourceType().equals(searchSettings.resourceTypeFile) ||
                briefObject.getResourceType().equals(searchSettings.resourceTypeAggregate)) {
            List<BriefObjectMetadataBean> neighbors = neighborService.getNeighboringItems(briefObject,
                    searchSettings.maxNeighborResults, principals);
            model.addAttribute("neighborList", neighbors);

            // Get previous and next record in the same folder if there are any
            Map<String, BriefObjectMetadataBean> previousNext = new HashMap<>();

            int selectedRecord = -1;
            for (BriefObjectMetadataBean neighbor : neighbors) {
                if (neighbor.getId().equals(briefObject.getId())) {
                    selectedRecord = neighbors.indexOf(neighbor);
                    break;
                }
            }

            if (selectedRecord != -1) {
                if (selectedRecord > 0) {
                    previousNext.put("previous", neighbors.get(selectedRecord - 1));
                }

                if (selectedRecord + 1 < neighbors.size()) {
                    previousNext.put("next", neighbors.get(selectedRecord + 1));
                }
            }

            model.addAttribute("previousNext", previousNext);
        }

//        if (briefObject.getResourceType().equals(searchSettings.resourceTypeCollection)
//                || briefObject.getResourceType().equals(searchSettings.resourceTypeFolder)) {
//            applyContainerSettings(pidString, principals, foxmlView, model, fullObjectView != null);
//        }

        model.addAttribute("pageSubtitle", briefObject.getTitle());
        return "fullRecord";
    }

    // The default collection tab views which are retrieved if no settings are found
    private static List<String> defaultViews =
            Arrays.asList(ContainerView.STRUCTURE.name(), ContainerView.EXPORTS.name());

    private static List<String> defaultViewsDescriptive =
            Arrays.asList(ContainerView.DESCRIPTION.name(), ContainerView.STRUCTURE.name(),
                    ContainerView.EXPORTS.name());

    private void applyContainerSettings(String pid, AccessGroupSet principals, Document foxml, Model model,
            boolean hasDescription) {
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
                    Arrays.asList(ResourceType.Work.name(), ResourceType.File.name()));
            SearchRequest listContentsRequest = new SearchRequest();
            listContentsRequest.setSearchState(searchState);
            listContentsRequest.setRetrieveFacets(false);
            listContentsRequest.setApplyCutoffs(false);
            listContentsRequest.setRootPid(pid);
            listContentsRequest.getSearchState().setRollup(true);

            SearchResultResponse contentListResponse = queryLayer.performSearch(listContentsRequest);
            childrenCountService.addChildrenCounts(contentListResponse.getResultList(), principals);
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

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
package edu.unc.lib.boxc.web.access.controllers;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.InvalidPidException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.services.ChildrenCountService;
import edu.unc.lib.boxc.search.solr.services.GetCollectionIdService;
import edu.unc.lib.boxc.search.solr.services.NeighborQueryService;
import edu.unc.lib.boxc.web.common.controllers.AbstractErrorHandlingSearchController;
import edu.unc.lib.boxc.web.common.exceptions.InvalidRecordRequestException;
import edu.unc.lib.boxc.web.common.exceptions.RenderViewException;
import edu.unc.lib.boxc.web.common.exceptions.ResourceNotFoundException;
import edu.unc.lib.boxc.web.common.services.AccessCopiesService;
import edu.unc.lib.boxc.web.common.services.FindingAidUrlService;
import edu.unc.lib.boxc.web.common.services.XmlDocumentFilteringService;
import edu.unc.lib.boxc.web.common.utils.ModsUtil;
import edu.unc.lib.boxc.web.common.view.XSLViewResolver;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.boxc.common.xml.SecureXMLFactory.createSAXBuilder;
import static edu.unc.lib.boxc.search.api.FacetConstants.MARKED_FOR_DELETION;

/**
 * Controller which retrieves data necessary for populating the full record page, retrieving supplemental information
 * according to the specifics of the object being retrieved.
 *
 * @author bbpennel
 */
@Controller
@RequestMapping("/record")
public class FullRecordController extends AbstractErrorHandlingSearchController {
    private static final Logger LOG = LoggerFactory.getLogger(FullRecordController.class);

    @Autowired
    private AccessControlService aclService;
    @Autowired
    private ChildrenCountService childrenCountService;
    @Autowired
    private NeighborQueryService neighborService;
    @Autowired
    private GetCollectionIdService collectionIdService;
    @Autowired
    private FindingAidUrlService findingAidUrlService;
    @Autowired
    private AccessCopiesService accessCopiesService;
    @Autowired
    private XmlDocumentFilteringService xmlDocumentFilteringService;

    @Autowired(required = true)
    private XSLViewResolver xslViewResolver;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;

    @GetMapping("/{pid}")
    public ModelAndView handleRequest(@PathVariable("pid") String pid, Model model, HttpServletRequest request) {
        String normalizedPid = normalizePid(pid);
        // Permanently redirect to the normalized PID
        if (!normalizedPid.equals(pid)) {
            request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.MOVED_PERMANENTLY);
            return new ModelAndView("redirect:/record/" + normalizedPid, model.asMap());
        }
        return new ModelAndView(getFullRecord(pid, model, request));
    }

    @GetMapping
    public ModelAndView handleOldRequest(@RequestParam("id") String id, Model model, HttpServletRequest request) {
        String normalizedPid = normalizePid(id);
        // Permanently redirect to current syntax
        request.setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, HttpStatus.MOVED_PERMANENTLY);
        return new ModelAndView("redirect:/record/" + normalizedPid, model.asMap());
    }

    private String normalizePid(String pid) {
        if (pid == null) {
            return null;
        }
        return pid.trim().replaceFirst("^uuid:", "");
    }

    @GetMapping("/{pid}/metadataView")
    @ResponseBody
    public String handleFullObjectRequest(@PathVariable("pid") String pid, Model model, HttpServletRequest request,
                                          HttpServletResponse response) {
        response.setContentType("text/html");
        response.setCharacterEncoding("UTF-8");
        return getFullObjectView(pid);
    }


    public String getFullObjectView(String pidString) {
        PID pid = PIDs.get(pidString);

        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        aclService.assertHasAccess("Insufficient permissions to access full record metadata for " + pidString,
                pid, principals, Permission.viewMetadata);

        SimpleIdRequest idRequest = new SimpleIdRequest(pid, principals);

        // Retrieve the objects description from Fedora
        String fullObjectView = null;
        try {
            ContentObject contentObj = (ContentObject) repositoryObjectLoader.getRepositoryObject(pid);

            BinaryObject modsObj = contentObj.getDescription();
            if (modsObj != null) {
                SAXBuilder builder = createSAXBuilder();
                try (InputStream modsStream = modsObj.getBinaryStream()) {
                    Document modsDoc = builder.build(modsStream);
                    xmlDocumentFilteringService.filterExclusions(modsDoc);
                    fullObjectView = xslViewResolver.renderView("external.xslView.fullRecord.url",
                            ModsUtil.removeEmptyNodes(modsDoc));
                }
            }
        } catch (NotFoundException e) {
            throw e;
        } catch (FedoraException e) {
            LOG.error("Failed to retrieve object {} from fedora", idRequest.getId(), e);
        } catch (RenderViewException e) {
            LOG.error("Failed to render full record view for {}", idRequest.getId(), e);
        } catch (JDOMException | IOException e) {
            LOG.error("Failed to parse MODS document for {}", idRequest.getId(), e);
        }

        if (fullObjectView == null) {
            return "No metadata is available for this item";
        }

        return fullObjectView;
    }

    public String getFullRecord(String pidString, Model model, HttpServletRequest request) {
        PID pid = PIDs.get(pidString);

        AccessGroupSet principals = getAgentPrincipals().getPrincipals();

        try {
            aclService.assertHasAccess("Insufficient permissions to access full record for " + pidString,
                    pid, principals, Permission.viewMetadata);
        } catch (AccessRestrictionException e) {
            LOG.info("{}", e.getMessage());
            throw new InvalidRecordRequestException();
        }

        // Retrieve the object's record from Solr
        SimpleIdRequest idRequest = new SimpleIdRequest(pid, principals);
        ContentObjectRecord briefObject = queryLayer.getObjectById(idRequest);

        if (briefObject == null) {
            throw new InvalidRecordRequestException();
        }

        // Get path information.
        model.addAttribute("briefObject", briefObject);

        Date embargoUntil = briefObject.getActiveEmbargo();
        if (embargoUntil != null) {
            model.addAttribute("embargoDate", embargoUntil);
        }

        // Get additional information depending on the type of object since the user has access
        String resourceType = briefObject.getResourceType();
        if (!resourceType.equals(searchSettings.resourceTypeFile)) {
            briefObject.getCountMap().put("child", childrenCountService.getChildrenCount(briefObject, principals));
        }

        if (resourceType.equals(searchSettings.resourceTypeFolder) ||
                resourceType.equals(searchSettings.resourceTypeFile) ||
                resourceType.equals(searchSettings.resourceTypeAggregate) ||
                resourceType.equals(searchSettings.resourceTypeCollection)) {
            String collectionId = collectionIdService.getCollectionId(briefObject);
            String faUrl = findingAidUrlService.getFindingAidUrl(collectionId);
            model.addAttribute("collectionId", collectionId);
            model.addAttribute("findingAidUrl", faUrl);
        }

        if (briefObject.getResourceType().equals(searchSettings.resourceTypeFile) ||
                briefObject.getResourceType().equals(searchSettings.resourceTypeAggregate)) {
            List<ContentObjectRecord> neighbors = neighborService.getNeighboringItems(briefObject,
                    searchSettings.maxNeighborResults, principals);
            model.addAttribute("neighborList", neighbors);
        }

        if (briefObject.getResourceType().equals(searchSettings.resourceTypeAggregate)) {
            boolean imageViewerNeeded = accessCopiesService.hasViewableFiles(briefObject, principals);
            model.addAttribute("imageViewerNeeded", imageViewerNeeded);

            boolean pdfViewerNeeded = false;

            if (!imageViewerNeeded) {
                // Check if primary object is a pdf
                pdfViewerNeeded = accessCopiesService.hasViewablePdf(briefObject, principals);

                // Check if first child is a pdf
                if (!pdfViewerNeeded) {
                    String childObjectPid = accessCopiesService.getViewablePdfFilePid(briefObject, principals);
                    if (childObjectPid != null) {
                        pdfViewerNeeded = true;
                        model.addAttribute("pdfViewerPid", childObjectPid);
                    }
                }
            }

            model.addAttribute("pdfViewerNeeded", pdfViewerNeeded);
        }

        List<String> objectStatus = briefObject.getStatus();
        boolean isMarkedForDeletion = false;

        if (objectStatus != null) {
            isMarkedForDeletion = objectStatus.contains(MARKED_FOR_DELETION);
        }
        model.addAttribute("markedForDeletion", isMarkedForDeletion);

        model.addAttribute("pageSubtitle", briefObject.getTitle());
        return "fullRecord";
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @ExceptionHandler({ InvalidRecordRequestException.class, InvalidPidException.class,
            NotFoundException.class, ResourceNotFoundException.class })
    public String handleInvalidRecordRequest(RuntimeException ex, HttpServletRequest request) {
        request.setAttribute("pageSubtitle", "Invalid request");
        LOG.debug("Invalid record request", ex);
        return "error/invalidRecord";
    }

    public void setXslViewResolver(XSLViewResolver xslViewResolver) {
        this.xslViewResolver = xslViewResolver;
    }
}

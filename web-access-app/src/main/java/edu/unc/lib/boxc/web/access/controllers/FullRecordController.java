package edu.unc.lib.boxc.web.access.controllers;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.services.ObjectAclFactory;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.FacetConstants;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.facets.FilterableDisplayValueFacet;
import edu.unc.lib.boxc.search.solr.services.ChildrenCountService;
import edu.unc.lib.boxc.search.solr.services.GetCollectionIdService;
import edu.unc.lib.boxc.search.solr.services.NeighborQueryService;
import edu.unc.lib.boxc.web.common.controllers.AbstractErrorHandlingSearchController;
import edu.unc.lib.boxc.web.common.exceptions.RenderViewException;
import edu.unc.lib.boxc.web.common.services.AccessCopiesService;
import edu.unc.lib.boxc.web.common.services.FindingAidUrlService;
import edu.unc.lib.boxc.web.common.services.XmlDocumentFilteringService;
import edu.unc.lib.boxc.web.common.utils.ModsUtil;
import edu.unc.lib.boxc.web.common.utils.SerializationUtil;
import edu.unc.lib.boxc.web.common.view.XSLViewResolver;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore.getAgentPrincipals;
import static edu.unc.lib.boxc.common.xml.SecureXMLFactory.createSAXBuilder;
import static edu.unc.lib.boxc.search.api.FacetConstants.MARKED_FOR_DELETION;
import static edu.unc.lib.boxc.web.common.services.AccessCopiesService.AUDIO_MIMETYPE_REGEX;
import static edu.unc.lib.boxc.web.common.services.AccessCopiesService.PDF_MIMETYPE_REGEX;
import static edu.unc.lib.boxc.web.common.services.AccessCopiesService.VIDEO_MIMETYPE_REGEX;
import static info.freelibrary.iiif.presentation.v3.MediaType.APPLICATION_PDF;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

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
    private final String VIEWER_PID = "viewerPid";
    private final String VIEWER_TYPE = "viewerType";
    private final String STREAMING_URL = "streamingUrl";
    private final String STREAMING_TYPE = "streamingType";

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
    @Autowired
    private ObjectAclFactory objectAclFactory;

    @Autowired(required = true)
    private XSLViewResolver xslViewResolver;
    @Autowired
    private RepositoryObjectLoader repositoryObjectLoader;

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
        LOG.debug("Getting metadata record for {}", pid);

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
        } catch (JDOMException | IOException | IllegalStateException e) {
            LOG.error("Failed to parse MODS document for {}", idRequest.getId(), e);
        }

        if (fullObjectView == null) {
            return "";
        }

        return fullObjectView;
    }

    /**
     * JSON representation of full record
     * Can remove the non-JSON version of the full record once all JSP files are ported over
     * @param pidString
     * @return
     */
    @GetMapping(path = "/{pid}/json", produces = APPLICATION_JSON_VALUE)
    public @ResponseBody String handleRequest(@PathVariable("pid") String pidString, HttpServletResponse response) {
        PID pid = PIDs.get(pidString);
        LOG.debug("Getting full record for {}", pid);

        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        aclService.assertHasAccess("Insufficient permissions to access full record for " + pidString,
                pid, principals, Permission.viewMetadata);

        // Retrieve the object's record from Solr
        SimpleIdRequest idRequest = new SimpleIdRequest(pid, principals);
        ContentObjectRecord briefObject = queryLayer.getObjectById(idRequest);

        if (briefObject == null) {
            throw new NotFoundException("No record found for " + pid.getId());
        }

        // Get additional information depending on the type of object since the user has access
        String resourceType = briefObject.getResourceType();
        if (!ResourceType.File.nameEquals(resourceType)) {
            briefObject.getCountMap().put("child", childrenCountService.getChildrenCount(briefObject, principals));
        }

        var recordProperties = new HashMap<String, Object>();
        recordProperties.put("resourceType", resourceType);

        // Get parent id
        if (ResourceType.File.nameEquals(resourceType)) {
            String[] ancestors = briefObject.getAncestorIds().split("/");
            recordProperties.put("containingWorkUUID", ancestors[ancestors.length - 1]);
        }

        if (!ResourceType.AdminUnit.nameEquals(resourceType)) {
            String collectionId = collectionIdService.getCollectionId(briefObject);
            String faUrl = findingAidUrlService.getFindingAidUrl(collectionId);
            recordProperties.put("collectionId", collectionId);
            recordProperties.put("findingAidUrl", faUrl);

            // Get digital exhibits found on a collection
            Map<String, String> exhibitList = getExhibitList(briefObject, principals);
            if (exhibitList != null) {
                recordProperties.put("exhibits", exhibitList);
            }
        }

        if (ResourceType.File.nameEquals(resourceType) ||
                ResourceType.Work.nameEquals(resourceType)) {
            List<ContentObjectRecord> neighbors = neighborService.getNeighboringItems(briefObject,
                    searchSettings.maxNeighborResults, principals);
            accessCopiesService.populateThumbnailIds(neighbors, principals, true);
            var neighborList = neighbors.stream()
                    .map(d -> SerializationUtil.metadataToMap(d, principals)).collect(Collectors.toList());
            recordProperties.put("neighborList", neighborList);
        }

        if (ResourceType.Work.nameEquals(resourceType) || ResourceType.File.nameEquals(resourceType)) {
            var viewerProperties = getViewerProperties(briefObject, principals);
            recordProperties.put(VIEWER_TYPE, viewerProperties.get(VIEWER_TYPE));
            recordProperties.put(VIEWER_PID, viewerProperties.get(VIEWER_PID));
            recordProperties.put(STREAMING_URL, viewerProperties.get(STREAMING_URL));
            recordProperties.put(STREAMING_TYPE, viewerProperties.get(STREAMING_TYPE));

            // Get the file to download
            String dataFileUrl = accessCopiesService.getDownloadUrl(briefObject, principals);
            recordProperties.put("dataFileUrl", dataFileUrl);
        }

        accessCopiesService.populateThumbnailId(briefObject, principals, true);

        List<String> objectStatus = briefObject.getStatus();
        boolean isMarkedForDeletion = false;

        if (objectStatus != null) {
            isMarkedForDeletion = objectStatus.contains(MARKED_FOR_DELETION);
        }
        recordProperties.put("markedForDeletion", isMarkedForDeletion);

        var embargoDate = objectAclFactory.getEmbargoUntil(pid);
        if (embargoDate != null) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
            dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
            recordProperties.put("embargoDate", dateFormatter.format(embargoDate));
        }
        recordProperties.put("briefObject", SerializationUtil.metadataToMap(briefObject, principals));
        recordProperties.put("pageSubtitle", briefObject.getTitle());
        return SerializationUtil.objectToJSON(recordProperties);
    }

    @GetMapping("/{pid}/pdfViewer")
    public String handlePdfViewerRequest(@PathVariable("pid") String pidString, Model model,
                                         HttpServletRequest request) {
        PID pid = PIDs.get(pidString);

        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        aclService.assertHasAccess("Insufficient permissions to access pdf for " + pidString,
                pid, principals, Permission.viewOriginal);

        // Retrieve the object's record from Solr
        SimpleIdRequest idRequest = new SimpleIdRequest(pid, principals);
        ContentObjectRecord briefObject = queryLayer.getObjectById(idRequest);

        String viewerPid = null;
        if (ResourceType.Work.nameEquals(briefObject.getResourceType())) {
            viewerPid = accessCopiesService.getFirstMatchingChild(briefObject, APPLICATION_PDF.toString(), principals).getId();
        } else {
            accessCopiesService.getDatastreamPid(briefObject, principals, PDF_MIMETYPE_REGEX);
        }

        model.addAttribute("viewerPid", viewerPid);
        model.addAttribute("briefObject", briefObject);
        model.addAttribute("template", "ajax");

        return "fullRecord/pdfViewer";
    }

    @GetMapping("/{pid}/uvViewer")
    public String handleUvViewerRequest(@PathVariable("pid") String pidString, Model model) {
        PID pid = PIDs.get(pidString);

        AccessGroupSet principals = getAgentPrincipals().getPrincipals();
        aclService.assertHasAccess("Insufficient permissions to access pdf for " + pidString,
                pid, principals, Permission.viewAccessCopies);

        model.addAttribute("template", "ajax");
        return "fullRecord/uvViewer";
    }

    private Map<String, Object> getViewerProperties(ContentObjectRecord briefObject, AccessGroupSet principals) {
        String viewerType = null;
        String viewerPid = null;
        String streamingUrl = null;
        String streamingType = null;
        ContentObjectRecord workStreamingContent = null;

        boolean imageViewerNeeded = accessCopiesService.hasViewableFiles(briefObject, principals);

        if (!imageViewerNeeded) {
            workStreamingContent = accessCopiesService.getFirstStreamingChild(briefObject, principals);
        }

        if (imageViewerNeeded) {
            viewerType = "clover";
        } else if (briefObject.getContentStatus().contains(FacetConstants.HAS_STREAMING) || workStreamingContent != null) {
            viewerType = "streaming";
            streamingUrl = (workStreamingContent != null) ? workStreamingContent.getStreamingUrl() :
                    briefObject.getStreamingUrl();
            streamingType = (workStreamingContent != null) ? workStreamingContent.getStreamingType() :
                    briefObject.getStreamingType();
        } else {
            viewerPid = accessCopiesService.getDatastreamPid(briefObject, principals, AUDIO_MIMETYPE_REGEX);
            if (viewerPid == null) {
                viewerPid = accessCopiesService.getDatastreamPid(briefObject, principals, VIDEO_MIMETYPE_REGEX);
            }

            if (viewerPid != null) {
                viewerType = "clover";
            } else {
                viewerPid = accessCopiesService.getDatastreamPid(briefObject, principals, PDF_MIMETYPE_REGEX);
                if (viewerPid != null) {
                    viewerType = "pdf";
                }
            }
        }

        var viewerProperties = new HashMap<String, Object>();
        viewerProperties.put(VIEWER_TYPE, viewerType);
        viewerProperties.put(VIEWER_PID, viewerPid);
        viewerProperties.put(STREAMING_URL, streamingUrl);
        viewerProperties.put(STREAMING_TYPE, streamingType);

        return viewerProperties;
    }
    /**
     * Get list of digital exhibits associated with an object
     * @param briefObject
     * @param principals
     * @return
     */
    private Map<String, String> getExhibitList(ContentObjectRecord briefObject, AccessGroupSet principals) {
        ContentObjectRecord exhibitObj = briefObject;

        if (!ResourceType.Collection.nameEquals(briefObject.getResourceType())) {
            var pColl = new FilterableDisplayValueFacet(
                    SearchFieldKey.PARENT_COLLECTION, briefObject.getParentCollection());
            PID parentCollPid = PIDs.get(pColl.getValue());
            SimpleIdRequest collIdRequest = new SimpleIdRequest(parentCollPid, principals);
            exhibitObj = queryLayer.getObjectById(collIdRequest);
        }

        List<String> collExhibits = exhibitObj.getExhibit();
        if (collExhibits != null) {
            Map<String, String> exhibitList = new HashMap<>();
            for (String exhibit : collExhibits) {
                String[] exhibitValues = exhibit.split("\\|");
                exhibitList.put(exhibitValues[0], exhibitValues[1]);
            }
            return exhibitList;
        }

        return null;
    }
}

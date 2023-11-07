package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.api.services.DatastreamPermissionUtil;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.web.common.services.AccessCopiesService;
import info.freelibrary.iiif.presentation.v3.AnnotationPage;
import info.freelibrary.iiif.presentation.v3.Canvas;
import info.freelibrary.iiif.presentation.v3.ImageContent;
import info.freelibrary.iiif.presentation.v3.Manifest;
import info.freelibrary.iiif.presentation.v3.PaintingAnnotation;
import info.freelibrary.iiif.presentation.v3.properties.Label;
import info.freelibrary.iiif.presentation.v3.properties.Metadata;
import info.freelibrary.iiif.presentation.v3.properties.RequiredStatement;
import info.freelibrary.iiif.presentation.v3.services.ImageService3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;

/**
 * Service for generating iiif v3 manifests for repository object
 * @author bbpennel
 */
public class IiifV3ManifestService {
    private static final Logger log = LoggerFactory.getLogger(IiifV3ManifestService.class);
    private AccessCopiesService accessCopiesService;
    private AccessControlService accessControlService;
    private String baseIiifv3Path;
    private String baseAccessPath;
    private String baseServicesApiPath;

    /**
     * Constructs a manifest record for the object identified by the provided PID
     * @param pid
     * @param agent
     * @return
     */
    public Manifest buildManifest(PID pid, AgentPrincipals agent) {
        assertHasAccess(pid, agent);
        var contentObjs = accessCopiesService.listViewableFiles(pid, agent.getPrincipals());
        if (contentObjs.size() == 0) {
            throw new NotFoundException("No objects were found for inclusion in manifest for object " + pid.getId());
        }
        log.debug("Constructing manifest for {} containing {} items", pid.getId(), contentObjs.size());
        ContentObjectRecord rootObj = contentObjs.get(0);
        var manifest = new Manifest(getManifestPath(rootObj), new Label(getTitle(rootObj)));
        manifest.setMetadata(constructMetadataSection(rootObj));
        addAttribution(manifest, rootObj);

        addCanvasItems(manifest, contentObjs);

        return manifest;
    }

    private List<Metadata> constructMetadataSection(ContentObjectRecord rootObj) {
        var metadataList = new ArrayList<Metadata>();
        String abstractText = rootObj.getAbstractText();
        if (abstractText != null) {
            metadataList.add(new Metadata("description", abstractText));
        }
        addMultiValuedMetadataField(metadataList, "Creators", rootObj.getCreator());
        addMultiValuedMetadataField(metadataList, "Subjects", rootObj.getSubject());
        addMultiValuedMetadataField(metadataList, "Languages", rootObj.getLanguage());
        metadataList.add(new Metadata("", "<a href=\"" +
                URIUtil.join(baseAccessPath, "record", rootObj.getId()) + "\">View full record</a>"));
        return metadataList;
    }

    private void addMultiValuedMetadataField(List<Metadata> metadataList, String fieldName, List<String> values) {
        if (!CollectionUtils.isEmpty(values)) {
            metadataList.add(new Metadata(fieldName, String.join(", ", values)));
        }
    }

    private String getTitle(ContentObjectRecord contentObj) {
        String title = contentObj.getTitle();
        return (title != null) ? title : "";
    }

    private void addAttribution(Manifest manifest, ContentObjectRecord rootObj) {
        String attribution = "University of North Carolina Libraries, Digital Collections Repository";
        String collection = rootObj.getParentCollectionName();
        if (collection != null) {
            attribution += " - Part of " + collection;
        }
        manifest.setRequiredStatement(new RequiredStatement("Attribution", attribution));
    }

    /**
     * Add canvas items for each record in the set being processed
     * @param manifest
     * @param contentObjs
     */
    private void addCanvasItems(Manifest manifest, List<ContentObjectRecord> contentObjs) {
        var canvases = new ArrayList<Canvas>();
        for (ContentObjectRecord contentObj : contentObjs) {
            // Add canvases for any records with displayable content
            if (hasViewableContent(contentObj)) {
                canvases.add(constructCanvasSection(contentObj));
            }
        }
        manifest.setCanvases(canvases);
    }

    /**
     * Constructs a standalone canvas document for the requested object
     * @param pid
     * @param agent
     * @return
     */
    public Canvas buildCanvas(PID pid, AgentPrincipals agent) {
        assertHasAccess(pid, agent);
        var contentObjs = accessCopiesService.listViewableFiles(pid, agent.getPrincipals());
        ContentObjectRecord rootObj = contentObjs.get(0);
        return constructCanvasSection(rootObj);
    }

    /**
     * Constructs a canvas record for the provided object
     * @param contentObj
     * @return
     */
    private Canvas constructCanvasSection(ContentObjectRecord contentObj) {
        String title = getTitle(contentObj);
        String uuid = contentObj.getId();

        var canvas = new Canvas(getCanvasPath(contentObj), title);

        // Set up thumbnail for the current item
        var thumbnail = new ImageContent(makeThumbnailUrl(uuid));
        canvas.setThumbnails(thumbnail);

        // Children of canvas are AnnotationPages
        var annoPage = new AnnotationPage<PaintingAnnotation>(getAnnotationPagePath(contentObj));
        canvas.setPaintingPages(annoPage);

        // Child of the AnnotationPage is an Annotation, specifically a PaintingAnnotation in this case
        var paintingAnno = new PaintingAnnotation(getAnnotationPath(contentObj), canvas);
        annoPage.addAnnotations(paintingAnno);

        // Child of the Annotation is the content resource, which is an ImageContent
        var imageContent = new ImageContent(getImagePath(contentObj));
        imageContent.setFormat("image/jpeg");
        paintingAnno.getBodies().add(imageContent);

        // Child of the content resource is an ImageService
        var imageService = new ImageService3(ImageService3.Profile.LEVEL_TWO, getServicePath(contentObj));
        imageContent.setServices(imageService);

        // Set the dimensions of this item on appropriate elements
        assignDimensions(contentObj, canvas, imageContent);

        return canvas;
    }

    private void assignDimensions(ContentObjectRecord contentObj, Canvas canvas, ImageContent imageContent) {
        Datastream fileDs = contentObj.getDatastreamObject(DatastreamType.ORIGINAL_FILE.getId());
        String extent = fileDs.getExtent();
        if (extent != null && !extent.equals("")) {
            String[] imgDimensions = extent.split("x");
            var width = Integer.parseInt(imgDimensions[1]); // in the datastream, the width is second
            var height = Integer.parseInt(imgDimensions[0]);
            canvas.setWidthHeight(width, height); // Dimensions for the canvas
            imageContent.setWidthHeight(width, height); // Dimensions for the actual image
        }
    }

    private void assertHasAccess(PID pid, AgentPrincipals agent) {
        Permission permission = DatastreamPermissionUtil.getPermissionForDatastream(JP2_ACCESS_COPY);

        log.debug("Checking if user {} has access to {} belonging to object {}.",
                agent.getUsername(), JP2_ACCESS_COPY, pid);
        accessControlService.assertHasAccess(pid, agent.getPrincipals(), permission);
    }

    private String getManifestPath(ContentObjectRecord contentObj) {
        return URIUtil.join(baseIiifv3Path, contentObj.getId(), "manifest");
    }

    private String getCanvasPath(ContentObjectRecord contentObj) {
        return URIUtil.join(baseIiifv3Path, contentObj.getId(), "canvas");
    }

    private String getAnnotationPagePath(ContentObjectRecord contentObj) {
        return URIUtil.join(baseIiifv3Path, contentObj.getId(), "page", "1");
    }

    private String getAnnotationPath(ContentObjectRecord contentObj) {
        return URIUtil.join(baseIiifv3Path, contentObj.getId(), "annotation", "1");
    }

    private String getImagePath(ContentObjectRecord contentObj) {
        return URIUtil.join(baseIiifv3Path, contentObj.getId(), "full", "max", "0", "default.jpg");
    }

    private String getServicePath(ContentObjectRecord contentObj) {
        return URIUtil.join(baseIiifv3Path, contentObj.getId());
    }

    private String makeThumbnailUrl(String id) {
        return URIUtil.join(baseServicesApiPath, "thumb", id, "large");
    }

    private boolean hasViewableContent(ContentObjectRecord contentObj) {
        var datastream = contentObj.getDatastreamObject(DatastreamType.JP2_ACCESS_COPY.getId());
        return datastream != null && contentObj.getResourceType().equals(ResourceType.File.name());
    }

    public void setAccessCopiesService(AccessCopiesService accessCopiesService) {
        this.accessCopiesService = accessCopiesService;
    }

    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    public void setBaseIiifv3Path(String baseIiifv3Path) {
        this.baseIiifv3Path = baseIiifv3Path;
    }

    public void setBaseAccessPath(String baseAccessPath) {
        this.baseAccessPath = baseAccessPath;
    }

    public void setBaseServicesApiPath(String baseServicesApiPath) {
        this.baseServicesApiPath = baseServicesApiPath;
    }
}

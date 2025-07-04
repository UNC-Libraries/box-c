package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.api.services.DatastreamPermissionUtil;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.filters.QueryFilterFactory;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import info.freelibrary.iiif.presentation.v3.AnnotationPage;
import info.freelibrary.iiif.presentation.v3.Canvas;
import info.freelibrary.iiif.presentation.v3.ImageContent;
import info.freelibrary.iiif.presentation.v3.Manifest;
import info.freelibrary.iiif.presentation.v3.PaintingAnnotation;
import info.freelibrary.iiif.presentation.v3.SoundContent;
import info.freelibrary.iiif.presentation.v3.VideoContent;
import info.freelibrary.iiif.presentation.v3.properties.Label;
import info.freelibrary.iiif.presentation.v3.properties.Metadata;
import info.freelibrary.iiif.presentation.v3.properties.RequiredStatement;
import info.freelibrary.iiif.presentation.v3.properties.ViewingDirection;
import info.freelibrary.iiif.presentation.v3.services.ImageService3;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;
import static info.freelibrary.iiif.presentation.v3.MediaType.AUDIO_AAC;
import static info.freelibrary.iiif.presentation.v3.MediaType.AUDIO_MP4;
import static info.freelibrary.iiif.presentation.v3.MediaType.AUDIO_MPEG;
import static info.freelibrary.iiif.presentation.v3.MediaType.IMAGE_JPEG;
import static info.freelibrary.iiif.presentation.v3.MediaType.VIDEO_MP4;
import static info.freelibrary.iiif.presentation.v3.MediaType.VIDEO_MPEG;
import static info.freelibrary.iiif.presentation.v3.MediaType.VIDEO_QUICKTIME;
import static info.freelibrary.iiif.presentation.v3.properties.behaviors.ManifestBehavior.from;

/**
 * Service for generating iiif v3 manifests for repository object
 * @author bbpennel
 */
public class IiifV3ManifestService {
    private static final Logger log = LoggerFactory.getLogger(IiifV3ManifestService.class);
    public static final String DURATION = "duration";
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";
    private AccessControlService accessControlService;
    private SolrSearchService solrSearchService;
    private GlobalPermissionEvaluator globalPermissionEvaluator;
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
        ContentObjectRecord rootObj = solrSearchService.getObjectById(new SimpleIdRequest(pid, agent.getPrincipals()));
        if (rootObj == null) {
            throw new NotFoundException("No objects were found for inclusion in manifest for object " + pid.getId());
        }
        var contentObjs = listViewableFiles(rootObj, agent.getPrincipals());
        log.debug("Constructing manifest for {} containing {} items", pid.getId(), contentObjs.size());

        var manifest = new Manifest(getManifestPath(rootObj), new Label(getTitle(rootObj)));
        manifest.setMetadata(constructMetadataSection(rootObj));
        addAttribution(manifest, rootObj);

        addCanvasItems(manifest, contentObjs);

        addViewingDirectionAndBehavior(manifest, rootObj);

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
        ContentObjectRecord obj = solrSearchService.getObjectById(new SimpleIdRequest(pid, agent.getPrincipals()));
        if (obj == null || !hasViewableContent(obj)) {
            throw new NotFoundException("No objects were found for canvas for object " + pid.getId());
        }

        return constructCanvasSection(obj);
    }

    /**
     * Constructs a canvas record for the provided object
     * @param contentObj
     * @return
     */
    private Canvas constructCanvasSection(ContentObjectRecord contentObj) {
        String title = getTitle(contentObj);

        var canvas = new Canvas(getCanvasPath(contentObj), title);

        // Children of canvas are AnnotationPages
        var annoPage = new AnnotationPage<PaintingAnnotation>(getAnnotationPagePath(contentObj));
        canvas.setPaintingPages(annoPage);

        // Child of the AnnotationPage is an Annotation, specifically a PaintingAnnotation in this case
        var paintingAnno = new PaintingAnnotation(getAnnotationPath(contentObj), canvas);
        annoPage.addAnnotations(paintingAnno);

        // Child of the Annotation is a Content Object
        var mimetype = getMimetype(contentObj);
        if (isAudio(mimetype, contentObj)) {
            setSoundContent(contentObj, paintingAnno, canvas);
        } else if (isVideo(mimetype)) {
            setVideoContent(contentObj, paintingAnno, canvas);
        } else {
            setImageContent(contentObj, paintingAnno, canvas);
            // Set up thumbnail for the current image
            String uuid = contentObj.getId();
            var thumbnail = new ImageContent(makeThumbnailUrl(uuid));
            canvas.setThumbnails(thumbnail);
        }

        return canvas;
    }

    private void setSoundContent(ContentObjectRecord contentObj, PaintingAnnotation paintingAnno, Canvas canvas) {
        SoundContent soundContent;
        if (contentObj.getDatastreamObject(DatastreamType.AUDIO_ACCESS_COPY.getId()) != null) {
            soundContent = new SoundContent(getAccessPath(contentObj));
        } else {
            soundContent = new SoundContent(getDownloadPath(contentObj));
        }
        soundContent.setFormat(AUDIO_MP4);
        var dimensions = getDimensions(contentObj);
        if (dimensions != null && (dimensions.get(DURATION) >= 0)) {
            var duration = dimensions.get(DURATION);
            soundContent.setDuration(duration);
            canvas.setDuration(duration);
        }
        var audioThumbPath = URIUtil.join(baseAccessPath, "static", "images", "file-audio.png");
        var thumbnail = new ImageContent(audioThumbPath);
        canvas.setThumbnails(thumbnail);
        paintingAnno.getBodies().add(soundContent);
    }

    private void setVideoContent(ContentObjectRecord contentObj, PaintingAnnotation paintingAnno, Canvas canvas) {
        var videoContent = new VideoContent(getDownloadPath(contentObj));
        videoContent.setFormat(VIDEO_MP4);
        assignVideoDimensions(contentObj, canvas, videoContent);
        paintingAnno.getBodies().add(videoContent);
    }

    private void assignVideoDimensions(ContentObjectRecord contentObj, Canvas canvas, VideoContent videoContent) {
        var dimensions = getDimensions(contentObj);
        if (dimensions != null) {
            var width = dimensions.get(WIDTH);
            var height = dimensions.get(HEIGHT);
            canvas.setWidthHeight(width, height); // Dimensions for the canvas
            videoContent.setWidthHeight(width, height); // Dimensions for the actual video
            var duration = dimensions.get(DURATION);
            if (duration >= 0) {
                videoContent.setDuration(duration);
                canvas.setDuration(duration);
            }
        }
    }

    private void setImageContent(ContentObjectRecord contentObj, PaintingAnnotation paintingAnno, Canvas canvas) {
        var imageContent = new ImageContent(getImagePath(contentObj));
        imageContent.setFormat(IMAGE_JPEG);
        paintingAnno.getBodies().add(imageContent);

        // Child of the content resource is an ImageService
        var imageService = new ImageService3(ImageService3.Profile.LEVEL_TWO, getServicePath(contentObj));
        imageContent.setServices(imageService);

        // Set the dimensions of this item on appropriate elements
        assignImageDimensions(contentObj, canvas, imageContent);
    }

    private void assignImageDimensions(ContentObjectRecord contentObj, Canvas canvas, ImageContent imageContent) {
        var dimensions = getDimensions(contentObj);
        if (dimensions != null) {
            var width = dimensions.get(WIDTH);
            var height = dimensions.get(HEIGHT);
            canvas.setWidthHeight(width, height); // Dimensions for the canvas
            imageContent.setWidthHeight(width, height); // Dimensions for the actual image
        }
    }

    private Map<String, Integer> getDimensions(ContentObjectRecord contentObj) {
        var fileDs = getFileDatastream(contentObj);
        String extent = fileDs.getExtent();
        if (extent != null && !extent.isEmpty()) {
            String[] imgDimensions = extent.split("x");
            if (imgDimensions.length >= 2) {
                return extractDimensions(imgDimensions);
            }
        }
        return null;
    }

    private static Map<String, Integer> extractDimensions(String[] imgDimensions) {
        var dimensions = new HashMap<String, Integer>();
        // [height, width, seconds]
        var height = imgDimensions[0];
        if (!height.isBlank()) {
            dimensions.put(HEIGHT, Integer.parseInt(height));
        }
        var width = imgDimensions[1];
        if (!width.isBlank()) {
            dimensions.put(WIDTH, Integer.parseInt(width));
        }
        if (imgDimensions.length == 3) {
            var duration = imgDimensions[2];
            dimensions.put(DURATION, Integer.parseInt(duration));
        }
        return dimensions;
    }

    private Datastream getFileDatastream(ContentObjectRecord contentObj) {
        return contentObj.getDatastreamObject(DatastreamType.ORIGINAL_FILE.getId());
    }

    private String getMimetype(ContentObjectRecord contentObj) {
        return getFileDatastream(contentObj).getMimetype();
    }

    private boolean isVideo(String mimetype) {
        return Objects.equals(mimetype, VIDEO_MP4.toString())  || Objects.equals(mimetype, VIDEO_MPEG.toString())
                || Objects.equals(mimetype, VIDEO_QUICKTIME.toString());
    }

    private boolean isAudio(String mimetype, ContentObjectRecord contentObj) {
        if (contentObj.getDatastreamObject(DatastreamType.AUDIO_ACCESS_COPY.getId()) != null) {
            return true;
        }

        return Objects.equals(mimetype, AUDIO_MP4.toString()) || Objects.equals(mimetype, AUDIO_MPEG.toString())
                || Objects.equals(mimetype, AUDIO_AAC.toString());
    }

    private boolean hasViewableContent(ContentObjectRecord contentObj) {
        // if obj is not a file
        if (!contentObj.getResourceType().equals(ResourceType.File.name())) {
            return false;
        }

        var jp2Datastream = contentObj.getDatastreamObject(DatastreamType.JP2_ACCESS_COPY.getId());
        var isValidDatastream = jp2Datastream != null && jp2Datastream.getFilesize() != 0;
        var originalDatastream = contentObj.getDatastreamObject(DatastreamType.ORIGINAL_FILE.getId());
        // check if original datastream mimetype is audio or video
        if (!isValidDatastream && originalDatastream != null) {
            var mimetype = originalDatastream.getMimetype();
            isValidDatastream = isAudio(mimetype, contentObj) || isVideo(mimetype);
        }

        return isValidDatastream;
    }

    /**
     * List viewable files for the specified object's manifest
     * @param rootObj
     * @param principals
     * @return
     */
    private List<ContentObjectRecord> listViewableFiles(ContentObjectRecord rootObj, AccessGroupSet principals) {
        String resourceType = rootObj.getResourceType();
        if (hasViewableContent(rootObj)) {
            return Collections.singletonList(rootObj);
        }
        if (!ResourceType.Work.nameEquals(resourceType)) {
            return Collections.emptyList();
        }

        return performQuery(rootObj, principals);
    }

    private List<ContentObjectRecord> performQuery(ContentObjectRecord briefObj, AccessGroupSet principals) {
        // Search for child objects with AV mimetypes with user can access
        SearchState searchState = new SearchState();
        if (!globalPermissionEvaluator.hasGlobalPrincipal(principals)) {
            searchState.setPermissionLimits(List.of(Permission.viewAccessCopies));
        }
        searchState.setIgnoreMaxRows(true);
        searchState.setRowsPerPage(2000);
        CutoffFacet selectedPath = briefObj.getPath();
        searchState.addFacet(selectedPath);
        searchState.setSortType("default");
        searchState.addFilter(QueryFilterFactory.createIIIFv3ViewableFilter());

        var searchRequest = new SearchRequest(searchState, principals);
        var resp = solrSearchService.getSearchResults(searchRequest);
        return resp.getResultList();
    }

    private void addViewingDirectionAndBehavior(Manifest manifest, ContentObjectRecord contentObj) {
        if (Objects.equals(contentObj.getResourceType(), ResourceType.Work.name())) {
            manifest.setViewingDirection(ViewingDirection.LEFT_TO_RIGHT);
            var behaviorString = contentObj.getViewBehavior();
            if (!StringUtils.isBlank(behaviorString)) {
                var behavior = from(behaviorString);
                manifest.setBehaviors(behavior);
            }
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

    private String getDownloadPath(ContentObjectRecord contentObj) {
        return URIUtil.join(baseServicesApiPath, "file", contentObj.getId());
    }

    private String getAccessPath(ContentObjectRecord contentObj) {
        return URIUtil.join(baseServicesApiPath, "file", contentObj.getId(),
                DatastreamType.AUDIO_ACCESS_COPY.getId());
    }

    public void setAccessControlService(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    public void setSolrSearchService(SolrSearchService solrSearchService) {
        this.solrSearchService = solrSearchService;
    }

    public void setGlobalPermissionEvaluator(GlobalPermissionEvaluator globalPermissionEvaluator) {
        this.globalPermissionEvaluator = globalPermissionEvaluator;
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

package edu.unc.lib.boxc.web.services.processing;

import de.digitalcollections.iiif.model.enums.ViewingDirection;
import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.operations.jms.viewSettings.ViewSettingRequest;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.models.DatastreamImpl;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import edu.unc.lib.boxc.web.services.rest.MvcTestHelpers;
import info.freelibrary.iiif.presentation.v3.Canvas;
import info.freelibrary.iiif.presentation.v3.ImageContent;
import info.freelibrary.iiif.presentation.v3.PaintingAnnotation;
import info.freelibrary.iiif.presentation.v3.VideoContent;
import info.freelibrary.iiif.presentation.v3.properties.behaviors.ManifestBehavior;
import info.freelibrary.iiif.presentation.v3.services.ImageService3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
public class IiifV3ManifestServiceTest {
    private static final String IIIF_BASE = "http://example.com/iiif/v3/";
    private static final String SERVICES_BASE = "http://example.com/services/api/";
    private static final String ACCESS_BASE = "http://example.com/";
    private static final String WORK_ID = "5d72b84a-983c-4a45-8caa-dc9857987da2";
    private static final String FILE1_ID = "faffb3e1-85fc-451f-9075-c60fc7584c7b";
    private static final String FILE2_ID = "b6c51e59-d931-41d6-ba26-ec54ba9b2ef5";
    private static final String COLL_ID = "fdce64cb-6a6f-43bb-8ed2-58f3b60148bf";
    private static final PID WORK_PID = PIDs.get(WORK_ID);
    private static final String IMAGE = "image";
    private static final String VIDEO = "video";

    @Mock
    private SolrSearchService solrSearchService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet principals;
    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;
    private AutoCloseable closeable;
    private ContentObjectSolrRecord workObj;
    private IiifV3ManifestService manifestService;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        manifestService = new IiifV3ManifestService();
        manifestService.setSolrSearchService(solrSearchService);
        manifestService.setAccessControlService(accessControlService);
        manifestService.setGlobalPermissionEvaluator(globalPermissionEvaluator);
        manifestService.setBaseIiifv3Path(IIIF_BASE);
        manifestService.setBaseServicesApiPath(SERVICES_BASE);
        manifestService.setBaseAccessPath(ACCESS_BASE);

        when(agent.getPrincipals()).thenReturn(principals);

        workObj = new ContentObjectSolrRecord();
        workObj.setId(WORK_ID);
        workObj.setResourceType(ResourceType.Work.name());
        workObj.setTitle("Test Work");
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    private ContentObjectRecord createFileRecord(String id, String type) {
        var fileObj = new ContentObjectSolrRecord();
        fileObj.setId(id);
        fileObj.setResourceType(ResourceType.File.name());
        fileObj.setTitle("File Object " + id);
        if (Objects.equals(type, VIDEO)) {
            setAsVideo(fileObj);
        } else {
            setAsImage(fileObj);
        }
        return fileObj;
    }

    private void setAsImage(ContentObjectSolrRecord fileObj) {
        var originalDs = new DatastreamImpl("original_file|image/jpeg|image.jpg|jpg|0|||240x750");
        var jp2Ds = new DatastreamImpl("jp2|image/jp2|image.jp2|jp2|0|||");
        fileObj.setDatastream(Arrays.asList(originalDs.toString(), jp2Ds.toString()));
    }

    private void setAsVideo(ContentObjectSolrRecord fileObj) {
        var originalDs = new DatastreamImpl("original_file|video/mp4|video.mp4|mp4|0|||240x750x500");
        fileObj.setDatastream(List.of(originalDs.toString()));
    }

    @Test
    public void buildManifestNoViewableFilesTest() {
        when(solrSearchService.getObjectById(any())).thenReturn(null);
        assertThrows(NotFoundException.class, () -> {
            manifestService.buildManifest(WORK_PID, agent);
        });
    }

    @Test
    public void buildManifestNoAccessTest() {
        doThrow(new AccessRestrictionException()).when(accessControlService)
                .assertHasAccess(eq(WORK_PID), any(), eq(Permission.viewAccessCopies));
        assertThrows(AccessRestrictionException.class, () -> {
            manifestService.buildManifest(WORK_PID, agent);
        });
    }

    @Test
    public void buildManifestWorkWithoutViewableFilesTest() {
        when(solrSearchService.getObjectById(any())).thenReturn(workObj);
        when(solrSearchService.getSearchResults(any()))
                .thenReturn(MvcTestHelpers.createSearchResponse(List.of(workObj)));
        when(globalPermissionEvaluator.hasGlobalPrincipal(any())).thenReturn(true);

        var manifest = manifestService.buildManifest(WORK_PID, agent);
        assertEquals("Test Work", manifest.getLabel().getString());
        assertEquals("http://example.com/iiif/v3/5d72b84a-983c-4a45-8caa-dc9857987da2/manifest", manifest.getID().toString());
        assertEquals("<a href=\"http://example.com/record/5d72b84a-983c-4a45-8caa-dc9857987da2\">View full record</a>",
                manifest.getMetadata().get(0).getValue().getString());
        assertTrue(manifest.getCanvases().isEmpty());
    }

    @Test
    public void buildManifestWorkWithViewableFilesTest() {
        var fileObj1 = createFileRecord(FILE1_ID, IMAGE);
        var fileObj2 = createFileRecord(FILE2_ID, IMAGE);
        when(solrSearchService.getObjectById(any())).thenReturn(workObj);
        when(solrSearchService.getSearchResults(any()))
                .thenReturn(MvcTestHelpers.createSearchResponse(Arrays.asList(workObj, fileObj1, fileObj2)));
        when(globalPermissionEvaluator.hasGlobalPrincipal(any())).thenReturn(true);

        var manifest = manifestService.buildManifest(WORK_PID, agent);
        assertEquals("Test Work", manifest.getLabel().getString());
        assertEquals("http://example.com/iiif/v3/5d72b84a-983c-4a45-8caa-dc9857987da2/manifest",
                manifest.getID().toString());
        var canvases = manifest.getCanvases();
        assertEquals(2, canvases.size());
        assertFileCanvasPopulated(canvases.get(0), FILE1_ID, IMAGE);
        assertFileCanvasPopulated(canvases.get(1), FILE2_ID, IMAGE);
    }

    @Test
    public void buildManifestViewableFileImageTest() {
        var fileObj1 = createFileRecord(FILE1_ID, IMAGE);
        var filePid = PIDs.get(FILE1_ID);
        when(solrSearchService.getObjectById(any())).thenReturn(fileObj1);

        var manifest = manifestService.buildManifest(filePid, agent);
        assertEquals("File Object faffb3e1-85fc-451f-9075-c60fc7584c7b", manifest.getLabel().getString());
        assertEquals("http://example.com/iiif/v3/faffb3e1-85fc-451f-9075-c60fc7584c7b/manifest",
                manifest.getID().toString());
        var canvases = manifest.getCanvases();
        assertEquals(1, canvases.size());
        assertFileCanvasPopulated(canvases.get(0), FILE1_ID, IMAGE);

        assertEquals("<a href=\"http://example.com/record/faffb3e1-85fc-451f-9075-c60fc7584c7b\">View full record</a>",
                manifest.getMetadata().get(0).getValue().getString());

        assertNull(manifest.getViewingDirection());
        assertTrue(manifest.getBehaviors().isEmpty());
    }

    @Test
    public void buildManifestViewableFileVideoTest() {
        var fileObj1 = createFileRecord(FILE1_ID, "video");
        var filePid = PIDs.get(FILE1_ID);
        when(solrSearchService.getObjectById(any())).thenReturn(fileObj1);

        var manifest = manifestService.buildManifest(filePid, agent);
        assertEquals("File Object faffb3e1-85fc-451f-9075-c60fc7584c7b", manifest.getLabel().getString());
        assertEquals("http://example.com/iiif/v3/faffb3e1-85fc-451f-9075-c60fc7584c7b/manifest",
                manifest.getID().toString());
        var canvases = manifest.getCanvases();
        assertEquals(1, canvases.size());
        assertFileCanvasPopulated(canvases.get(0), FILE1_ID, VIDEO);

        assertEquals("<a href=\"http://example.com/record/faffb3e1-85fc-451f-9075-c60fc7584c7b\">View full record</a>",
                manifest.getMetadata().get(0).getValue().getString());

        assertNull(manifest.getViewingDirection());
        assertTrue(manifest.getBehaviors().isEmpty());
    }

    @Test
    public void buildCanvasViewableFileTest() {
        var fileObj1 = createFileRecord(FILE1_ID, IMAGE);
        var filePid = PIDs.get(FILE1_ID);
        when(solrSearchService.getObjectById(any())).thenReturn(fileObj1);

        var canvas = manifestService.buildCanvas(filePid, agent);
        assertFileCanvasPopulated(canvas, FILE1_ID, IMAGE);
    }

    @Test
    public void buildManifestViewInfoMultipleFilesTest() {
        var fileObj1 = createFileRecord(FILE1_ID, IMAGE);
        var fileObj2 = createFileRecord(FILE2_ID, IMAGE);
        when(solrSearchService.getObjectById(any())).thenReturn(workObj);
        when(solrSearchService.getSearchResults(any()))
                .thenReturn(MvcTestHelpers.createSearchResponse(Arrays.asList(workObj, fileObj1, fileObj2)));
        workObj.setViewBehavior(ViewSettingRequest.ViewBehavior.PAGED.getString());

        var manifest = manifestService.buildManifest(WORK_PID, agent);

        assertEquals(ViewingDirection.LEFT_TO_RIGHT.toString(), manifest.getViewingDirection().toString());
        assertEquals(ManifestBehavior.PAGED, manifest.getBehaviors().get(0));
    }

    @Test
    public void buildManifestWorkWithMetadataTest() {
        workObj.setSubject(Arrays.asList("Images", "Transformation"));
        workObj.setAbstractText("This is a test work");
        workObj.setCreator(Arrays.asList("Boxy", "Boxc"));
        workObj.setLanguage(Arrays.asList("English", "Spanish"));
        workObj.setParentCollection("Image Collection|" + COLL_ID);
        when(solrSearchService.getObjectById(any())).thenReturn(workObj);
        when(solrSearchService.getSearchResults(any()))
                .thenReturn(MvcTestHelpers.createSearchResponse(List.of(workObj)));

        var manifest = manifestService.buildManifest(WORK_PID, agent);
        assertEquals("http://example.com/iiif/v3/5d72b84a-983c-4a45-8caa-dc9857987da2/manifest", manifest.getID().toString());
        assertEquals("Test Work", manifest.getLabel().getString());
        assertEquals("University of North Carolina Libraries, Digital Collections Repository - Part of Image Collection",
                manifest.getRequiredStatement().getValue().getString());
        var abstractMd = manifest.getMetadata().get(0);
        assertEquals("description", abstractMd.getLabel().getString());
        assertEquals("This is a test work", abstractMd.getValue().getString());

        var creatorsMd = manifest.getMetadata().get(1);
        assertEquals("Creators", creatorsMd.getLabel().getString());
        assertEquals("Boxy, Boxc", creatorsMd.getValue().getString());

        var subjectsMd = manifest.getMetadata().get(2);
        assertEquals("Subjects", subjectsMd.getLabel().getString());
        assertEquals("Images, Transformation", subjectsMd.getValue().getString());

        var languagesMd = manifest.getMetadata().get(3);
        assertEquals("Languages", languagesMd.getLabel().getString());
        assertEquals("English, Spanish", languagesMd.getValue().getString());

        var recordLinkMd = manifest.getMetadata().get(4);
        assertEquals("", recordLinkMd.getLabel().getString());
        assertEquals("<a href=\"http://example.com/record/5d72b84a-983c-4a45-8caa-dc9857987da2\">View full record</a>",
                recordLinkMd.getValue().getString());
    }

    private void assertFileCanvasPopulated(Canvas fileCanvas, String expectedId, String type) {
        assertEquals("http://example.com/iiif/v3/" + expectedId + "/canvas",
                fileCanvas.getID().toString());
        assertEquals(240, fileCanvas.getHeight());
        assertEquals(750, fileCanvas.getWidth());
        assertEquals("http://example.com/services/api/thumb/" + expectedId + "/large",
                fileCanvas.getThumbnails().get(0).getID().toString());
        var annoPage = fileCanvas.getPaintingPages().get(0);
        var annotation = annoPage.getAnnotations().get(0);
        assertEquals("painting", annotation.getMotivation());

        if (Objects.equals(type, VIDEO)) {
            checkVideoContent(annotation);
        } else {
            checkImageContent(annotation, expectedId);
        }

    }

    private void checkImageContent(PaintingAnnotation annotation, String expectedId) {
        var imageContent = (ImageContent) annotation.getBodies().get(0);
        assertEquals(240, imageContent.getHeight());
        assertEquals(750, imageContent.getWidth());
        assertEquals("image/jpeg", imageContent.getFormat().get().toString());
        var imageService = (ImageService3) imageContent.getServices().get(0);
        assertEquals("http://example.com/iiif/v3/" + expectedId, imageService.getID().toString());
        assertEquals("level2", imageService.getProfile().get().string());
    }

    private void checkVideoContent(PaintingAnnotation annotation) {
        var videoContent = (VideoContent) annotation.getBodies().get(0);
        assertEquals(240, videoContent.getHeight());
        assertEquals(750, videoContent.getWidth());
        assertEquals(500, videoContent.getDuration());
        assertEquals("video/mp4", videoContent.getFormat().get().toString());
    }
}

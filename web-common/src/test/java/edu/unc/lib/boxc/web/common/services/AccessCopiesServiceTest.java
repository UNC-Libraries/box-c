package edu.unc.lib.boxc.web.common.services;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.search.api.ContentCategory;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.solr.filters.HasPopulatedFieldFilter;
import edu.unc.lib.boxc.search.solr.filters.NamedDatastreamFilter;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static edu.unc.lib.boxc.auth.api.Permission.viewOriginal;
import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.web.common.services.AccessCopiesService.AUDIO_MIMETYPE_REGEX;
import static edu.unc.lib.boxc.web.common.services.AccessCopiesService.PDF_MIMETYPE_REGEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author lfarrell
 */
public class AccessCopiesServiceTest  {
    private PermissionsHelper helper;

    private ContentObjectSolrRecord mdObject;

    private ContentObjectSolrRecord mdObjectImg;

    private ContentObjectSolrRecord mdObjectAudio;

    private ContentObjectSolrRecord mdObjectVideo;

    private ContentObjectSolrRecord noOriginalFileObj;

    private ContentObjectSolrRecord mdObjectXml;

    private AccessGroupSet principals;

    private AccessCopiesService accessCopiesService;

    private AutoCloseable closeable;

    @Mock
    private AccessControlService accessControlService;
    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;
    @Mock
    private SolrSearchService solrSearchService;
    @Mock
    private SearchResultResponse searchResultResponse;
    @Captor
    private ArgumentCaptor<SearchRequest> searchRequestCaptor;

    @BeforeEach
    public void init() throws IOException, SolrServerException {
        closeable = openMocks(this);

        mdObject = createPdfObject(ResourceType.Work);
        mdObjectImg = createImgObject(ResourceType.Work);
        mdObjectAudio = createAudioObject(ResourceType.Work);
        mdObjectVideo = createVideoObject(ResourceType.Work);

        noOriginalFileObj = new ContentObjectSolrRecord();
        noOriginalFileObj.setResourceType(ResourceType.Work.name());
        noOriginalFileObj.setId("45c8d1c5-14a1-4ed3-b0c0-6da67fa5f6d1");

        mdObjectXml = createXmlObject(ResourceType.Work);

        principals = new AccessGroupSetImpl("group");

        helper = new PermissionsHelper();
        helper.setAccessControlService(accessControlService);

        accessCopiesService = new AccessCopiesService();
        accessCopiesService.setPermissionsHelper(helper);
        accessCopiesService.setSolrSearchService(solrSearchService);
        accessCopiesService.setGlobalPermissionEvaluator(globalPermissionEvaluator);

        when(solrSearchService.getSearchResults(searchRequestCaptor.capture())).thenReturn(searchResultResponse);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    private ContentObjectSolrRecord createAudioObject(ResourceType resourceType) {
        var mdObjectAudio = new ContentObjectSolrRecord();
        mdObjectAudio.setResourceType(resourceType.name());
        mdObjectAudio.setId(UUID.randomUUID().toString());
        List<String> audioDatastreams = Collections.singletonList(
                ORIGINAL_FILE.getId() + "|audio/mpeg|file.mp3|mp3|766|urn:sha1:checksum|");
        mdObjectAudio.setFileFormatCategory(Collections.singletonList(ContentCategory.audio.getDisplayName()));
        mdObjectAudio.setFileFormatType(Collections.singletonList("audio/mpeg"));
        mdObjectAudio.setDatastream(audioDatastreams);
        mdObjectAudio.setStreamingUrl("https://durastream.lib.unc.edu/player?spaceId=open-hls&filename=04950_VT0008_0003");
        mdObjectAudio.setStreamingType("sound");
        return mdObjectAudio;
    }

    private ContentObjectSolrRecord createVideoObject(ResourceType resourceType) {
        var mdObjectAudio = new ContentObjectSolrRecord();
        mdObjectAudio.setResourceType(resourceType.name());
        mdObjectAudio.setId(UUID.randomUUID().toString());
        List<String> audioDatastreams = Collections.singletonList(
                ORIGINAL_FILE.getId() + "|video/mp4|file.mp4|mp4|766|urn:sha1:checksum|");
        mdObjectAudio.setFileFormatCategory(Collections.singletonList(ContentCategory.video.getDisplayName()));
        mdObjectAudio.setFileFormatType(Collections.singletonList("video/mp4"));
        mdObjectAudio.setDatastream(audioDatastreams);
        mdObjectAudio.setStreamingUrl("https://durastream.lib.unc.edu/player?spaceId=open-hls&filename=04950_VT0008_0001");
        mdObjectAudio.setStreamingType("video");
        return mdObjectAudio;
    }

    private ContentObjectSolrRecord createPdfObject(ResourceType resourceType) {
        var mdObject = new ContentObjectSolrRecord();
        mdObject.setResourceType(resourceType.name());
        mdObject.setId(UUID.randomUUID().toString());
        List<String> datastreams = Collections.singletonList(
                ORIGINAL_FILE.getId() + "|application/pdf|file.pdf|pdf|766|urn:sha1:checksum|");
        mdObject.setFileFormatCategory(Collections.singletonList(ContentCategory.text.getDisplayName()));
        mdObject.setFileFormatType(Collections.singletonList("application/pdf"));
        mdObject.setDatastream(datastreams);
        return mdObject;
    }

    private ContentObjectSolrRecord createImgObject(ResourceType resourceType) {
        var mdObjectImg = new ContentObjectSolrRecord();
        mdObjectImg.setResourceType(resourceType.name());
        var id = UUID.randomUUID().toString();
        mdObjectImg.setId(id);
        List<String> imgDatastreams = List.of(
                ORIGINAL_FILE.getId() + "|image/png|file.png|png|766|urn:sha1:checksum|",
                JP2_ACCESS_COPY.getId() + "|image/jp2|bunny.jp2|jp2|||" + id + "|1200x1200");
        mdObjectImg.setFileFormatCategory(Collections.singletonList(ContentCategory.image.getDisplayName()));
        mdObjectImg.setFileFormatType(Collections.singletonList("image/png"));
        mdObjectImg.setDatastream(imgDatastreams);
        return mdObjectImg;
    }

    private ContentObjectSolrRecord createXmlObject(ResourceType resourceType) {
        var mdObjectXml = new ContentObjectSolrRecord();
        mdObjectXml.setResourceType(resourceType.name());
        mdObjectXml.setId(UUID.randomUUID().toString());
        List<String> xmlDatastreams = Collections.singletonList(
                TECHNICAL_METADATA.getId() + "|text.xml|file.xml|xml|766|urn:sha1:checksum|");
        mdObjectXml.setFileFormatCategory(Collections.singletonList(ContentCategory.text.getDisplayName()));
        mdObjectXml.setFileFormatType(Collections.singletonList("text/xml"));
        mdObjectXml.setDatastream(xmlDatastreams);
        return mdObjectXml;
    }

    @Test
    public void testHasViewablePdfPid() {
        hasPermissions(mdObject, true);

        String filePid = accessCopiesService.getDatastreamPid(mdObject, principals, PDF_MIMETYPE_REGEX);
        assertNotNull(filePid);
        assertEquals(filePid, mdObject.getId());
    }

    @Test
    public void testHasViewablePdfPidNoPermission() {
        hasPermissions(mdObject, false);

        String filePid = accessCopiesService.getDatastreamPid(mdObject, principals, PDF_MIMETYPE_REGEX);
        assertNull(filePid);
    }

    @Test
    public void testDoesNotHaveViewablePdfPidOneContentObject() {
        hasPermissions(mdObjectImg, true);

        String filePid = accessCopiesService.getDatastreamPid(mdObjectImg, principals, PDF_MIMETYPE_REGEX);
        assertNull(filePid);
    }

    @Test
    public void testDoesNotHaveViewablePdfPidMultipleFileObjects() {
        hasPermissions(mdObjectImg, true);

        String filePid = accessCopiesService.getDatastreamPid(mdObjectImg, principals, PDF_MIMETYPE_REGEX);
        assertNull(filePid);
    }

    @Test
    public void testPrimaryObjectHasDownloadUrl() {
        hasPermissions(mdObjectAudio, true);
        String downloadUrl = accessCopiesService.getDownloadUrl(mdObjectAudio, principals);
        assertEquals("content/" + mdObjectAudio.getId(), downloadUrl);
    }

    @Test
    public void testDoesNotHaveDownloadUrl() {
        hasPermissions(noOriginalFileObj, true);

        String downloadUrl = accessCopiesService.getDownloadUrl(noOriginalFileObj, principals);

        assertEquals("", downloadUrl);
    }

    @Test
    public void testMultipleFileObjectsDoesNotHaveDownloadUrl() {
        hasPermissions(noOriginalFileObj, true);

        String downloadUrl = accessCopiesService.getDownloadUrl(noOriginalFileObj, principals);

        assertEquals("", downloadUrl);
    }

    @Test
    public void hasPlayableAudiofile() {
        hasPermissions(mdObjectAudio, true);
        assertEquals(mdObjectAudio.getId(),
                accessCopiesService.getDatastreamPid(mdObjectAudio, principals, AUDIO_MIMETYPE_REGEX));
    }

    @Test
    public void doesNotHavePlayableAudiofile() {
        hasPermissions(mdObjectImg, true);
        assertNull(accessCopiesService.getDatastreamPid(mdObjectImg, principals, AUDIO_MIMETYPE_REGEX),
                "Playable audio file pid found");
    }

    @Test
    public void primaryObjThumbnail() {
        hasPermissions(mdObjectImg, true);

        assertEquals(mdObjectImg.getId(), accessCopiesService.getThumbnailId(mdObjectImg, principals, false));
        assertEquals(mdObjectImg.getId(), accessCopiesService.getThumbnailId(mdObjectImg, principals, true));
    }

    @Test
    public void noPrimaryObjThumbnailMultipleFiles() {
        hasPermissions(noOriginalFileObj, true);
        hasPermissions(mdObjectXml, true);
        hasPermissions(mdObjectImg, true);
        noOriginalFileObj.setFileFormatCategory(Collections.singletonList(ContentCategory.image.getDisplayName()));
        noOriginalFileObj.setFileFormatType(Collections.singletonList("png"));
        populateResultList(mdObjectImg);
        when(searchResultResponse.getResultCount()).thenReturn(2L);

        assertEquals(noOriginalFileObj.getId(), accessCopiesService.getThumbnailId(noOriginalFileObj, principals, false));
        // Gets the ID of the specific child with a thumbnail
        assertEquals(mdObjectImg.getId(), accessCopiesService.getThumbnailId(noOriginalFileObj, principals, true));
        assertRequestedDatastreamFilter(DatastreamType.JP2_ACCESS_COPY);
        assertSortType("default");
    }

    @Test
    public void noPrimaryObjNoThumbnail() {
        hasPermissions(noOriginalFileObj, true);
        hasPermissions(mdObjectXml, true);
        noOriginalFileObj.setFileFormatCategory(Collections.singletonList(ContentCategory.text.getDisplayName()));
        noOriginalFileObj.setFileFormatType(Collections.singletonList("txt"));

        populateResultList();

        assertNull(accessCopiesService.getThumbnailId(noOriginalFileObj, principals, false));
        assertNull(accessCopiesService.getThumbnailId(noOriginalFileObj, principals, true));
    }

    @Test
    public void getThumbnailIdNoPrimaryMultipleImages() {
        var mdObjectImg2 = new ContentObjectSolrRecord();
        mdObjectImg2.setResourceType(ResourceType.File.name());
        mdObjectImg2.setId(UUID.randomUUID().toString());
        var imgDatastreams = List.of(
                ORIGINAL_FILE.getId() + "|image/jpg|file2.png|png|555|urn:sha1:checksum|");
        mdObjectImg2.setFileFormatCategory(Collections.singletonList(ContentCategory.image.getDisplayName()));
        mdObjectImg2.setFileFormatType(Collections.singletonList("png"));
        mdObjectImg2.setDatastream(imgDatastreams);

        hasPermissions(noOriginalFileObj, true);
        hasPermissions(mdObjectImg2, true);
        hasPermissions(mdObjectImg, true);
        noOriginalFileObj.setFileFormatCategory(Collections.singletonList(ContentCategory.image.getDisplayName()));
        noOriginalFileObj.setFileFormatType(Collections.singletonList("png"));
        populateResultList(mdObjectImg2);
        when(searchResultResponse.getResultCount()).thenReturn(2L);

        assertEquals(noOriginalFileObj.getId(), accessCopiesService.getThumbnailId(noOriginalFileObj, principals, false));

        // Gets the ID of the specific child with a thumbnail
        assertEquals(mdObjectImg2.getId(), accessCopiesService.getThumbnailId(noOriginalFileObj, principals, true));
        assertRequestedDatastreamFilter(DatastreamType.JP2_ACCESS_COPY);
        assertSortType("default");
    }

    private void assertRequestedDatastreamFilter(DatastreamType expectedType) {
        var searchState = searchRequestCaptor.getValue().getSearchState();
        var queryFilter = (NamedDatastreamFilter) searchState.getFilters().get(0);
        assertEquals(expectedType, queryFilter.getDatastreamType(),
                "Expected request to be filtered by datastream " + expectedType.name());
    }

    private void assertHasPopulatedFieldFilter(SearchFieldKey expectedKey) {
        var searchState = searchRequestCaptor.getValue().getSearchState();
        var queryFilter = (HasPopulatedFieldFilter) searchState.getFilters().get(0);
        assertEquals(expectedKey, queryFilter.getFieldKey(),
                "Expected request to be filtered by key " + expectedKey.name());
    }

    private void assertSortType(String expectedSort) {
        var searchState = searchRequestCaptor.getValue().getSearchState();
        assertEquals(expectedSort, searchState.getSortType(), "Expected request to be sorted by type");
    }

    @Test
    public void noFilesThumbnailMultipleFiles() {
        hasPermissions(noOriginalFileObj, true);
        populateResultList();

        assertNull(accessCopiesService.getThumbnailId(noOriginalFileObj, principals, false));
        assertNull(accessCopiesService.getThumbnailId(noOriginalFileObj, principals, true));
    }

    @Test
    public void populateThumbnailIdWithThumb() {
        hasPermissions(mdObjectImg, true);
        assertNull(mdObjectImg.getThumbnailId());
        accessCopiesService.populateThumbnailId(mdObjectImg, principals, false);
        assertEquals(mdObjectImg.getId(), mdObjectImg.getThumbnailId());
    }

    @Test
    public void populateThumbnailIdWithoutThumb() {
        hasPermissions(noOriginalFileObj, true);
        assertNull(noOriginalFileObj.getThumbnailId());
        accessCopiesService.populateThumbnailId(noOriginalFileObj, principals, false);
        assertNull(noOriginalFileObj.getThumbnailId());
    }

    @Test
    public void populateThumbnailIds() {
        hasPermissions(mdObjectImg, true);
        hasPermissions(noOriginalFileObj, true);
        accessCopiesService.populateThumbnailIds(Arrays.asList(mdObjectImg, noOriginalFileObj), principals, false);
        assertNull(noOriginalFileObj.getThumbnailId());
        assertEquals(mdObjectImg.getId(), mdObjectImg.getThumbnailId());
    }

    @Test
    public void hasViewableFilesImageFileTest() {
        var mdObjectImg = createImgObject(ResourceType.File);
        hasPermissions(mdObjectImg, true);

        assertTrue(accessCopiesService.hasViewableFiles(mdObjectImg, principals));
    }

    @Test
    public void hasViewableFilesAudioFileTest() {
        var mdObjectAudio = createAudioObject(ResourceType.File);
        hasPermissions(mdObjectAudio, true);
        when(searchResultResponse.getResultCount()).thenReturn(1L);
        assertFalse(accessCopiesService.hasViewableFiles(mdObjectAudio, principals));
    }

    @Test
    public void hasViewableFilesImageWorkTest() {
        hasPermissions(mdObjectImg, true);
        when(searchResultResponse.getResultCount()).thenReturn(1L);
        assertTrue(accessCopiesService.hasViewableFiles(mdObjectImg, principals));
        assertRequestedDatastreamFilter(DatastreamType.JP2_ACCESS_COPY);
    }

    @Test
    public void hasStreamingSoundTest() {
        var mdObjectAudio = createAudioObject(ResourceType.Work);
        hasPermissions(mdObjectAudio, true);

        when(searchResultResponse.getResultList()).thenReturn(List.of(mdObjectAudio));
        when(searchResultResponse.getResultCount()).thenReturn(1L);
        var audioObj = accessCopiesService.getFirstStreamingChild(mdObjectAudio, principals);
        assertEquals("sound", audioObj.getStreamingType());
        assertHasPopulatedFieldFilter(SearchFieldKey.STREAMING_TYPE);
    }

    @Test
    public void hasStreamingVideoTest() {
        var mdObjectVideo = createVideoObject(ResourceType.Work);
        hasPermissions(mdObjectVideo, true);
        when(searchResultResponse.getResultList()).thenReturn(List.of(mdObjectVideo));
        when(searchResultResponse.getResultCount()).thenReturn(1L);
        var videoObj = accessCopiesService.getFirstStreamingChild(mdObjectVideo, principals);
        assertEquals("video", videoObj.getStreamingType());
        assertHasPopulatedFieldFilter(SearchFieldKey.STREAMING_TYPE);
    }

    @Test
    public void doesNotHaveStreamingTest() {
        hasPermissions(mdObjectImg, true);
        when(searchResultResponse.getResultCount()).thenReturn(0L);
        assertNull(accessCopiesService.getFirstStreamingChild(mdObjectImg, principals));
        assertHasPopulatedFieldFilter(SearchFieldKey.STREAMING_TYPE);
    }

    @Test
    public void doesNotHaveStreamingNonWorkTest() {
        var mdObjectVideoFile = createVideoObject(ResourceType.File);
        hasPermissions(mdObjectVideoFile, true);

        assertNull(accessCopiesService.getFirstStreamingChild(mdObjectVideoFile, principals));
    }

    @Test
    public void hasMatchingChildTest() {
        var mdObjectPdf = createPdfObject(ResourceType.Work);
        hasPermissions(mdObjectPdf, true);
        when(searchResultResponse.getResultList()).thenReturn(List.of(mdObjectPdf));
        when(searchResultResponse.getResultCount()).thenReturn(1L);
        var pdfObj = accessCopiesService.getFirstMatchingChild(mdObjectPdf,
                "application/pdf", principals);
        assertNotNull(pdfObj);
        assertTrue(pdfObj.getFileFormatType().contains("application/pdf"));
    }

    @Test
    public void hasNoMatchingChildForSpecifiedFileTypeTest() {
        hasPermissions(mdObjectXml, true);
        when(searchResultResponse.getResultList()).thenReturn(List.of(mdObjectXml));
        when(searchResultResponse.getResultCount()).thenReturn(0L);
        var xmlObj = accessCopiesService.getFirstMatchingChild(mdObjectXml,
                "application/pdf", principals);

        assertNull(xmlObj);
    }

    @Test
    public void hasNoMatchingChildForFilesTest() {
        var mdObject = createPdfObject(ResourceType.File);
        hasPermissions(mdObject, true);
        when(searchResultResponse.getResultList()).thenReturn(List.of(mdObject));
        var obj = accessCopiesService.getFirstMatchingChild(mdObject,
                "application/pdf", principals);
        assertNull(obj);
    }

    private void hasPermissions(ContentObjectSolrRecord contentObject, boolean hasAccess) {
        when(accessControlService.hasAccess(contentObject.getPid(), principals, viewOriginal)).thenReturn(hasAccess);
    }

    private void populateResultList(ContentObjectRecord... objects) {
        when(searchResultResponse.getResultList()).thenReturn(Arrays.asList(objects));
    }
}
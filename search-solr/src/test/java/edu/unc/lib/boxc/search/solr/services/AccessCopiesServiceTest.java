package edu.unc.lib.boxc.search.solr.services;

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
import edu.unc.lib.boxc.search.solr.filters.IIIFv3ViewableFilter;
import edu.unc.lib.boxc.search.solr.filters.NamedDatastreamFilter;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.utils.PermissionsHelper;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static edu.unc.lib.boxc.auth.api.Permission.viewOriginal;
import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.search.solr.services.AccessCopiesService.AUDIO_MIMETYPE_REGEX;
import static edu.unc.lib.boxc.search.solr.services.AccessCopiesService.PDF_MIMETYPE_REGEX;
import static edu.unc.lib.boxc.search.solr.services.AccessCopiesService.VIDEO_MIMETYPE_REGEX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
        var mdObjectVideo = new ContentObjectSolrRecord();
        mdObjectVideo.setResourceType(resourceType.name());
        mdObjectVideo.setId(UUID.randomUUID().toString());
        List<String> audioDatastreams = Collections.singletonList(
                ORIGINAL_FILE.getId() + "|video/mp4|file.mp4|mp4|766|urn:sha1:checksum|");
        mdObjectVideo.setFileFormatCategory(Collections.singletonList(ContentCategory.video.getDisplayName()));
        mdObjectVideo.setFileFormatType(Collections.singletonList("video/mp4"));
        mdObjectVideo.setDatastream(audioDatastreams);
        mdObjectVideo.setStreamingUrl("https://durastream.lib.unc.edu/player?spaceId=open-hls&filename=04950_VT0008_0001");
        mdObjectVideo.setStreamingType("video");
        return mdObjectVideo;
    }

    private ContentObjectSolrRecord createPdfObject(ResourceType resourceType) {
        var mdObject = new ContentObjectSolrRecord();
        mdObject.setResourceType(resourceType.name());
        mdObject.setId(UUID.randomUUID().toString());
        List<String> datastreams = List.of(
                ORIGINAL_FILE.getId() + "|application/pdf|file.pdf|pdf|766|urn:sha1:checksum|",
                JP2_ACCESS_COPY.getId() + "|image/jp2|file.jp2|jp2||||1200x1200");
        mdObject.setFileFormatCategory(Collections.singletonList(ContentCategory.text.getDisplayName()));
        mdObject.setFileFormatType(Collections.singletonList("application/pdf"));
        mdObject.setDatastream(datastreams);
        return mdObject;
    }

    private ContentObjectSolrRecord createXPdfObject(ResourceType resourceType) {
        var mdObject = new ContentObjectSolrRecord();
        mdObject.setResourceType(resourceType.name());
        mdObject.setId(UUID.randomUUID().toString());
        List<String> datastreams = List.of(
                ORIGINAL_FILE.getId() + "|application/x-pdf|file.pdf|pdf|766|urn:sha1:checksum|",
                JP2_ACCESS_COPY.getId() + "|image/jp2|file.jp2|jp2||||1200x1200");
        mdObject.setFileFormatCategory(Collections.singletonList(ContentCategory.text.getDisplayName()));
        mdObject.setFileFormatType(Collections.singletonList("application/x-pdf"));
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
    public void hasPlayableVideoFile() {
        hasPermissions(mdObjectVideo, true);
        assertEquals(mdObjectVideo.getId(),
                accessCopiesService.getDatastreamPid(mdObjectVideo, principals, VIDEO_MIMETYPE_REGEX));
    }

    @Test
    public void doesNotHavePlayableVideoFile() {
        hasPermissions(mdObjectImg, true);
        assertNull(accessCopiesService.getDatastreamPid(mdObjectImg, principals, VIDEO_MIMETYPE_REGEX),
                "Playable video file pid found");
    }

    @Test
    public void primaryObjThumbnail() {
        hasPermissions(mdObjectImg, true);

        assertEquals(mdObjectImg, accessCopiesService.getThumbnailRecord(mdObjectImg, principals, false));
        assertEquals(mdObjectImg, accessCopiesService.getThumbnailRecord(mdObjectImg, principals, true));
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

        assertEquals(noOriginalFileObj, accessCopiesService.getThumbnailRecord(noOriginalFileObj, principals, false));
        // Gets the ID of the specific child with a thumbnail
        assertEquals(mdObjectImg, accessCopiesService.getThumbnailRecord(noOriginalFileObj, principals, true));
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

        assertNull(accessCopiesService.getThumbnailRecord(noOriginalFileObj, principals, false));
        assertNull(accessCopiesService.getThumbnailRecord(noOriginalFileObj, principals, true));
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

        var thumbnailRecord = accessCopiesService.getThumbnailRecord(noOriginalFileObj, principals, false);
        assertEquals(noOriginalFileObj.getId(), thumbnailRecord.getId());

        // Gets the ID of the specific child with a thumbnail
        var thumbnailRecordChildren = accessCopiesService.getThumbnailRecord(noOriginalFileObj, principals, true);
        assertEquals(mdObjectImg2.getId(), thumbnailRecordChildren.getId());
        assertRequestedDatastreamFilter(DatastreamType.JP2_ACCESS_COPY);
        assertSortType("default");
    }

    @Test
    public void getThumbnailRecordAssignedThumbnailMultipleImages() {
        var mdObjectImg2 = new ContentObjectSolrRecord();
        mdObjectImg2.setResourceType(ResourceType.File.name());
        mdObjectImg2.setId(UUID.randomUUID().toString());
        var imgDatastreams = List.of(
                ORIGINAL_FILE.getId() + "|image/jpg|file2.png|png|555|urn:sha1:checksum|");
        mdObjectImg2.setFileFormatCategory(Collections.singletonList(ContentCategory.image.getDisplayName()));
        mdObjectImg2.setFileFormatType(Collections.singletonList("png"));
        mdObjectImg2.setDatastream(imgDatastreams);

        var workRecord = new ContentObjectSolrRecord();
        workRecord.setResourceType(ResourceType.Work.name());
        var id = UUID.randomUUID().toString();
        workRecord.setId(id);
        List<String> workDatastreams = List.of(
                ORIGINAL_FILE.getId() + "|image/png|file.png|png|766|urn:sha1:checksum|" + mdObjectImg2.getId() + "|1200x1200",
                JP2_ACCESS_COPY.getId() + "|image/jp2|bunny.jp2|jp2|||" + mdObjectImg2.getId() + "|1200x1200");
        workRecord.setFileFormatCategory(Collections.singletonList(ContentCategory.image.getDisplayName()));
        workRecord.setFileFormatType(Collections.singletonList("image/png"));
        workRecord.setDatastream(workDatastreams);

        hasPermissions(workRecord, true);
        hasPermissions(mdObjectImg2, true);
        hasPermissions(mdObjectImg, true);
        when(solrSearchService.getObjectById(any())).thenReturn(mdObjectImg2);

        var thumbnailRecord = accessCopiesService.getThumbnailRecord(workRecord, principals, false);
        assertEquals(mdObjectImg2.getId(), thumbnailRecord.getId());

        var thumbnailRecordChildren = accessCopiesService.getThumbnailRecord(workRecord, principals, true);
        assertEquals(mdObjectImg2.getId(), thumbnailRecordChildren.getId());
    }

    @Test
    public void workWithImageWithNoThumbnail() {
        hasPermissions(noOriginalFileObj, true);
        hasPermissions(mdObjectXml, true);
        hasPermissions(mdObjectImg, true);
        noOriginalFileObj.setFileFormatCategory(Collections.singletonList(ContentCategory.image.getDisplayName()));
        noOriginalFileObj.setFileFormatType(Collections.singletonList("png"));
        when(searchResultResponse.getResultCount()).thenReturn(0L);

        assertEquals(noOriginalFileObj, accessCopiesService.getThumbnailRecord(noOriginalFileObj, principals, false));
        assertNull(accessCopiesService.getThumbnailRecord(noOriginalFileObj, principals, true));
    }

    private void assertRequestedDatastreamFilter(DatastreamType expectedType) {
        var searchState = searchRequestCaptor.getValue().getSearchState();
        var queryFilter = (NamedDatastreamFilter) searchState.getFilters().get(0);
        assertEquals(expectedType, queryFilter.getDatastreamType(),
                "Expected request to be filtered by datastream " + expectedType.name());
    }

    private void assertRequestedIiifV3Filters() {
        var searchState = searchRequestCaptor.getValue().getSearchState();
        var queryFilter = (IIIFv3ViewableFilter) searchState.getFilters().get(0);
        assertEquals("((fileFormatType:video/mp4 OR fileFormatType:video/mpeg" +
                        " OR fileFormatType:video/quicktime OR fileFormatType:video/mp4 OR fileFormatType:audio/mpeg)" +
                        " OR (datastream:jp2|*) OR (datastream:audio|*)) AND !fileFormatType:\"application/pdf\"",
                queryFilter.toFilterString());
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

        assertNull(accessCopiesService.getThumbnailRecord(noOriginalFileObj, principals, false));
        assertNull(accessCopiesService.getThumbnailRecord(noOriginalFileObj, principals, true));
    }

    @Test
    public void populateThumbnailInfoWithThumb() {
        hasPermissions(mdObjectImg, true);
        assertNull(mdObjectImg.getThumbnailId());
        accessCopiesService.populateThumbnailInfo(mdObjectImg, principals, false);
        assertEquals(mdObjectImg.getId(), mdObjectImg.getThumbnailId());
    }

    @Test
    public void populateThumbnailIdWithoutThumb() {
        hasPermissions(noOriginalFileObj, true);
        assertNull(noOriginalFileObj.getThumbnailId());
        accessCopiesService.populateThumbnailInfo(noOriginalFileObj, principals, false);
        assertNull(noOriginalFileObj.getThumbnailId());
    }

    @Test
    public void populateThumbnailInfoForList() {
        hasPermissions(mdObjectImg, true);
        hasPermissions(noOriginalFileObj, true);
        accessCopiesService.populateThumbnailInfoForList(Arrays.asList(mdObjectImg, noOriginalFileObj), principals, false);
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
    public void hasViewableFilesVideoFileTest() {
        var mdObjectVideo = createVideoObject(ResourceType.File);
        hasPermissions(mdObjectVideo, true);
        when(searchResultResponse.getResultCount()).thenReturn(1L);
        assertFalse(accessCopiesService.hasViewableFiles(mdObjectVideo, principals));
    }

    @Test
    public void hasViewableFilesImageWorkTest() {
        hasPermissions(mdObjectImg, true);
        when(searchResultResponse.getResultCount()).thenReturn(1L);
        assertTrue(accessCopiesService.hasViewableFiles(mdObjectImg, principals));
        assertRequestedIiifV3Filters();
    }

    @Test
    public void hasViewableFilesPdfFileTest() {
        var mdObjectImg = createPdfObject(ResourceType.File);
        hasPermissions(mdObjectImg, true);

        assertFalse(accessCopiesService.hasViewableFiles(mdObjectImg, principals));
    }

    @Test
    public void hasViewableFilesPdfXFileTest() {
        var mdObjectImg = createXPdfObject(ResourceType.File);
        hasPermissions(mdObjectImg, true);

        assertFalse(accessCopiesService.hasViewableFiles(mdObjectImg, principals));
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
    public void getFirstViewableFileTest() {
        var mdObjectVideo = createVideoObject(ResourceType.Work);
        hasPermissions(mdObjectVideo, true);
        accessCopiesService.getFirstViewableFile(mdObjectVideo, principals);
        assertRequestedIiifV3Filters();
        var searchState = searchRequestCaptor.getValue().getSearchState();
        assertEquals(1, searchState.getRowsPerPage());
    }

    @Test
    public void hasMatchingChildTest() {
        var mdObjectPdf = createPdfObject(ResourceType.Work);
        hasPermissions(mdObjectPdf, true);
        when(searchResultResponse.getResultList()).thenReturn(List.of(mdObjectPdf));
        when(searchResultResponse.getResultCount()).thenReturn(1L);
        var pdfObj = accessCopiesService.getFirstMatchingChild(mdObjectPdf,
                List.of("application/pdf"), principals);
        assertNotNull(pdfObj);
        assertTrue(pdfObj.getFileFormatType().contains("application/pdf"));
    }

    @Test
    public void hasMatchingChildXPDFTest() {
        var mdObjectPdf = createXPdfObject(ResourceType.Work);
        hasPermissions(mdObjectPdf, true);
        when(searchResultResponse.getResultList()).thenReturn(List.of(mdObjectPdf));
        when(searchResultResponse.getResultCount()).thenReturn(1L);
        var pdfObj = accessCopiesService.getFirstMatchingChild(mdObjectPdf,
                Arrays.asList("application/pdf", "application/x-pdf"), principals);
        assertNotNull(pdfObj);
        assertTrue(pdfObj.getFileFormatType().contains("application/x-pdf"));
    }

    @Test
    public void hasNoMatchingChildForSpecifiedFileTypeTest() {
        hasPermissions(mdObjectXml, true);
        when(searchResultResponse.getResultList()).thenReturn(List.of(mdObjectXml));
        when(searchResultResponse.getResultCount()).thenReturn(0L);
        var xmlObj = accessCopiesService.getFirstMatchingChild(mdObjectXml,
                List.of("application/pdf"), principals);

        assertNull(xmlObj);
    }

    @Test
    public void hasNoMatchingChildForFilesTest() {
        var mdObject = createPdfObject(ResourceType.File);
        hasPermissions(mdObject, true);
        when(searchResultResponse.getResultList()).thenReturn(List.of(mdObject));
        var obj = accessCopiesService.getFirstMatchingChild(mdObject,
                List.of("application/pdf"), principals);
        assertNull(obj);
    }

    @Test
    public void listViewableFilesForWorkTest() {
        hasPermissions(noOriginalFileObj, true);
        hasPermissions(mdObjectXml, true);
        hasPermissions(mdObjectImg, true);
        populateResultList(mdObjectImg);
        when(searchResultResponse.getResultCount()).thenReturn(1L);

        when(solrSearchService.getObjectById(any())).thenReturn(noOriginalFileObj);

        var results = accessCopiesService.listViewableFiles(noOriginalFileObj.getPid(), principals);
        assertEquals(2, results.size());
        assertTrue(results.contains(noOriginalFileObj));
        assertTrue(results.contains(mdObjectImg));
    }

    @Test
    public void listViewableFilesForViewableFileTest() {
        mdObjectImg.setResourceType(ResourceType.File.name());
        hasPermissions(mdObjectImg, true);
        when(solrSearchService.getObjectById(any())).thenReturn(mdObjectImg);

        var results = accessCopiesService.listViewableFiles(mdObjectImg.getPid(), principals);
        assertEquals(1, results.size());
        assertTrue(results.contains(mdObjectImg));
    }

    @Test
    public void listViewableFilesForNonViewableFileTest() {
        mdObjectXml.setResourceType(ResourceType.File.name());
        hasPermissions(mdObjectXml, true);
        when(solrSearchService.getObjectById(any())).thenReturn(mdObjectXml);

        var results = accessCopiesService.listViewableFiles(mdObjectXml.getPid(), principals);
        assertTrue(results.isEmpty());
    }

    @Test
    public void listViewableFilesForFolderTest() {
        var folderObj = createXmlObject(ResourceType.Folder);
        hasPermissions(folderObj, true);
        when(solrSearchService.getObjectById(any())).thenReturn(folderObj);

        var results = accessCopiesService.listViewableFiles(folderObj.getPid(), principals);
        assertTrue(results.isEmpty());
    }

    private void hasPermissions(ContentObjectSolrRecord contentObject, boolean hasAccess) {
        when(accessControlService.hasAccess(contentObject.getPid(), principals, viewOriginal)).thenReturn(hasAccess);
    }

    private void populateResultList(ContentObjectRecord... objects) {
        when(searchResultResponse.getResultList()).thenReturn(new ArrayList<>(List.of(objects)));
    }
}
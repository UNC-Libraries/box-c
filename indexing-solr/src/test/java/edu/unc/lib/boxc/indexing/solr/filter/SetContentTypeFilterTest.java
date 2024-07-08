package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.boxc.indexing.solr.utils.TechnicalMetadataService;
import edu.unc.lib.boxc.model.api.StreamingConstants;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.services.ContentPathFactory;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.search.api.ContentCategory;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.solr.facets.CutoffFacetImpl;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.util.MimeType;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static edu.unc.lib.boxc.indexing.solr.test.MockRepositoryObjectHelpers.makeFileObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 *
 * @author harring
 *
 */
public class SetContentTypeFilterTest {

    private DocumentIndexingPackage dip;
    private PID pid;
    private AutoCloseable closeable;
    @Mock
    private FileObject fileObj;
    @Mock
    private WorkObject workObj;
    @Mock
    private BinaryObject binObj;
    @Mock
    private BinaryObject techMdObj;
    @Mock
    private FolderObject folderObj;
    private IndexDocumentBean idb;
    @Captor
    private ArgumentCaptor<List<String>> listCaptor;
    @Mock
    private SolrSearchService solrSearchService;
    @Mock
    private ContentPathFactory contentPathFactory;
    @Mock
    private DocumentIndexingPackageDataLoader documentIndexingPackageDataLoader;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    private TechnicalMetadataService technicalMetadataService;
    private CutoffFacet ancestorPath;
    @Mock
    private SearchResultResponse searchResultResponse;

    private SetContentTypeFilter filter;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);

        pid = PIDs.get(UUID.randomUUID().toString());
        dip = new DocumentIndexingPackage(pid, null, documentIndexingPackageDataLoader);
        dip.setPid(pid);
        idb = dip.getDocument();

        when(fileObj.getOriginalFile()).thenReturn(binObj);
        ancestorPath = new CutoffFacetImpl(SearchFieldKey.ANCESTOR_PATH.name(), Arrays.asList(
                "1,1ed05130-d25f-4890-9086-02d98625275f", "2,5aa1ad67-c494-48dc-839e-241826559abb"), 0);
        when(solrSearchService.getSearchResults(any(SearchRequest.class))).thenReturn(searchResultResponse);
        when(solrSearchService.getAncestorPath(pid.getId(), null)).thenReturn(ancestorPath);

        technicalMetadataService = new TechnicalMetadataService();
        technicalMetadataService.setRepositoryObjectLoader(repositoryObjectLoader);
        when(repositoryObjectLoader.getBinaryObject(any(PID.class))).thenReturn(techMdObj);
        technicalMetadataService.init();

        filter = new SetContentTypeFilter();
        filter.setSolrSearchService(solrSearchService);
        filter.setTechnicalMetadataService(technicalMetadataService);
        filter.setContentPathFactory(contentPathFactory);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetContentTypeFromWorkObjectWithPrimary() throws Exception {
        dip.setContentObject(workObj);
        when(workObj.getPrimaryObject()).thenReturn(fileObj);

        var fileRec = new ContentObjectSolrRecord();
        fileRec.setFileFormatCategory(Collections.singletonList(ContentCategory.text.getDisplayName()));
        fileRec.setFileFormatType(Collections.singletonList("text/xml"));
        fileRec.setFileFormatDescription(Collections.singletonList("Extensible Markup Language"));
        when(searchResultResponse.getResultList()).thenReturn(Collections.singletonList(fileRec));

        filter.filter(dip);

        assertHasFileTypes(idb, "text/xml");
        assertHasFileDescriptions(idb, "Extensible Markup Language");
        assertHasCategories(idb, ContentCategory.text);
    }

    @Test
    public void testGetContentTypeFromFileObject() throws Exception {
        mockFile("data.csv", "Unknown Binary", "text/csv");

        filter.filter(dip);

        assertHasFileTypes(idb, "text/csv");
        assertHasFileDescriptions(idb, "Comma-Separated Values");
        assertHasCategories(idb, ContentCategory.spreadsheet);
    }

     @Test
    public void testExtensionNotFoundInMapping() throws Exception {
        // use filename with raw image extension not found in our mapping
        mockFile("image.x3f", "Unknown Binary", "some_wacky_type");

        filter.filter(dip);

        assertHasFileTypes(idb, "some_wacky_type");
        assertNull(idb.getFileFormatDescription());
        assertHasCategories(idb, ContentCategory.unknown);
    }

    @Test
    public void testNotWorkAndNotFileObject() throws Exception {
        dip.setContentObject(folderObj);

        filter.filter(dip);

        assertTrue(CollectionUtils.isEmpty(idb.getFileFormatType()));
        assertTrue(CollectionUtils.isEmpty(idb.getFileFormatDescription()));
        assertTrue(CollectionUtils.isEmpty(idb.getFileFormatCategory()));
    }

    @Test
    public void testWorkWithoutPrimaryObject() throws Exception {
        dip.setContentObject(workObj);

        var fileRec = new ContentObjectSolrRecord();
        fileRec.setFileFormatCategory(Collections.singletonList(ContentCategory.text.getDisplayName()));
        fileRec.setFileFormatType(Collections.singletonList("text/xml"));
        fileRec.setFileFormatDescription(Collections.singletonList("Extensible Markup Language"));
        when(searchResultResponse.getResultList()).thenReturn(Collections.singletonList(fileRec));

        filter.filter(dip);

        assertHasFileTypes(idb, "text/xml");
        assertHasFileDescriptions(idb, "Extensible Markup Language");
        assertHasCategories(idb, ContentCategory.text);
    }

    @Test
    public void testGetPlainTextContentType() throws Exception {
        mockFile("file.txt", "Unknown Binary", "text/plain");

        filter.filter(dip);

        assertHasFileTypes(idb, "text/plain");
        assertHasFileDescriptions(idb, "Plain Text");
        assertHasCategories(idb, ContentCategory.text);
    }

    @Test
    public void testGetPlainTextWithCharsetContentType() throws Exception {
        mockFile("file.txt", "Unknown Binary", "text/plain; charset=UTF-8");
        when(techMdObj.getPid()).thenReturn(DatastreamPids.getTechnicalMetadataPid(pid));
        var mime = MimeType.valueOf("text/plain");

        filter.filter(dip);

        assertHasFileTypes(idb, "text/plain");
        assertHasFileDescriptions(idb, "Plain Text");
        assertHasCategories(idb, ContentCategory.text);
    }

    @Test
    public void testRtfMimetypeOverride() throws Exception {
        mockFile("file.rtf", "Unknown Binary", "text/plain");

        filter.filter(dip);

        assertHasFileTypes(idb, "application/rtf");
        assertHasFileDescriptions(idb, "Rich Text Format");
        assertHasCategories(idb, ContentCategory.text);
    }

    @Test
    public void testAppleDoublePdf() throws Exception {
        mockFile("._doc.pdf", "AppleDouble Resource Fork", "multipart/appledouble");

        filter.filter(dip);

        assertHasFileTypes(idb, "multipart/appledouble");
        assertHasFileDescriptions(idb, "AppleDouble Resource Fork");
        assertHasCategories(idb, ContentCategory.unknown);
    }

    @Test
    public void testAccessDb() throws Exception {
        mockFile("._doc.mdb", "Microsoft Access database", "application/x-msaccess");

        filter.filter(dip);

        assertHasFileTypes(idb, "application/x-msaccess");
        assertHasFileDescriptions(idb, "Microsoft Access database");
        assertHasCategories(idb, ContentCategory.database);
    }

    @Test
    public void testEmail() throws Exception {
        mockFile("email.msg", "Email", "message/partial");

        filter.filter(dip);

        assertHasFileTypes(idb, "message/partial");
        assertHasFileDescriptions(idb, "Email");
        assertHasCategories(idb, ContentCategory.email);
    }

    @Test
    public void testImageJpg() throws Exception {
        mockFile("picture.jpg", "JPEG EXIF", "image/jpeg");

        filter.filter(dip);

        assertHasFileTypes(idb, "image/jpeg");
        assertHasFileDescriptions(idb, "JPEG Image");
        assertHasCategories(idb, ContentCategory.image);
    }

    @Test
    public void testVideoMp4() throws Exception {
        mockFile("my_video", "MPEG-4", "video/mp4");

        filter.filter(dip);

        assertHasFileTypes(idb, "video/mp4");
        assertHasFileDescriptions(idb, "MPEG-4");
        assertHasCategories(idb, ContentCategory.video);
    }

    @Test
    public void testAudioWav() throws Exception {
        mockFile("sound_file.wav", "WAVE Audio File Format", "audio/wav");

        filter.filter(dip);

        assertHasFileTypes(idb, "audio/wav");
        assertHasFileDescriptions(idb, "WAVE Audio File Format");
        assertHasCategories(idb, ContentCategory.audio);
    }

    @Test
    public void testNoMimetype() throws Exception {
        mockFile("unidentified.stuff", "Unknown Binary", null);

        filter.filter(dip);

        assertHasFileTypes(idb, "application/octet-stream");
        assertNull(idb.getFileFormatDescription());
        assertHasCategories(idb, ContentCategory.unknown);
    }

    @Test
    public void testNoExtensionNoMimetype() throws Exception {
        mockFile("unidentified", "Unknown Binary", null);

        filter.filter(dip);

        assertHasFileTypes(idb, "application/octet-stream");
        assertNull(idb.getFileFormatDescription());
        assertHasCategories(idb, ContentCategory.unknown);
    }

    @Test
    public void testBlankMimetype() throws Exception {
        mockFile("unidentified", "Unknown Binary", "");

        filter.filter(dip);

        assertHasFileTypes(idb, "application/octet-stream");
        assertNull(idb.getFileFormatDescription());
        assertHasCategories(idb, ContentCategory.unknown);
    }

    @Test
    public void testWorkWithNoFiles() throws Exception {
        dip.setContentObject(workObj);

        when(searchResultResponse.getResultList()).thenReturn(Collections.emptyList());

        filter.filter(dip);

        assertTrue(CollectionUtils.isEmpty(idb.getFileFormatType()));
        assertTrue(CollectionUtils.isEmpty(idb.getFileFormatDescription()));
        assertTrue(CollectionUtils.isEmpty(idb.getFileFormatCategory()));
    }

    @Test
    public void testWorkWithMultipleFileTypes() throws Exception {
        dip.setContentObject(workObj);

        var fileRec1 = new ContentObjectSolrRecord();
        fileRec1.setFileFormatCategory(Collections.singletonList(ContentCategory.text.getDisplayName()));
        fileRec1.setFileFormatType(Collections.singletonList("text/xml"));
        fileRec1.setFileFormatDescription(Collections.singletonList("Extensible Markup Language"));
        var fileRec2 = new ContentObjectSolrRecord();
        fileRec2.setFileFormatCategory(Collections.singletonList(ContentCategory.text.getDisplayName()));
        fileRec2.setFileFormatType(Collections.singletonList("text/plain"));
        fileRec2.setFileFormatDescription(Collections.singletonList("Plain Text"));
        var fileRec3 = new ContentObjectSolrRecord();
        fileRec3.setFileFormatCategory(Collections.singletonList(ContentCategory.text.getDisplayName()));
        fileRec3.setFileFormatType(Collections.singletonList("text/plain"));
        fileRec3.setFileFormatDescription(Collections.singletonList("Plain Text"));
        var fileRec4 = new ContentObjectSolrRecord();
        fileRec4.setFileFormatCategory(Collections.singletonList(ContentCategory.audio.getDisplayName()));
        fileRec4.setFileFormatType(Collections.singletonList("audio/wav"));
        fileRec4.setFileFormatDescription(Collections.singletonList("WAVE Audio File Format"));
        when(searchResultResponse.getResultList()).thenReturn(Arrays.asList(
                fileRec1, fileRec2, fileRec3, fileRec4));

        filter.filter(dip);

        assertHasFileTypes(idb, "text/xml", "text/plain", "audio/wav");
        assertHasFileDescriptions(idb, "Extensible Markup Language", "Plain Text", "WAVE Audio File Format");
        assertHasCategories(idb, ContentCategory.text, ContentCategory.audio);
    }

    @Test
    public void testWorkInPipelineAfterAncestorPathSet() throws Exception {
        dip.setContentObject(workObj);
        idb.setAncestorPath(Arrays.asList("2," + pid.getId()));

        var fileRec = new ContentObjectSolrRecord();
        fileRec.setFileFormatCategory(Collections.singletonList(ContentCategory.text.getDisplayName()));
        fileRec.setFileFormatType(Collections.singletonList("text/xml"));
        fileRec.setFileFormatDescription(Collections.singletonList("Extensible Markup Language"));
        when(searchResultResponse.getResultList()).thenReturn(Collections.singletonList(fileRec));

        filter.filter(dip);

        assertHasFileTypes(idb, "text/xml");
        assertHasFileDescriptions(idb, "Extensible Markup Language");
        assertHasCategories(idb, ContentCategory.text);
        verify(solrSearchService, never()).getAncestorPath(anyString(), any(AccessGroupSet.class));
    }

    @Test
    public void testStreamingOnlyVideo() throws Exception {
        var filePid = TestHelper.makePid();
        var fileObj =  makeFileObject(filePid, repositoryObjectLoader);
        when(dip.getContentObject()).thenReturn(fileObj);
        doThrow(NotFoundException.class).when(fileObj).getOriginalFile();
        var fileResc = fileObj.getResource();
        fileResc.addLiteral(Cdr.streamingUrl, "http://example.com/streaming/video");
        fileResc.addLiteral(Cdr.streamingType, StreamingConstants.STREAMING_TYPE_VIDEO);

        filter.filter(dip);

        assertNull(idb.getFileFormatType());
        assertHasFileDescriptions(idb, "Streaming Video");
        assertHasCategories(idb, ContentCategory.video);
    }

    @Test
    public void testStreamingOnlySound() throws Exception {
        var filePid = TestHelper.makePid();
        var fileObj =  makeFileObject(filePid, repositoryObjectLoader);
        when(dip.getContentObject()).thenReturn(fileObj);
        doThrow(NotFoundException.class).when(fileObj).getOriginalFile();
        var fileResc = fileObj.getResource();
        fileResc.addLiteral(Cdr.streamingUrl, "http://example.com/streaming/audio");
        fileResc.addLiteral(Cdr.streamingType, StreamingConstants.STREAMING_TYPE_SOUND);

        filter.filter(dip);

        assertNull(idb.getFileFormatType());
        assertHasFileDescriptions(idb, "Streaming Audio");
        assertHasCategories(idb, ContentCategory.audio);
    }

    private void mockFile(String filename, String identity, String mimetype) {
        when(dip.getContentObject()).thenReturn(fileObj);
        when(fileObj.getOriginalFile()).thenReturn(binObj);
        when(binObj.getFilename()).thenReturn(filename);
        if (identity != null) {
            when(techMdObj.getPid()).thenReturn(DatastreamPids.getTechnicalMetadataPid(pid));
            when(techMdObj.getBinaryStream()).thenReturn(createTechMdBody(identity));
        }
        when(binObj.getMimetype()).thenReturn(mimetype);
        when(fileObj.getPid()).thenReturn(pid);
    }

    private void assertHasFileDescriptions(IndexDocumentBean idb, String... expectedDescs) {
        for (var expected: expectedDescs) {
            assertTrue(idb.getFileFormatDescription().contains(expected),
                    "Object did not have expected description " + expected + ", were: " + idb.getFileFormatDescription());
        }
        assertEquals(expectedDescs.length, idb.getFileFormatDescription().size(),
                "Incorrect number of descriptions, expected: " + expectedDescs + ", found: " + idb.getFileFormatDescription());
    }

    private void assertHasFileTypes(IndexDocumentBean idb, String... expectedTypes) {
        for (var expected: expectedTypes) {
            assertTrue(idb.getFileFormatType().contains(expected),
                    "Object did not have expected type " + expected + ", types were: " + idb.getFileFormatType());
        }
        assertEquals(expectedTypes.length, idb.getFileFormatType().size(),
                "Incorrect number of types, expected: " + expectedTypes + ", found: " + idb.getFileFormatType());
    }

    private void assertHasCategories(IndexDocumentBean idb, ContentCategory... expectedCats) {
        for (var expected: expectedCats) {
            assertTrue(idb.getFileFormatCategory().contains(expected.getDisplayName()),
                    "Object did not have expected category " + expected
                    + ", categories were: " + idb.getFileFormatCategory());
        }
        assertEquals(expectedCats.length, idb.getFileFormatCategory().size(),
                "Incorrect number of categories, expected: " + expectedCats + ", found: " + idb.getFileFormatCategory());
    }

    private InputStream createTechMdBody(String formatIdentity) {
        String body =
                "<premis3:premis xmlns:premis3=\"http://www.loc.gov/premis/v3\">\n" +
                "  <premis3:object xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"premis3:file\">\n" +
                "    <premis3:objectCharacteristics>\n" +
                "      <premis3:compositionLevel>0</premis3:compositionLevel>\n" +
                "      <premis3:format>\n" +
                "        <premis3:formatDesignation>\n" +
                "          <premis3:formatName>" + formatIdentity + "</premis3:formatName>\n" +
                "        </premis3:formatDesignation>\n" +
                "      </premis3:format>\n" +
                "     </premis3:objectCharacteristics>\n" +
                "  </premis3:object>\n" +
                "</premis3:premis>";
        return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8));
    }
}

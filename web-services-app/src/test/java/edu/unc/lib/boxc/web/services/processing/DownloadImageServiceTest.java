package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.models.Datastream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.core.io.UrlResource;

import static edu.unc.lib.boxc.operations.api.images.ImageServerUtil.FULL_SIZE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class DownloadImageServiceTest {
    private static final String IIIF_BASE = "http://example.com/iiif/v3/";
    private DownloadImageService downloadImageService;
    private AutoCloseable closeable;
    @Mock
    private ContentObjectRecord contentObjectRecord;
    @Mock
    private Datastream datastream, jp2Datastream;
    @Mock
    private UrlResource urlResource;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        downloadImageService = new DownloadImageService();
        downloadImageService.setIiifBasePath(IIIF_BASE);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testIsInvalidJP2DatastreamWithValidDataStream() {
        when(contentObjectRecord.getDatastreamObject(eq(DatastreamType.JP2_ACCESS_COPY.getId())))
                .thenReturn(jp2Datastream);
        when(jp2Datastream.getFilesize()).thenReturn(2048L);
        assertFalse(downloadImageService.isInvalidJP2Datastream(contentObjectRecord));
    }

    @Test
    public void testIsInvalidJP2DatastreamWithNullDataStream() {
        when(contentObjectRecord.getDatastreamObject(eq(DatastreamType.JP2_ACCESS_COPY.getId())))
                .thenReturn(null);
        assertTrue(downloadImageService.isInvalidJP2Datastream(contentObjectRecord));
    }

    @Test
    public void testIsInvalidJP2DatastreamWithEmptyDataStream() {
        when(contentObjectRecord.getDatastreamObject(eq(DatastreamType.JP2_ACCESS_COPY.getId())))
                .thenReturn(jp2Datastream);
        when(jp2Datastream.getFilesize()).thenReturn(0L);
        assertTrue(downloadImageService.isInvalidJP2Datastream(contentObjectRecord));
    }

    @Test
    public void testGetDownloadFilenameFullSizeFromOriginalFile() {
        var filename = "original_file";
        var extension = ".png";
        when(contentObjectRecord.getDatastreamObject(any())).thenReturn(datastream);
        when(datastream.getFilename()).thenReturn(filename + extension);

        assertEquals( filename + "_" + FULL_SIZE + ".jpg",
                downloadImageService.getDownloadFilename(contentObjectRecord, FULL_SIZE));
    }

    @Test
    public void testGetDownloadFilenameFormattedSizeFromJP2() {
        var filename = "derivative_file";
        var extension = ".png";
        when(contentObjectRecord.getDatastreamObject(eq(DatastreamType.ORIGINAL_FILE.getId())))
                .thenReturn(null);
        when(contentObjectRecord.getDatastreamObject(eq(DatastreamType.JP2_ACCESS_COPY.getId())))
                .thenReturn(jp2Datastream);
        when(jp2Datastream.getFilename()).thenReturn(filename + extension);

        assertEquals( "derivative_file_800px.jpg",
                downloadImageService.getDownloadFilename(contentObjectRecord, "800"));
    }

    @Test
    public void testParseSizeNoErrors() {
        assertEquals(800, downloadImageService.parseSize("800"));
    }

    @Test
    public void testParseSizeNotANumber() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            downloadImageService.parseSize("not a number");
        });
    }

    @Test
    public void testGetSizeBiggerThanFull() {
        when(contentObjectRecord.getDatastreamObject(eq(DatastreamType.JP2_ACCESS_COPY.getId())))
                .thenReturn(jp2Datastream);
        when(jp2Datastream.getExtent()).thenReturn("800x800");
        assertEquals(FULL_SIZE, downloadImageService.getSize(contentObjectRecord, "1200"));
    }

    @Test
    public void testGetSizeDatastreamExtentsAreNull() {
        when(contentObjectRecord.getDatastreamObject(eq(DatastreamType.JP2_ACCESS_COPY.getId())))
                .thenReturn(jp2Datastream);
        when(jp2Datastream.getExtent()).thenReturn(null);
        when(contentObjectRecord.getDatastreamObject(eq(DatastreamType.ORIGINAL_FILE.getId())))
                .thenReturn(datastream);
        when(datastream.getExtent()).thenReturn(null);
        assertNull(downloadImageService.getSize(contentObjectRecord, "800"));
    }

    @Test
    public void testGetSizeNoDatastreamObject() {
        Assertions.assertThrows(NotFoundException.class, () -> {
            when(contentObjectRecord.getDatastreamObject(eq(DatastreamType.JP2_ACCESS_COPY.getId())))
                    .thenReturn(null);
            when(contentObjectRecord.getDatastreamObject(eq(DatastreamType.ORIGINAL_FILE.getId())))
                    .thenReturn(null);
            downloadImageService.getSize(contentObjectRecord, "800");
        });
    }

    @Test
    public void testGetSizeFullSize() {
        assertEquals(FULL_SIZE, downloadImageService.getSize(contentObjectRecord, FULL_SIZE));
    }

    @Test
    public void testGetSizeMax() {
        when(contentObjectRecord.getDatastreamObject(eq(DatastreamType.JP2_ACCESS_COPY.getId())))
                .thenReturn(jp2Datastream);
        when(jp2Datastream.getExtent()).thenReturn("800x500");
        assertEquals(FULL_SIZE, downloadImageService.getSize(contentObjectRecord, "800"));
    }

    @Test
    public void testGetSize() {
        when(contentObjectRecord.getDatastreamObject(eq(DatastreamType.JP2_ACCESS_COPY.getId())))
                .thenReturn(jp2Datastream);
        when(jp2Datastream.getExtent()).thenReturn("500x500");
        assertEquals("125", downloadImageService.getSize(contentObjectRecord, "125"));
    }

    @Test
    public void testGetSizeOriginalFileDatastream() {
        when(contentObjectRecord.getDatastreamObject(eq(DatastreamType.JP2_ACCESS_COPY.getId())))
                .thenReturn(null);
        when(contentObjectRecord.getDatastreamObject(eq(DatastreamType.ORIGINAL_FILE.getId())))
                .thenReturn(datastream);

    }

    @Test
    public void testStreamDownload() {
//        try (MockedConstruction<UrlResource> urlResourceClass = Mockito.mockConstruction(UrlResource.class,(mock, context) -> {
//            when(mock.processPayment()).thenReturn("Credit");
//        })){
//        }
    }
}

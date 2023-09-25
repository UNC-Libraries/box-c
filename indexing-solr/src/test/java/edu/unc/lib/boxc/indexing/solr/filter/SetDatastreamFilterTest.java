package edu.unc.lib.boxc.indexing.solr.filter;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackage;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageDataLoader;
import edu.unc.lib.boxc.indexing.solr.utils.TechnicalMetadataService;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.ContentObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService.Derivative;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static edu.unc.lib.boxc.model.api.DatastreamType.JP2_ACCESS_COPY;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.model.api.DatastreamType.THUMBNAIL_LARGE;
import static edu.unc.lib.boxc.model.api.DatastreamType.THUMBNAIL_SMALL;
import static edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids.getOriginalFilePid;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 *
 * @author bbpennel
 *
 */
public class SetDatastreamFilterTest {

    private static final String BASE_URI = "http://example.com/rest/";

    private static final String PID_STRING = "uuid:07d9594f-310d-4095-ab67-79a1056e7430";

    private static final String FILE_MIMETYPE = "text/plain";
    private static final String FILE_NAME = "test.txt";
    private static final String FILE_DIGEST = "urn:sha1:82022e1782b92dce5461ee636a6c5bea8509ffee";
    private static final long FILE_SIZE = 5062l;

    private static final String FILE2_MIMETYPE = "text/xml";
    private static final String FILE2_NAME = "fits.xml";
    private static final String FILE2_DIGEST = "urn:sha1:afbf62faf8a82d00969e0d4d965d62a45bb8c69b";
    private static final long FILE2_SIZE = 7231l;

    private static final String FILE3_MIMETYPE = "image/png";
    private static final String FILE3_NAME = "image.png";
    private static final String FILE3_DIGEST = "urn:sha1:280f5922b6487c39d6d01a5a8e93bfa07b8f1740";
    private static final long FILE3_SIZE = 17136l;
    private static final String FILE3_EXTENT = "375x250";

    private static final String MODS_MIMETYPE = "text/xml";
    private static final String MODS_NAME = "mods.xml";
    private static final String MODS_DIGEST = "urn:sha1:aa0c62faf8a82d00969e0d4d965d62a45bb8c69b";
    private static final long MODS_SIZE = 540l;

    private static final String PREMIS_MIMETYPE = "text/xml";
    private static final String PREMIS_NAME = "premis.xml";
    private static final String PREMIS_DIGEST = "urn:sha1:da39a3ee5e6b4b0d3255bfef95601890afd80709";
    private static final long PREMIS_SIZE = 893l;

    private AutoCloseable closeable;

    @TempDir
    public Path derivDir;

    private DocumentIndexingPackage dip;
    private IndexDocumentBean idb;
    private PID pid;

    @Mock
    private FileObject fileObj;
    @Mock
    private BinaryObject binObj;
    @Captor
    private ArgumentCaptor<List<String>> listCaptor;

    @Mock
    private DerivativeService derivativeService;
    @Mock
    private DocumentIndexingPackageDataLoader documentIndexingPackageDataLoader;
    private TechnicalMetadataService technicalMetadataService;

    private SetDatastreamFilter filter;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);

        pid = PIDs.get(PID_STRING);

        dip = new DocumentIndexingPackage(pid, null, documentIndexingPackageDataLoader);
        dip.setPid(pid);
        idb = dip.getDocument();
        when(fileObj.getOriginalFile()).thenReturn(binObj);
        when(binObj.getPid()).thenReturn(DatastreamPids.getOriginalFilePid(pid));
        when(fileObj.getBinaryObjects()).thenReturn(Arrays.asList(binObj));

        technicalMetadataService = new TechnicalMetadataService();
        technicalMetadataService.init();

        filter = new SetDatastreamFilter();
        filter.setDerivativeService(derivativeService);
        filter.setTechnicalMetadataService(technicalMetadataService);

        when(binObj.getResource()).thenReturn(
                fileResource(ORIGINAL_FILE.getId(), FILE_SIZE, FILE_MIMETYPE, FILE_NAME, FILE_DIGEST));
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void fileObjectTest() throws Exception {
        dip.setContentObject(fileObj);

        filter.filter(dip);

        assertContainsDatastream(idb.getDatastream(), ORIGINAL_FILE.getId(),
                FILE_SIZE, FILE_MIMETYPE, FILE_NAME, FILE_DIGEST, null, null);

        assertEquals(FILE_SIZE, (long) idb.getFilesizeSort());
        assertEquals(FILE_SIZE, (long) idb.getFilesizeTotal());
    }

    @Test
    public void fileObjectMultipleBinariesTest() throws Exception {
        BinaryObject binObj2 = mock(BinaryObject.class);
        when(binObj2.getPid()).thenReturn(DatastreamPids.getTechnicalMetadataPid(pid));
        when(binObj2.getResource()).thenReturn(
                fileResource(TECHNICAL_METADATA.getId(), FILE2_SIZE, FILE2_MIMETYPE, FILE2_NAME, FILE2_DIGEST));
        when(binObj2.getBinaryStream()).thenReturn(getClass().getResourceAsStream("/datastream/techmd.xml"));

        BinaryObject binObj3 = mock(BinaryObject.class);
        when(binObj3.getPid()).thenReturn(PIDs.get(pid.getId() + "/" + THUMBNAIL_LARGE.getId()));
        when(binObj3.getResource()).thenReturn(
                fileResource(THUMBNAIL_LARGE.getId(), FILE3_SIZE, FILE3_MIMETYPE, FILE3_NAME, FILE3_DIGEST));

        when(fileObj.getBinaryObjects()).thenReturn(Arrays.asList(binObj, binObj2, binObj3));
        dip.setContentObject(fileObj);

        filter.filter(dip);

        assertContainsDatastream(idb.getDatastream(), ORIGINAL_FILE.getId(),
                FILE_SIZE, FILE_MIMETYPE, FILE_NAME, FILE_DIGEST, null, null);
        assertContainsDatastream(idb.getDatastream(), TECHNICAL_METADATA.getId(),
                FILE2_SIZE, FILE2_MIMETYPE, FILE2_NAME, FILE2_DIGEST, null, null);
        assertContainsDatastream(idb.getDatastream(), THUMBNAIL_LARGE.getId(),
                FILE3_SIZE, FILE3_MIMETYPE, FILE3_NAME, FILE3_DIGEST, null, null);

        assertEquals(FILE_SIZE, (long) idb.getFilesizeSort());
        assertEquals(FILE_SIZE + FILE2_SIZE + FILE3_SIZE, (long) idb.getFilesizeTotal());
    }

    @Test
    public void fileObjectImageBinaryTest() throws Exception {
        when(binObj.getResource()).thenReturn(
                fileResource(ORIGINAL_FILE.getId(), FILE_SIZE, FILE3_MIMETYPE, "test.png", FILE_DIGEST));

        BinaryObject binObj2 = mock(BinaryObject.class);
        when(binObj2.getPid()).thenReturn(DatastreamPids.getTechnicalMetadataPid(pid));
        when(binObj2.getResource()).thenReturn(
                fileResource(TECHNICAL_METADATA.getId(), FILE2_SIZE, FILE2_MIMETYPE, FILE2_NAME, FILE2_DIGEST));
        when(binObj2.getBinaryStream()).thenReturn(getClass().getResourceAsStream("/datastream/techmd.xml"));

        BinaryObject binObj3 = mock(BinaryObject.class);
        when(binObj3.getPid()).thenReturn(PIDs.get(pid.getId() + "/" + JP2_ACCESS_COPY.getId()));
        when(binObj3.getResource()).thenReturn(
                fileResource(THUMBNAIL_LARGE.getId(), FILE3_SIZE, JP2_ACCESS_COPY.getMimetype(),
                        JP2_ACCESS_COPY.getDefaultFilename(), FILE3_DIGEST));

        BinaryObject binObj4 = mock(BinaryObject.class);
        when(binObj4.getPid()).thenReturn(PIDs.get(pid.getId() + "/" + THUMBNAIL_LARGE.getId()));
        when(binObj4.getResource()).thenReturn(
                fileResource(THUMBNAIL_LARGE.getId(), FILE3_SIZE, FILE3_MIMETYPE, FILE3_NAME, FILE3_DIGEST));

        when(fileObj.getBinaryObjects()).thenReturn(Arrays.asList(binObj, binObj2, binObj3, binObj4));
        dip.setContentObject(fileObj);

        filter.filter(dip);

        assertContainsDatastream(idb.getDatastream(), ORIGINAL_FILE.getId(),
                FILE_SIZE, FILE3_MIMETYPE, "test.png", FILE_DIGEST, null, FILE3_EXTENT);
        assertContainsDatastream(idb.getDatastream(), TECHNICAL_METADATA.getId(),
                FILE2_SIZE, FILE2_MIMETYPE, FILE2_NAME, FILE2_DIGEST, null, null);
        assertContainsDatastream(idb.getDatastream(), THUMBNAIL_LARGE.getId(),
                FILE3_SIZE, FILE3_MIMETYPE, FILE3_NAME, FILE3_DIGEST, null, null);

        assertEquals(FILE_SIZE, (long) idb.getFilesizeSort());
        // JP2 and thumbnail set to same size
        assertEquals(FILE_SIZE + FILE2_SIZE + (FILE3_SIZE * 2), (long) idb.getFilesizeTotal());
    }

    @Test
    public void fileObjectImageBinaryNoDimensionsTest() throws Exception {
        when(binObj.getResource()).thenReturn(
                fileResource(ORIGINAL_FILE.getId(), FILE_SIZE, FILE3_MIMETYPE, "test.png", FILE_DIGEST));

        BinaryObject binObj2 = mock(BinaryObject.class);
        when(binObj2.getPid()).thenReturn(DatastreamPids.getTechnicalMetadataPid(pid));
        when(binObj2.getResource()).thenReturn(
                fileResource(TECHNICAL_METADATA.getId(), FILE2_SIZE, FILE2_MIMETYPE, FILE2_NAME, FILE2_DIGEST));
        when(binObj2.getBinaryStream()).thenReturn(getClass().getResourceAsStream("/datastream/techmdImageNoDimensions.xml"));

        when(fileObj.getBinaryObjects()).thenReturn(Arrays.asList(binObj, binObj2));
        dip.setContentObject(fileObj);

        filter.filter(dip);

        assertContainsDatastream(idb.getDatastream(), ORIGINAL_FILE.getId(),
                FILE_SIZE, FILE3_MIMETYPE, "test.png", FILE_DIGEST, null, null);
        assertContainsDatastream(idb.getDatastream(), TECHNICAL_METADATA.getId(),
                FILE2_SIZE, FILE2_MIMETYPE, FILE2_NAME, FILE2_DIGEST, null, null);

        assertEquals(FILE_SIZE, (long) idb.getFilesizeSort());
        // JP2 and thumbnail set to same size
        assertEquals(FILE_SIZE + FILE2_SIZE, (long) idb.getFilesizeTotal());
    }

    @Test
    public void fileObjectNoOriginalTest() throws Exception {
        Assertions.assertThrows(IndexingException.class, () -> {
            when(binObj.getResource()).thenReturn(
                    fileResource(TECHNICAL_METADATA.getId(), FILE2_SIZE, FILE2_MIMETYPE, FILE2_NAME, FILE2_DIGEST));

            when(fileObj.getBinaryObjects()).thenReturn(Arrays.asList(binObj));
            dip.setContentObject(fileObj);

            filter.filter(dip);
        });
    }

    @Test
    public void fileObjectWithMetadataTest() throws Exception {
        dip.setContentObject(fileObj);
        addMetadataDatastreams(fileObj);

        filter.filter(dip);

        assertContainsDatastream(idb.getDatastream(), ORIGINAL_FILE.getId(),
                FILE_SIZE, FILE_MIMETYPE, FILE_NAME, FILE_DIGEST, null, null);
        assertContainsMetadataDatastreams(idb.getDatastream());

        assertEquals(FILE_SIZE, (long) idb.getFilesizeSort());
        assertEquals(FILE_SIZE + FILE2_SIZE + MODS_SIZE + PREMIS_SIZE, (long) idb.getFilesizeTotal());
    }

    @Test
    public void workObjectTest() throws Exception {
        WorkObject workObj = mock(WorkObject.class);
        when(workObj.getPrimaryObject()).thenReturn(fileObj);
        when(workObj.getPid()).thenReturn(pid);
        addMetadataDatastreams(workObj);

        dip.setContentObject(workObj);

        String fileId = "055ed112-f548-479e-ab4b-bf1aad40d470";
        PID filePid = PIDs.get(fileId);
        when(fileObj.getPid()).thenReturn(filePid);
        when(binObj.getPid()).thenReturn(getOriginalFilePid(filePid));

        filter.filter(dip);

        assertContainsDatastream(idb.getDatastream(), ORIGINAL_FILE.getId(),
                FILE_SIZE, FILE_MIMETYPE, FILE_NAME, FILE_DIGEST, fileId, null);
        assertContainsMetadataDatastreams(idb.getDatastream());

        // Sort size is based off primary object's size
        assertEquals(FILE_SIZE, (long) idb.getFilesizeSort());
        // Work has no datastreams of its own
        assertEquals(FILE2_SIZE + MODS_SIZE + PREMIS_SIZE, (long) idb.getFilesizeTotal());
    }

    @Test
    public void workObjectWithoutPrimaryObjectTest() throws Exception {
        WorkObject workObj = mock(WorkObject.class);

        dip.setContentObject(workObj);
        filter.filter(dip);

        assertNotNull(idb.getDatastream());
        assertNull(idb.getFilesizeSort());
        assertNotNull(idb.getFilesizeTotal());
    }

    @Test
    public void workObjectWithThumbnailNoPrimaryObjectTest() throws Exception {
        WorkObject workObj = mock(WorkObject.class);
        when(workObj.getThumbnailObject()).thenReturn(fileObj);
        when(workObj.getPid()).thenReturn(pid);

        String fileId = "055ed112-f548-479e-ab4b-bf1aad40d470";
        PID filePid = PIDs.get(fileId);
        when(fileObj.getPid()).thenReturn(filePid);
        when(binObj.getPid()).thenReturn(getOriginalFilePid(filePid));
        setUpDerivatives(filePid);

        dip.setContentObject(workObj);
        filter.filter(dip);

        assertNotNull(idb.getDatastream());
        assertNull(idb.getFilesizeSort());
        assertNotNull(idb.getFilesizeTotal());
        assertThumbnailDatastreams(fileId);
    }

    @Test
    public void workObjectTestWithPrimaryAndThumbnailObjects() throws Exception {
        WorkObject workObj = mock(WorkObject.class);
        when(workObj.getPrimaryObject()).thenReturn(fileObj);
        when(workObj.getPid()).thenReturn(pid);
        addMetadataDatastreams(workObj);

        dip.setContentObject(workObj);

        String fileId = "055ed112-f548-479e-ab4b-bf1aad40d470";
        PID filePid = PIDs.get(fileId);
        when(fileObj.getPid()).thenReturn(filePid);
        when(binObj.getPid()).thenReturn(getOriginalFilePid(filePid));

        // set up thumbnail file object
        FileObject thumbnailObj = mock(FileObject.class);
        when(workObj.getThumbnailObject()).thenReturn(thumbnailObj);
        String thumbnailId = "066ed112-f548-479e-ab4b-bf1aad40d678";
        PID thumbnailPid = PIDs.get(thumbnailId);
        when(thumbnailObj.getPid()).thenReturn(thumbnailPid);
        setUpDerivatives(thumbnailPid);

        filter.filter(dip);

        assertContainsDatastream(idb.getDatastream(), ORIGINAL_FILE.getId(),
                FILE_SIZE, FILE_MIMETYPE, FILE_NAME, FILE_DIGEST, fileId, null);
        assertThumbnailDatastreams(thumbnailId);
        assertContainsMetadataDatastreams(idb.getDatastream());

        // Sort size is based off primary object's size
        assertEquals(FILE_SIZE, (long) idb.getFilesizeSort());
        // Work has no datastreams of its own
        assertEquals(FILE2_SIZE + MODS_SIZE + PREMIS_SIZE, (long) idb.getFilesizeTotal());
    }

    @Test
    public void folderObjectWithMetadataTest() throws Exception {
        FolderObject folderObj = mock(FolderObject.class);
        addMetadataDatastreams(folderObj);

        dip.setContentObject(folderObj);

        filter.filter(dip);

        assertContainsMetadataDatastreams(idb.getDatastream());
        assertNull(idb.getFilesizeSort());
        assertEquals(FILE2_SIZE + MODS_SIZE + PREMIS_SIZE, (long) idb.getFilesizeTotal());
    }

    @Test
    public void fileObjectWithDerivativeTest() throws Exception {
        when(fileObj.getPid()).thenReturn(pid);
        when(fileObj.getBinaryObjects()).thenReturn(Arrays.asList(binObj));
        dip.setContentObject(fileObj);

        File derivFile = derivDir.resolve("deriv.png").toFile();
        FileUtils.write(derivFile, "content", "UTF-8");
        long derivSize = 7l;

        List<Derivative> derivs = Arrays.asList(new Derivative(THUMBNAIL_SMALL, derivFile));
        when(derivativeService.getDerivatives(pid)).thenReturn(derivs);

        filter.filter(dip);

        assertContainsDatastream(idb.getDatastream(), ORIGINAL_FILE.getId(),
                FILE_SIZE, FILE_MIMETYPE, FILE_NAME, FILE_DIGEST, null, null);
        assertContainsDatastream(idb.getDatastream(), THUMBNAIL_SMALL.getId(),
                derivSize, THUMBNAIL_SMALL.getMimetype(), derivFile.getName(), null, null, null);

        assertEquals(FILE_SIZE, (long) idb.getFilesizeSort());
        assertEquals(FILE_SIZE + derivSize, (long) idb.getFilesizeTotal());
    }

    @Test
    public void fileObjectNoDetailsTest() throws Exception {
        dip.setContentObject(fileObj);

        Model model = ModelFactory.createDefaultModel();
        when(binObj.getResource()).thenReturn(model.getResource(BASE_URI + ORIGINAL_FILE.getId()));

        filter.filter(dip);

        assertTrue(idb.getDatastream().contains(ORIGINAL_FILE.getId() + "|||||||"), "Did not contain datastream");
        assertEquals(0, (long) idb.getFilesizeSort());
        assertEquals(0, (long) idb.getFilesizeTotal());
    }

    private Resource fileResource(String name, long filesize, String mimetype, String filename, String digest) {
        Model model = ModelFactory.createDefaultModel();
        Resource resc = model.getResource(BASE_URI + name);
        resc.addLiteral(Premis.hasSize, filesize);
        resc.addLiteral(Ebucore.hasMimeType, mimetype);
        resc.addLiteral(Ebucore.filename, filename);
        resc.addProperty(Premis.hasMessageDigest, createResource(digest));

        return resc;
    }

    private void assertContainsDatastream(List<String> values, String name, long filesize, String mimetype,
                                          String filename, String digest, String owner, String extent) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1);
        List<Object> components = Arrays.asList(
                name, mimetype, filename, extension, filesize, digest, owner, extent);
        String joined = components.stream()
                .map(c -> c == null ? "" : c.toString())
                .collect(Collectors.joining("|"));
        assertTrue(values.contains(joined), "Did not contain datastream " + name);
    }

    private void addMetadataDatastreams(ContentObject obj) throws Exception {
        BinaryObject fitsBin = mock(BinaryObject.class);
        when(fitsBin.getPid()).thenReturn(DatastreamPids.getTechnicalMetadataPid(pid));
        when(fitsBin.getResource()).thenReturn(
                fileResource(TECHNICAL_METADATA.getId(), FILE2_SIZE, FILE2_MIMETYPE, FILE2_NAME, FILE2_DIGEST));
        when(fitsBin.getBinaryStream()).thenReturn(getClass().getResourceAsStream("/datastream/techmd.xml"));

        BinaryObject modsBin = mock(BinaryObject.class);
        when(modsBin.getResource()).thenReturn(
                fileResource(DatastreamType.MD_DESCRIPTIVE.getId(),
                        MODS_SIZE, MODS_MIMETYPE, MODS_NAME, MODS_DIGEST));
        BinaryObject premisBin = mock(BinaryObject.class);
        when(premisBin.getResource()).thenReturn(
                fileResource(DatastreamType.MD_EVENTS.getId(),
                        PREMIS_SIZE, PREMIS_MIMETYPE, PREMIS_NAME, PREMIS_DIGEST));
        List<BinaryObject> mdBins = Arrays.asList(fitsBin, premisBin, modsBin);

        when(obj.listMetadata()).thenReturn(mdBins);
    }

    private void assertContainsMetadataDatastreams(List<String> values) {
        assertContainsDatastream(values, DatastreamType.MD_DESCRIPTIVE.getId(),
                        MODS_SIZE, MODS_MIMETYPE, MODS_NAME, MODS_DIGEST, null, null);
        assertContainsDatastream(values, DatastreamType.MD_EVENTS.getId(),
                PREMIS_SIZE, PREMIS_MIMETYPE, PREMIS_NAME, PREMIS_DIGEST, null, null);
    }

    private void setUpDerivatives(PID filePid) throws IOException {
        File smallFile = derivDir.resolve("small.png").toFile();
        FileUtils.write(smallFile, "content", "UTF-8");

        File largeFile = derivDir.resolve("large.png").toFile();
        FileUtils.write(largeFile, "large content", "UTF-8");

        List<Derivative> derivs = Arrays.asList(new Derivative(THUMBNAIL_SMALL, smallFile),
                new Derivative(THUMBNAIL_LARGE, largeFile));
        when(derivativeService.getDerivatives(filePid)).thenReturn(derivs);
    }

    private void assertThumbnailDatastreams(String thumbnailId) {
        assertContainsDatastream(idb.getDatastream(), THUMBNAIL_SMALL.getId(),
                7l, THUMBNAIL_SMALL.getMimetype(), "small.png", null, thumbnailId, null);
        assertContainsDatastream(idb.getDatastream(), THUMBNAIL_LARGE.getId(),
                13l, THUMBNAIL_LARGE.getMimetype(), "large.png", null, thumbnailId, null);
    }
}

package edu.unc.lib.boxc.deposit.validate;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.FITS_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.PREMIS_V3_NS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.util.MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.springframework.util.MimeTypeUtils;

import edu.unc.lib.boxc.common.test.SelfReturningAnswer;
import edu.unc.lib.boxc.deposit.api.DepositConstants;
import edu.unc.lib.boxc.deposit.fcrepo4.AbstractDepositJobTest;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelHelpers;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.operations.api.events.PremisEventBuilder;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;

/**
 *
 * @author bbpennel
 *
 */
public class ExtractTechnicalMetadataJobTest extends AbstractDepositJobTest {
    private static final Logger log = getLogger(ExtractTechnicalMetadataJobTest.class);

    private final static String FITS_BASE_URI = "http://example.com/fits";

    private final static String OCTET_MIMETYPE = "application/octet-stream";

    private final static String IMAGE_FILEPATH = "path/image.jpg";
    private final static String IMAGE_MD5 = "2b8dac0b2c0ca845dc8d517a2792dcf4";
    private final static String IMAGE_MIMETYPE = "image/jpeg";
    private final static String IMAGE_FORMAT = "JPEG File Interchange Format";

    private final static String CONFLICT_FILEPATH = "path/conflict.wav";
    private final static String CONFLICT_MD5 = "1d442d115b472b21437893000b79c97a";
    private final static String CONFLICT_MIMETYPE = "audio/x-wave";
    private final static String CONFLICT_FORMAT = "Waveform Audio";

    private final static String UNKNOWN_FILEPATH = "path/unknown.stuff";
    private final static String UNKNOWN_MD5 = "2748ba561254b629c2103cb2e1be3fc2";
    private final static String UNKNOWN_FORMAT = "Unknown";

    private static final Path TMP_PATH = Paths.get(System.getProperty("java.io.tmpdir"));

    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private CloseableHttpResponse httpResp;
    @Mock
    private StatusLine statusLine;
    @Mock
    private HttpEntity respEntity;

    @Mock
    private PremisLoggerFactory premisLoggerFactory;
    @Mock
    private PremisLogger premisLogger;
    private PremisEventBuilder premisEventBuilder;

    private ExtractTechnicalMetadataJob job;

    private Bag depositBag;
    private Model model;

    private File techmdDir;
    private Path fitsCommand;

    private final static ExecutorService executorService = Executors.newFixedThreadPool(2);

    @BeforeEach
    public void init() throws Exception {
        Path fitsHomePath = tmpFolder.resolve("fits");
        Files.createDirectory(fitsHomePath);
        File fitsHome = fitsHomePath.toFile();
        // Create fits command and make it executable
        fitsCommand = new File(fitsHome, "fits.sh").toPath();

        job = new ExtractTechnicalMetadataJob(jobUUID, depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "pidMinter", pidMinter);
        job.setHttpClient(httpClient);
        job.setFitsHomePath(fitsHome.getAbsolutePath());
        job.setBaseFitsUri(FITS_BASE_URI);

        // Setup logging dependencies
        premisEventBuilder = mock(PremisEventBuilder.class, new SelfReturningAnswer());
        when(premisLoggerFactory.createPremisLogger(any(PID.class), any(File.class)))
                .thenReturn(premisLogger);
        when(premisLogger.buildEvent(any(Resource.class))).thenReturn(premisEventBuilder);
        job.setPremisLoggerFactory(premisLoggerFactory);

        setField(job, "depositModelManager", depositModelManager);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "jobStatusFactory", jobStatusFactory);
        setField(job, "executorService", executorService);
        job.setFlushRate(100);
        job.initJob();

        model = job.getWritableModel();
        depositBag = model.createBag(depositPid.getRepositoryPath());

        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResp);
        when(httpResp.getEntity()).thenReturn(respEntity);
        when(httpResp.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        techmdDir = new File(job.getDepositDirectory(), DepositConstants.TECHMD_DIR);
    }

    @AfterAll
    public static void afterTestClass() {
        executorService.shutdown();
    }

    private void respondWithFile(String path) throws Exception {
        when(respEntity.getContent()).thenAnswer(new Answer<InputStream>() {
            @Override
            public InputStream answer(InvocationOnMock invocation) throws Throwable {
                return ExtractTechnicalMetadataJobTest.class.getResourceAsStream(path);
            }
        });
    }

    @Test
    public void nestedFileTest() throws Exception {
        respondWithFile("/fitsReports/imageReport.xml");

        // Create the work object which nests the file
        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        Bag workBag = model.createBag(workPid.getRepositoryPath());
        workBag.addProperty(RDF.type, Cdr.Work);
        depositBag.add(workBag);
        PID filePid = addFileObject(workBag, IMAGE_FILEPATH, IMAGE_MIMETYPE, IMAGE_MD5);

        job.closeModel();

        job.run();

        verifyRequestParameters(IMAGE_FILEPATH);
        verifyFileResults(filePid, IMAGE_MIMETYPE, IMAGE_FORMAT, IMAGE_MD5, 1);
    }

    @Test
    public void resolveConflictingMimetypeTest() throws Exception {
        respondWithFile("/fitsReports/conflictTypeReport.xml");

        // Providing octet stream mimetype to be overrridden
        PID filePid = addFileObject(depositBag, CONFLICT_FILEPATH, OCTET_MIMETYPE, null);
        job.closeModel();

        job.run();

        verifyRequestParameters(CONFLICT_FILEPATH);
        verifyFileResults(filePid, CONFLICT_MIMETYPE, CONFLICT_FORMAT, CONFLICT_MD5, 1);
    }

    @Test
    public void exifSymlinkConflictMimetypeTest() throws Exception {
        respondWithFile("/fitsReports/exifSymlinkConflict.xml");

        // Providing octet stream mimetype to be overridden
        PID filePid = addFileObject(depositBag, CONFLICT_FILEPATH, OCTET_MIMETYPE, null);
        job.closeModel();

        job.run();

        verifyRequestParameters(CONFLICT_FILEPATH);
        verifyFileResults(filePid, CONFLICT_MIMETYPE, CONFLICT_FORMAT, CONFLICT_MD5, 1);
    }

    @Test
    public void multiRankingSpecificityConflictMimetypeTest() throws Exception {
        respondWithFile("/fitsReports/conflictRankingReport.xml");

        // Providing octet stream mimetype to be overridden
        PID filePid = addFileObject(depositBag, CONFLICT_FILEPATH, OCTET_MIMETYPE, null);
        job.closeModel();

        job.run();

        verifyRequestParameters(CONFLICT_FILEPATH);
        verifyFileResults(filePid, "image/x-nikon-nef", "NEF EXIF", CONFLICT_MD5, 1);
    }

    @Test
    public void exifMimetypeTest() throws Exception {
        respondWithFile("/fitsReports/exifReport.xml");

        // Providing octet stream mimetype to be overrridden
        PID filePid = addFileObject(depositBag, CONFLICT_FILEPATH, OCTET_MIMETYPE, null);
        job.closeModel();

        job.run();

        verifyRequestParameters(CONFLICT_FILEPATH);
        verifyFileResults(filePid, CONFLICT_MIMETYPE, CONFLICT_FORMAT, CONFLICT_MD5, 1);
    }

    @Test
    public void singleResultExifMimetypeTest() throws Exception {
        respondWithFile("/fitsReports/exifSingleResult.xml");

        // Providing octet stream mimetype to be overrridden
        PID filePid = addFileObject(depositBag, CONFLICT_FILEPATH, OCTET_MIMETYPE, null);
        job.closeModel();

        job.run();

        verifyRequestParameters(CONFLICT_FILEPATH);
        verifyFileResults(filePid, CONFLICT_MIMETYPE, CONFLICT_FORMAT, CONFLICT_MD5, 1);
    }

    @Test
    public void overrideProvidedMimetypeTest() throws Exception {
        respondWithFile("/fitsReports/imageReport.xml");

        // Providing octet stream mimetype to be overrridden
        PID filePid = addFileObject(depositBag, IMAGE_FILEPATH, OCTET_MIMETYPE, null);
        job.closeModel();

        job.run();

        verifyRequestParameters(IMAGE_FILEPATH);
        verifyFileResults(filePid, IMAGE_MIMETYPE, IMAGE_FORMAT, IMAGE_MD5, 1);
    }

    @Test
    public void ignoreInvalidProvidedTest() throws Exception {
        respondWithFile("/fitsReports/unknownReport.xml");

        // Providing octet stream mimetype to be overrridden
        PID filePid = addFileObject(depositBag, UNKNOWN_FILEPATH, "notvalid", null);
        job.closeModel();

        job.run();

        verifyRequestParameters(UNKNOWN_FILEPATH);
        verifyFileResults(filePid, APPLICATION_OCTET_STREAM_VALUE, UNKNOWN_FORMAT, UNKNOWN_MD5, 1);
    }

    @Test
    public void retainMoreMeaningfulProvidedMimetypeTest() throws Exception {
        respondWithFile("/fitsReports/textReport.xml");

        // Providing octet stream mimetype to be overrridden
        PID filePid = addFileObject(depositBag, "path/text.txt", "application/json", null);
        job.closeModel();

        job.run();

        verifyRequestParameters("/path/text.txt");
        verifyFileResults(filePid, "application/json", "Text", IMAGE_MD5, 1);
    }

    @Test
    public void overrideProvidedTextPlainWithMoreMeaningful() throws Exception {
        respondWithFile("/fitsReports/imageReport.xml");

        // Providing octet stream mimetype to be overrridden
        PID filePid = addFileObject(depositBag, IMAGE_FILEPATH, MimeTypeUtils.TEXT_PLAIN_VALUE, null);
        job.closeModel();

        job.run();

        verifyRequestParameters(IMAGE_FILEPATH);
        verifyFileResults(filePid, IMAGE_MIMETYPE, IMAGE_FORMAT, IMAGE_MD5, 1);
    }

    @Test
    public void preferFitsMimetypeOverProvidedTest() throws Exception {
        respondWithFile("/fitsReports/imageReport.xml");

        // Providing octet stream mimetype to be overrridden
        PID filePid = addFileObject(depositBag, IMAGE_FILEPATH, "image/ofsomekind", IMAGE_MD5);
        job.closeModel();

        job.run();

        verifyRequestParameters(IMAGE_FILEPATH);
        verifyFileResults(filePid, IMAGE_MIMETYPE, IMAGE_FORMAT, IMAGE_MD5, 1);
    }

    @Test
    public void addMissingMimetypeTest() throws Exception {
        respondWithFile("/fitsReports/imageReport.xml");

        // Providing no mimetype
        PID filePid = addFileObject(depositBag, IMAGE_FILEPATH, null, null);
        job.closeModel();

        job.run();

        verifyRequestParameters(IMAGE_FILEPATH);
        verifyFileResults(filePid, IMAGE_MIMETYPE, IMAGE_FORMAT, IMAGE_MD5, 1);
    }

    @Test
    public void resumeJobTest() throws Exception {
        respondWithFile("/fitsReports/imageReport.xml");

        techmdDir.mkdir();
        PID skippedPid = addFileObject(depositBag, "skipped/object.jpg", null, null);
        Path skippedPath = job.getTechMdPath(skippedPid, true);
        Files.createFile(skippedPath);

        PID filePid = addFileObject(depositBag, IMAGE_FILEPATH, null, null);

        when(depositStatusFactory.isResumedDeposit(anyString())).thenReturn(true);

        job.closeModel();

        job.run();

        await().atMost(Duration.ofSeconds(2))
            .until(() -> techmdDir.list().length == 2);
        verifyRequestParameters(IMAGE_FILEPATH);
        verifyFileResults(filePid, IMAGE_MIMETYPE, IMAGE_FORMAT, IMAGE_MD5, 2);
    }

    @Test
    public void unknownFormatTest() throws Exception {
        respondWithFile("/fitsReports/unknownReport.xml");

        // Providing octet stream mimetype to be overrridden
        PID filePid = addFileObject(depositBag, UNKNOWN_FILEPATH, null, null);
        job.closeModel();

        job.run();

        verifyRequestParameters(UNKNOWN_FILEPATH);
        verifyFileResults(filePid, OCTET_MIMETYPE, UNKNOWN_FORMAT, UNKNOWN_MD5, 1);
    }

    @Test
    public void depositLotsMissingMimetypeTest() throws Exception {
        respondWithFile("/fitsReports/imageReport.xml");
        for (int i = 0; i < 100; i++) {
            addFileObject(depositBag, IMAGE_FILEPATH, null, null);
        }
        job.closeModel();

        long start = System.nanoTime();
        job.run();

        Model resultModel = job.getReadOnlyModel();

        List<Statement> typesAdded = resultModel.listStatements(null, CdrDeposit.mimetype, IMAGE_MIMETYPE).toList();
        assertEquals(100, typesAdded.size());

        log.info("Finished in {}", ((System.nanoTime() - start)/1000000));
    }

    // Verify that job fails when know task fails in the middle of the list of jobs
    @Test
    public void depositLotsWithFailureTest() throws Exception {
        when(respEntity.getContent()).thenAnswer(new Answer<InputStream>() {
            private int count = 0;
            @Override
            public InputStream answer(InvocationOnMock invocation) throws Throwable {
                count++;
                if (count != 10) {
                    return ExtractTechnicalMetadataJobTest.class.getResourceAsStream("/fitsReports/imageReport.xml");
                }
                throw new RepositoryException("Boom");
            }
        });
        for (int i = 0; i < 20; i++) {
            addFileObject(depositBag, IMAGE_FILEPATH, null, null);
        }
        job.closeModel();

        try {
            job.run();
            fail("Expected job to fail");
        } catch (RepositoryException e) {
            assertEquals("Boom", e.getMessage(), "Expect failure with message");
        }
    }

    @Test
    public void unicodeFilenameTest() throws Exception {
        setupFitsCommand("src/test/resources/fitsReports/imageReport.xml");

        // Create the work object which nests the file
        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        Bag workBag = model.createBag(workPid.getRepositoryPath());
        workBag.addProperty(RDF.type, Cdr.Work);
        depositBag.add(workBag);
        String filename = "weird\uD83D\uDC7D.mov";
        Path sourcePath = tmpFolder.resolve(filename);
        Files.createFile(sourcePath);
        PID filePid = addFileObject(workBag, sourcePath.toFile().toURI().toString(), IMAGE_MIMETYPE, IMAGE_MD5);

        job.closeModel();

        job.run();

        verifyFileResults(filePid, IMAGE_MIMETYPE, IMAGE_FORMAT, IMAGE_MD5, 1);
    }

    @Test
    public void filenameFromLabelTest() throws Exception {
        respondWithFile("/fitsReports/imageReport.xml");

        // Create the work object which nests the file
        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        Bag workBag = model.createBag(workPid.getRepositoryPath());
        workBag.addProperty(RDF.type, Cdr.Work);
        depositBag.add(workBag);
        String filename = "ambiguous_file_name";
        Path sourcePath = tmpFolder.resolve(filename);
        Files.createFile(sourcePath);
        PID filePid = addFileObject(workBag, sourcePath.toFile().toURI().toString(), IMAGE_MIMETYPE, IMAGE_MD5);
        Resource fileResc = model.getResource(filePid.getRepositoryPath());
        fileResc.addProperty(CdrDeposit.label, "boxys_favorite_file.nef");

        job.closeModel();

        job.run();

        verifyRequestParameters("boxys_favorite_file.nef");
        verifyFileResults(filePid, IMAGE_MIMETYPE, IMAGE_FORMAT, IMAGE_MD5, 1);
    }

    @Test
    public void movFileTest() throws Exception {
        setupFitsCommand("src/test/resources/fitsReports/imageReport.xml");

        // Create the work object which nests the file
        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        Bag workBag = model.createBag(workPid.getRepositoryPath());
        workBag.addProperty(RDF.type, Cdr.Work);
        depositBag.add(workBag);
        String filename = "commencement.MOV";
        File sourceFile = tmpFolder.resolve(filename).toFile();
        PID filePid = addFileObject(workBag, sourceFile.getAbsolutePath(), IMAGE_MIMETYPE, IMAGE_MD5);

        job.closeModel();

        job.run();

        verifyFileResults(filePid, IMAGE_MIMETYPE, IMAGE_FORMAT, IMAGE_MD5, 1);
    }

    @Test
    public void movFileParenthesisTest() throws Exception {
        setupFitsCommand("src/test/resources/fitsReports/imageReport.xml");

        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        Bag workBag = model.createBag(workPid.getRepositoryPath());
        workBag.addProperty(RDF.type, Cdr.Work);
        depositBag.add(workBag);
        String parentFolderName = "paren(goes)here";
        File parentFolder = tmpFolder.resolve(parentFolderName).toFile();
        String filename = "commencement.MOV";
        File sourceFile = new File(parentFolder, "commence.mov");
        PID filePid = addFileObject(workBag, sourceFile.getAbsolutePath(), IMAGE_MIMETYPE, IMAGE_MD5);

        job.closeModel();

        job.run();

        verifyFileResults(filePid, IMAGE_MIMETYPE, IMAGE_FORMAT, IMAGE_MD5, 1);
    }

    @Test
    public void sanitizeCliPathNoSpecialCharactersTest() throws Exception {
        var expected = Paths.get("/path/to/file.txt");
        assertEquals(expected, job.sanitizeCliPath(expected));
    }

    @Test
    public void sanitizeCliPathUnicodeTest() throws Exception {
        var stagingUri = new File("/path/\uD83D\uDC7Eto/fi\uD83D\uDC7Dle.txt").getAbsolutePath();
        var stagingPath = Paths.get(stagingUri);
        var expected = Paths.get("/path/__to/fi__le.txt");
        assertEquals(expected, job.sanitizeCliPath(stagingPath));
    }

    @Test
    public void sanitizeCliPathReservedTest() throws Exception {
        var stagingPath = Paths.get("/path/to/fi$4l?*\"'!e.txt<<1");
        var expected = Paths.get("/path/to/fi_4l_e.txt_1");
        assertEquals(expected, job.sanitizeCliPath(stagingPath));
    }

    @Test
    public void makeSymlinkForStagedPathProblemCharactersTest() throws Exception {
        var originalPath = tmpFolder.resolve("fil£.txt");
        var result = job.makeSymlinkForStagedPath(originalPath.toUri().toString(), null);

        assertEquals("fil_.txt", result.getFileName().toString());
        assertTrue(Files.isSymbolicLink(result));
        assertEquals(originalPath, Files.readSymbolicLink(result));
    }

    @Test
    public void makeSymlinkForStagedPathWithLabelTest() throws Exception {
        var originalPath = tmpFolder.resolve("file");
        var result = job.makeSymlinkForStagedPath(originalPath.toUri().toString(), "boxys_favorite_file.nef");

        assertEquals("boxys_favorite_file.nef", result.getFileName().toString());
        assertTrue(Files.isSymbolicLink(result));
        assertEquals(originalPath, Files.readSymbolicLink(result));
    }

    private HttpUriRequest getRequest() throws Exception {
        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient).execute(requestCaptor.capture());
        return requestCaptor.getValue();
    }

    private String getSubmittedFilePath(HttpUriRequest request) {
        String submitted = request.getURI().toString();
        if (!submitted.startsWith(FITS_BASE_URI + "/examine?file=")) {
            return null;
        }
        return submitted.replace(FITS_BASE_URI + "/examine?file=", "");
    }

    private void verifyRequestParameters(String expectedFilepath) throws Exception {
        HttpUriRequest request = getRequest();
        String submittedPath = getSubmittedFilePath(request);

        String failMessage = "FITS service called with wrong path. Expected " + expectedFilepath + " but got " + submittedPath;
        assertTrue(submittedPath.startsWith(job.getDepositDirectory().toString().replace("/", "%2F")), failMessage);
        assertTrue(submittedPath.endsWith("%2F" + Paths.get(expectedFilepath).getFileName()), failMessage);
    }

    private void verifyFileResults(PID filePid, String expectedMimetype, String expectedFormat,
            String expectedChecksum, int numberReports) throws Exception {

        model = job.getReadOnlyModel();
        // Post-run model info for the file object
        Resource fileResc = model.getResource(filePid.getRepositoryPath());

        assertEquals(numberReports, techmdDir.list().length, "Incorrect number of reports in output dir");

        File reportFile = job.getTechMdPath(filePid, false).toFile();
        assertTrue(reportFile.exists(), "Report file not created");

        Document premisDoc = new SAXBuilder().build(new FileInputStream(reportFile));
        Element premisEl = premisDoc.getRootElement();
        Element premisObjEl = premisEl.getChild("object", PREMIS_V3_NS);
        String identifier = premisObjEl.getChild("objectIdentifier", PREMIS_V3_NS)
                .getChildText("objectIdentifierValue", PREMIS_V3_NS);
        assertEquals(identifier, filePid.getRepositoryPath());

        Element premisObjCharsEl = premisObjEl.getChild("objectCharacteristics", PREMIS_V3_NS);

        // Test that the size property is set and a numeric value
        Long.parseLong(premisObjCharsEl.getChildText("size", PREMIS_V3_NS));

        // Verify that the FITS result report was added to the premis
        Element fitsEl = premisObjCharsEl.getChild("objectCharacteristicsExtension", PREMIS_V3_NS)
                .getChild("fits", FITS_NS);
        assertNotNull(fitsEl, "FITS results not added to report");
        assertNotNull(fitsEl.getChild("identification", FITS_NS), "FITS contents missing from report");

        // Check that the format got set
        String formatName = premisObjCharsEl.getChild("format", PREMIS_V3_NS)
                .getChild("formatDesignation", PREMIS_V3_NS)
                .getChildText("formatName", PREMIS_V3_NS);
        assertEquals(expectedFormat, formatName, "Format not set in premis report");

        Resource origResc = DepositModelHelpers.getDatastream(fileResc);
        assertEquals(expectedMimetype, origResc.getProperty(CdrDeposit.mimetype).getString(),
                "Mimetype not set in deposit model");

        verify(jobStatusFactory).setTotalCompletion(eq(jobUUID), eq(1));
        verify(jobStatusFactory, times(1)).incrCompletion(eq(jobUUID), eq(1));
    }

    private PID addFileObject(Bag parent, String stagingLocation, String mimetype, String md5sum) throws IOException {
        try {
            Path stagingPath = job.getDepositDirectory().toPath().resolve(stagingLocation);
            Files.createDirectories(stagingPath.getParent());
            Files.createFile(stagingPath);
        } catch (FileAlreadyExistsException e) {
            // Ignoring
        }
        PID filePid = makePid(RepositoryPathConstants.CONTENT_BASE);

        Resource fileResc = parent.getModel().createResource(filePid.getRepositoryPath());
        fileResc.addProperty(RDF.type, Cdr.FileObject);
        Resource origResc = DepositModelHelpers.addDatastream(fileResc);
        origResc.addProperty(CdrDeposit.stagingLocation, stagingLocation);
        if (mimetype != null) {
            origResc.addProperty(CdrDeposit.mimetype, mimetype);
        }
        if (md5sum != null) {
            origResc.addProperty(CdrDeposit.md5sum, md5sum);
        }

        parent.add(fileResc);

        return filePid;
    }

    private void setupFitsCommand(String docPath) throws IOException {
        Files.createFile(fitsCommand);
        FileUtils.write(fitsCommand.toFile(), "#!/usr/bin/env bash\n"
                + "cat " + Paths.get(docPath).toAbsolutePath() + "\n"
                + "exit 0", StandardCharsets.US_ASCII);
        Files.setPosixFilePermissions(fitsCommand, PosixFilePermissions.fromString("rwxr--r--"));
    }
}

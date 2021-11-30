/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.boxc.deposit.validate;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.FITS_NS;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.PREMIS_V3_NS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
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

    @Before
    public void init() throws Exception {
        File fitsHome = tmpFolder.newFolder("fits");
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

    @AfterClass
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
            assertEquals("Expect failure with message", "Boom", e.getMessage());
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
        String filename = "weird\uD83D\uDC7D.txt";
        File sourceFile = tmpFolder.newFile(filename);
        PID filePid = addFileObject(workBag, sourceFile.getAbsolutePath(), IMAGE_MIMETYPE, IMAGE_MD5);

        job.closeModel();

        job.run();

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
        File sourceFile = tmpFolder.newFile(filename);
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
        File parentFolder = tmpFolder.newFolder(parentFolderName);
        String filename = "commencement.MOV";
        File sourceFile = new File(parentFolder, "commence.mov");
        PID filePid = addFileObject(workBag, sourceFile.getAbsolutePath(), IMAGE_MIMETYPE, IMAGE_MD5);

        job.closeModel();

        job.run();

        verifyFileResults(filePid, IMAGE_MIMETYPE, IMAGE_FORMAT, IMAGE_MD5, 1);
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
        String absFilePath = Paths.get(depositDir.getAbsolutePath(), expectedFilepath).toString();
        HttpUriRequest request = getRequest();
        String submittedPath = getSubmittedFilePath(request);

        assertEquals("FITS service not called with the expected path", absFilePath.replace("/", "%2F"), submittedPath);
    }

    private void verifyFileResults(PID filePid, String expectedMimetype, String expectedFormat,
            String expectedChecksum, int numberReports) throws Exception {

        model = job.getReadOnlyModel();
        // Post-run model info for the file object
        Resource fileResc = model.getResource(filePid.getRepositoryPath());

        assertEquals("Incorrect number of reports in output dir",
                numberReports, techmdDir.list().length);

        File reportFile = job.getTechMdPath(filePid, false).toFile();
        assertTrue("Report file not created", reportFile.exists());

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
        assertNotNull("FITS results not added to report", fitsEl);
        assertNotNull("FITS contents missing from report",
                fitsEl.getChild("identification", FITS_NS));

        // Check that the format got set
        String formatName = premisObjCharsEl.getChild("format", PREMIS_V3_NS)
                .getChild("formatDesignation", PREMIS_V3_NS)
                .getChildText("formatName", PREMIS_V3_NS);
        assertEquals("Format not set in premis report", expectedFormat, formatName);

        Resource origResc = DepositModelHelpers.getDatastream(fileResc);
        assertEquals("Mimetype not set in deposit model", expectedMimetype,
                origResc.getProperty(CdrDeposit.mimetype).getString());

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

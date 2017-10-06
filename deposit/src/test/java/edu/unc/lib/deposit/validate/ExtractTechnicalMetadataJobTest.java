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
package edu.unc.lib.deposit.validate;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.FITS_NS;
import static edu.unc.lib.dl.xml.JDOMNamespaceUtil.PREMIS_V3_NS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import edu.unc.lib.deposit.fcrepo4.AbstractDepositJobTest;
import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.event.PremisEventBuilder;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.event.PremisLoggerFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.test.SelfReturningAnswer;
import edu.unc.lib.dl.util.DepositConstants;

/**
 *
 * @author bbpennel
 *
 */
public class ExtractTechnicalMetadataJobTest extends AbstractDepositJobTest {

    private final static String FITS_BASE_URI = "http://example.com/fits";

    private final static String OCTET_MIMETYPE = "application/octet-stream";

    private final static String IMAGE_FILEPATH = "/path/image.jpg";
    private final static String IMAGE_MD5 = "2b8dac0b2c0ca845dc8d517a2792dcf4";
    private final static String IMAGE_MIMETYPE = "image/jpeg";
    private final static String IMAGE_FORMAT = "JPEG File Interchange Format";

    private final static String CONFLICT_FILEPATH = "/path/conflict.wav";
    private final static String CONFLICT_MD5 = "1d442d115b472b21437893000b79c97a";
    private final static String CONFLICT_MIMETYPE = "audio/x-wave";
    private final static String CONFLICT_FORMAT = "Waveform Audio";

    private final static String UNKNOWN_FILEPATH = "/path/unknown.stuff";
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

    @Before
    public void init() throws Exception {
        job = new ExtractTechnicalMetadataJob(jobUUID, depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "pidMinter", pidMinter);
        job.setHttpClient(httpClient);
        job.setProcessFilesLocally(true);
        job.setBaseFitsUri(FITS_BASE_URI);

        // Setup logging dependencies
        premisEventBuilder = mock(PremisEventBuilder.class, new SelfReturningAnswer());
        when(premisLoggerFactory.createPremisLogger(any(PID.class), any(File.class)))
                .thenReturn(premisLogger);
        when(premisLogger.buildEvent(any(Resource.class))).thenReturn(premisEventBuilder);
        job.setPremisLoggerFactory(premisLoggerFactory);

        setField(job, "dataset", dataset);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        job.initJob();

        model = job.getWritableModel();
        depositBag = model.createBag(depositPid.getRepositoryPath());

        when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResp);
        when(httpResp.getEntity()).thenReturn(respEntity);
        when(httpResp.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        techmdDir = new File(job.getDepositDirectory(), DepositConstants.TECHMD_DIR);
    }

    private void respondWithFile(String path) throws Exception {
        when(respEntity.getContent()).thenReturn(
                ExtractTechnicalMetadataJobTest.class.getResourceAsStream(path));
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

        verifyFileResults(filePid, IMAGE_MIMETYPE, IMAGE_FORMAT, IMAGE_MD5, IMAGE_FILEPATH, 1);
        verify(premisLogger).buildEvent(eq(Premis.MessageDigestCalculation));
    }

    @Test
    public void resolveConflictingMimetypeTest() throws Exception {
        respondWithFile("/fitsReports/conflictTypeReport.xml");

        // Providing octet stream mimetype to be overrridden
        PID filePid = addFileObject(depositBag, CONFLICT_FILEPATH, OCTET_MIMETYPE, null);
        job.closeModel();

        job.run();

        verifyFileResults(filePid, CONFLICT_MIMETYPE, CONFLICT_FORMAT, CONFLICT_MD5, CONFLICT_FILEPATH, 1);
    }

    @Test
    public void exifSymlinkConflictMimetypeTest() throws Exception {
        respondWithFile("/fitsReports/exifSymlinkConflict.xml");

        // Providing octet stream mimetype to be overrridden
        PID filePid = addFileObject(depositBag, CONFLICT_FILEPATH, OCTET_MIMETYPE, null);
        job.closeModel();

        job.run();

        verifyFileResults(filePid, CONFLICT_MIMETYPE, CONFLICT_FORMAT, CONFLICT_MD5, CONFLICT_FILEPATH, 1);
    }

    @Test
    public void exifMimetypeTest() throws Exception {
        respondWithFile("/fitsReports/exifReport.xml");

        // Providing octet stream mimetype to be overrridden
        PID filePid = addFileObject(depositBag, CONFLICT_FILEPATH, OCTET_MIMETYPE, null);
        job.closeModel();

        job.run();

        verifyFileResults(filePid, CONFLICT_MIMETYPE, CONFLICT_FORMAT, CONFLICT_MD5, CONFLICT_FILEPATH, 1);
    }

    @Test
    public void singleResultExifMimetypeTest() throws Exception {
        respondWithFile("/fitsReports/exifSingleResult.xml");

        // Providing octet stream mimetype to be overrridden
        PID filePid = addFileObject(depositBag, CONFLICT_FILEPATH, OCTET_MIMETYPE, null);
        job.closeModel();

        job.run();

        verifyFileResults(filePid, CONFLICT_MIMETYPE, CONFLICT_FORMAT, CONFLICT_MD5, CONFLICT_FILEPATH, 1);
    }

    @Test
    public void overrideProvidedMimetypeTest() throws Exception {
        respondWithFile("/fitsReports/imageReport.xml");

        // Providing octet stream mimetype to be overrridden
        PID filePid = addFileObject(depositBag, IMAGE_FILEPATH, OCTET_MIMETYPE, null);
        job.closeModel();

        job.run();

        verifyFileResults(filePid, IMAGE_MIMETYPE, IMAGE_FORMAT, IMAGE_MD5, IMAGE_FILEPATH, 1);
    }

    @Test
    public void addMissingMimetypeTest() throws Exception {
        respondWithFile("/fitsReports/imageReport.xml");

        // Providing no mimetype
        PID filePid = addFileObject(depositBag, IMAGE_FILEPATH, null, null);
        job.closeModel();

        job.run();

        verifyFileResults(filePid, IMAGE_MIMETYPE, IMAGE_FORMAT, IMAGE_MD5, IMAGE_FILEPATH, 1);
    }

    @Test(expected = JobFailedException.class)
    public void md5MismatchTest() throws Exception {
        respondWithFile("/fitsReports/imageReport.xml");

        // Providing incorrect checksum value
        addFileObject(depositBag, IMAGE_FILEPATH, null, "111111111111");
        job.closeModel();

        job.run();
    }

    @Test
    public void resumeJobTest() throws Exception {
        respondWithFile("/fitsReports/imageReport.xml");

        techmdDir.mkdir();
        PID skippedPid = addFileObject(depositBag, "/skipped/object.jpg", null, null);
        File skippedFile = new File(techmdDir, skippedPid.getUUID() + ".xml");
        skippedFile.createNewFile();

        PID filePid = addFileObject(depositBag, IMAGE_FILEPATH, null, null);

        when(depositStatusFactory.isResumedDeposit(anyString())).thenReturn(true);

        job.closeModel();

        job.run();

        verifyFileResults(filePid, IMAGE_MIMETYPE, IMAGE_FORMAT, IMAGE_MD5, IMAGE_FILEPATH, 2);
    }

    @Test
    public void unknownFormatTest() throws Exception {
        respondWithFile("/fitsReports/unknownReport.xml");

        // Providing octet stream mimetype to be overrridden
        PID filePid = addFileObject(depositBag, UNKNOWN_FILEPATH, null, null);
        job.closeModel();

        job.run();

        verifyFileResults(filePid, OCTET_MIMETYPE, UNKNOWN_FORMAT, UNKNOWN_MD5, UNKNOWN_FILEPATH, 1);
    }

    private void verifyFileResults(PID filePid, String expectedMimetype, String expectedFormat,
            String expectedChecksum, String expectedFilepath, int numberReports) throws Exception {

        String absFilePath = Paths.get(depositDir.getAbsolutePath(), expectedFilepath).toString();

        model = job.getReadOnlyModel();
        // Post-run model info for the file object
        Resource fileResc = model.getResource(filePid.getRepositoryPath());

        ArgumentCaptor<HttpUriRequest> requestCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        verify(httpClient).execute(requestCaptor.capture());
        HttpUriRequest request = requestCaptor.getValue();

        assertEquals("FITS service not called with the expected path",
                FITS_BASE_URI + "/examine?file=" + absFilePath.replace("/", "%2F"),
                request.getURI().toString());

        assertEquals("Incorrect number of reports in output dir",
                numberReports, techmdDir.list().length);

        File reportFile = new File(techmdDir, filePid.getUUID() + ".xml");
        assertTrue("Report file not created", reportFile.exists());

        Document premisDoc = new SAXBuilder().build(new FileInputStream(reportFile));
        Element premisEl = premisDoc.getRootElement();
        Element premisObjEl = premisEl.getChild("object", PREMIS_V3_NS);
        String identifier = premisObjEl.getChild("objectIdentifier", PREMIS_V3_NS)
                .getChildText("objectIdentifierValue", PREMIS_V3_NS);
        assertEquals(identifier, filePid.getRepositoryPath());

        Element premisObjCharsEl = premisObjEl.getChild("objectCharacteristics", PREMIS_V3_NS);

        String checksum = premisObjCharsEl.getChild("fixity", PREMIS_V3_NS)
                .getChildText("messageDigest", PREMIS_V3_NS);
        assertEquals("Checksum not recorded in premis report", expectedChecksum, checksum);

        assertEquals("Checksum not set in deposit model", expectedChecksum,
                fileResc.getProperty(CdrDeposit.md5sum).getString());

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

        assertEquals("Mimetype not set in deposit model", expectedMimetype,
                fileResc.getProperty(CdrDeposit.mimetype).getString());
    }

    private PID addFileObject(Bag parent, String stagingLocation, String mimetype, String md5sum) {
        PID filePid = makePid(RepositoryPathConstants.CONTENT_BASE);

        Resource fileResc = parent.getModel().createResource(filePid.getRepositoryPath());
        fileResc.addProperty(RDF.type, Cdr.FileObject);
        fileResc.addProperty(CdrDeposit.stagingLocation, stagingLocation);
        if (mimetype != null) {
            fileResc.addProperty(CdrDeposit.mimetype, mimetype);
        }
        if (md5sum != null) {
            fileResc.addProperty(CdrDeposit.md5sum, md5sum);
        }

        parent.add(fileResc);

        return filePid;
    }
}

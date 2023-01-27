package edu.unc.lib.boxc.deposit.validate;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.util.concurrent.MoreExecutors;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.fcrepo4.AbstractDepositJobTest;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelHelpers;
import edu.unc.lib.boxc.deposit.validate.VirusScanJob;
import edu.unc.lib.boxc.deposit.work.JobFailedException;
import edu.unc.lib.boxc.deposit.work.JobInterruptedException;
import edu.unc.lib.boxc.model.api.exceptions.RepositoryException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import fi.solita.clamav.ClamAVClient;
import fi.solita.clamav.ScanResult;
import fi.solita.clamav.ScanResult.Status;

/**
 *
 * @author bbpennel
 *
 */
public class VirusScanJobTest extends AbstractDepositJobTest {

    private PID depositPid;

    private VirusScanJob job;

    @Mock
    private ClamAVClient clamClient;

    @Mock
    private ScanResult scanResult;

    private final static ExecutorService executorService = MoreExecutors.newDirectExecutorService();

    @BeforeEach
    public void init() throws Exception {

        initializeJob();

        when(depositStatusFactory.getState(anyString()))
                .thenReturn(DepositState.running);

        depositPid = job.getDepositPID();

        when(pidMinter.mintPremisEventPid(any(PID.class))).thenAnswer(new Answer<PID>() {
            @Override
            public PID answer(InvocationOnMock invocation) throws Throwable {
                PID pid = mock(PID.class);
                String path = URIUtil.join(FEDORA_BASE, "event", UUID.randomUUID().toString());
                when(pid.getRepositoryPath()).thenReturn(path);
                return pid;
            }
        });

        when(clamClient.scanWithResult(any(Path.class))).thenReturn(scanResult);

        File examplesFile = new File("src/test/resources/examples");
        FileUtils.copyDirectory(examplesFile, depositDir);

        when(premisEventBuilder.addOutcome(anyBoolean())).thenReturn(premisEventBuilder);
    }

    private void initializeJob() {
        job = new VirusScanJob();
        job.setJobUUID(jobUUID);
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "pidMinter", pidMinter);
        job.setClamClient(clamClient);
        job.setPremisLoggerFactory(premisLoggerFactory);
        setField(job, "depositModelManager", depositModelManager);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "jobStatusFactory", jobStatusFactory);
        depositJobId = depositUUID + ":" + this.getClass().getName();
        setField(job, "depositJobId", depositJobId);
        job.setExecutorService(executorService);
        job.init();
    }

    @AfterAll
    public static void afterTestClass() {
        executorService.shutdown();
    }

    @Test
    public void passScanTest() throws Exception {
        when(scanResult.getStatus()).thenReturn(Status.PASSED);

        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());
        File manifestFile = new File(depositDir, "manifest.txt");
        manifestFile.createNewFile();
        Resource manifestResc = DepositModelHelpers.addManifest(depBag, "manifest.txt");
        manifestResc.addLiteral(CdrDeposit.stagingLocation, manifestFile.toPath().toUri().toString());

        File pdfFile = new File(depositDir, "pdf.pdf");
        File textFile = new File(depositDir, "text.txt");
        PID file1Pid = addFileObject(depBag, pdfFile);
        PID file2Pid = addFileObject(depBag, textFile);

        job.closeModel();

        job.run();

        verify(clamClient, times(3)).scanWithResult(any(Path.class));

        verify(jobStatusFactory).setTotalCompletion(eq(jobUUID), eq(3));
        verify(jobStatusFactory, times(3)).incrCompletion(eq(jobUUID), eq(1));

        verify(premisLogger, times(4)).buildEvent(eq(Premis.VirusCheck));
        verify(premisLoggerFactory).createPremisLogger(eq(file1Pid), any(File.class));
        verify(premisLoggerFactory).createPremisLogger(eq(file2Pid), any(File.class));
        verify(premisLoggerFactory, times(2)).createPremisLogger(eq(depositPid), any(File.class));
        verify(premisEventBuilder, times(3)).addOutcome(true);
    }

    @Test
    public void alreadyRunScanTest() throws Exception {
        when(scanResult.getStatus()).thenReturn(Status.PASSED);

        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        File pdfFile = new File(depositDir, "pdf.pdf");
        File textFile = new File(depositDir, "text.txt");
        PID file1Pid = addFileObject(depBag, pdfFile);
        PID orig1Pid = DatastreamPids.getOriginalFilePid(file1Pid);
        PID file2Pid = addFileObject(depBag, textFile);

        job.closeModel();

        when(jobStatusFactory.objectIsCompleted(depositJobId, orig1Pid.getQualifiedId())).thenReturn(true);
        job.run();

        verify(jobStatusFactory).setTotalCompletion(eq(jobUUID), eq(2));
        verify(jobStatusFactory, times(2)).incrCompletion(eq(jobUUID), eq(1));

        verify(premisLogger, times(2)).buildEvent(eq(Premis.VirusCheck));
        verify(premisLoggerFactory).createPremisLogger(eq(file2Pid), any(File.class));
        verify(premisLoggerFactory).createPremisLogger(eq(depositPid), any(File.class));
        verify(premisEventBuilder, times(1)).addOutcome(true);
    }

    @Test
    public void failOneScanTest() throws Exception {
        // Fail the text scan, but not the pdf
        when(scanResult.getStatus()).thenReturn(Status.PASSED);
        ScanResult result2 = mock(ScanResult.class);
        when(result2.getStatus()).thenReturn(Status.FOUND);
        File pdfFile = new File(depositDir, "pdf.pdf");
        File textFile = new File(depositDir, "text.txt");
        when(clamClient.scanWithResult(any(Path.class)))
                .thenReturn(scanResult)
                .thenReturn(result2);

        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        PID file1Pid = addFileObject(depBag, pdfFile);
        addFileObject(depBag, textFile);

        job.closeModel();

        try {
            job.run();
            fail();
        } catch(JobFailedException e) {
            // Both files should have been scanned
            verify(jobStatusFactory).setTotalCompletion(eq(jobUUID), eq(2));

            // Only marks the successful file as completed
            verify(jobStatusFactory, times(1)).incrCompletion(eq(jobUUID), eq(1));

            // Only the file that passed should have a premis log
            verify(premisLogger).buildEvent(eq(Premis.VirusCheck));
            verify(premisLoggerFactory).createPremisLogger(any(PID.class), any(File.class));
            verify(premisLoggerFactory).createPremisLogger(eq(file1Pid), any(File.class));
            verify(premisEventBuilder).addOutcome(true);
        }
    }

    @Test
    public void failAndRescanTest() throws Exception {
        when(scanResult.getSignature()).thenReturn("Badness");
        when(scanResult.getStatus()).thenReturn(Status.FOUND)
                .thenReturn(Status.PASSED);
        File textFile = new File(depositDir, "text.txt");
        doReturn(scanResult).when(clamClient).scanWithResult(any(InputStream.class));

        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        PID filePid = addFileObject(depBag, textFile);

        job.closeModel();

        try {
            job.run();
            fail();
        } catch(JobFailedException e) {
            verify(jobStatusFactory).setTotalCompletion(eq(jobUUID), eq(1));
            verify(jobStatusFactory, never()).incrCompletion(eq(jobUUID), eq(0));
        }

        job.run();

        verify(jobStatusFactory, times(2)).setTotalCompletion(eq(jobUUID), eq(1));
        verify(jobStatusFactory).incrCompletion(eq(jobUUID), eq(1));

        verify(premisLogger, times(2)).buildEvent(eq(Premis.VirusCheck));
        verify(premisLoggerFactory).createPremisLogger(eq(filePid), any(File.class));
        verify(premisEventBuilder).addOutcome(true);
    }

    @Test
    public void foundUnidentifiedRecoveryTest() throws Exception {
        when(scanResult.getStatus()).thenReturn(Status.FOUND)
                .thenReturn(Status.PASSED);
        File textFile = new File(depositDir, "text.txt");
        doReturn(scanResult).when(clamClient).scanWithResult(any(InputStream.class));

        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        PID filePid = addFileObject(depBag, textFile);

        job.closeModel();

        job.run();

        verify(jobStatusFactory).setTotalCompletion(eq(jobUUID), eq(1));
        verify(jobStatusFactory).incrCompletion(eq(jobUUID), eq(1));

        verify(premisLogger, times(2)).buildEvent(eq(Premis.VirusCheck));
        verify(premisLoggerFactory).createPremisLogger(eq(filePid), any(File.class));
        verify(premisEventBuilder).addOutcome(true);
    }

    @Test
    public void errorScanTest() throws Exception {
        // Fail the text scan, but not the pdf
        when(scanResult.getStatus()).thenReturn(Status.ERROR);
        Exception mockE = mock(Exception.class);
        when(mockE.getLocalizedMessage()).thenReturn("Something isn't work");
        when(scanResult.getException()).thenReturn(mockE);

        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        File pdfFile = new File(depositDir, "pdf.pdf");
        addFileObject(depBag, pdfFile);

        job.closeModel();

        try {
            job.run();
            fail();
        } catch(RepositoryException e) {
            // No files should have completed scanning
            verify(jobStatusFactory).setTotalCompletion(eq(jobUUID), eq(1));
            verify(jobStatusFactory, never()).incrCompletion(anyString(), anyInt());

            // No premis logs should have been created
            verify(premisLoggerFactory, never()).createPremisLogger(any(PID.class), any(File.class));
        }
    }

    @Test
    public void pauseScanTest() throws Exception {
        when(scanResult.getStatus()).thenReturn(Status.PASSED);

        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        File pdfFile = new File(depositDir, "pdf.pdf");
        File textFile = new File(depositDir, "text.txt");
        PID file1Pid = addFileObject(depBag, pdfFile);
        PID file2Pid = addFileObject(depBag, textFile);

        // Should be running for the first file, then paused
        when(depositStatusFactory.getState(depositUUID))
                .thenReturn(DepositState.running)
                .thenReturn(DepositState.running)
                .thenReturn(DepositState.paused);

        job.closeModel();

        try {
            job.run();
            fail("Job must be interrupted due to pausing");
        } catch (JobInterruptedException e) {
            // expected
        }

        // Resume the job
        when(depositStatusFactory.getState(depositUUID))
                .thenReturn(DepositState.running);

        initializeJob();
        job.run();

        verify(jobStatusFactory, times(2)).setTotalCompletion(eq(jobUUID), eq(2));
        verify(jobStatusFactory, times(3)).incrCompletion(eq(jobUUID), eq(1));

        verify(premisLogger, times(3)).buildEvent(eq(Premis.VirusCheck));
        verify(premisLoggerFactory).createPremisLogger(eq(file1Pid), any(File.class));
        verify(premisLoggerFactory).createPremisLogger(eq(file2Pid), any(File.class));
        verify(premisLoggerFactory).createPremisLogger(eq(depositPid), any(File.class));
        verify(premisEventBuilder, times(2)).addOutcome(true);
    }

    @Test
    public void passScanUnicodeFilenameTest() throws Exception {
        when(clamClient.scanWithResult(any(InputStream.class))).thenReturn(scanResult);
        when(scanResult.getStatus()).thenReturn(Status.PASSED);

        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        File alienFile = new File(depositDir, "alienfile\uD83D\uDC7D.txt");
        alienFile.createNewFile();
        PID file1Pid = addFileObject(depBag, alienFile);

        job.closeModel();

        job.run();

        verify(clamClient).scanWithResult(any(InputStream.class));

        verify(jobStatusFactory).setTotalCompletion(eq(jobUUID), eq(1));
        verify(jobStatusFactory).incrCompletion(eq(jobUUID), eq(1));

        verify(premisLogger, times(2)).buildEvent(eq(Premis.VirusCheck));
        verify(premisLoggerFactory).createPremisLogger(eq(file1Pid), any(File.class));
        verify(premisLoggerFactory).createPremisLogger(eq(depositPid), any(File.class));
        verify(premisEventBuilder).addOutcome(true);
    }

    @Test
    public void passScanUnicodeParentDirTest() throws Exception {
        when(clamClient.scanWithResult(any(InputStream.class))).thenReturn(scanResult);
        when(scanResult.getStatus()).thenReturn(Status.PASSED);

        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        File alienDir = new File(depositDir, "topsecret\uD83D\uDC7D");
        alienDir.mkdir();
        File alienFile = new File(alienDir, "ufo.txt");
        alienFile.createNewFile();
        PID file1Pid = addFileObject(depBag, alienFile);

        job.closeModel();

        job.run();

        verify(clamClient).scanWithResult(any(InputStream.class));

        verify(jobStatusFactory).setTotalCompletion(eq(jobUUID), eq(1));
        verify(jobStatusFactory).incrCompletion(eq(jobUUID), eq(1));

        verify(premisLogger, times(2)).buildEvent(eq(Premis.VirusCheck));
        verify(premisLoggerFactory).createPremisLogger(eq(file1Pid), any(File.class));
        verify(premisLoggerFactory).createPremisLogger(eq(depositPid), any(File.class));
        verify(premisEventBuilder).addOutcome(true);
    }

    private PID addFileObject(Bag parent, File stagedFile) {
        PID filePid = makePid(RepositoryPathConstants.CONTENT_BASE);

        Resource fileResc = parent.getModel().createResource(filePid.getRepositoryPath());
        fileResc.addProperty(RDF.type, Cdr.FileObject);
        Resource origResc = DepositModelHelpers.addDatastream(fileResc);
        origResc.addLiteral(CdrDeposit.stagingLocation, stagedFile.toPath().toUri().toString());

        parent.add(fileResc);

        return filePid;
    }
}

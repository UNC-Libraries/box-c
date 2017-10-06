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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.philvarner.clamavj.ClamScan;
import com.philvarner.clamavj.ScanResult;
import com.philvarner.clamavj.ScanResult.Status;

import edu.unc.lib.deposit.fcrepo4.AbstractDepositJobTest;
import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositState;
import edu.unc.lib.dl.util.URIUtil;

/**
 *
 * @author bbpennel
 *
 */
public class VirusScanJobTest extends AbstractDepositJobTest {

    private PID depositPid;

    private VirusScanJob job;

    @Mock
    private ClamScan clamScan;

    @Mock
    private ScanResult scanResult;

    @Before
    public void init() throws Exception {

        job = new VirusScanJob();
        job.setJobUUID(jobUUID);
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "pidMinter", pidMinter);
        job.setClamScan(clamScan);
        job.setPremisLoggerFactory(premisLoggerFactory);
        setField(job, "dataset", dataset);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "jobStatusFactory", jobStatusFactory);
        job.init();

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

        when(clamScan.scan(any(File.class))).thenReturn(scanResult);

        File examplesFile = new File("src/test/resources/examples");
        FileUtils.copyDirectory(examplesFile, depositDir);
    }

    @Test
    public void passScanTest() throws Exception {
        when(scanResult.getStatus()).thenReturn(Status.PASSED);

        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        PID file1Pid = addFileObject(depBag, "pdf.pdf");
        PID file2Pid = addFileObject(depBag, "text.txt");

        job.closeModel();

        job.run();

        verify(jobStatusFactory).setTotalCompletion(eq(jobUUID), eq(2));
        verify(jobStatusFactory, times(2)).incrCompletion(eq(jobUUID), eq(1));

        verify(premisLogger, times(3)).buildEvent(eq(Premis.VirusCheck));
        verify(premisLoggerFactory).createPremisLogger(eq(file1Pid), any(File.class));
        verify(premisLoggerFactory).createPremisLogger(eq(file2Pid), any(File.class));
        verify(premisLoggerFactory).createPremisLogger(eq(depositPid), any(File.class));
    }

    @Test
    public void failOneScanTest() throws Exception {
        // Fail the text scan, but not the pdf
        when(scanResult.getStatus()).thenReturn(Status.PASSED);
        ScanResult result2 = mock(ScanResult.class);
        when(result2.getStatus()).thenReturn(Status.FAILED);
        File pdfFile = new File(depositDir, "pdf.pdf");
        File textFile = new File(depositDir, "text.txt");
        doReturn(scanResult).when(clamScan).scan(argThat(new FileArgumentMatcher(pdfFile)));
        doReturn(result2).when(clamScan).scan(argThat(new FileArgumentMatcher(textFile)));

        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        PID file1Pid = addFileObject(depBag, pdfFile.getName());
        addFileObject(depBag, textFile.getName());

        job.closeModel();

        try {
            job.run();
            fail();
        } catch(JobFailedException e) {
            // Both files should have been scanned
            verify(jobStatusFactory).setTotalCompletion(eq(jobUUID), eq(2));
            verify(jobStatusFactory, times(2)).incrCompletion(eq(jobUUID), eq(1));

            // Only the file that passed should have a premis log
            verify(premisLogger).buildEvent(eq(Premis.VirusCheck));
            verify(premisLoggerFactory).createPremisLogger(any(PID.class), any(File.class));
            verify(premisLoggerFactory).createPremisLogger(eq(file1Pid), any(File.class));
        }
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
        addFileObject(depBag, pdfFile.getName());

        job.closeModel();

        try {
            job.run();
            fail();
        } catch(Error e) {
            // No files should have completed scanning
            verify(jobStatusFactory).setTotalCompletion(eq(jobUUID), eq(1));
            verify(jobStatusFactory, never()).incrCompletion(anyString(), anyInt());

            // No premis logs should have been created
            verify(premisLoggerFactory, never()).createPremisLogger(any(PID.class), any(File.class));
        }
    }

    private PID addFileObject(Bag parent, String stagingLocation) {
        PID filePid = makePid(RepositoryPathConstants.CONTENT_BASE);

        Resource fileResc = parent.getModel().createResource(filePid.getRepositoryPath());
        fileResc.addProperty(RDF.type, Cdr.FileObject);
        fileResc.addProperty(CdrDeposit.stagingLocation, stagingLocation);

        parent.add(fileResc);

        return filePid;
    }

    private class FileArgumentMatcher extends ArgumentMatcher<File> {
        private File file;

        public FileArgumentMatcher(File file) {
            this.file = file;
        }

        @Override
        public boolean matches(Object argument) {
            File compareFile = (File) argument;
            return file.getAbsolutePath().equals(compareFile.getAbsolutePath());
        }

    }
}

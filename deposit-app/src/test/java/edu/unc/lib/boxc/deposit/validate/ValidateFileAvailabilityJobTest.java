package edu.unc.lib.boxc.deposit.validate;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.fcrepo4.AbstractDepositJobTest;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelHelpers;
import edu.unc.lib.boxc.deposit.validate.ValidateFileAvailabilityJob;
import edu.unc.lib.boxc.deposit.work.JobFailedException;
import edu.unc.lib.boxc.deposit.work.JobInterruptedException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.persist.api.exceptions.UnknownIngestSourceException;
import edu.unc.lib.boxc.persist.api.sources.IngestSource;
import edu.unc.lib.boxc.persist.api.sources.IngestSourceManager;

/**
 *
 * @author bbpennel
 *
 */
public class ValidateFileAvailabilityJobTest extends AbstractDepositJobTest {
    private PID depositPid;

    private ValidateFileAvailabilityJob job;

    @Mock
    private IngestSourceManager sourceManager;
    @Mock
    private IngestSource ingestSource;

    @BeforeEach
    public void init() throws Exception {

        job = new ValidateFileAvailabilityJob();
        job.setJobUUID(jobUUID);
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "pidMinter", pidMinter);
        job.setIngestSourceManager(sourceManager);
        setField(job, "depositModelManager", depositModelManager);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "jobStatusFactory", jobStatusFactory);
        job.init();

        when(sourceManager.getIngestSourceForUri(any(URI.class))).thenReturn(ingestSource);
        when(ingestSource.exists(any(URI.class))).thenReturn(true);

        depositPid = job.getDepositPID();

        File examplesFile = new File("src/test/resources/examples");
        FileUtils.copyDirectory(examplesFile, depositDir);
    }

    @Test
    public void multipleRelativePresentTest() {
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        Bag workBag = model.createBag(workPid.getRepositoryPath());
        workBag.addProperty(RDF.type, Cdr.Work);

        depBag.add(workBag);
        addFileObject(workBag, "pdf.pdf");
        addFileObject(workBag, "text.txt");

        job.closeModel();

        job.run();

        verify(jobStatusFactory).setTotalCompletion(eq(jobUUID), eq(2));
        verify(jobStatusFactory, times(2)).incrCompletion(eq(jobUUID), eq(1));
    }

    @Test
    public void absolutePresentTest() {
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        Bag workBag = model.createBag(workPid.getRepositoryPath());
        workBag.addProperty(RDF.type, Cdr.Work);

        depBag.add(workBag);
        addFileObject(workBag, Paths.get(depositDir.getAbsolutePath(), "pdf.pdf")
                .toAbsolutePath().toString());
        addFileObject(workBag, Paths.get(depositDir.getAbsolutePath(), "text.txt")
                .toAbsolutePath().toString());

        job.closeModel();

        job.run();

        verify(jobStatusFactory).setTotalCompletion(eq(jobUUID), eq(2));
        verify(jobStatusFactory, times(2)).incrCompletion(eq(jobUUID), eq(1));
    }

    @Test
    public void invalidSourceTest() {
        Assertions.assertThrows(JobFailedException.class, () -> {
            Model model = job.getWritableModel();
            Bag depBag = model.createBag(depositPid.getRepositoryPath());

            when(sourceManager.getIngestSourceForUri(any(URI.class)))
                    .thenThrow(new UnknownIngestSourceException("Not source"));

            addFileObject(depBag, "missing.pdf");

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void missingFileTest() {
        Assertions.assertThrows(JobFailedException.class, () -> {
            Model model = job.getWritableModel();
            Bag depBag = model.createBag(depositPid.getRepositoryPath());

            when(ingestSource.exists(any(URI.class))).thenReturn(false);

            addFileObject(depBag, "missing.pdf");

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void interruptedTest() {
        Assertions.assertThrows(JobInterruptedException.class, () -> {
            when(depositStatusFactory.getState(anyString()))
                    .thenReturn(DepositState.paused);

            Model model = job.getWritableModel();
            Bag depBag = model.createBag(depositPid.getRepositoryPath());

            addFileObject(depBag, "pdf.pdf");

            job.closeModel();

            job.run();

            verify(jobStatusFactory, never()).incrCompletion(eq(jobUUID), eq(1));
        });
    }

    @Test
    public void badStagingLocation() {
        Exception exception = Assertions.assertThrows(JobFailedException.class, () -> {
            when(sourceManager.getIngestSourceForUri(any(URI.class)))
                    .thenThrow(new UnknownIngestSourceException("nope"));
            Model model = job.getWritableModel();
            Bag depBag = model.createBag(depositPid.getRepositoryPath());
            addFileObject(depBag, "some/random/location");
            job.closeModel();
            job.run();
        });

        assertTrue(exception.getMessage().contains("Deposit references invalid files"));
    }

    private PID addFileObject(Bag parent, String stagingLocation) {
        PID filePid = makePid(RepositoryPathConstants.CONTENT_BASE);

        Resource fileResc = parent.getModel().createResource(filePid.getRepositoryPath());
        fileResc.addProperty(RDF.type, Cdr.FileObject);
        Resource origResc = DepositModelHelpers.addDatastream(fileResc);
        origResc.addLiteral(CdrDeposit.stagingLocation, stagingLocation);

        parent.add(fileResc);

        return filePid;
    }
}

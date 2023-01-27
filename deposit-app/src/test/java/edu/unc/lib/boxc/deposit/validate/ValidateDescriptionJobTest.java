package edu.unc.lib.boxc.deposit.validate;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.deposit.fcrepo4.AbstractDepositJobTest;
import edu.unc.lib.boxc.deposit.validate.ValidateDescriptionJob;
import edu.unc.lib.boxc.deposit.work.JobFailedException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.operations.api.exceptions.MetadataValidationException;
import edu.unc.lib.boxc.operations.impl.validation.MODSValidator;

/**
 *
 * @author bbpennel
 *
 */
public class ValidateDescriptionJobTest extends AbstractDepositJobTest {
    @Mock
    private MODSValidator modsValidator;

    private ValidateDescriptionJob job;

    @BeforeEach
    public void init() {
        job = new ValidateDescriptionJob();
        job.setJobUUID(jobUUID);
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "pidMinter", pidMinter);
        setField(job, "depositModelManager", depositModelManager);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "jobStatusFactory", jobStatusFactory);
        job.setModsValidator(modsValidator);
        job.init();
    }

    @Test
    public void testNoDescriptions() {
        job.run();
    }

    @Test
    public void testNoFiles() {
        job.getDescriptionDir().mkdir();
        job.run();
    }

    @Test
    public void testValid() throws Exception {
        makeDescriptionFile();

        job.run();
    }

    @Test
    public void testInvalid() throws Exception {
        Assertions.assertThrows(JobFailedException.class, () -> {
            doThrow(new MetadataValidationException()).when(modsValidator).validate(any(File.class));

            PID pid1 = makeDescriptionFile();

            try {
                job.run();
            } catch (JobFailedException e) {
                assertTrue(e.getDetails().contains(pid1.toString()));
                throw e;
            }
        });
    }

    @Test
    public void testMultipleInvalid() throws Exception {
        Assertions.assertThrows(JobFailedException.class, () -> {
            doThrow(new MetadataValidationException()).when(modsValidator).validate(any(File.class));

            PID pid1 = makeDescriptionFile();
            PID pid2 = makeDescriptionFile();

            try {
                job.run();
            } catch (JobFailedException e) {
                assertTrue(e.getDetails().contains(pid1.toString()));
                assertTrue(e.getDetails().contains(pid2.toString()));
                throw e;
            }
        });
    }

    @Test
    public void testOneInvalid() throws Exception {
        Assertions.assertThrows(JobFailedException.class, () -> {
            PID pid1 = makeDescriptionFile();
            PID pid2 = makeDescriptionFile();
            PID pid3 = makeDescriptionFile();

            doNothing().when(modsValidator).validate(any(File.class));
            doThrow(new MetadataValidationException()).when(modsValidator).validate(eq(getDescriptionFile(pid2)));

            try {
                job.run();
            } catch (JobFailedException e) {
                assertFalse(e.getDetails().contains(pid1.toString()));
                assertTrue(e.getDetails().contains(pid2.toString()));
                assertFalse(e.getDetails().contains(pid3.toString()));
                throw e;
            }
        });
    }

    @Test
    public void testIOException() throws Exception {
        Exception exception = Assertions.assertThrows(JobFailedException.class, () -> {
            doThrow(new IOException()).when(modsValidator).validate(any(File.class));

            makeDescriptionFile();

            job.run();
        });

        assertTrue(exception.getMessage().contains("Failed to read description"));
    }

    private PID makeDescriptionFile() throws IOException {
        PID pid = makePid();
        File descriptFile = job.getModsPath(pid, true).toFile();
        Files.createFile(descriptFile.toPath());
        return pid;
    }

    private File getDescriptionFile(PID pid) {
        return job.getModsPath(pid).toFile();
    }
}

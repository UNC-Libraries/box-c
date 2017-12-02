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
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;

import edu.unc.lib.deposit.fcrepo4.AbstractDepositJobTest;
import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.validation.MODSValidator;
import edu.unc.lib.dl.validation.MetadataValidationException;

/**
 *
 * @author bbpennel
 *
 */
public class ValidateDescriptionJobTest extends AbstractDepositJobTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private MODSValidator modsValidator;

    private ValidateDescriptionJob job;

    @Before
    public void init() {
        job = new ValidateDescriptionJob();
        job.setJobUUID(jobUUID);
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "pidMinter", pidMinter);
        setField(job, "dataset", dataset);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "jobStatusFactory", jobStatusFactory);
        job.setModsValidator(modsValidator);
        job.init();

        job.getDescriptionDir().mkdir();
    }

    @Test
    public void testNoDescriptions() {
        job.getDescriptionDir().delete();
        job.run();
    }

    @Test
    public void testNoFiles() {
        job.run();
    }

    @Test
    public void testValid() throws Exception {
        makeDescriptionFile();

        job.run();
    }

    @Test(expected = JobFailedException.class)
    public void testInvalid() throws Exception {
        doThrow(new MetadataValidationException()).when(modsValidator).validate(any(File.class));

        PID pid1 = makeDescriptionFile();

        try {
            job.run();
        } catch (JobFailedException e) {
            assertTrue(e.getDetails().contains(pid1.toString()));
            throw e;
        }
    }

    @Test(expected = JobFailedException.class)
    public void testMultipleInvalid() throws Exception {
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
    }

    @Test(expected = JobFailedException.class)
    public void testOneInvalid() throws Exception {
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
    }

    @Test
    public void testIOException() throws Exception {
        doThrow(new IOException()).when(modsValidator).validate(any(File.class));

        makeDescriptionFile();

        thrown.expect(JobFailedException.class);
        thrown.expectCause(isA(IOException.class));
        thrown.expectMessage("Failed to read description");

        job.run();
    }

    private PID makeDescriptionFile() throws IOException {
        PID pid = makePid();
        File descriptFile = getDescriptionFile(pid);
        Files.createFile(descriptFile.toPath());
        return pid;
    }

    private File getDescriptionFile(PID pid) {
        return new File(job.getDescriptionDir(), pid.getUUID() + ".xml");
    }
}

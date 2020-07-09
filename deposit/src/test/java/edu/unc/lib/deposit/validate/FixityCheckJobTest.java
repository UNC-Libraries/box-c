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
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.jgroups.util.UUID;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.deposit.fcrepo4.AbstractDepositJobTest;
import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.event.PremisLoggerFactory;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryPIDMinter;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.DigestAlgorithm;

/**
 * @author bbpennel
 */
public class FixityCheckJobTest extends AbstractDepositJobTest {
    private static final String CONTENT1 = "Something to digest";
    private static final String CONTENT1_MD5 = "7afbf05666feeebe7fbbf1c9071584e6";
    private static final String CONTENT1_SHA1 = "23d51c61a578a8cb00c5eec6b29c12b7da15c8de";
    private static final String CONTENT2 = "Boxy eats a checksum";
    private static final String CONTENT2_MD5 = "d4568cb41bf223e418cfd4f23b1a385b";
    private static final String CONTENT2_SHA1 = "f14b0716d448386a4f0671112097c55fc0d91313";

    private RepositoryPIDMinter pidMinter;

    private FixityCheckJob job;

    private File stagingDir;

    @Before
    public void setup() throws Exception {
        pidMinter = new RepositoryPIDMinter();

        premisLoggerFactory = new PremisLoggerFactory();
        premisLoggerFactory.setPidMinter(pidMinter);

        job = new FixityCheckJob(jobUUID, depositUUID);
        job.setDepositStatusFactory(depositStatusFactory);
        setField(job, "dataset", dataset);
        setField(job, "premisLoggerFactory", premisLoggerFactory);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "jobStatusFactory", jobStatusFactory);
        job.init();

        stagingDir = tmpFolder.newFolder("staged");
    }

    @Test
    public void depositWithNoDigests() throws Exception {
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        String stagingPath = stageFile(CONTENT1);
        PID filePid = addFileObject(depBag, stagingPath);
        job.closeModel();

        job.run();

        Model resultModel = job.getReadOnlyModel();
        Resource fileResc = resultModel.getResource(filePid.getRepositoryPath());
        assertTrue(fileResc.hasProperty(CdrDeposit.sha1sum, CONTENT1_SHA1));

        assertChecksumEvent(filePid, DigestAlgorithm.SHA1, CONTENT1_SHA1);
    }

    @Test
    public void depositWithValidSha1() throws Exception {
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        String stagingPath = stageFile(CONTENT1);
        PID filePid = addFileObject(depBag, stagingPath);
        addDigest(model, filePid, DigestAlgorithm.SHA1, CONTENT1_SHA1);
        job.closeModel();

        job.run();

        Model resultModel = job.getReadOnlyModel();
        Resource fileResc = resultModel.getResource(filePid.getRepositoryPath());
        assertTrue(fileResc.hasProperty(CdrDeposit.sha1sum, CONTENT1_SHA1));

        assertChecksumEvent(filePid, DigestAlgorithm.SHA1, CONTENT1_SHA1);
    }

    @Test
    public void depositWithInvalidSha1() throws Exception {
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        String stagingPath = stageFile(CONTENT1);
        PID filePid = addFileObject(depBag, stagingPath);
        addDigest(model, filePid, DigestAlgorithm.SHA1, "ohsha");
        job.closeModel();

        try {
            job.run();
            fail("Expected job to fail");
        } catch (JobFailedException e) {
            assertTrue(e.getMessage().contains("Fixity check failed for " + stagingPath));
            assertTrue(e.getDetails().contains("Checksum mismatch, computed SHA1"));
        }
    }

    @Test
    public void depositWithValidMd5() throws Exception {
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        String stagingPath = stageFile(CONTENT1);
        PID filePid = addFileObject(depBag, stagingPath);
        addDigest(model, filePid, DigestAlgorithm.MD5, CONTENT1_MD5);
        job.closeModel();

        job.run();

        // Both the provided md5 and the computed sha1 must be present
        Model resultModel = job.getReadOnlyModel();
        Resource fileResc = resultModel.getResource(filePid.getRepositoryPath());
        assertTrue(fileResc.hasProperty(CdrDeposit.sha1sum, CONTENT1_SHA1));
        assertTrue(fileResc.hasProperty(CdrDeposit.md5sum, CONTENT1_MD5));

        assertChecksumEvent(filePid, DigestAlgorithm.SHA1, CONTENT1_SHA1);
        assertChecksumEvent(filePid, DigestAlgorithm.MD5, CONTENT1_MD5);
    }

    @Test
    public void depositWithInvalidMd5() throws Exception {
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        String stagingPath = stageFile(CONTENT1);
        PID filePid = addFileObject(depBag, stagingPath);
        addDigest(model, filePid, DigestAlgorithm.MD5, "mdwhat");
        job.closeModel();

        try {
            job.run();
            fail("Expected job to fail");
        } catch (JobFailedException e) {
            assertTrue(e.getMessage().contains("Fixity check failed for " + stagingPath));
            assertTrue(e.getDetails().contains("Checksum mismatch, computed MD5"));
        }
    }

    @Test
    public void depositWithNoFiles() throws Exception {
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        Bag folderBag = addFolderObject(depBag);
        PID folderPid = PIDs.get(folderBag.getURI());
        job.closeModel();

        job.run();

        Model resultModel = job.getReadOnlyModel();
        Resource folderResc = resultModel.getResource(folderPid.getRepositoryPath());
        assertFalse(folderResc.hasProperty(CdrDeposit.sha1sum));
    }

    @Test
    public void depositMultipleFilesWithValidDigests() throws Exception {
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        String stagingPath1 = stageFile(CONTENT1);
        PID filePid1 = addFileObject(depBag, stagingPath1);
        addDigest(model, filePid1, DigestAlgorithm.MD5, CONTENT1_MD5);
        addDigest(model, filePid1, DigestAlgorithm.SHA1, CONTENT1_SHA1);
        String stagingPath2 = stageFile(CONTENT2);
        PID filePid2 = addFileObject(depBag, stagingPath2);
        addDigest(model, filePid2, DigestAlgorithm.MD5, CONTENT2_MD5);
        job.closeModel();

        job.run();

        // Both the provided md5 and the computed sha1 must be present
        Model resultModel = job.getReadOnlyModel();
        Resource fileResc1 = resultModel.getResource(filePid1.getRepositoryPath());
        assertTrue(fileResc1.hasProperty(CdrDeposit.sha1sum, CONTENT1_SHA1));
        assertTrue(fileResc1.hasProperty(CdrDeposit.md5sum, CONTENT1_MD5));

        assertChecksumEvent(filePid1, DigestAlgorithm.SHA1, CONTENT1_SHA1);
        assertChecksumEvent(filePid1, DigestAlgorithm.MD5, CONTENT1_MD5);

        Resource fileResc2 = resultModel.getResource(filePid2.getRepositoryPath());
        assertTrue(fileResc2.hasProperty(CdrDeposit.sha1sum, CONTENT2_SHA1));
        assertTrue(fileResc2.hasProperty(CdrDeposit.md5sum, CONTENT2_MD5));

        assertChecksumEvent(filePid2, DigestAlgorithm.SHA1, CONTENT2_SHA1);
        assertChecksumEvent(filePid2, DigestAlgorithm.MD5, CONTENT2_MD5);
    }

    @Test
    public void depositResumeAfterFailure() throws Exception {
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        String stagingPath1 = stageFile(CONTENT1);
        PID filePid1 = addFileObject(depBag, stagingPath1);
        String stagingPath2 = stageFile(CONTENT2);
        PID filePid2 = addFileObject(depBag, stagingPath2);
        addDigest(model, filePid2, DigestAlgorithm.MD5, CONTENT2_MD5);
        job.closeModel();

        Path flappingPath = Paths.get(URI.create(stagingPath2));
        // Delete one of the staged files
        Files.delete(flappingPath);

        try {
            job.run();
            fail("Job expected to fail");
        } catch (JobFailedException e) {
            // expected
        }

        // Write the file back into place
        FileUtils.write(flappingPath.toFile(), CONTENT2, UTF_8);

        job.run();

        Model resultModel = job.getReadOnlyModel();
        Resource fileResc1 = resultModel.getResource(filePid1.getRepositoryPath());
        assertTrue(fileResc1.hasProperty(CdrDeposit.sha1sum, CONTENT1_SHA1));

        assertChecksumEvent(filePid1, DigestAlgorithm.SHA1, CONTENT1_SHA1);

        Resource fileResc2 = resultModel.getResource(filePid2.getRepositoryPath());
        assertTrue(fileResc2.hasProperty(CdrDeposit.sha1sum, CONTENT2_SHA1));
        assertTrue(fileResc2.hasProperty(CdrDeposit.md5sum, CONTENT2_MD5));

        assertChecksumEvent(filePid2, DigestAlgorithm.SHA1, CONTENT2_SHA1);
        assertChecksumEvent(filePid2, DigestAlgorithm.MD5, CONTENT2_MD5);
    }

    private String stageFile(String content) throws IOException {
        String fileId = UUID.randomUUID().toString();
        File stagedFile = new File(stagingDir, fileId);
        FileUtils.write(stagedFile, content, UTF_8);
        return stagedFile.toPath().toUri().toString();
    }

    private PID addFileObject(Bag parent, String stagingLocation) {
        PID filePid = makePid();

        Resource fileResc = parent.getModel().createResource(filePid.getRepositoryPath());
        fileResc.addProperty(RDF.type, Cdr.FileObject);
        fileResc.addProperty(CdrDeposit.stagingLocation, stagingLocation);

        parent.add(fileResc);

        return filePid;
    }

    private Bag addFolderObject(Bag parent) {
        PID folderPid = makePid();

        Bag folderBag = parent.getModel().createBag(folderPid.getRepositoryPath());
        folderBag.addProperty(RDF.type, Cdr.Folder);

        parent.add(folderBag);

        return folderBag;
    }

    private void addDigest(Model model, PID filePid, DigestAlgorithm alg, String digest) {
        Resource fileResc = model.createResource(filePid.getRepositoryPath());
        fileResc.addLiteral(alg.getDepositProperty(), digest);
    }

    private void assertChecksumEvent(PID pid, DigestAlgorithm alg, String digest) {
        Model eventsModel = job.getPremisLogger(pid).getEventsModel();
        List<Resource> events = eventsModel.listResourcesWithProperty(
                RDF.type, Premis.MessageDigestCalculation).toList();
        assertEquals("Unexpected number of premis events", 1, events.size());
        Resource eventResc = events.get(0);
        eventResc.hasProperty(Premis.note, alg.getName().toUpperCase() + " checksum calculated: " + digest);
    }
}

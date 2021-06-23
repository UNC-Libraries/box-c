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
package edu.unc.lib.deposit.normalize;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;

import edu.unc.lib.deposit.work.JobInterruptedException;
import edu.unc.lib.dl.persist.services.deposit.DepositModelHelpers;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

public class DirectoryToBagJobTest extends AbstractNormalizationJobTest {
    private static final Logger log = getLogger(DirectoryToBagJobTest.class);

    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    private DirectoryToBagJob job;

    private Map<String, String> status;

    private File depositDirectory;

    @Before
    public void setup() throws Exception {
        depositDirectory = tmpDir.newFolder("directory-deposit");

        File emptyDir = new File(depositDirectory, "empty_test");
        emptyDir.mkdir();

        File testDirectory = new File(depositDirectory, "test");
        testDirectory.mkdir();

        File testFile = new File(testDirectory, "lorem.txt");
        testFile.createNewFile();

        status = new HashMap<>();

        when(depositStatusFactory.get(anyString())).thenReturn(status);

        job = new DirectoryToBagJob();
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "pidMinter", pidMinter);
        job.setPremisLoggerFactory(premisLoggerFactory);
        setField(job, "depositModelManager", depositModelManager);
        setField(job, "depositsDirectory", depositDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);

        job.init();
    }

    @Test
    public void testConversion() throws Exception {
        status.put(DepositField.sourceUri.name(), depositDirectory.toURI().toString());
        status.put(DepositField.fileName.name(), "Test File");

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getURI());

        assertEquals(1, depositBag.size());

        Bag bagFolder = model.getBag((Resource) depositBag.iterator().next());
        assertEquals("Bag folder label was not set", "Test File", bagFolder.getProperty(CdrDeposit.label).getString());
        assertTrue("Content model was not set", bagFolder.hasProperty(RDF.type, RDF.Bag));
        assertTrue("Content model was not set", bagFolder.hasProperty(RDF.type, Cdr.Folder));

        Resource emptyFolder = getChildByLabel(bagFolder, "empty_test");
        assertTrue("Content model was not set", emptyFolder.hasProperty(RDF.type, Cdr.Folder));

        Bag emptyBag = model.getBag(emptyFolder.getURI());

        assertEquals(0, emptyBag.size());

        Resource folder = getChildByLabel(bagFolder, "test");
        assertTrue("Content model was not set", folder.hasProperty(RDF.type, Cdr.Folder));

        Bag childrenBag = model.getBag(folder.getURI());

        assertEquals(1, childrenBag.size());

        // Verify that file and its properties were added to work
        Resource work = getChildByLabel(childrenBag, "lorem.txt");
        assertTrue("Type was not set", work.hasProperty(RDF.type, Cdr.Work));

        Bag workBag = model.getBag(work.getURI());
        Resource file = getChildByLabel(workBag, "lorem.txt");
        assertTrue("Type was not set", file.hasProperty(RDF.type, Cdr.FileObject));

        Resource originalResc = DepositModelHelpers.getDatastream(file);
        String tagPath = originalResc.getProperty(CdrDeposit.stagingLocation).getString();
        assertTrue(tagPath.endsWith("directory-deposit/test/lorem.txt"));
    }

    @Test
    public void interruptionTest() throws Exception {
        status.put(DepositField.sourceUri.name(), depositDirectory.toURI().toString());
        status.put(DepositField.fileName.name(), "Test File");

        AtomicBoolean gotJobInterrupted = new AtomicBoolean(false);
        AtomicReference<Exception> otherException = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                job.run();
            } catch (JobInterruptedException e) {
                gotJobInterrupted.set(true);
            } catch (Exception e) {
                otherException.set(e);
            }
        });
        thread.start();

        // Wait random amount of time and then interrupt thread if still alive
        Thread.sleep(20 + (long) new Random().nextFloat() * 100);
        if (thread.isAlive()) {
            thread.interrupt();
            thread.join();

            if (gotJobInterrupted.get()) {
                // success
            } else {
                if (otherException.get() != null) {
                    throw otherException.get();
                }
            }
        } else {
            log.warn("Job completed before interruption");
        }
    }

    @Test
    public void nestedDirectoryTest() throws Exception {
        File nestedDepositDir = tmpDir.newFolder("nested");

        File subdir1 = new File(nestedDepositDir, "subdir1");
        subdir1.mkdir();

        File subdir2 = new File(subdir1, "subdir2");
        subdir2.mkdir();

        File testFile1 = new File(subdir2, "lorem.txt");
        testFile1.createNewFile();

        File subdir3 = new File(subdir2, "subdir3");
        subdir3.mkdir();

        File testFile2 = new File(subdir3, "ipsum.txt");
        testFile2.createNewFile();

        status.put(DepositField.sourceUri.name(), nestedDepositDir.toURI().toString());
        status.put(DepositField.fileName.name(), "Test File");

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getURI());

        assertEquals(1, depositBag.size());

        Bag bagRootContainer = model.getBag((Resource) depositBag.iterator().next());
        assertEquals("Bag folder label was not set", "Test File", bagRootContainer.getProperty(CdrDeposit.label).getString());
        assertEquals(1, bagRootContainer.iterator().toList().size());

        Resource rescSubdir1 = getChildByLabel(bagRootContainer, "subdir1");
        assertTrue("Content model was not set", rescSubdir1.hasProperty(RDF.type, Cdr.Folder));

        Bag bagSubdir1 = model.getBag(rescSubdir1);
        assertEquals(1, bagSubdir1.iterator().toList().size());

        Resource rescSubdir2 = getChildByLabel(bagSubdir1, "subdir2");
        assertTrue("Content model was not set", rescSubdir2.hasProperty(RDF.type, Cdr.Folder));

        Bag bagSubdir2 = model.getBag(rescSubdir2);
        assertEquals(2, bagSubdir2.iterator().toList().size());

        Resource rescFile1 = getChildByLabel(bagSubdir2, "lorem.txt");
        assertTrue("Type was not set", rescFile1.hasProperty(RDF.type, Cdr.Work));

        Resource rescSubdir3 = getChildByLabel(bagSubdir2, "subdir3");
        assertTrue("Content model was not set", rescSubdir3.hasProperty(RDF.type, Cdr.Folder));

        Bag bagSubdir3 = model.getBag(rescSubdir3);
        assertEquals(1, bagSubdir3.iterator().toList().size());

        Resource rescFile2 = getChildByLabel(bagSubdir3, "ipsum.txt");
        assertTrue("Type was not set", rescFile2.hasProperty(RDF.type, Cdr.Work));
    }

    @Test
    public void unicodeFilenameTest() throws Exception {
        File unicodeDepDir = tmpDir.newFolder("unicode_test");
        String filename = "weird\uD83D\uDC7D.txt";
        File testFile2 = new File(unicodeDepDir, filename);
        testFile2.createNewFile();

        status.put(DepositField.sourceUri.name(), unicodeDepDir.toURI().toString());
        status.put(DepositField.fileName.name(), "Unicode Test File");

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getURI());

        assertEquals(1, depositBag.size());

        Bag bagFolder = model.getBag((Resource) depositBag.iterator().next());
        assertEquals("Bag folder label was not set", "Unicode Test File",
                bagFolder.getProperty(CdrDeposit.label).getString());
        assertTrue("Content model was not set", bagFolder.hasProperty(RDF.type, RDF.Bag));
        assertTrue("Content model was not set", bagFolder.hasProperty(RDF.type, Cdr.Folder));

        // Verify that file and its properties were added to work
        Resource work = getChildByLabel(bagFolder, testFile2.getName());
        assertTrue("Type was not set", work.hasProperty(RDF.type, Cdr.Work));
        assertEquals("Work label incorrect", filename, work.getProperty(CdrDeposit.label).getString());

        Bag workBag = model.getBag(work.getURI());
        Resource file = getChildByLabel(workBag, testFile2.getName());
        assertTrue("Type was not set", file.hasProperty(RDF.type, Cdr.FileObject));
        assertEquals("File label incorrect", filename, file.getProperty(CdrDeposit.label).getString());

        Resource originalResc = DepositModelHelpers.getDatastream(file);
        String tagPath = originalResc.getProperty(CdrDeposit.stagingLocation).getString();
        assertTrue("Unexpected path " + tagPath, tagPath.endsWith("unicode_test/weird%F0%9F%91%BD.txt"));
    }

    @Test
    public void unicodeNestedDirectoryTest() throws Exception {
        File unicodeDepDir = tmpDir.newFolder("unicode_test");
        String nestedFilename = "\uD83D\uDC7D_sightings";
        Path unicodeNested = Paths.get(unicodeDepDir.getAbsolutePath(), nestedFilename);
        Files.createDirectory(unicodeNested);
        Path testFile = unicodeNested.resolve("ufo.txt");
        Files.createFile(testFile);

        status.put(DepositField.sourceUri.name(), unicodeDepDir.toURI().toString());
        status.put(DepositField.fileName.name(), "Unicode Nested Test File");

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getURI());

        assertEquals(1, depositBag.size());

        Bag bagFolder = model.getBag((Resource) depositBag.iterator().next());
        assertEquals("Bag folder label was not set", "Unicode Nested Test File",
                bagFolder.getProperty(CdrDeposit.label).getString());
        assertTrue("Content model was not set", bagFolder.hasProperty(RDF.type, RDF.Bag));
        assertTrue("Content model was not set", bagFolder.hasProperty(RDF.type, Cdr.Folder));

        Bag nestedFolder = model.getBag((Resource) bagFolder.iterator().next());
        assertEquals("Bag folder label was not set", nestedFilename,
                nestedFolder.getProperty(CdrDeposit.label).getString());

        // Verify that file and its properties were added to work
        Resource work = getChildByLabel(nestedFolder, testFile.getFileName().toString());
        assertTrue("Type was not set", work.hasProperty(RDF.type, Cdr.Work));

        Bag workBag = model.getBag(work.getURI());
        Resource file = getChildByLabel(workBag, testFile.getFileName().toString());
        assertTrue("Type was not set", file.hasProperty(RDF.type, Cdr.FileObject));

        Resource originalResc = DepositModelHelpers.getDatastream(file);
        String tagPath = originalResc.getProperty(CdrDeposit.stagingLocation).getString();
        assertTrue("Unexpected path " + tagPath, tagPath.endsWith("unicode_test/%F0%9F%91%BD_sightings/ufo.txt"));
    }

    private Resource getChildByLabel(Bag bagResc, String seekLabel) {
        NodeIterator iterator = bagResc.iterator();
        while (iterator.hasNext()) {
            Resource childResc = (Resource) iterator.next();
            String label = childResc.getProperty(CdrDeposit.label).getString();
            if (label.equals(seekLabel)) {
                iterator.close();
                return childResc;
            }
        }
        throw new AssertionError("Failed to find child with label " + seekLabel + " in bag " + bagResc);
    }
}
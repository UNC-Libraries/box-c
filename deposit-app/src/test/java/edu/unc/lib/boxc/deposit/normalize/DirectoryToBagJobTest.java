package edu.unc.lib.boxc.deposit.normalize;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.anyString;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelHelpers;
import edu.unc.lib.boxc.deposit.work.JobFailedException;
import edu.unc.lib.boxc.deposit.work.JobInterruptedException;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;

public class DirectoryToBagJobTest extends AbstractNormalizationJobTest {
    private static final Logger log = getLogger(DirectoryToBagJobTest.class);

    private DirectoryToBagJob job;

    private Map<String, String> status;

    private File depositDirectory;

    @BeforeEach
    public void setup() throws Exception {
        Files.createDirectory(tmpFolder.resolve("directory-deposit"));
        depositDirectory = tmpFolder.resolve("directory-deposit").toFile();

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
        status.put(DepositField.createParentFolder.name(), "true");

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getRepositoryPath());

        assertEquals(1, depositBag.size());

        Bag bagFolder = model.getBag((Resource) depositBag.iterator().next());
        assertEquals("Test File", bagFolder.getProperty(CdrDeposit.label).getString(), "Bag folder label was not set");
        assertTrue(bagFolder.hasProperty(RDF.type, RDF.Bag), "Content model was not set");
        assertTrue(bagFolder.hasProperty(RDF.type, Cdr.Folder), "Content model was not set");

        Resource emptyFolder = getChildByLabel(bagFolder, "empty_test");
        assertTrue(emptyFolder.hasProperty(RDF.type, Cdr.Folder), "Content model was not set");

        Bag emptyBag = model.getBag(emptyFolder.getURI());

        assertEquals(0, emptyBag.size());

        Resource folder = getChildByLabel(bagFolder, "test");
        assertTrue(folder.hasProperty(RDF.type, Cdr.Folder), "Content model was not set");

        Bag childrenBag = model.getBag(folder.getURI());

        assertEquals(1, childrenBag.size());

        // Verify that file and its properties were added to work
        Resource work = getChildByLabel(childrenBag, "lorem.txt");
        assertTrue(work.hasProperty(RDF.type, Cdr.Work), "Type was not set");

        Bag workBag = model.getBag(work.getURI());
        Resource file = getChildByLabel(workBag, "lorem.txt");
        assertTrue(file.hasProperty(RDF.type, Cdr.FileObject), "Type was not set");

        Resource originalResc = DepositModelHelpers.getDatastream(file);
        String tagPath = originalResc.getProperty(CdrDeposit.stagingLocation).getString();
        assertTrue(tagPath.endsWith("directory-deposit/test/lorem.txt"));
    }

    @Test
    public void interruptionTest() throws Exception {
        status.put(DepositField.sourceUri.name(), depositDirectory.toURI().toString());
        status.put(DepositField.fileName.name(), "Test File");
        status.put(DepositField.createParentFolder.name(), "true");

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
        File nestedDepositDir = tmpFolder.resolve("nested").toFile();
        Files.createDirectory(tmpFolder.resolve("nested"));

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
        status.put(DepositField.createParentFolder.name(), "true");

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getRepositoryPath());

        assertEquals(1, depositBag.size());

        Bag bagRootContainer = model.getBag((Resource) depositBag.iterator().next());
        assertEquals("Test File", bagRootContainer.getProperty(CdrDeposit.label).getString(),
                "Bag folder label was not set");
        assertEquals(1, bagRootContainer.iterator().toList().size());

        Resource rescSubdir1 = getChildByLabel(bagRootContainer, "subdir1");
        assertTrue(rescSubdir1.hasProperty(RDF.type, Cdr.Folder), "Content model was not set");

        Bag bagSubdir1 = model.getBag(rescSubdir1);
        assertEquals(1, bagSubdir1.iterator().toList().size());

        Resource rescSubdir2 = getChildByLabel(bagSubdir1, "subdir2");
        assertTrue(rescSubdir2.hasProperty(RDF.type, Cdr.Folder), "Content model was not set");

        Bag bagSubdir2 = model.getBag(rescSubdir2);
        assertEquals(2, bagSubdir2.iterator().toList().size());

        Resource rescFile1 = getChildByLabel(bagSubdir2, "lorem.txt");
        assertTrue(rescFile1.hasProperty(RDF.type, Cdr.Work), "Type was not set");

        Resource rescSubdir3 = getChildByLabel(bagSubdir2, "subdir3");
        assertTrue(rescSubdir3.hasProperty(RDF.type, Cdr.Folder), "Content model was not set");

        Bag bagSubdir3 = model.getBag(rescSubdir3);
        assertEquals(1, bagSubdir3.iterator().toList().size());

        Resource rescFile2 = getChildByLabel(bagSubdir3, "ipsum.txt");
        assertTrue(rescFile2.hasProperty(RDF.type, Cdr.Work), "Type was not set");
    }

    @Test
    public void unicodeFilenameTest() throws Exception {
        File unicodeDepDir = tmpFolder.resolve("unicode_test").toFile();
        Files.createDirectory(tmpFolder.resolve("unicode_test"));
        String filename = "weird\uD83D\uDC7D.txt";
        File testFile2 = new File(unicodeDepDir, filename);
        testFile2.createNewFile();

        status.put(DepositField.sourceUri.name(), unicodeDepDir.toURI().toString());
        status.put(DepositField.fileName.name(), "Unicode Test File");
        status.put(DepositField.createParentFolder.name(), "true");

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getRepositoryPath());

        assertEquals(1, depositBag.size());

        Bag bagFolder = model.getBag((Resource) depositBag.iterator().next());
        assertEquals("Unicode Test File", bagFolder.getProperty(CdrDeposit.label).getString(),
                "Bag folder label was not set");
        assertTrue(bagFolder.hasProperty(RDF.type, RDF.Bag), "Content model was not set");
        assertTrue(bagFolder.hasProperty(RDF.type, Cdr.Folder), "Content model was not set");

        // Verify that file and its properties were added to work
        Resource work = getChildByLabel(bagFolder, testFile2.getName());
        assertTrue(work.hasProperty(RDF.type, Cdr.Work), "Type was not set");
        assertEquals(filename, work.getProperty(CdrDeposit.label).getString(), "Work label incorrect");

        Bag workBag = model.getBag(work.getURI());
        Resource file = getChildByLabel(workBag, testFile2.getName());
        assertTrue(file.hasProperty(RDF.type, Cdr.FileObject), "Type was not set");
        assertEquals(filename, file.getProperty(CdrDeposit.label).getString(), "File label incorrect");

        Resource originalResc = DepositModelHelpers.getDatastream(file);
        String tagPath = originalResc.getProperty(CdrDeposit.stagingLocation).getString();
        assertTrue(tagPath.endsWith("unicode_test/weird%F0%9F%91%BD.txt"), "Unexpected path " + tagPath);
    }

    @Test
    public void unicodeNestedDirectoryTest() throws Exception {
        File unicodeDepDir = tmpFolder.resolve("unicode_test").toFile();
        Files.createDirectory(tmpFolder.resolve("unicode_test"));
        String nestedFilename = "\uD83D\uDC7D_sightings";
        Path unicodeNested = Paths.get(unicodeDepDir.getAbsolutePath(), nestedFilename);
        Files.createDirectory(unicodeNested);
        Path testFile = unicodeNested.resolve("ufo.txt");
        Files.createFile(testFile);

        status.put(DepositField.sourceUri.name(), unicodeDepDir.toURI().toString());
        status.put(DepositField.fileName.name(), "Unicode Nested Test File");
        status.put(DepositField.createParentFolder.name(), "true");

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getRepositoryPath());

        assertEquals(1, depositBag.size());

        Bag bagFolder = model.getBag((Resource) depositBag.iterator().next());
        assertEquals("Unicode Nested Test File", bagFolder.getProperty(CdrDeposit.label).getString(),
                "Bag folder label was not set");
        assertTrue(bagFolder.hasProperty(RDF.type, RDF.Bag), "Content model was not set");
        assertTrue(bagFolder.hasProperty(RDF.type, Cdr.Folder), "Content model was not set");

        Bag nestedFolder = model.getBag((Resource) bagFolder.iterator().next());
        assertEquals(nestedFilename, nestedFolder.getProperty(CdrDeposit.label).getString(),
                "Bag folder label was not set");

        // Verify that file and its properties were added to work
        Resource work = getChildByLabel(nestedFolder, testFile.getFileName().toString());
        assertTrue(work.hasProperty(RDF.type, Cdr.Work), "Type was not set");

        Bag workBag = model.getBag(work.getURI());
        Resource file = getChildByLabel(workBag, testFile.getFileName().toString());
        assertTrue(file.hasProperty(RDF.type, Cdr.FileObject), "Type was not set");

        Resource originalResc = DepositModelHelpers.getDatastream(file);
        String tagPath = originalResc.getProperty(CdrDeposit.stagingLocation).getString();
        assertTrue(tagPath.endsWith("unicode_test/%F0%9F%91%BD_sightings/ufo.txt"), "Unexpected path " + tagPath);
    }

    @Test
    public void testSkipParentFolder() throws Exception {
        status.put(DepositField.sourceUri.name(), depositDirectory.toURI().toString());
        status.put(DepositField.fileName.name(), "Test File");
        status.put(DepositField.createParentFolder.name(), "false");

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getRepositoryPath());

        assertEquals(2, depositBag.size());

        Resource emptyFolder = getChildByLabel(depositBag, "empty_test");
        assertTrue(emptyFolder.hasProperty(RDF.type, Cdr.Folder), "Content model was not set");

        Bag emptyBag = model.getBag(emptyFolder.getURI());

        assertEquals(0, emptyBag.size());

        Resource folder = getChildByLabel(depositBag, "test");
        assertTrue(folder.hasProperty(RDF.type, Cdr.Folder), "Content model was not set");

        Bag childrenBag = model.getBag(folder.getURI());

        assertEquals(1, childrenBag.size());

        // Verify that file and its properties were added to work
        Resource work = getChildByLabel(childrenBag, "lorem.txt");
        assertTrue(work.hasProperty(RDF.type, Cdr.Work), "Type was not set");

        Bag workBag = model.getBag(work.getURI());
        Resource file = getChildByLabel(workBag, "lorem.txt");
        assertTrue(file.hasProperty(RDF.type, Cdr.FileObject), "Type was not set");

        Resource originalResc = DepositModelHelpers.getDatastream(file);
        String tagPath = originalResc.getProperty(CdrDeposit.stagingLocation).getString();
        assertTrue(tagPath.endsWith("directory-deposit/test/lorem.txt"));
    }

    @Test
    public void testFilesOnlyModeWithNestedFolders() throws Exception {
        status.put(DepositField.sourceUri.name(), depositDirectory.toURI().toString());
        status.put(DepositField.fileName.name(), "Test File");
        status.put(DepositField.filesOnlyMode.name(), "true");

        try {
            job.run();
            fail();
        } catch (JobFailedException e) {
            assertTrue(e.getMessage().contains("Subfolders are not allowed for this deposit"),
                    "Incorrect exception message: " + e.getMessage());
        }
    }

    @Test
    public void testFilesOnlyModeWithFlatStructure() throws Exception {
        File flatDepositDir = tmpFolder.resolve("flat").toFile();
        Files.createDirectory(tmpFolder.resolve("flat"));

        File file1 = new File(flatDepositDir, "file1.txt");
        file1.createNewFile();

        File file2 = new File(flatDepositDir, "file2.txt");
        file2.createNewFile();

        status.put(DepositField.sourceUri.name(), flatDepositDir.toURI().toString());
        status.put(DepositField.fileName.name(), "Flat File");
        status.put(DepositField.filesOnlyMode.name(), "true");

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getRepositoryPath());

        assertEquals(2, depositBag.size());

        Resource file1Resc = getChildByLabel(depositBag, "file1.txt");
        assertTrue(file1Resc.hasProperty(RDF.type, Cdr.FileObject), "Content model was not set");
        Resource originalResc1 = DepositModelHelpers.getDatastream(file1Resc);
        String tagPath1 = originalResc1.getProperty(CdrDeposit.stagingLocation).getString();
        assertTrue(tagPath1.endsWith("flat/file1.txt"), "Unexpected path " + tagPath1);

        Resource file2Resc = getChildByLabel(depositBag, "file2.txt");
        assertTrue(file2Resc.hasProperty(RDF.type, Cdr.FileObject), "Content model was not set");
        Resource originalResc2 = DepositModelHelpers.getDatastream(file2Resc);
        String tagPath2 = originalResc2.getProperty(CdrDeposit.stagingLocation).getString();
        assertTrue(tagPath2.endsWith("flat/file2.txt"), "Unexpected path " + tagPath2);
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
package edu.unc.lib.boxc.deposit.normalize;

import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelHelpers;
import edu.unc.lib.boxc.deposit.work.JobFailedException;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;

public class BagIt2N3BagJobTest extends AbstractNormalizationJobTest {

    private BagIt2N3BagJob job;

    private Map<String, String> status;

    @Captor
    private ArgumentCaptor<String> filePathCaptor;

    private ExecutorService executorService;

    @BeforeEach
    public void setup() throws Exception {
        status = new HashMap<>();
        when(depositStatusFactory.get(anyString())).thenReturn(status);

        executorService = Executors.newSingleThreadExecutor();
        initJob(depositUUID);
    }

    private void initJob(String uuid) {
        job = new BagIt2N3BagJob();
        job.setDepositUUID(uuid);
        job.setDepositDirectory(depositDir);
        job.setExecutorService(executorService);
        setField(job, "depositModelManager", depositModelManager);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        job.init();
    }

    @AfterEach
    public void tearDownTest() throws Exception {
        executorService.shutdown();
    }

    @Test
    public void testConversion() throws Exception {
        URI sourceUri = Paths.get("src/test/resources/paths/valid-bag").toAbsolutePath().toUri();
        status.put(DepositField.sourceUri.name(), sourceUri.toString());
        status.put(DepositField.fileName.name(), "Test File");
        status.put(DepositField.createParentFolder.name(), "true");

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getRepositoryPath());

        assertEquals(1, depositBag.size());

        Bag bagFolder = model.getBag((Resource) depositBag.iterator().next());
        assertEquals("Test File", bagFolder.getProperty(CdrDeposit.label).getString(), "Bag folder label was not set");
        assertTrue(bagFolder.hasProperty(RDF.type, Cdr.Folder ), "Missing RDF type");

        Resource folder = (Resource) bagFolder.iterator().next();

        assertEquals("test", folder.getProperty(CdrDeposit.label).getString(), "Folder label was not set");
        assertTrue(folder.hasProperty(RDF.type, Cdr.Folder), "Missing RDF type");

        Bag childrenBag = model.getBag(folder.getURI());

        assertEquals(2, childrenBag.size());

        // Put children into a map since we can't guarantee order from jena
        Map<String, Resource> children = new HashMap<>(2);
        NodeIterator childIt = childrenBag.iterator();
        while (childIt.hasNext()) {
            Resource file = (Resource) childIt.next();
            children.put(file.getProperty(CdrDeposit.label).getString(), file);
        }

        // Verify that all manifests were added.
        List<String> manifestPaths = depositBag.listProperties(CdrDeposit.hasDatastreamManifest)
                .toList().stream()
                .map(Statement::getResource)
                .map(r -> r.getProperty(CdrDeposit.stagingLocation).getString())
                .collect(Collectors.toList());
        assertEquals(2, manifestPaths.size(), "Unexpected number of manifests");
        List<String> expectedFilePaths = Arrays.asList(sourceUri.toString() + "bagit.txt",
                sourceUri.toString() + "manifest-md5.txt");
        assertTrue(manifestPaths.containsAll(expectedFilePaths),
                "Must contain all of the expected manifest files, but contained " + manifestPaths);

        // Verify that files and their properties were added
        assertFileAdded(children.get("lorem.txt"), "fa5c89f3c88b81bfd5e821b0316569af",
                sourceUri.toString() + "data/test/lorem.txt");

        assertFileAdded(children.get("ipsum.txt"), "e78f5438b48b39bcbdea61b73679449d",
                sourceUri.toString() + "data/test/ipsum.txt");

        // Verify that the description file for the bag exists
        File modsFile = job.getModsPath(PIDs.get(bagFolder.getURI())).toFile();
        assertTrue(modsFile.exists());

        Set<String> cleanupSet = new HashSet<>();
        StmtIterator it = depositBag.listProperties(CdrDeposit.cleanupLocation);
        while (it.hasNext()) {
            Statement stmt = it.nextStatement();
            cleanupSet.add(stmt.getString());
        }

        assertEquals(1, cleanupSet.size(), "Incorrect number of objects identified for cleanup");
        assertTrue(cleanupSet.contains(sourceUri.toString()), "Cleanup of bag not set");
    }

    @Test
    public void testConversionWithoutCreateParentFolder() throws Exception {
        URI sourceUri = Paths.get("src/test/resources/paths/valid-bag").toAbsolutePath().toUri();
        status.put(DepositField.sourceUri.name(), sourceUri.toString());
        status.put(DepositField.fileName.name(), "Test File");
        status.put(DepositField.createParentFolder.name(), "false");

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getRepositoryPath());

        assertEquals(1, depositBag.size());

        Resource folder = (Resource) depositBag.iterator().next();

        assertEquals("test", folder.getProperty(CdrDeposit.label).getString(), "Folder label was not set");
        assertTrue(folder.hasProperty(RDF.type, Cdr.Folder), "Missing RDF type");

        Bag childrenBag = model.getBag(folder.getURI());

        assertEquals(2, childrenBag.size());

        // Put children into a map since we can't guarantee order from jena
        Map<String, Resource> children = new HashMap<>(2);
        NodeIterator childIt = childrenBag.iterator();
        while (childIt.hasNext()) {
            Resource file = (Resource) childIt.next();
            children.put(file.getProperty(CdrDeposit.label).getString(), file);
        }

        // Verify that all manifests were added.
        List<String> manifestPaths = depositBag.listProperties(CdrDeposit.hasDatastreamManifest)
                .toList().stream()
                .map(Statement::getResource)
                .map(r -> r.getProperty(CdrDeposit.stagingLocation).getString())
                .collect(Collectors.toList());
        assertEquals(2, manifestPaths.size(), "Unexpected number of manifests");
        List<String> expectedFilePaths = Arrays.asList(sourceUri.toString() + "bagit.txt",
                sourceUri.toString() + "manifest-md5.txt");
        assertTrue(manifestPaths.containsAll(expectedFilePaths),
                "Must contain all of the expected manifest files, but contained " + manifestPaths);

        // Verify that files and their properties were added
        assertFileAdded(children.get("lorem.txt"), "fa5c89f3c88b81bfd5e821b0316569af",
                sourceUri.toString() + "data/test/lorem.txt");

        assertFileAdded(children.get("ipsum.txt"), "e78f5438b48b39bcbdea61b73679449d",
                sourceUri.toString() + "data/test/ipsum.txt");

        Set<String> cleanupSet = new HashSet<>();
        StmtIterator it = depositBag.listProperties(CdrDeposit.cleanupLocation);
        while (it.hasNext()) {
            Statement stmt = it.nextStatement();
            cleanupSet.add(stmt.getString());
        }

        assertEquals(1, cleanupSet.size(), "Incorrect number of objects identified for cleanup");
        assertTrue(cleanupSet.contains(sourceUri.toString()), "Cleanup of bag not set");
    }

    // BXC-3217
    @Test
    public void testTwoJobs() throws Exception {
        URI sourceUri = Paths.get("src/test/resources/paths/valid-bag").toAbsolutePath().toUri();
        status.put(DepositField.sourceUri.name(), sourceUri.toString());
        status.put(DepositField.fileName.name(), "Test File");
        status.put(DepositField.createParentFolder.name(), "true");

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getRepositoryPath());

        Bag bagFolder = model.getBag((Resource) depositBag.iterator().next());
        Resource folder = (Resource) bagFolder.iterator().next();
        Bag childrenBag = model.getBag(folder.getURI());
        assertEquals(2, childrenBag.size());

        // Verify that all manifests were added.
        List<String> manifestPaths = depositBag.listProperties(CdrDeposit.hasDatastreamManifest)
                .toList().stream()
                .map(Statement::getResource)
                .map(r -> r.getProperty(CdrDeposit.stagingLocation).getString())
                .collect(Collectors.toList());
        assertEquals(2, manifestPaths.size(), "Unexpected number of manifests");

        job.closeModel();

        // Initialize and run a second job to make sure two of them can run in a row
        initJob(UUID.randomUUID().toString());
        job.run();

        Model model2 = job.getReadOnlyModel();
        Bag depositBag2 = model2.getBag(job.getDepositPID().getRepositoryPath());

        Bag bagFolder2 = model2.getBag((Resource) depositBag2.iterator().next());
        Resource folder2 = (Resource) bagFolder2.iterator().next();
        Bag childrenBag2 = model2.getBag(folder2.getURI());
        assertEquals(2, childrenBag2.size());

        // Verify that all manifests were added.
        List<String> manifestPaths2 = depositBag2.listProperties(CdrDeposit.hasDatastreamManifest)
                .toList().stream()
                .map(Statement::getResource)
                .map(r -> r.getProperty(CdrDeposit.stagingLocation).getString())
                .collect(Collectors.toList());
        assertEquals(2, manifestPaths2.size(), "Unexpected number of manifests");

    }

    @Test
    public void testFilesOnlyModeWithNestedFolders() throws Exception {
        URI sourceUri = Paths.get("src/test/resources/paths/valid-bag").toAbsolutePath().toUri();
        status.put(DepositField.sourceUri.name(), sourceUri.toString());
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
        URI sourceUri = Paths.get("src/test/resources/paths/flat-bag").toAbsolutePath().toUri();
        status.put(DepositField.sourceUri.name(), sourceUri.toString());
        status.put(DepositField.fileName.name(), "Test File");
        status.put(DepositField.createParentFolder.name(), "true");
        status.put(DepositField.filesOnlyMode.name(), "true");

        job.run();

        Model model = job.getReadOnlyModel();
        Bag depositBag = model.getBag(job.getDepositPID().getRepositoryPath());

        Bag childrenBag = model.getBag(depositBag.getURI());

        assertEquals(2, childrenBag.size());

        Resource file1Resc = getChildByLabel(depositBag, "lorem.txt");
        assertTrue(file1Resc.hasProperty(RDF.type, Cdr.FileObject), "Content model was not set");
        Resource originalResc1 = DepositModelHelpers.getDatastream(file1Resc);
        String tagPath1 = originalResc1.getProperty(CdrDeposit.stagingLocation).getString();
        assertEquals(sourceUri.toString() + "data/lorem.txt", tagPath1);

        Resource file2Resc = getChildByLabel(depositBag, "ipsum.txt");
        assertTrue(file2Resc.hasProperty(RDF.type, Cdr.FileObject), "Content model was not set");
        Resource originalResc2 = DepositModelHelpers.getDatastream(file2Resc);
        String tagPath2 = originalResc2.getProperty(CdrDeposit.stagingLocation).getString();
        assertTrue(tagPath2.endsWith("data/ipsum.txt"), "Unexpected path " + tagPath2);

        // Verify that all manifests were added.
        List<String> manifestPaths = depositBag.listProperties(CdrDeposit.hasDatastreamManifest)
                .toList().stream()
                .map(Statement::getResource)
                .map(r -> r.getProperty(CdrDeposit.stagingLocation).getString())
                .collect(Collectors.toList());
        assertEquals(2, manifestPaths.size(), "Unexpected number of manifests");
        List<String> expectedFilePaths = Arrays.asList(sourceUri.toString() + "bagit.txt",
                sourceUri.toString() + "manifest-md5.txt");
        assertTrue(manifestPaths.containsAll(expectedFilePaths),
                "Must contain all of the expected manifest files, but contained " + manifestPaths);

        Set<String> cleanupSet = new HashSet<>();
        StmtIterator it = depositBag.listProperties(CdrDeposit.cleanupLocation);
        while (it.hasNext()) {
            Statement stmt = it.nextStatement();
            cleanupSet.add(stmt.getString());
        }

        assertEquals(1, cleanupSet.size(), "Incorrect number of objects identified for cleanup");
        assertTrue(cleanupSet.contains(sourceUri.toString()), "Cleanup of bag not set");
    }

    private void assertFileAdded(Resource work, String md5sum, String fileLocation) {
        assertTrue(work.hasProperty(RDF.type, Cdr.Work), "Missing RDF type");
        String[] pathParts = fileLocation.split("/");

        Bag workBag = work.getModel().getBag(work.getURI());
        Resource file = getChildByLabel(workBag, pathParts[pathParts.length - 1]);
        Resource originalResc = DepositModelHelpers.getDatastream(file);

        assertTrue(file.hasProperty(RDF.type, Cdr.FileObject), "Missing RDF type");
        assertEquals(md5sum, originalResc.getProperty(CdrDeposit.md5sum).getString(),
                "Checksum was not set");
        assertEquals(fileLocation, originalResc.getProperty(CdrDeposit.stagingLocation).getString(),
                "Binary location not set");
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

    @Test
    public void testInvalid() throws Exception {
        Assertions.assertThrows(JobFailedException.class, () -> {
            URI sourceUri = Paths.get("src/test/resources/paths/invalid-bag").toAbsolutePath().toUri();
            status.put(DepositField.sourceUri.name(), sourceUri.toString());

            job.run();
        });
    }
}

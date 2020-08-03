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
package edu.unc.lib.deposit.transfer;

import static edu.unc.lib.dl.model.DatastreamPids.getOriginalFilePid;
import static edu.unc.lib.dl.persist.services.ingest.IngestSourceTestHelper.createConfigFile;
import static edu.unc.lib.dl.persist.services.ingest.IngestSourceTestHelper.createFilesystemConfig;
import static edu.unc.lib.dl.persist.services.ingest.IngestSourceTestHelper.serializeLocationMappings;
import static edu.unc.lib.dl.test.TestHelpers.setField;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;

import edu.unc.lib.deposit.normalize.AbstractNormalizationJobTest;
import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.deposit.work.JobInterruptedException;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.api.storage.StorageLocationManager;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferException;
import edu.unc.lib.dl.persist.api.transfer.BinaryTransferSession;
import edu.unc.lib.dl.persist.services.ingest.IngestSourceManagerImpl;
import edu.unc.lib.dl.persist.services.storage.StorageLocationTestHelper;
import edu.unc.lib.dl.persist.services.transfer.BinaryTransferServiceImpl;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;

/**
 * @author bbpennel
 *
 */
public class TransferBinariesToStorageJobTest extends AbstractNormalizationJobTest {

    private static final Logger log = getLogger(TransferBinariesToStorageJobTest.class);

    private final static String LOC1_ID = "loc1";
    private final static String LOC2_ID = "loc2";
    private final static String SOURCE_ID = "source1";
    private final static String DEPOSITS_SOURCE_ID = "deposits";

    private final static String FILE_CONTENT1 = "Some content";
    private final static String FILE_CONTENT2 = "Other stuff";

    private TransferBinariesToStorageJob job;

    private IngestSourceManagerImpl sourceManager;

    private StorageLocationManager locationManager;
    private StorageLocation storageLoc;
    private BinaryTransferServiceImpl transferService;

    private StorageLocationTestHelper locTestHelper;

    private Model depositModel;
    private Bag depBag;

    private Path loc1Path;
    private Path loc2Path;
    private Path sourcePath;
    private Path candidatePath;

    @Mock
    private RepositoryObjectFactory repoObjFactory;

    @Before
    public void init() throws Exception {
        loc1Path = tmpFolder.newFolder("loc1").toPath();
        loc2Path = tmpFolder.newFolder("loc2").toPath();

        locTestHelper = new StorageLocationTestHelper();
        locTestHelper.addStorageLocation(LOC1_ID, "Location 1", loc1Path.toString());
        locTestHelper.addStorageLocation(LOC2_ID, "Location 2", loc2Path.toString());

        locationManager = locTestHelper.createLocationManager(null);
        storageLoc = locationManager.getStorageLocationById(LOC1_ID);

        sourcePath = tmpFolder.newFolder(SOURCE_ID).toPath();
        candidatePath = Files.createDirectories(sourcePath.resolve(depositUUID));
        sourceManager = new IngestSourceManagerImpl();
        sourceManager.setConfigPath(createConfigFile(
                createFilesystemConfig(SOURCE_ID, "Source", sourcePath, asList("*"), false),
                createFilesystemConfig(DEPOSITS_SOURCE_ID, "Deposits", depositsDirectory.toPath(), asList("*"), false)

            ).toString());
        sourceManager.setMappingPath(serializeLocationMappings(emptyList()).toString());
        sourceManager.init();

        transferService = new BinaryTransferServiceImpl();
        transferService.setIngestSourceManager(sourceManager);

        job = new TransferBinariesToStorageJob(jobUUID, depositUUID);
        setField(job, "locationManager", locationManager);
        setField(job, "transferService", transferService);
        setField(job, "dataset", dataset);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "jobStatusFactory", jobStatusFactory);
        setField(job, "repoObjFactory", repoObjFactory);
        job.init();

        depositModel = job.getWritableModel();
        depBag = depositModel.createBag(depositPid.getRepositoryPath());
        depBag.addProperty(Cdr.storageLocation, LOC1_ID);
    }

    @Test
    public void depositWithNoFilesToTransfer() throws Exception {
        addContainerObject(depBag, Cdr.Folder);
        job.closeModel();

        job.run();

        assertEquals("No file should have been transferred", 0, loc1Path.toFile().list().length);
    }

    @Test
    public void depositWithWorkContainingFileAndFits() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc = addFileObject(workBag, FILE_CONTENT1, true);
        workBag.addProperty(Cdr.primaryObject, fileResc);

        job.closeModel();

        job.run();

        Model model = job.getReadOnlyModel();
        Resource postFileResc = model.getResource(fileResc.getURI());

        assertOriginalFileTransferred(postFileResc, FILE_CONTENT1);
        assertFitsFileTransferred(postFileResc);
    }

    // Ensure that interruptions come through as JobInterruptedExceptions
    @Test
    public void interruptionTest() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc = addFileObject(workBag, FILE_CONTENT1, true);
        workBag.addProperty(Cdr.primaryObject, fileResc);

        Bag workBag1 = addContainerObject(depBag, Cdr.Work);
        Resource fileResc1 = addFileObject(workBag1, FILE_CONTENT1, true);
        workBag1.addProperty(Cdr.primaryObject, fileResc1);
        job.closeModel();

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
        Thread.sleep((long) new Random().nextFloat() * 20);
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
    public void depositWithFolderWithModsHistory() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Folder);
        job.closeModel();

        String historyContent = "This is pretty much history";
        PID pid = PIDs.get(workBag.getURI());
        File historyFile = job.getModsHistoryPath(pid).toFile();
        FileUtils.writeStringToFile(historyFile, historyContent, UTF_8);

        job.run();

        Model model = job.getReadOnlyModel();
        Resource postWorkResc = model.getResource(workBag.getURI());

        assertFalse("History file should no longer exist in deposits directory", historyFile.exists());

        URI historyUri = URI.create(postWorkResc.getProperty(CdrDeposit.descriptiveHistoryStorageUri).getString());
        Path historyPath = Paths.get(historyUri);
        assertTrue("History file should exist at storage uri", Files.exists(historyPath));
        assertTrue("Transfered history must be in the expected storage location", storageLoc.isValidUri(historyUri));

        assertEquals(historyContent, FileUtils.readFileToString(historyPath.toFile(), UTF_8));
    }

    @Test
    public void depositWithMultipleFilesResumed() throws Exception {
        String manifest1Name = "manifest1.txt";
        File manifestFile1 = new File(depositDir, manifest1Name);

        when(depositStatusFactory.getManifestURIs(depositUUID)).thenReturn(asList(
                manifestFile1.toPath().toUri().toString()));

        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc1 = addFileObject(workBag, FILE_CONTENT1, true);
        workBag.addProperty(Cdr.primaryObject, fileResc1);

        job.closeModel();
        // First, incomplete run
        job.run();

        depositModel = job.getWritableModel();
        // Refresh from update model
        workBag = depositModel.getBag(workBag);
        // Add in the second file
        Resource fileResc2 = addFileObject(workBag, FILE_CONTENT2, true);

        job.closeModel();

        job.run();

        Model model = job.getReadOnlyModel();
        Resource postFileResc1 = model.getResource(fileResc1.getURI());
        assertOriginalFileTransferred(postFileResc1, FILE_CONTENT1);
        assertFitsFileTransferred(postFileResc1);

        Resource postFileResc2 = model.getResource(fileResc2.getURI());
        assertOriginalFileTransferred(postFileResc2, FILE_CONTENT2);
        assertFitsFileTransferred(postFileResc2);
    }

    @Test
    public void depositWithManifests() throws Exception {
        String manifest1Name = "manifest1.txt";
        String manifest2Name = "manifest2.txt";
        File manifestFile1 = new File(depositDir, manifest1Name);
        FileUtils.writeStringToFile(manifestFile1, FILE_CONTENT1, "UTF-8");
        File manifestFile2 = new File(depositDir, manifest2Name);
        FileUtils.writeStringToFile(manifestFile2, FILE_CONTENT2, "UTF-8");

        when(depositStatusFactory.getManifestURIs(depositUUID)).thenReturn(asList(
                manifestFile1.toPath().toUri().toString(),
                manifestFile2.toPath().toUri().toString()));

        addContainerObject(depBag, Cdr.Folder);
        job.closeModel();

        job.run();

        Model model = job.getReadOnlyModel();
        Bag postDepBag = model.getBag(depBag);

        List<URI> manifestStorageUris = new ArrayList<>();
        StmtIterator it = postDepBag.listProperties(CdrDeposit.storageUri);
        while (it.hasNext()) {
            Statement stmt = it.next();
            manifestStorageUris.add(URI.create(stmt.getString()));
        }

        assertManifestTranferred(manifestStorageUris, manifest1Name);
        assertManifestTranferred(manifestStorageUris, manifest2Name);
    }

    @Test
    public void depositResumeFailed() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc = addFileObject(workBag, FILE_CONTENT1, true);
        workBag.addProperty(Cdr.primaryObject, fileResc);

        Bag workBag2 = addContainerObject(depBag, Cdr.Work);
        Resource fileResc2 = addFileObject(workBag2, FILE_CONTENT2, true);
        workBag2.addProperty(Cdr.primaryObject, fileResc2);

        String filePath2 = fileResc2.getProperty(CdrDeposit.stagingLocation).getString();
        Path flappingPath = Paths.get(URI.create(filePath2));
        Files.delete(flappingPath);

        job.closeModel();

        try {
            job.run();
            fail("Job expected to fail");
        } catch (BinaryTransferException e) {
            // expected
        }

        // Restore the contents
        FileUtils.writeStringToFile(flappingPath.toFile(), FILE_CONTENT2, "UTF-8");
        // Resume the job
        job.run();

        Model model = job.getReadOnlyModel();
        Resource postFileResc = model.getResource(fileResc.getURI());

        assertOriginalFileTransferred(postFileResc, FILE_CONTENT1);
        assertFitsFileTransferred(postFileResc);

        Resource postFileResc2 = model.getResource(fileResc2.getURI());

        assertOriginalFileTransferred(postFileResc2, FILE_CONTENT2);
        assertFitsFileTransferred(postFileResc2);
    }

    @Test
    public void pidCollision() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc = addFileObject(workBag, FILE_CONTENT1, true);
        workBag.addProperty(Cdr.primaryObject, fileResc);

        String existingContent = "This thing already exists";

        PID originalPid;
        URI fileUri;
        // Put the file into the storage location beforehand
        try (BinaryTransferSession session = transferService.getSession(storageLoc)) {
            originalPid = getOriginalFilePid(PIDs.get(fileResc.getURI()));
            fileUri = session.transfer(originalPid, IOUtils.toInputStream(existingContent, UTF_8));
        }

        // Indicate that an object already exists in the repository
        when(repoObjFactory.objectExists(originalPid.getRepositoryUri())).thenReturn(true);

        job.closeModel();

        try {
            job.run();
            fail("Expected exception");
        } catch (JobFailedException e) {
            assertTrue("Expected exception indicating that the object to which the binary belongs already exists",
                    e.getMessage().contains("already exists"));
            assertEquals("Existing file must not have been modified",
                    existingContent, FileUtils.readFileToString(new File(fileUri), "UTF-8"));
        }
    }

    @Test
    public void fileAlreadyTransferredButNotRecorded() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc = addFileObject(workBag, FILE_CONTENT1, true);
        workBag.addProperty(Cdr.primaryObject, fileResc);

        // Put the file into the storage location beforehand
        try (BinaryTransferSession session = transferService.getSession(storageLoc)) {
            PID originalPid = getOriginalFilePid(PIDs.get(fileResc.getURI()));
            URI fileUri = URI.create(fileResc.getProperty(CdrDeposit.stagingLocation).getString());
            session.transfer(originalPid, fileUri);
        }

        job.closeModel();

        job.run();

        Model model = job.getReadOnlyModel();
        Resource postFileResc = model.getResource(fileResc.getURI());

        assertFileTransferred(postFileResc, FILE_CONTENT1);
    }

    @Test
    public void filePartiallyTransferred() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Work);
        Resource fileResc = addFileObject(workBag, FILE_CONTENT1, true);
        workBag.addProperty(Cdr.primaryObject, fileResc);

        // Transfer part of the file beforehand
        try (BinaryTransferSession session = transferService.getSession(storageLoc)) {
            PID originalPid = getOriginalFilePid(PIDs.get(fileResc.getURI()));
            session.transfer(originalPid, IOUtils.toInputStream("Some co", UTF_8));
        }

        job.closeModel();

        job.run();

        Model model = job.getReadOnlyModel();
        Resource postFileResc = model.getResource(fileResc.getURI());

        // Expect the full file to be present after running
        assertFileTransferred(postFileResc, FILE_CONTENT1);
    }

    private void assertManifestTranferred(List<URI> manifestUris, String name) {
        URI manifestUri = manifestUris.stream().filter(m -> m.toString().endsWith(name)).findFirst().orElseGet(null);

        assertTrue(Paths.get(manifestUri).toFile().exists());
        assertTrue(storageLoc.isValidUri(manifestUri));
    }

    private Bag addContainerObject(Bag parent, Resource type) {
        PID objPid = makePid();
        Bag objBag = depositModel.createBag(objPid.getRepositoryPath());
        objBag.addProperty(RDF.type, type);
        parent.add(objBag);

        return objBag;
    }

    private void assertOriginalFileTransferred(Resource resc, String expectedContent) throws Exception {
        URI stagingUri = URI.create(resc.getProperty(CdrDeposit.stagingLocation).getString());
        assertFalse("Staged file should no longer exist", Files.exists(Paths.get(stagingUri)));

        assertFileTransferred(resc, expectedContent);
    }

    private void assertFileTransferred(Resource resc, String expectedContent) throws Exception {
        URI storageUri = URI.create(resc.getProperty(CdrDeposit.storageUri).getString());
        Path storagePath = Paths.get(storageUri);
        assertTrue("Binary should exist at storage uri", Files.exists(storagePath));
        assertEquals(expectedContent, FileUtils.readFileToString(storagePath.toFile(), "UTF-8"));
        assertTrue("Transfered file must be in the expected storage location", storageLoc.isValidUri(storageUri));
    }

    private void assertFitsFileTransferred(Resource resc) throws Exception {
        PID objPid = PIDs.get(resc.getURI());

        File fitsSource = job.getTechMdPath(objPid, false).toFile();
        assertFalse("FITS file should no longer exist in deposits directory", fitsSource.exists());

        URI fitsUri = URI.create(resc.getProperty(CdrDeposit.fitsStorageUri).getString());
        Path fitsPath = Paths.get(fitsUri);
        assertTrue("FITS file should exist at storage uri", Files.exists(fitsPath));
        assertTrue("Transfered FITS must be in the expected storage location", storageLoc.isValidUri(fitsUri));
    }

    private Resource addFileObject(Bag parent, String content, boolean withFits) throws Exception {
        PID objPid = makePid();
        Resource objResc = depositModel.getResource(objPid.getRepositoryPath());
        objResc.addProperty(RDF.type, Cdr.FileObject);

        File originalFile = candidatePath.resolve(objPid.getId() + ".txt").toFile();
        FileUtils.writeStringToFile(originalFile, content, "UTF-8");
        objResc.addProperty(CdrDeposit.stagingLocation, originalFile.toPath().toUri().toString());

        if (withFits) {
            Files.createFile(job.getTechMdPath(objPid, true));
        }

        parent.add(objResc);
        return objResc;
    }
}

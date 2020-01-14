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

import static edu.unc.lib.dl.persist.services.ingest.IngestSourceTestHelper.createConfigFile;
import static edu.unc.lib.dl.persist.services.ingest.IngestSourceTestHelper.createFilesystemConfig;
import static edu.unc.lib.dl.persist.services.ingest.IngestSourceTestHelper.serializeLocationMappings;
import static edu.unc.lib.dl.test.TestHelpers.setField;
import static edu.unc.lib.dl.util.DepositConstants.DESCRIPTION_DIR;
import static edu.unc.lib.dl.util.DepositConstants.TECHMD_DIR;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;

import edu.unc.lib.deposit.normalize.AbstractNormalizationJobTest;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.persist.api.storage.StorageLocation;
import edu.unc.lib.dl.persist.services.ingest.IngestSourceManagerImpl;
import edu.unc.lib.dl.persist.services.storage.StorageLocationManagerImpl;
import edu.unc.lib.dl.persist.services.storage.StorageLocationTestHelper;
import edu.unc.lib.dl.persist.services.transfer.BinaryTransferServiceImpl;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;

/**
 * @author bbpennel
 *
 */
public class TransferBinariesToStorageJobTest extends AbstractNormalizationJobTest {

    private final static String LOC1_ID = "loc1";
    private final static String LOC2_ID = "loc2";
    private final static String SOURCE_ID = "source1";
    private final static String DEPOSITS_SOURCE_ID = "deposits";

    private final static String FILE_CONTENT1 = "Some content";
    private final static String FILE_CONTENT2 = "Other stuff";

    private TransferBinariesToStorageJob job;

    private IngestSourceManagerImpl sourceManager;

    private StorageLocationManagerImpl locationManager;
    private StorageLocation storageLoc;
    private BinaryTransferServiceImpl transferService;

    private StorageLocationTestHelper locTestHelper;

    private Model depositModel;
    private Bag depBag;
    private File techmdDir;
    private File modsDir;

    private Path loc1Path;
    private Path loc2Path;
    private Path sourcePath;
    private Path candidatePath;

    @Before
    public void init() throws Exception {
        loc1Path = tmpFolder.newFolder("loc1").toPath();
        loc2Path = tmpFolder.newFolder("loc2").toPath();

        locTestHelper = new StorageLocationTestHelper();
        locTestHelper.addStorageLocation(LOC1_ID, "Location 1", loc1Path.toString());
        locTestHelper.addStorageLocation(LOC2_ID, "Location 2", loc2Path.toString());

        locationManager = new StorageLocationManagerImpl();
        locationManager.setConfigPath(locTestHelper.serializeLocationConfig());
        locationManager.setMappingPath(locTestHelper.serializeLocationMappings());
        locationManager.init();
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
        job.init();

        depositModel = job.getWritableModel();
        depBag = depositModel.createBag(depositPid.getRepositoryPath());
        depBag.addProperty(Cdr.storageLocation, LOC1_ID);

        techmdDir = new File(depositDir, TECHMD_DIR);
        techmdDir.mkdir();
        modsDir = new File(depositDir, DESCRIPTION_DIR);
        modsDir.mkdir();
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

    @Test
    public void depositWithFolderWithMods() throws Exception {
        Bag workBag = addContainerObject(depBag, Cdr.Folder);
        job.closeModel();

        String modsContent = "This is pretty much mods";
        addModsFile(workBag, modsContent);

        job.run();

        Model model = job.getReadOnlyModel();
        Resource postWorkResc = model.getResource(workBag.getURI());

        assertModsFileTransferred(postWorkResc, modsContent);
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

        File fitsSource = new File(techmdDir, objPid.getId() + ".xml");
        assertFalse("FITS file should no longer exist in deposits directory", fitsSource.exists());

        URI fitsUri = URI.create(resc.getProperty(CdrDeposit.fitsStorageUri).getString());
        Path fitsPath = Paths.get(fitsUri);
        assertTrue("FITS file should exist at storage uri", Files.exists(fitsPath));
        assertTrue("Transfered FITS must be in the expected storage location", storageLoc.isValidUri(fitsUri));
    }

    private void assertModsFileTransferred(Resource resc, String expectedContent) throws Exception {
        PID objPid = PIDs.get(resc.getURI());

        File modsSource = new File(modsDir, objPid.getId() + ".xml");
        assertFalse("MODS file should no longer exist in deposits directory", modsSource.exists());

        URI modsUri = URI.create(resc.getProperty(CdrDeposit.descriptiveStorageUri).getString());
        Path modsPath = Paths.get(modsUri);
        assertTrue("MODS file should exist at storage uri", Files.exists(modsPath));
        assertTrue("Transfered MODS must be in the expected storage location", storageLoc.isValidUri(modsUri));

        assertEquals(expectedContent, FileUtils.readFileToString(modsPath.toFile(), "UTF-8"));
    }

    private Resource addFileObject(Bag parent, String content, boolean withFits) throws Exception {
        PID objPid = makePid();
        Resource objResc = depositModel.getResource(objPid.getRepositoryPath());
        objResc.addProperty(RDF.type, Cdr.FileObject);

        File originalFile = candidatePath.resolve(objPid.getId() + ".txt").toFile();
        FileUtils.writeStringToFile(originalFile, content, "UTF-8");
        objResc.addProperty(CdrDeposit.stagingLocation, originalFile.toPath().toUri().toString());

        if (withFits) {
            File fitsFile = new File(techmdDir, objPid.getId() + ".xml");
            fitsFile.createNewFile();
        }

        parent.add(objResc);
        return objResc;
    }

    private void addModsFile(Resource resc, String content) throws Exception {
        PID pid = PIDs.get(resc.getURI());
        File modsFile = new File(modsDir, pid.getId() + ".xml");
        modsFile.createNewFile();
        FileUtils.writeStringToFile(modsFile, content, UTF_8);
    }
}

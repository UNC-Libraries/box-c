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
package edu.unc.lib.deposit.fcrepo4;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static edu.unc.lib.dl.util.DepositConstants.TECHMD_DIR;
import static org.apache.jena.rdf.model.ResourceFactory.createResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.deposit.validate.VerifyObjectsAreInFedoraService;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.event.FilePremisLogger;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentContainerObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FedoraTransaction;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fcrepo4.PremisEventObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObject;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectFactory;
import edu.unc.lib.dl.fcrepo4.RepositoryObjectLoader;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fcrepo4.TransactionCancelledException;
import edu.unc.lib.dl.fcrepo4.TransactionManager;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrAcl;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.rdf.PcdmModels;
import edu.unc.lib.dl.rdf.Premis;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;
import edu.unc.lib.dl.util.RedisWorkerConstants.JobField;
import edu.unc.lib.dl.util.SoftwareAgentConstants.SoftwareAgent;

/**
 *
 * @author bbpennel
 *
 */
public class IngestContentObjectsJobIT extends AbstractFedoraDepositJobIT {

    private static final String INGESTOR_PRINC = "ingestor";

    private IngestContentObjectsJob job;

    private PID destinationPid;

    private static final String FILE1_LOC = "pdf.pdf";
    private static final String FILE1_MIMETYPE = "application/pdf";
    private static final String FILE1_SHA1 = "7185198c0f158a3b3caa3f387efa3df990d2a904";
    private static final String FILE1_MD5 = "b5808604069f9f61d94e0660409616ba";
    private static final long FILE1_SIZE = 739L;
    private static final String FILE2_LOC = "text.txt";
    private static final String FILE2_MIMETYPE = "text/plain";
    private static final long FILE2_SIZE = 4L;

    private File techmdDir;

    @Autowired
    private AccessControlService aclService;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private RepositoryObjectFactory repoObjFactory;
    @Autowired
    private TransactionManager txManager;
    @Autowired
    private FcrepoClient fcrepoClient;
    @Autowired
    private Model queryServiceModel;
    @Autowired VerifyObjectsAreInFedoraService verificationService;

    @Before
    public void init() throws Exception {

        job = new IngestContentObjectsJob();
        job.setJobUUID(jobUUID);
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        job.setPremisLoggerFactory(premisLoggerFactory);
        setField(job, "pidMinter", pidMinter);
        setField(job, "aclService", aclService);
        setField(job, "dataset", dataset);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "txManager", txManager);
        setField(job, "jobStatusFactory", jobStatusFactory);
        setField(job, "metricsClient", metricsClient);
        setField(job, "repoObjLoader", repoObjLoader);
        setField(job, "repoObjFactory", repoObjFactory);
        setField(job, "fcrepoClient", fcrepoClient);
        setField(job, "verificationService", verificationService);
        job.init();

        createBaseContainer(RepositoryPathConstants.CONTENT_BASE);
        // Create a destination folder where deposits will be ingested to
        setupDestination();

        FileUtils.copyDirectory(new File("src/test/resources/examples"), depositDir);

        techmdDir = new File(depositDir, TECHMD_DIR);
        techmdDir.mkdir();
    }

    private void setupDestination() {
        PID contentPid = PIDs.get(RepositoryPathConstants.CONTENT_ROOT_ID);
        Resource contentResc = createResource(contentPid.getRepositoryPath());
        PID unitPid = pidMinter.mintContentPid();
        Resource unitResc = createResource(unitPid.getRepositoryPath());

        FolderObject destFolder = repoObjFactory.createFolderObject(null);
        destinationPid = destFolder.getPid();
        Resource destResc = createResource(destinationPid.getRepositoryPath());

        queryServiceModel.add(contentResc, PcdmModels.hasMember, unitResc);
        queryServiceModel.add(unitResc, PcdmModels.hasMember, destResc);
        queryServiceModel.add(destResc, CdrAcl.canIngest, INGESTOR_PRINC);

        Map<String, String> status = new HashMap<>();
        status.put(DepositField.containerId.name(), destinationPid.getRepositoryPath());
        status.put(DepositField.permissionGroups.name(), INGESTOR_PRINC);
        depositStatusFactory.save(depositUUID, status);
    }

    /**
     * Test that a single folder can be created
     */
    @Test
    public void ingestEmptyFolderTest() {
        String label = "testfolder";

        // Construct the deposit model, containing a deposit with one empty folder
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        // Constructing the folder in the deposit model with a title
        PID folderPid = pidMinter.mintContentPid();
        Bag folderBag = model.createBag(folderPid.getRepositoryPath());
        folderBag.addProperty(RDF.type, Cdr.Folder);
        folderBag.addProperty(CdrDeposit.label, label);

        depBag.add(folderBag);
        job.closeModel();

        // Execute the ingest job
        job.run();

        // Verify that the destination has the folder added to it
        RepositoryObject destObj = repoObjLoader.getRepositoryObject(destinationPid);
        List<ContentObject> destMembers = ((ContentContainerObject) destObj).getMembers();
        assertEquals("Incorrect number of children at destination", 1, destMembers.size());

        // Make sure that the folder is present and is actually a folder
        ContentObject mFolder = findContentObjectByPid(destMembers, folderPid);
        assertTrue("Child was not a folder", mFolder instanceof FolderObject);

        // Try directly retrieving the folder
        FolderObject folder = repoObjLoader.getFolderObject(folderPid);
        // Verify that its title was set to the expected value
        String title = folder.getResource().getProperty(DC.title).getString();
        assertEquals("Folder title was not correctly set", label, title);
        // Verify that ingestion event gets added for folder
        List<PremisEventObject> events = folder.getPremisLog().getEvents();
        assertEquals(1, events.size());
        assertEquals("ingested as PID: " + folder.getPid().getQualifiedId(), events.get(0).getResource()
            .getProperty(Premis.hasEventDetail).getString());

        assertClickCount(1);
    }

    /**
     * Test that work objects are created along with relationships to their children.
     */
    @Test
    public void ingestWorkObjectTest() throws Exception {
        String label = "testwork";

        // Construct the deposit model with work object
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        // Constructing the work in the deposit model with a label
        PID workPid = pidMinter.mintContentPid();
        Bag workBag = model.createBag(workPid.getRepositoryPath());
        workBag.addProperty(RDF.type, Cdr.Work);
        workBag.addProperty(CdrDeposit.label, label);

        PID mainPid = addFileObject(workBag, FILE1_LOC, FILE1_MIMETYPE, FILE1_SHA1, FILE1_MD5);
        PID supPid = addFileObject(workBag, FILE2_LOC, FILE2_MIMETYPE, null, null);

        depBag.add(workBag);

        workBag.asResource().addProperty(Cdr.primaryObject,
                model.getResource(mainPid.getRepositoryPath()));

        job.closeModel();

        job.run();

        ContentContainerObject destObj = (ContentContainerObject) repoObjLoader.getRepositoryObject(destinationPid);
        List<ContentObject> destMembers = destObj.getMembers();
        assertEquals("Incorrect number of children at destination", 1, destMembers.size());

        // Make sure that the work is present and is actually a work
        WorkObject mWork = (WorkObject) findContentObjectByPid(destMembers, workPid);

        String title = mWork.getResource().getProperty(DC.title).getString();
        assertEquals("Work title was not correctly set", label, title);

        // Verify that the properties of the primary object were added
        FileObject primaryObj = mWork.getPrimaryObject();
        assertBinaryProperties(primaryObj, FILE1_LOC, FILE1_MIMETYPE, FILE1_SHA1, FILE1_MD5, FILE1_SIZE);

        // Check the right number of members are present
        List<ContentObject> workMembers = mWork.getMembers();
        assertEquals("Incorrect number of members in work", 2, workMembers.size());
        FileObject supObj = (FileObject) findContentObjectByPid(workMembers, supPid);
        assertNotNull(supObj);

        // Verify the properties and content of the supplemental file
        assertBinaryProperties(supObj, FILE2_LOC, FILE2_MIMETYPE, null, null, FILE2_SIZE);
        // Verify that ingestion event gets added for work
        List<PremisEventObject> eventsWork = mWork.getPremisLog().getEvents();
        assertEquals(2, eventsWork.size());

        assertNotNull(findPremisEventByEventDetail(eventsWork, "ingested as PID: " + mWork.getPid().getQualifiedId()));
        assertNotNull(findPremisEventByEventDetail(eventsWork, "added 2 child objects to this container"));

        // Verify that ingestion event gets added for primary object
        List<PremisEventObject> eventsPrimObj = primaryObj.getPremisLog().getEvents();
        assertEquals(1, eventsPrimObj.size());
        assertNotNull(findPremisEventByEventDetail(eventsPrimObj, "ingested as PID: " + mainPid.getQualifiedId()
            + "\n ingested as filename: " + FILE1_LOC));

        // Verify that ingestion event gets added for supplementary object
        List<PremisEventObject> eventsSupObj = supObj.getPremisLog().getEvents();
        assertEquals(1, eventsSupObj.size());
        assertNotNull(findPremisEventByEventDetail(eventsSupObj, "ingested as PID: " + supPid.getQualifiedId()
            + "\n ingested as filename: " + FILE2_LOC));

        assertClickCount(3);
    }


    /**
     * Ensure that deposit fails on a sha1 checksum mismatch for a single file deposit
     */
    @Test(expected = TransactionCancelledException.class)
    public void ingestFileObjectChecksumMismatch() throws Exception {
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        String badSha1 = "111111111111111111111111111111111111";
        PID workPid = addWorkWithFileObject(depBag, FILE1_LOC, FILE1_MIMETYPE, badSha1, null);

        job.closeModel();

        try {
            job.run();
        } finally {
            assertFalse(FedoraTransaction.hasTxId());
            assertFalse(FedoraTransaction.isStillAlive());
            assertFalse(objectExists(workPid));
        }
    }

    /**
     * Ensure that deposit fails on a md5 checksum mismatch for a single file deposit
     */
    @Test(expected = TransactionCancelledException.class)
    public void ingestFileObjectMd5ChecksumMismatch() throws Exception {
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        String badMd5 = "111111111111111111111111111111111111";
        addWorkWithFileObject(depBag, FILE1_LOC, FILE1_MIMETYPE, null, badMd5);

        job.closeModel();

        job.run();
    }

    /**
     * Verify that resuming a completed deposit doesn't result in extra objects
     */
    @Test
    public void resumeCompletedWorkIngestTest() throws Exception {
        ingestWorkObjectTest();

        job.run();

        RepositoryObject destObj = repoObjLoader.getRepositoryObject(destinationPid);
        List<ContentObject> destMembers = ((ContentContainerObject) destObj).getMembers();
        assertEquals("Incorrect number of children at destination", 1, destMembers.size());

        List<ContentObject> workMembers = ((ContentContainerObject) destMembers.get(0)).getMembers();
        assertEquals("Incorrect number of members in work", 2, workMembers.size());

        assertClickCount(3);
    }

    /**
     * Tests ingest of a folder with two files in it, which fails on the second
     * child and then attempts to resume
     */
    @Test
    public void resumeIngestFolderTest() throws Exception {
        String label = "testfolder";

        // Construct the deposit model, containing a deposit with one empty folder
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        // Constructing the folder in the deposit model with a title
        PID folderPid = pidMinter.mintContentPid();
        Bag folderBag = model.createBag(folderPid.getRepositoryPath());
        folderBag.addProperty(RDF.type, Cdr.Folder);
        folderBag.addProperty(CdrDeposit.label, label);

        depBag.add(folderBag);

        // Add children, where the second child is invalid due to missing location
        PID file1Pid = addWorkWithFileObject(folderBag, FILE1_LOC, FILE1_MIMETYPE, FILE1_SHA1, FILE1_MD5);
        PID file2Pid = addWorkWithFileObject(folderBag, null, FILE2_MIMETYPE, null, null);

        job.closeModel();

        // Execute the ingest job
        try {
            job.run();
            fail("Test should not reach this line. Job should have thrown exception");
        } catch (TransactionCancelledException e) {
            // expected, lets continue
        }

        // Check that the folder and first child successfully made it in
        RepositoryObject destObj = repoObjLoader.getRepositoryObject(destinationPid);
        List<ContentObject> destMembersFailed = ((ContentContainerObject) destObj).getMembers();
        assertEquals("Incorrect number of children at destination", 1, destMembersFailed.size());

        ContentObject mFolderFailed = destMembersFailed.get(0);
        List<ContentObject> folderMembersFailed = ((ContentContainerObject) mFolderFailed).getMembers();
        assertEquals("Incorrect number of children in folder", 1, folderMembersFailed.size());

        // Fix the staging location of the second file
        model = job.getWritableModel();
        Resource file2Resc = model.getResource(file2Pid.getRepositoryPath());
        file2Resc.addProperty(CdrDeposit.stagingLocation, Paths.get(depositDir.getAbsolutePath(),
                FILE2_LOC).toUri().toString());
        job.closeModel();

        // Second run of job
        job.run();

        List<ContentObject> destMembers = ((ContentContainerObject) destObj).getMembers();
        assertEquals("Incorrect number of children at destination", 1, destMembers.size());

        ContentObject mFolder = destMembers.get(0);
        List<ContentObject> folderMembers = ((ContentContainerObject) mFolder).getMembers();
        assertEquals("Incorrect number of children in folder", 2, folderMembers.size());

        // Order of the children isn't guaranteed, so find by primary obj pid
        WorkObject workA = (WorkObject) folderMembers.get(0);
        WorkObject workB = (WorkObject) folderMembers.get(1);

        FileObject file1Obj;
        FileObject file2Obj;
        if (file1Pid.equals(workA.getPrimaryObject().getPid())) {
            file1Obj = workA.getPrimaryObject();
            file2Obj = workB.getPrimaryObject();
        } else {
            file2Obj = workA.getPrimaryObject();
            file1Obj = workB.getPrimaryObject();
        }

        assertBinaryProperties(file1Obj, FILE1_LOC, FILE1_MIMETYPE, FILE1_SHA1, FILE1_MD5, FILE1_SIZE);
        assertBinaryProperties(file2Obj, FILE2_LOC, FILE2_MIMETYPE, null, null, FILE2_SIZE);

        // Count includes folder, two works each with a file
        assertClickCount(5);
    }

    /**
     * Test a deeply nested hierarchy of folders with binary at the bottom of
     * the tree
     */
    @Test
    public void ingestDeepHierarchy() throws Exception {
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        // Constructing the nested folders
        int nestingDepth = 6;
        Bag previousBag = depBag;
        for (int i = 0; i < nestingDepth; i++) {
            PID folderPid = pidMinter.mintContentPid();
            Bag folderBag = model.createBag(folderPid.getRepositoryPath());
            folderBag.addProperty(RDF.type, Cdr.Folder);
            previousBag.add(folderBag);
            previousBag = folderBag;
        }

        addWorkWithFileObject(previousBag, FILE1_LOC, FILE1_MIMETYPE, FILE1_SHA1, FILE1_MD5);

        job.closeModel();

        job.run();

        RepositoryObject destObj = repoObjLoader.getRepositoryObject(destinationPid);
        List<ContentObject> members = ((ContentContainerObject) destObj).getMembers();

        for (int i = 0; i < nestingDepth; i++) {
            assertEquals("Incorrect number of children", 1, members.size());
            FolderObject folder = (FolderObject) members.get(0);
            members = folder.getMembers();
        }

        assertEquals("Incorrect number of children in last tier", 1, members.size());
        WorkObject work = (WorkObject) members.get(0);
        FileObject primaryFile = work.getPrimaryObject();
        assertBinaryProperties(primaryFile, FILE1_LOC, FILE1_MIMETYPE, FILE1_SHA1, FILE1_MD5, FILE1_SIZE);

        // Nesting depth plus 2 for the final work and its file
        assertClickCount(nestingDepth + 2);
    }

    @Test
    public void addDescriptionTest() throws IOException {
        File modsFolder = job.getDescriptionDir();
        modsFolder.mkdir();

        PID folderPid = pidMinter.mintContentPid();
        File modsFile = new File(modsFolder, folderPid.getUUID() + ".xml");
        modsFile.createNewFile();

        String label = "testfolder";

        // Construct the deposit model, containing a deposit with one empty folder
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        // Constructing the folder in the deposit model with a title
        Bag folderBag = model.createBag(folderPid.getRepositoryPath());
        folderBag.addProperty(RDF.type, Cdr.Folder);
        folderBag.addProperty(CdrDeposit.label, label);

        depBag.add(folderBag);

        job.closeModel();

        job.run();

        ContentContainerObject destObj = (ContentContainerObject) repoObjLoader.getRepositoryObject(destinationPid);
        List<ContentObject> destMembers = destObj.getMembers();
        // Make sure that the folder is present and is actually a folder
        FolderObject folderObj = (FolderObject) findContentObjectByPid(destMembers, folderPid);

        assertNotNull(folderObj.getDescription());
    }

    @Test
    public void noDescriptionAddedTest() {
        PID folderPid = pidMinter.mintContentPid();

        String label = "testfolder";

        // Construct the deposit model, containing a deposit with one empty folder
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        // Constructing the folder in the deposit model with a title
        Bag folderBag = model.createBag(folderPid.getRepositoryPath());
        folderBag.addProperty(RDF.type, Cdr.Folder);
        folderBag.addProperty(CdrDeposit.label, label);

        depBag.add(folderBag);

        job.closeModel();

        job.run();

        ContentContainerObject destObj = (ContentContainerObject) repoObjLoader.getRepositoryObject(destinationPid);
        List<ContentObject> destMembers = destObj.getMembers();
        // Make sure that the folder is present and is actually a folder
        FolderObject folderObj = (FolderObject) findContentObjectByPid(destMembers, folderPid);

        assertNull(folderObj.getDescription());
    }

    @Test
    public void addPremisEventsTest() throws IOException {
        File premisEventsDir = job.getEventsDirectory();
        premisEventsDir.mkdir();

        PID folderObjPid = pidMinter.mintContentPid();

        File premisEventsFile = new File(premisEventsDir, folderObjPid.getUUID() + ".ttl");
        premisEventsFile.createNewFile();

        String label = "testfolder";

        // Construct the deposit model, containing a deposit with one empty folder
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        // Constructing the folder in the deposit model with a title
        Bag folderBag = model.createBag(folderObjPid.getRepositoryPath());
        folderBag.addProperty(RDF.type, Cdr.Folder);
        folderBag.addProperty(CdrDeposit.label, label);

        depBag.add(folderBag);

        FilePremisLogger premisLogger = new FilePremisLogger(folderObjPid, premisEventsFile, pidMinter,
                repoObjLoader, repoObjFactory, null);
        // build event 1
        premisLogger.buildEvent(Premis.Normalization)
                .addEventDetail("Event 1")
                .addAuthorizingAgent(SoftwareAgent.depositService.getFullname())
                .write();
        // build event 2
        premisLogger.buildEvent(Premis.VirusCheck)
                .addEventDetail("Event 2")
                .addSoftwareAgent(SoftwareAgent.clamav.getFullname())
                .write();

        job.closeModel();

        job.run();

        FolderObject folder = repoObjLoader.getFolderObject(folderObjPid);
        List<PremisEventObject> events = folder.getPremisLog().getEvents();
        // there should be three events total: the ingestion event, plus the two added in the test
        assertEquals(3, events.size());
        assertTrue(events.contains(findPremisEventByType(events, Premis.Normalization)));
        assertTrue(events.contains(findPremisEventByType(events, Premis.VirusCheck)));
    }

    @Test
    public void onlyIngestionEventAddedTest() throws IOException {
        File premisEventsDir = job.getEventsDirectory();
        premisEventsDir.mkdir();

        PID folderObjPid = pidMinter.mintContentPid();

        File premisEventsFile = new File(premisEventsDir, folderObjPid.getUUID() + ".xml");
        premisEventsFile.createNewFile();

        String label = "testfolder";

        // Construct the deposit model, containing a deposit with one empty folder
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        // Constructing the folder in the deposit model with a title
        Bag folderBag = model.createBag(folderObjPid.getRepositoryPath());
        folderBag.addProperty(RDF.type, Cdr.Folder);
        folderBag.addProperty(CdrDeposit.label, label);

        depBag.add(folderBag);

        job.closeModel();

        job.run();

        FolderObject folder = repoObjLoader.getFolderObject(folderObjPid);
        List<PremisEventObject> events = folder.getPremisLog().getEvents();
        assertEquals(1, events.size());
    }

    private void assertBinaryProperties(FileObject fileObj, String loc, String mimetype,
            String sha1, String md5, long size) {
        BinaryObject binary = fileObj.getOriginalFile();
        assertEquals(loc, binary.getFilename());
        if (sha1 != null) {
            assertEquals("urn:sha1:" + sha1, binary.getSha1Checksum());
        } else {
            assertNotNull(binary.getSha1Checksum());
        }
        // md5 isn't required, so not all tests will need to ensure it isn't null
        // TODO Reenable check once issue in fcrepo 4.7.4 is resolved
//        if (md5 != null) {
//            assertEquals("urn:md5:" + md5, binary.getMd5Checksum());
//        }
        assertEquals(size, binary.getFilesize().longValue());
        assertEquals(mimetype, binary.getMimetype());
    }

    private void assertClickCount(int count) {
        Map<String, String> jobStatus = jobStatusFactory.get(jobUUID);
        assertEquals(count, Integer.parseInt(jobStatus.get(JobField.num.name())));
    }

    private PID addFileObject(Bag parent, String stagingLocation,
                String mimetype, String sha1, String md5) throws Exception {
        PID filePid = pidMinter.mintContentPid();

        Resource fileResc = parent.getModel().createResource(filePid.getRepositoryPath());
        fileResc.addProperty(RDF.type, Cdr.FileObject);
        if (stagingLocation != null) {
            fileResc.addProperty(CdrDeposit.stagingLocation, Paths.get(depositDir.getAbsolutePath(),
                    stagingLocation).toUri().toString());
        }
        fileResc.addProperty(CdrDeposit.mimetype, mimetype);
        if (sha1 != null) {
            fileResc.addProperty(CdrDeposit.sha1sum, sha1);
        }
        if (md5 != null) {
            fileResc.addProperty(CdrDeposit.md5sum, md5);
        }

        parent.add(fileResc);

        // Create the accompanying fake premis report file
        new File(techmdDir, filePid.getUUID() + ".xml").createNewFile();

        return filePid;
    }

    private PID addWorkWithFileObject(Bag parent, String stagingLocation,
            String mimetype, String sha1, String md5) throws Exception {
        Model model = parent.getModel();
        // Constructing the work in the deposit model with a label
        PID workPid = pidMinter.mintContentPid();
        Bag workBag = model.createBag(workPid.getRepositoryPath());
        workBag.addProperty(RDF.type, Cdr.Work);
        workBag.addProperty(CdrDeposit.label, "testwork");

        PID fileObjPid = addFileObject(workBag, stagingLocation, mimetype, sha1, md5);
        workBag.addProperty(Cdr.primaryObject, createResource(fileObjPid.getRepositoryPath()));
        parent.add(workBag);

        return fileObjPid;

    }

    private PremisEventObject findPremisEventByType(List<PremisEventObject> objs, final Resource type) {
        return objs.stream()
                .filter(p -> p.getResource().getPropertyResourceValue(Premis.hasEventType).equals(type)).findAny().get();
    }

    private PremisEventObject findPremisEventByEventDetail(List<PremisEventObject> objs, final String message) {
        return objs.stream()
                .filter(p -> p.getResource().hasProperty(Premis.hasEventDetail, message)).findAny().get();
    }

}

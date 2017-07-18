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

import static edu.unc.lib.dl.fcrepo4.RepositoryPathConstants.TECHNICAL_METADATA;
import static edu.unc.lib.dl.test.TestHelpers.setField;
import static edu.unc.lib.dl.util.DepositConstants.TECHMD_DIR;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.deposit.work.JobFailedException;
import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.service.AccessControlService;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.Permission;
import edu.unc.lib.dl.event.PremisEventBuilder;
import edu.unc.lib.dl.event.PremisLogger;
import edu.unc.lib.dl.event.PremisLoggerFactory;
import edu.unc.lib.dl.fcrepo4.BinaryObject;
import edu.unc.lib.dl.fcrepo4.ContentObject;
import edu.unc.lib.dl.fcrepo4.FileObject;
import edu.unc.lib.dl.fcrepo4.FolderObject;
import edu.unc.lib.dl.fcrepo4.Repository;
import edu.unc.lib.dl.fcrepo4.RepositoryPathConstants;
import edu.unc.lib.dl.fcrepo4.TransactionCancelledException;
import edu.unc.lib.dl.fcrepo4.WorkObject;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.rdf.CdrDeposit;
import edu.unc.lib.dl.test.SelfReturningAnswer;
import edu.unc.lib.dl.util.RedisWorkerConstants.DepositField;

/**
 * 
 * @author bbpennel
 *
 */
public class IngestContentObjectsJobTest extends AbstractDepositJobTest {

    private IngestContentObjectsJob job;

    private PID depositPid;

    private PID destinationPid;

    private FolderObject destinationObj;

    @Mock
    private PremisLoggerFactory mockPremisLoggerFactory;
    
    @Mock
    private PremisLogger mockPremisLogger;
    
    private PremisEventBuilder mockPremisEventBuilder;

    @Mock
    private AccessControlService aclService;

    @Mock
    private FileObject mockFileObj;
    
    @Mock
    private BinaryObject mockBinaryObj;

    private File techmdDir;

    @Before
    public void init() throws Exception {
        Dataset dataset = TDBFactory.createDataset();

        job = new IngestContentObjectsJob();
        job.setJobUUID(jobUUID);
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        job.setRepository(repository);
        setField(job, "premisLoggerFactory", mockPremisLoggerFactory);
        setField(job, "aclService", aclService);
        setField(job, "dataset", dataset);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "jobStatusFactory", jobStatusFactory);
        setField(job, "metricsClient", metricsClient);
        job.init();

        depositPid = job.getDepositPID();

        setupDestination();

        FileUtils.copyDirectory(new File("src/test/resources/examples"), depositDir);

        techmdDir = new File(depositDir, TECHMD_DIR);
        techmdDir.mkdir();
        
        // Setup logging dependencies
        mockPremisEventBuilder = mock(PremisEventBuilder.class, new SelfReturningAnswer());
        when(mockPremisLoggerFactory.createPremisLogger(any(PID.class), any(File.class), any(Repository.class)))
                .thenReturn(mockPremisLogger);
        when(mockPremisLogger.buildEvent(any(Resource.class))).thenReturn(mockPremisEventBuilder);

        when(mockFileObj.getOriginalFile()).thenReturn(mockBinaryObj);
    }

    private void setupDestination() {
        // Establish a destination object to deposit to
        destinationPid = makePid(RepositoryPathConstants.CONTENT_BASE);

        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.containerId.name(), destinationPid.getQualifiedId());
        depositStatus.put(DepositField.permissionGroups.name(), "depositor");

        when(depositStatusFactory.get(anyString())).thenReturn(depositStatus);

        destinationObj = mock(FolderObject.class);
        when(destinationObj.getPid()).thenReturn(destinationPid);
        when(repository.getContentObject(eq(destinationPid))).thenReturn(destinationObj);
    }

    /**
     * Test that an empty folder is successfully deposited
     */
    @Test
    public void ingestEmptyFolderTest() {

        FolderObject folder = mock(FolderObject.class);
        when(repository.createFolderObject(any(PID.class), any(Model.class))).thenReturn(folder);
        
        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        PID folderPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        Bag folderBag = model.createBag(folderPid.getRepositoryPath());
        folderBag.addProperty(RDF.type, Cdr.Folder);
        when(folder.getPid()).thenReturn(folderPid);

        depBag.add(folderBag);

        job.closeModel();

        job.run();

        verify(repository).createFolderObject(eq(folderPid), any(Model.class));
        verify(destinationObj).addMember(eq(folder));
        verify(jobStatusFactory).incrCompletion(eq(jobUUID), eq(1));
    }

    private Bag setupWork(PID workPid, WorkObject work, Model model) {
        when(repository.createWorkObject(any(PID.class), any(Model.class))).thenReturn(work);
        when(work.getPid()).thenReturn(workPid);

        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        Bag workBag = model.createBag(workPid.getRepositoryPath());
        workBag.addProperty(RDF.type, Cdr.Work);

        depBag.add(workBag);

        return workBag;
    }

    /**
     * Test that a deposit involving a work containing two files is successful
     */
    @Test
    public void ingestWorkObjectTest() throws Exception {
        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        Model model = job.getWritableModel();
        WorkObject work = mock(WorkObject.class);
        Bag workBag = setupWork(workPid, work, model);

        String mainLoc = "pdf.pdf";
        String mainMime = "application/pdf";
        PID mainPid = addFileObject(workBag, mainLoc, mainMime);
        String supLoc = "text.txt";
        String supMime = "text/plain";
        PID supPid = addFileObject(workBag, supLoc, supMime);

        workBag.asResource().addProperty(Cdr.primaryObject,
                model.getResource(mainPid.getRepositoryPath()));

        job.closeModel();

        when(work.addDataFile(any(PID.class), any(InputStream.class), anyString(), anyString(), anyString()))
                .thenReturn(mockFileObj);
        when(mockFileObj.getPid()).thenReturn(mainPid).thenReturn(supPid);

        job.run();

        verify(repository).createWorkObject(eq(workPid), any(Model.class));
        verify(destinationObj).addMember(eq(work));

        verify(work).addDataFile(eq(mainPid), any(InputStream.class), eq(mainLoc),
                eq(mainMime), anyString());
        verify(work).addDataFile(eq(supPid), any(InputStream.class), eq(supLoc),
                eq(supMime), anyString());
        verify(work).setPrimaryObject(mainPid);

        verify(jobStatusFactory, times(3)).incrCompletion(eq(jobUUID), eq(1));

        verify(mockFileObj, times(2)).addBinary(eq(TECHNICAL_METADATA), any(InputStream.class),
                anyString(), anyString(), any(Property.class), eq(DCTerms.conformsTo),
                any(Resource.class));
    }

    /**
     * Test that the deposit fails if a file object is specified but not given a
     * staging location
     */
    @Test(expected = TransactionCancelledException.class)
    public void ingestWorkWithFileWithoutLocation() throws Exception {
        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        Model model = job.getWritableModel();
        WorkObject work = mock(WorkObject.class);
        Bag workBag = setupWork(workPid, work, model);

        PID filePid = makePid(RepositoryPathConstants.CONTENT_BASE);

        Resource fileResc = model.createResource(filePid.getRepositoryPath());
        fileResc.addProperty(RDF.type, Cdr.FileObject);
        fileResc.addProperty(CdrDeposit.mimetype, "text/plain");
        workBag.add(fileResc);

        job.closeModel();

        job.run();
    }

    /**
     * Test that deposit fails if depositing a file path that doesn't exist
     */
    @Test(expected = TransactionCancelledException.class)
    public void ingestWorkWithFileDoesNotExist() throws Exception {
        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        Model model = job.getWritableModel();
        WorkObject work = mock(WorkObject.class);
        Bag workBag = setupWork(workPid, work, model);

        addFileObject(workBag, "doesnotexist.txt", "text/plain");

        job.closeModel();

        job.run();
    }

    /**
     * Test that deposit fails when no permission to write to destination
     */
    @Test(expected = AccessRestrictionException.class)
    public void ingestFailNoPermissionsTest() throws Exception {
        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        Model model = job.getWritableModel();
        WorkObject work = mock(WorkObject.class);
        Bag workBag = setupWork(workPid, work, model);

        addFileObject(workBag, "pdf.pdf", "application/pdf");

        doThrow(new AccessRestrictionException()).when(aclService)
                .assertHasAccess(anyString(), eq(destinationPid),
                        any(AccessGroupSet.class), eq(Permission.ingest));

        job.closeModel();

        job.run();
    }

    /**
     * Test that a standalone file object ingested to a destination which is not
     * a work successfully ingests as the child of a generated work object
     */
    @Test
    public void ingestFileObjectAsWorkTest() throws Exception {
        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        when(repository.mintContentPid()).thenReturn(workPid);
        WorkObject work = mock(WorkObject.class);
        when(work.getPid()).thenReturn(workPid);
        when(repository.createWorkObject(eq(workPid), any(Model.class))).thenReturn(work);

        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        String fileLoc = "text.txt";
        String fileMime = "text/plain";
        PID filePid = addFileObject(depBag, fileLoc, fileMime);

        job.closeModel();

        when(work.addDataFile(any(PID.class), any(InputStream.class), anyString(), anyString(), anyString()))
                .thenReturn(mockFileObj);
        when(mockFileObj.getPid()).thenReturn(filePid);
        job.run();

        // Verify that a work object was generated and added to the destination
        verify(repository).mintContentPid();
        verify(repository).createWorkObject(eq(workPid), any(Model.class));
        verify(destinationObj).addMember(eq(work));

        // Verify that a FileObject was added to the generated work as primary obj
        verify(work).setPrimaryObject(filePid);
        verify(work).addDataFile(eq(filePid), any(InputStream.class), eq(fileLoc),
                eq(fileMime), anyString());

        // Only one ticket should register since the work object is generated
        verify(jobStatusFactory).incrCompletion(eq(jobUUID), eq(1));
    }

    @Test(expected = JobFailedException.class)
    public void ingestfileObjectAsWorkNoStaging() throws Exception {
        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        when(repository.mintContentPid()).thenReturn(workPid);
        WorkObject work = mock(WorkObject.class);
        when(work.getPid()).thenReturn(workPid);
        when(repository.createWorkObject(eq(workPid), any(Model.class))).thenReturn(work);

        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        PID filePid = makePid(RepositoryPathConstants.CONTENT_BASE);

        Resource fileResc = model.createResource(filePid.getRepositoryPath());
        fileResc.addProperty(RDF.type, Cdr.FileObject);
        fileResc.addProperty(CdrDeposit.mimetype, "text/plain");
        depBag.add(fileResc);

        job.closeModel();

        job.run();
    }

    /**
     * Test resuming a Work ingest where the work and the main file were already
     * ingested
     */
    @Test
    public void resumeIngestWorkObjectTest() throws Exception {
        // Mark the deposit as resumed
        when(depositStatusFactory.isResumedDeposit(anyString())).thenReturn(true);

        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        Model model = job.getWritableModel();
        WorkObject work = mock(WorkObject.class);
        Bag workBag = setupWork(workPid, work, model);
        when(repository.getWorkObject(eq(workPid))).thenReturn(work);

        Model workModel = ModelFactory.createDefaultModel();
        Resource workResc = workModel.createResource(workPid.getRepositoryPath())
                .addProperty(RDF.type, Cdr.Work);
        when(work.getResource()).thenReturn(workResc);

        String mainLoc = "pdf.pdf";
        String mainMime = "application/pdf";
        PID mainPid = addFileObject(workBag, mainLoc, mainMime);
        String supLoc = "text.txt";
        String supMime = "text/plain";
        PID supPid = addFileObject(workBag, supLoc, supMime);

        workBag.asResource().addProperty(Cdr.primaryObject,
                model.getResource(mainPid.getRepositoryPath()));

        job.closeModel();

        when(work.addDataFile(any(PID.class), any(InputStream.class), anyString(), anyString(), anyString()))
                .thenReturn(mockFileObj);
        when(mockFileObj.getPid()).thenReturn(mainPid).thenReturn(supPid);

        when(repository.objectExists(eq(workPid))).thenReturn(true);
        when(repository.objectExists(eq(mainPid))).thenReturn(true);

        job.run();

        // Check that the work object was retrieved rather than created
        verify(repository).objectExists(eq(workPid));
        verify(repository, never()).createWorkObject(any(PID.class));
        verify(repository).getWorkObject(any(PID.class));

        // Main file object should not be touched
        verify(repository).objectExists(eq(mainPid));
        verify(work, never()).addDataFile(eq(mainPid), any(InputStream.class),
                anyString(), anyString(), anyString());
        verify(repository, never()).getFileObject(eq(mainPid));

        // Supplemental file should be created
        verify(repository).objectExists(eq(supPid));
        verify(repository, never()).getFileObject(eq(supPid));
        verify(work).addDataFile(eq(supPid), any(InputStream.class), eq(supLoc),
                eq(supMime), anyString());

        // Ensure that the primary object still got set
        verify(work).setPrimaryObject(mainPid);

        verify(jobStatusFactory).setCompletion(eq(jobUUID), eq(2));
        verify(jobStatusFactory).incrCompletion(eq(jobUUID), eq(1));
    }

    @Test
    public void resumeIngestFileObjectAsWorkTest() throws Exception {
        // Mark the deposit as resumed
        when(depositStatusFactory.isResumedDeposit(anyString())).thenReturn(true);

        Model model = job.getWritableModel();
        Bag depBag = model.createBag(depositPid.getRepositoryPath());

        String fileLoc = "text.txt";
        String fileMime = "text/plain";
        PID filePid = addFileObject(depBag, fileLoc, fileMime);

        when(repository.objectExists(eq(filePid))).thenReturn(true);

        job.closeModel();

        job.run();

        // Check that the generated work was neither generated nor linked
        verify(repository, never()).createWorkObject(any(PID.class), any(Model.class));
        verify(destinationObj, never()).addMember(any(ContentObject.class));

        // Only tick should be from preprocessing
        verify(jobStatusFactory, never()).incrCompletion(eq(jobUUID), anyInt());
        verify(jobStatusFactory).setCompletion(eq(jobUUID), eq(1));
    }

    /**
     * Check that no significant deposit behaviors change with a file object as
     * work deposit due to resume being turned on
     */
    @Test
    public void resumeIngestNoCompleteTest() throws Exception {
        // Mark the deposit as resumed
        when(depositStatusFactory.isResumedDeposit(anyString())).thenReturn(true);

        ingestFileObjectAsWorkTest();

        // No preprocessing ticks
        verify(jobStatusFactory).setCompletion(eq(jobUUID), eq(0));
    }
    
    private PID addFileObject(Bag parent, String stagingLocation, String mimetype) throws Exception {
        PID filePid = makePid(RepositoryPathConstants.CONTENT_BASE);

        String absolutePath = Paths.get(depositDir.getAbsolutePath(), stagingLocation).toUri().toString();
        Resource fileResc = parent.getModel().createResource(filePid.getRepositoryPath());
        fileResc.addProperty(RDF.type, Cdr.FileObject);
        fileResc.addProperty(CdrDeposit.stagingLocation, absolutePath);
        fileResc.addProperty(CdrDeposit.mimetype, mimetype);

        parent.add(fileResc);

        // Create the accompanying fake FITS report file
        new File(techmdDir, filePid.getUUID() + ".xml").createNewFile();

        return filePid;
    }
}

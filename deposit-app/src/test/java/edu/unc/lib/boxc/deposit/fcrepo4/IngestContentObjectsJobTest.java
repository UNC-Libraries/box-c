package edu.unc.lib.boxc.deposit.fcrepo4;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static edu.unc.lib.boxc.common.test.TestHelpers.setField;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.api.DatastreamType.TECHNICAL_METADATA;
import static edu.unc.lib.boxc.model.api.StreamingConstants.STREAMREAPER_PREFIX_URL;
import static edu.unc.lib.boxc.persist.impl.storage.StorageLocationTestHelper.LOC1_ID;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.HeadBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.common.test.SelfReturningAnswer;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositField;
import edu.unc.lib.boxc.deposit.api.RedisWorkerConstants.DepositState;
import edu.unc.lib.boxc.deposit.impl.model.DepositModelHelpers;
import edu.unc.lib.boxc.deposit.validate.VerifyObjectsAreInFedoraService;
import edu.unc.lib.boxc.deposit.work.JobFailedException;
import edu.unc.lib.boxc.deposit.work.JobInterruptedException;
import edu.unc.lib.boxc.fcrepo.exceptions.ChecksumMismatchException;
import edu.unc.lib.boxc.fcrepo.exceptions.TransactionCancelledException;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.event.PremisLog;
import edu.unc.lib.boxc.model.api.exceptions.FedoraException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentContainerObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;
import edu.unc.lib.boxc.model.api.rdf.CdrDeposit;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectFactoryImpl;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryObjectLoaderImpl;
import edu.unc.lib.boxc.operations.api.events.PremisEventBuilder;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.boxc.persist.api.storage.StorageLocation;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferSession;

/**
 *
 * @author bbpennel
 *
 */
public class IngestContentObjectsJobTest extends AbstractDepositJobTest {

    private IngestContentObjectsJob job;

    private PID depositPid;

    private PID destinationPid;

    private Bag depBag;

    private Model model;

    private ContentContainerObject destinationObj;

    @Mock
    private PremisLoggerFactory mockPremisLoggerFactory;

    @Mock
    private RepositoryObjectLoaderImpl repoObjLoader;
    @Mock
    private RepositoryObjectFactoryImpl repoObjFactory;
    @Mock
    private TransactionManager txManager;
    @Mock
    private FcrepoClient fcrepoClient;

    @Mock
    private FedoraTransaction mockTx;

    @Mock
    private HeadBuilder headBuilder;

    @Mock
    private PremisLogger mockPremisLogger;
    @Mock
    private PremisLog mockPremisLog;

    private PremisEventBuilder mockPremisEventBuilder;

    @Mock
    private AccessControlService aclService;

    @Mock
    private FileObject mockFileObj;
    @Mock
    private FileObject streamingFileObj;

    @Mock
    private BinaryObject mockBinaryObj;

    @Mock
    private BinaryTransferService binaryTransferService;
    @Mock
    private StorageLocationManager storageLocationManager;
    @Mock
    private StorageLocation storageLocation;
    @Mock
    private BinaryTransferSession mockTransferSession;
    @Mock
    private UpdateDescriptionService updateDescService;
    @Captor
    private ArgumentCaptor<Model> modelCaptor;

    private Path storageLocPath;

    private static final String STREAMING_TYPE = "video";

    @Mock
    private VerifyObjectsAreInFedoraService verificationService;

    @BeforeEach
    public void init() throws Exception {
        job = new IngestContentObjectsJob();
        job.setJobUUID(jobUUID);
        job.setDepositUUID(depositUUID);
        job.setDepositDirectory(depositDir);
        setField(job, "premisLoggerFactory", mockPremisLoggerFactory);
        setField(job, "aclService", aclService);
        setField(job, "depositModelManager", depositModelManager);
        setField(job, "depositsDirectory", depositsDirectory);
        setField(job, "depositStatusFactory", depositStatusFactory);
        setField(job, "jobStatusFactory", jobStatusFactory);
        setField(job, "metricsClient", metricsClient);
        setField(job, "pidMinter", pidMinter);
        setField(job, "repoObjLoader", repoObjLoader);
        setField(job, "repoObjFactory", repoObjFactory);
        setField(job, "fcrepoClient", fcrepoClient);
        setField(job, "txManager", txManager);
        setField(job, "verificationService", verificationService);
        setField(job, "transferService", binaryTransferService);
        setField(job, "locationManager", storageLocationManager);
        setField(job, "updateDescService", updateDescService);
        setField(job, "depositModelManager", depositModelManager);

        job.init();

        depositPid = job.getDepositPID();

        setupDestination();

        FileUtils.copyDirectory(new File("src/test/resources/examples"), depositDir);

        storageLocPath = tmpFolder.resolve("storageLoc");
        Files.createDirectory(storageLocPath);

        // Setup logging dependencies
        mockPremisEventBuilder = mock(PremisEventBuilder.class, new SelfReturningAnswer());
        when(mockPremisLoggerFactory.createPremisLogger(any())).thenReturn(mockPremisLogger);
        when(mockPremisLoggerFactory.createPremisLogger(any(PID.class), any(File.class)))
                .thenReturn(mockPremisLogger);
        when(mockPremisLogger.buildEvent(any(Resource.class))).thenReturn(mockPremisEventBuilder);

        when(mockFileObj.getOriginalFile()).thenReturn(mockBinaryObj);

        // Get a writeable model
        model = job.getWritableModel();
        depBag = model.createBag(depositPid.getRepositoryPath());

        when(fcrepoClient.head(any(URI.class))).thenReturn(headBuilder);

        when(txManager.startTransaction()).thenReturn(mockTx);
        doThrow(new TransactionCancelledException()).when(mockTx).cancel(any(Exception.class));

        depBag.addProperty(Cdr.storageLocation, LOC1_ID);
        when(storageLocationManager.getStorageLocationById(anyString())).thenReturn(storageLocation);
        when(binaryTransferService.getSession(any(StorageLocation.class))).thenReturn(mockTransferSession);
    }

    private void setupDestination() {
        // Establish a destination object to deposit to
        destinationPid = makePid(RepositoryPathConstants.CONTENT_BASE);

        Map<String, String> depositStatus = new HashMap<>();
        depositStatus.put(DepositField.containerId.name(), destinationPid.getQualifiedId());
        depositStatus.put(DepositField.permissionGroups.name(), "depositor");
        depositStatus.put(DepositField.depositorName.name(), "depositor");

        when(depositStatusFactory.get(anyString())).thenReturn(depositStatus);

        destinationObj = mock(FolderObject.class);
        when(destinationObj.getPremisLog()).thenReturn(mockPremisLog);
        when(destinationObj.getPid()).thenReturn(destinationPid);
        when(repoObjLoader.getRepositoryObject(eq(destinationPid))).thenReturn(destinationObj);
    }

    /**
     * Test that an empty folder is successfully deposited
     */
    @Test
    public void ingestEmptyFolderTest() {
        FolderObject folder = mock(FolderObject.class);
        when(repoObjFactory.createFolderObject(any(PID.class), any(Model.class))).thenReturn(folder);

        PID folderPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        Bag folderBag = model.createBag(folderPid.getRepositoryPath());
        folderBag.addProperty(RDF.type, Cdr.Folder);
        when(folder.getPid()).thenReturn(folderPid);

        depBag.add(folderBag);

        job.closeModel();

        job.run();

        verify(repoObjFactory).createFolderObject(eq(folderPid), any(Model.class));
        verify(destinationObj).addMember(eq(folder));
        verify(jobStatusFactory).incrCompletion(eq(jobUUID), eq(1));
    }

    private Bag setupWork(PID workPid, WorkObject work) {
        when(repoObjFactory.createWorkObject(any(PID.class), any(Model.class))).thenReturn(work);
        when(work.getPid()).thenReturn(workPid);

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
        WorkObject work = mock(WorkObject.class);
        Bag workBag = setupWork(workPid, work);

        String mainLoc = "pdf.pdf";
        String mainMime = "application/pdf";
        PID mainPid = addFileObject(workBag, mainLoc, mainMime);
        String supLoc = "text.txt";
        String supMime = "text/plain";
        PID supPid = addFileObject(workBag, supLoc, supMime);

        workBag.asResource().addProperty(Cdr.primaryObject,
                model.getResource(mainPid.getRepositoryPath()));

        job.closeModel();

        when(work.addDataFile(any(PID.class), any(URI.class),
                anyString(), anyString(), isNull(), isNull(), any(Model.class)))
                .thenReturn(mockFileObj);
        when(mockFileObj.getPid()).thenReturn(mainPid).thenReturn(supPid);
        when(repoObjLoader.getWorkObject(eq(workPid))).thenReturn(work);

        job.run();

        verify(repoObjFactory).createWorkObject(eq(workPid), any(Model.class));
        verify(destinationObj).addMember(eq(work));

        verify(work).addDataFile(eq(mainPid), any(URI.class), eq(mainLoc),
                eq(mainMime), isNull(), isNull(), any(Model.class));
        verify(work).addDataFile(eq(supPid), any(URI.class), eq(supLoc),
                eq(supMime), isNull(), isNull(), any(Model.class));
        verify(work).setPrimaryObject(mainPid);

        // Add work and file count
        verify(jobStatusFactory, times(3)).incrCompletion(eq(jobUUID), eq(1));

        verify(mockFileObj, times(2)).addBinary(any(PID.class), any(URI.class),
                anyString(), anyString(), isNull(String.class), isNull(String.class),
                any(Property.class), eq(DCTerms.conformsTo), any(Resource.class));
    }

    /**
     * Test that ingest will work if a file object has streaming properties and no
     * original file and staging location
     */
    @Test
    public void ingestWorkWithFileWithStreamingPropertiesNoOriginalFile() {
        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        WorkObject work = mock(WorkObject.class);
        Bag workBag = setupWork(workPid, work);

        PID filePid = makePid(RepositoryPathConstants.CONTENT_BASE);

        Resource fileResc = model.createResource(filePid.getRepositoryPath());
        fileResc.addProperty(RDF.type, Cdr.FileObject);
        fileResc.addProperty(CdrDeposit.mimetype, "text/plain");
        fileResc.addProperty(Cdr.streamingUrl, STREAMREAPER_PREFIX_URL);
        fileResc.addProperty(Cdr.streamingUrl, STREAMING_TYPE);
        workBag.add(fileResc);

        job.closeModel();

        when(repoObjLoader.getWorkObject(eq(workPid))).thenReturn(work);
        when(repoObjFactory.createFileObject(eq(filePid), any())).thenReturn(streamingFileObj);
        when(streamingFileObj.getPid()).thenReturn(filePid);
        when(streamingFileObj.getOriginalFile()).thenThrow(NotFoundException.class);

        job.run();

        verify(repoObjFactory).createWorkObject(eq(workPid), any(Model.class));
        verify(destinationObj).addMember(eq(work));

        verify(jobStatusFactory, times(2)).incrCompletion(eq(jobUUID), eq(1));
    }

    /**
     * Test that ingest will work if a file object both streaming properties and
     * original file and staging location
     */
    @Test
    public void ingestWorkWithFileWithStreamingPropertiesAndOriginalFile() throws Exception {
        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        WorkObject work = mock(WorkObject.class);
        Bag workBag = setupWork(workPid, work);

        String loc = "pdf.pdf";
        String mime = "application/pdf";
        PID filePid = addFileObject(workBag, loc, mime);

        var fileResc = model.getResource(filePid.getRepositoryPath());
        fileResc.addProperty(RDF.type, Cdr.FileObject);
        fileResc.addProperty(CdrDeposit.mimetype, "text/plain");
        fileResc.addProperty(Cdr.streamingUrl, STREAMREAPER_PREFIX_URL);
        fileResc.addProperty(Cdr.streamingType, STREAMING_TYPE);
        workBag.add(fileResc);

        job.closeModel();

        when(work.addDataFile(any(PID.class), any(URI.class),
                anyString(), anyString(), isNull(), isNull(), any(Model.class)))
                .thenReturn(mockFileObj);
        when(mockFileObj.getPid()).thenReturn(filePid);
        when(repoObjLoader.getWorkObject(eq(workPid))).thenReturn(work);

        job.run();

        verify(repoObjFactory).createWorkObject(eq(workPid), any(Model.class));
        verify(destinationObj).addMember(eq(work));

        verify(jobStatusFactory, times(3)).incrCompletion(eq(jobUUID), eq(1));
    }

    /**
     * Test that deposit fails if depositing a file path that doesn't exist
     */
    @Test
    public void ingestWorkWithFileDoesNotExist() throws Exception {
        Assertions.assertThrows(JobFailedException.class, () -> {
            PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
            WorkObject work = mock(WorkObject.class);
            Bag workBag = setupWork(workPid, work);

            addFileObject(workBag, "doesnotexist.txt", "text/plain");

            job.closeModel();

            when(work.addDataFile(any(PID.class), any(URI.class),
                    anyString(), anyString(), isNull(), isNull(), any(Model.class)))
                    .thenThrow(new FedoraException("Fail"))
                    .thenReturn(mockFileObj);
            when(repoObjLoader.getWorkObject(eq(workPid))).thenReturn(work);

            job.run();
        });
    }

    /**
     * Test that deposit fails when no permission to write to destination
     */
    @Test
    public void ingestFailNoPermissionsTest() throws Exception {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
            WorkObject work = mock(WorkObject.class);
            Bag workBag = setupWork(workPid, work);

            addFileObject(workBag, "pdf.pdf", "application/pdf");

            doThrow(new AccessRestrictionException()).when(aclService)
                    .assertHasAccess(anyString(), eq(destinationPid),
                            any(AccessGroupSetImpl.class), eq(Permission.ingest));

            job.closeModel();

            job.run();
        });
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
        WorkObject work = mock(WorkObject.class);
        Bag workBag = setupWork(workPid, work);
        when(repoObjLoader.getWorkObject(eq(workPid))).thenReturn(work);

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

        when(work.addDataFile(any(PID.class), any(URI.class),
                anyString(), anyString(), isNull(), isNull(), any(Model.class)))
                .thenReturn(mockFileObj);
        when(mockFileObj.getPid()).thenReturn(mainPid).thenReturn(supPid);

        HeadBuilder notFoundBuilder = mock(HeadBuilder.class);
        when(fcrepoClient.head(eq(supPid.getRepositoryUri()))).thenReturn(notFoundBuilder);
        when(notFoundBuilder.perform()).thenThrow(new FcrepoOperationFailedException(
                destinationPid.getRepositoryUri(), HttpStatus.SC_NOT_FOUND, ""));

        job.run();

        // Check that the work object was retrieved rather than created
        // verify(repository).objectExists(eq(workPid));
        verify(repoObjFactory, never()).createWorkObject(any(PID.class), any(Model.class));
        verify(repoObjLoader).getWorkObject(any(PID.class));

        // Main file object should not be touched
        verify(work, never()).addDataFile(eq(mainPid), any(URI.class),
                anyString(), anyString(), isNull(), isNull(), any(Model.class));
        verify(repoObjLoader, never()).getFileObject(eq(mainPid));

        // Supplemental file should be created
        verify(repoObjLoader, never()).getFileObject(eq(supPid));
        verify(work).addDataFile(eq(supPid), any(URI.class), eq(supLoc),
                eq(supMime), isNull(), isNull(), any(Model.class));

        // Ensure that the primary object still got set
        verify(work).setPrimaryObject(mainPid);

        verify(jobStatusFactory).setCompletion(eq(jobUUID), eq(2));
        verify(jobStatusFactory).setTotalCompletion(eq(jobUUID), eq(3));
    }

    @Test
    public void ingestWorkObjectWithTransientChecksumFailure() throws Exception {
        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        WorkObject work = mock(WorkObject.class);
        Bag workBag = setupWork(workPid, work);

        BinaryObject mockDescBin = mock(BinaryObject.class);
        when(mockDescBin.getContentUri()).thenReturn(URI.create("file://path/to/desc"));
        when(updateDescService.updateDescription(any(UpdateDescriptionRequest.class))).thenReturn(mockDescBin);

        Path modsPath = job.getModsPath(workPid, true);
        FileUtils.writeStringToFile(modsPath.toFile(), "Mods content", UTF_8);

        String mainLoc = "pdf.pdf";
        String mainMime = "application/pdf";
        PID mainPid = addFileObject(workBag, mainLoc, mainMime);
        String supLoc = "text.txt";
        String supMime = "text/plain";
        PID supPid = addFileObject(workBag, supLoc, supMime);

        workBag.asResource().addProperty(Cdr.primaryObject,
                model.getResource(mainPid.getRepositoryPath()));

        job.closeModel();

        when(work.addDataFile(any(PID.class), any(URI.class),
                anyString(), anyString(), isNull(), isNull(), any(Model.class)))
                .thenThrow(new ChecksumMismatchException("Temporarily bad"))
                .thenReturn(mockFileObj);
        when(mockFileObj.getPid()).thenReturn(mainPid).thenReturn(supPid);
        when(repoObjLoader.getWorkObject(eq(workPid))).thenReturn(work);

        job.run();

        verify(repoObjFactory).createWorkObject(eq(workPid), any(Model.class));
        verify(destinationObj).addMember(eq(work));

        verify(updateDescService).updateDescription(any(UpdateDescriptionRequest.class));

        verify(work, times(2)).addDataFile(eq(mainPid), any(URI.class), eq(mainLoc),
                eq(mainMime), isNull(), isNull(), any(Model.class));
        verify(work).addDataFile(eq(supPid), any(URI.class), eq(supLoc),
                eq(supMime), isNull(), isNull(), any(Model.class));
        verify(work).setPrimaryObject(mainPid);

        // Add work and file count
        verify(jobStatusFactory, times(3)).incrCompletion(eq(jobUUID), eq(1));

        verify(mockFileObj, times(2)).addBinary(any(PID.class), any(URI.class),
                anyString(), anyString(), isNull(String.class), isNull(String.class),
                any(Property.class), eq(DCTerms.conformsTo), any(Resource.class));
    }

    @Test
    public void pauseAndResumeTest() throws Exception {
        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        WorkObject work = mock(WorkObject.class);
        Bag workBag = setupWork(workPid, work);

        String mainLoc = "pdf.pdf";
        String mainMime = "application/pdf";
        PID mainPid = addFileObject(workBag, mainLoc, mainMime);

        workBag.addProperty(Cdr.primaryObject, model.getResource(mainPid.getRepositoryPath()));

        job.closeModel();

        when(work.addDataFile(any(PID.class), any(URI.class),
                anyString(), anyString(), isNull(), isNull(), any(Model.class)))
                .thenReturn(mockFileObj);
        when(mockFileObj.getPid()).thenReturn(mainPid);
        when(repoObjLoader.getWorkObject(eq(workPid))).thenReturn(work);

        when(depositStatusFactory.getState(depositUUID))
                .thenReturn(DepositState.running)
                .thenReturn(DepositState.running)
                .thenReturn(DepositState.paused);

        try {
            job.run();
            fail("Job must be interrupted due to pausing");
        } catch (JobInterruptedException e) {
            // expected
        }

        when(depositStatusFactory.getState(depositUUID))
                .thenReturn(DepositState.running);

        job.run();

        verify(repoObjFactory, times(2)).createWorkObject(eq(workPid), any(Model.class));
        verify(destinationObj, times(2)).addMember(eq(work));

        verify(work).addDataFile(eq(mainPid), any(URI.class), eq(mainLoc),
                eq(mainMime), isNull(), isNull(), any(Model.class));
        verify(work).setPrimaryObject(mainPid);

        verify(jobStatusFactory, times(2)).setTotalCompletion(eq(jobUUID), eq(2));
    }

    @Test
    public void ingestAdminUnitTest() {
        destinationObj = mock(ContentRootObject.class);
        when(destinationObj.getPid()).thenReturn(destinationPid);
        when(destinationObj.getPremisLog()).thenReturn(mockPremisLog);
        when(repoObjLoader.getRepositoryObject(eq(destinationPid))).thenReturn(destinationObj);

        AdminUnit adminUnit = mock(AdminUnit.class);
        when(repoObjFactory.createAdminUnit(any(PID.class), any(Model.class))).thenReturn(adminUnit);

        PID adminPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        Bag adminBag = model.createBag(adminPid.getRepositoryPath());
        adminBag.addProperty(RDF.type, Cdr.AdminUnit);
        adminBag.addProperty(CdrAcl.canManage, "staff");
        when(adminUnit.getPid()).thenReturn(adminPid);

        depBag.add(adminBag);

        job.closeModel();

        job.run();

        verify(repoObjFactory).createAdminUnit(eq(adminPid), modelCaptor.capture());
        verify(destinationObj).addMember(eq(adminUnit));
        verify(jobStatusFactory).incrCompletion(eq(jobUUID), eq(1));

        Resource adminAipResc = modelCaptor.getValue().getResource(adminPid.getRepositoryPath());
        assertTrue(adminAipResc.hasProperty(CdrAcl.canManage), "Admin object did not contain assigned restriction");
    }

    @Test
    public void ingestAdminUnitNoPermissionTest() {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            destinationObj = mock(ContentRootObject.class);
            when(destinationObj.getPid()).thenReturn(destinationPid);
            when(repoObjLoader.getRepositoryObject(eq(destinationPid))).thenReturn(destinationObj);

            // Throw access exception for creating admin unit
            doThrow(new AccessRestrictionException()).when(aclService)
                    .assertHasAccess(anyString(), eq(destinationPid), any(AccessGroupSetImpl.class),
                            eq(Permission.createAdminUnit));

            AdminUnit adminUnit = mock(AdminUnit.class);
            when(repoObjFactory.createAdminUnit(any(PID.class), any(Model.class))).thenReturn(adminUnit);

            PID adminPid = makePid(RepositoryPathConstants.CONTENT_BASE);
            Bag adminBag = model.createBag(adminPid.getRepositoryPath());
            adminBag.addProperty(RDF.type, Cdr.AdminUnit);
            when(adminUnit.getPid()).thenReturn(adminPid);

            depBag.add(adminBag);

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void ingestCollectionTest() {
        destinationObj = mock(AdminUnit.class);
        when(destinationObj.getPid()).thenReturn(destinationPid);
        when(destinationObj.getPremisLog()).thenReturn(mockPremisLog);
        when(repoObjLoader.getRepositoryObject(eq(destinationPid))).thenReturn(destinationObj);

        CollectionObject collection = mock(CollectionObject.class);
        when(repoObjFactory.createCollectionObject(any(PID.class), any(Model.class))).thenReturn(collection);

        PID collectionPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        Bag collectionBag = model.createBag(collectionPid.getRepositoryPath());
        collectionBag.addProperty(RDF.type, Cdr.Collection);
        collectionBag.addProperty(CdrAcl.canViewOriginals, AUTHENTICATED_PRINC);

        when(collection.getPid()).thenReturn(collectionPid);

        depBag.add(collectionBag);

        job.closeModel();

        job.run();

        verify(repoObjFactory).createCollectionObject(eq(collectionPid), modelCaptor.capture());
        verify(destinationObj).addMember(eq(collection));
        verify(jobStatusFactory).incrCompletion(eq(jobUUID), eq(1));

        Resource collAipResc = modelCaptor.getValue().getResource(collectionPid.getRepositoryPath());
        assertTrue(collAipResc.hasProperty(CdrAcl.canViewOriginals, AUTHENTICATED_PRINC),
                "Collection object did not contain assigned restriction");
    }

    @Test
    public void ingestCollectionNoPermisionTest() {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            destinationObj = mock(AdminUnit.class);
            when(destinationObj.getPid()).thenReturn(destinationPid);
            when(repoObjLoader.getRepositoryObject(eq(destinationPid))).thenReturn(destinationObj);

            // Throw access exception for creating collection
            doThrow(new AccessRestrictionException()).when(aclService)
                    .assertHasAccess(anyString(), eq(destinationPid), any(AccessGroupSetImpl.class),
                            eq(Permission.createCollection));

            CollectionObject collection = mock(CollectionObject.class);
            when(repoObjFactory.createCollectionObject(any(PID.class), any(Model.class))).thenReturn(collection);

            PID collectionPid = makePid(RepositoryPathConstants.CONTENT_BASE);
            Bag collectionBag = model.createBag(collectionPid.getRepositoryPath());
            collectionBag.addProperty(RDF.type, Cdr.Collection);
            when(collection.getPid()).thenReturn(collectionPid);

            depBag.add(collectionBag);

            job.closeModel();

            job.run();
        });
    }

    @Test
    public void ingestFolderWithAcls() {
        FolderObject folder = mock(FolderObject.class);
        when(repoObjFactory.createFolderObject(any(PID.class), any(Model.class))).thenReturn(folder);

        PID folderPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        when(folder.getPid()).thenReturn(folderPid);

        Bag folderBag = model.createBag(folderPid.getRepositoryPath());
        folderBag.addProperty(RDF.type, Cdr.Folder);
        folderBag.addProperty(CdrAcl.canViewMetadata, PUBLIC_PRINC);

        depBag.add(folderBag);

        job.closeModel();

        job.run();

        verify(repoObjFactory).createFolderObject(eq(folderPid), modelCaptor.capture());

        Model aipModel = modelCaptor.getValue();
        Resource aipResc = aipModel.getResource(folderPid.getRepositoryPath());
        assertTrue(aipResc.hasProperty(CdrAcl.canViewMetadata));
    }

    @Test
    public void ingestWorkWithFileObjWithAcls() throws Exception {
        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        WorkObject work = mock(WorkObject.class);
        Bag workBag = setupWork(workPid, work);
        workBag.addProperty(CdrAcl.embargoUntil, model.createTypedLiteral(Calendar.getInstance()));

        String mainLoc = "pdf.pdf";
        String mainMime = "application/pdf";
        PID mainPid = addFileObject(workBag, mainLoc, mainMime);
        model.getResource(mainPid.getRepositoryPath())
                .addProperty(CdrAcl.canViewOriginals, AUTHENTICATED_PRINC);

        workBag.addProperty(Cdr.primaryObject,
                model.getResource(mainPid.getRepositoryPath()));

        job.closeModel();

        when(work.addDataFile(any(PID.class), any(URI.class),
                anyString(), anyString(), isNull(), isNull(), any(Model.class)))
                .thenReturn(mockFileObj);
        when(mockFileObj.getPid()).thenReturn(mainPid);
        when(repoObjLoader.getWorkObject(eq(workPid))).thenReturn(work);

        job.run();

        verify(repoObjFactory).createWorkObject(eq(workPid), modelCaptor.capture());

        Resource workAipResc = modelCaptor.getValue().getResource(workPid.getRepositoryPath());
        assertTrue(workAipResc.hasProperty(CdrAcl.embargoUntil), "Work object did not contain assigned restriction");

        verify(work).addDataFile(eq(mainPid), any(URI.class), eq(mainLoc),
                eq(mainMime), isNull(), isNull(), modelCaptor.capture());

        Resource fileAipResc = modelCaptor.getValue().getResource(mainPid.getRepositoryPath());
        assertTrue(fileAipResc.hasProperty(CdrAcl.canViewOriginals), "File object did not contain assigned restriction");
    }

    @Test
    public void testObjectsNotInFedoraAfterIngest() throws Exception {
        Assertions.assertThrows(JobFailedException.class, () -> {
            FolderObject folder = mock(FolderObject.class);
            when(repoObjFactory.createFolderObject(any(PID.class), any(Model.class))).thenReturn(folder);

            PID folderPid = makePid(RepositoryPathConstants.CONTENT_BASE);
            Bag folderBag = model.createBag(folderPid.getRepositoryPath());
            folderBag.addProperty(RDF.type, Cdr.Folder);
            when(folder.getPid()).thenReturn(folderPid);

            depBag.add(folderBag);

            job.closeModel();

            // have verification service return a non-empty list
            when(verificationService.listObjectsNotInFedora(anyCollection()))
                    .thenReturn(Arrays.asList(folderPid));

            job.run();
        });
    }

    @Test
    public void ingestWorkWithMemberOrder() {
        PID workPid = makePid(RepositoryPathConstants.CONTENT_BASE);
        WorkObject work = mock(WorkObject.class);
        Bag workBag = setupWork(workPid, work);
        String child1 = "f277bb38-272c-471c-a28a-9887a1328a1f";
        String child2 = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
        String memberOrder = child1 + "|" + child2;

        workBag.addProperty(Cdr.memberOrder, memberOrder);

        job.closeModel();

        when(repoObjLoader.getWorkObject(eq(workPid))).thenReturn(work);

        job.run();

        verify(repoObjFactory).createWorkObject(eq(workPid), modelCaptor.capture());

        Resource workAipResc = modelCaptor.getValue().getResource(workPid.getRepositoryPath());
        assertTrue(workAipResc.hasProperty(Cdr.memberOrder), "Work object did not contain member order");
    }

    private PID addFileObject(Bag parent, String stagingLocation, String mimetype) throws Exception {
        PID filePid = makePid(RepositoryPathConstants.CONTENT_BASE);

        Model model = parent.getModel();
        String stagingPath = Paths.get(depositDir.getAbsolutePath(), stagingLocation).toUri().toString();
        String storagePath = storageLocPath.resolve(filePid.getId() + ".txt").toUri().toString();
        Resource fileResc = model.createResource(filePid.getRepositoryPath());
        fileResc.addProperty(RDF.type, Cdr.FileObject);
        fileResc.addProperty(CdrDeposit.label, stagingLocation);

        Resource origResc = DepositModelHelpers.addDatastream(fileResc, ORIGINAL_FILE);
        origResc.addProperty(CdrDeposit.stagingLocation, stagingPath);
        origResc.addProperty(CdrDeposit.storageUri, storagePath);
        origResc.addProperty(CdrDeposit.mimetype, mimetype);

        parent.add(fileResc);

        // Create the accompanying fake FITS report file
        Path fitsPath = job.getTechMdPath(filePid, true);
        Files.createFile(fitsPath);
        Resource fitsResc = DepositModelHelpers.addDatastream(fileResc, TECHNICAL_METADATA);
        fitsResc.addProperty(CdrDeposit.storageUri, fitsPath.toUri().toString());

        return filePid;
    }
}

package edu.unc.lib.boxc.services.camel.destroyDerivatives;

import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_DEPTH;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.HASHED_PATH_SIZE;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.idToPath;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import edu.unc.lib.boxc.model.fcrepo.test.TestRepositoryDeinitializer;
import edu.unc.lib.boxc.operations.jms.order.MemberOrderRequestSender;
import org.apache.camel.BeanInject;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.fcrepo.client.FcrepoClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.InheritedAclFactory;
import edu.unc.lib.boxc.fcrepo.utils.FedoraSparqlUpdateService;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.AclModelBuilder;
import edu.unc.lib.boxc.model.fcrepo.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.impl.delete.MarkForDeletionJob;
import edu.unc.lib.boxc.operations.impl.destroy.DestroyObjectsJob;
import edu.unc.lib.boxc.operations.impl.events.PremisLoggerFactoryImpl;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationManagerImpl;
import edu.unc.lib.boxc.search.api.models.ObjectPath;
import edu.unc.lib.boxc.search.solr.services.ObjectPathFactory;

/**
 *
 * @author lfarrell
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextHierarchy({
        @ContextConfiguration("/spring-test/cdr-client-container.xml"),
        @ContextConfiguration("/spring-test/jms-context.xml"),
        @ContextConfiguration("/spring-test/acl-service-context.xml"),
        @ContextConfiguration("/destroy-derivatives-router-it-context.xml")
})
public class DestroyDerivativesRouterIT {
    @Autowired
    private String baseAddress;
    @Autowired
    private RepositoryObjectFactory repoObjectFactory;
    @javax.annotation.Resource(name = "repositoryObjectLoader")
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private TransactionManager txManager;
    @Autowired
    private ObjectPathFactory pathFactory;
    @Autowired
    private FcrepoClient fcrepoClient;
    @Autowired
    private Model queryModel;
    @Autowired
    private StorageLocationManagerImpl locationManager;
    @Autowired
    private BinaryTransferService transferService;
    @Autowired
    private AccessControlService aclService;
    @Autowired
    private InheritedAclFactory inheritedAclFactory;
    @Autowired
    private RepositoryInitializer repositoryInitializer;
    @Autowired
    private FedoraSparqlUpdateService sparqlUpdateService;
    @Autowired
    private PremisLoggerFactoryImpl premisLoggerFactory;
    @Autowired
    private CamelContext cdrDestroyDerivatives;
    @Autowired
    private MessageSender binaryDestroyedMessageSender;

    @Mock
    private IndexingMessageSender indexingMessageSender;
    @Mock
    private ObjectPath path;
    @Mock
    private MemberOrderRequestSender memberOrderRequestSender;

    @BeanInject(value = "destroyedMsgProcessor")
    private DestroyedMsgProcessor destroyedMsgProcessor;

    @BeanInject(value = "destroyCollectionSrcImgProcessor")
    private DestroyDerivativesProcessor destroyCollectionSrcImgProcessor;

    @BeanInject(value = "destroySmallThumbnailProcessor")
    private DestroyDerivativesProcessor destroySmallThumbnailProcessor;

    @BeanInject(value = "destroyLargeThumbnailProcessor")
    private DestroyDerivativesProcessor destroyLargeThumbnailProcessor;

    @BeanInject(value = "destroyAccessCopyProcessor")
    private DestroyDerivativesProcessor destroyAccessCopyProcessor;

    @BeanInject(value = "destroyFulltextProcessor")
    private DestroyDerivativesProcessor destroyFulltextProcessor;

    private DestroyObjectsJob destroyJob;

    private AgentPrincipals agent;

    private RepositoryObjectTreeIndexer treeIndexer;

    private AdminUnit adminUnit;

    private CollectionObject collection;

    private final static String LOC1_ID = "loc1";

    private AutoCloseable closeable;

    @Before
    public void init() {
        closeable = openMocks(this);

        TestHelper.setContentBase(baseAddress);

        repositoryInitializer.initializeRepository();
        PID contentRootPid = getContentRootPid();

        AccessGroupSet testPrincipals = new AccessGroupSetImpl("edu:unc:lib:cdr:admin");
        agent = new AgentPrincipalsImpl("testUser", testPrincipals);

        ContentRootObject contentRoot = repoObjLoader.getContentRootObject(contentRootPid);
        adminUnit = repoObjectFactory.createAdminUnit(new AclModelBuilder("Unit")
                .addUnitOwner(agent.getUsernameUri())
                .model);

        collection = repoObjectFactory.createCollectionObject(null);

        contentRoot.addMember(adminUnit);
        adminUnit.addMember(collection);

        reset(destroyCollectionSrcImgProcessor);
        reset(destroySmallThumbnailProcessor);
        reset(destroyLargeThumbnailProcessor);
        reset(destroyAccessCopyProcessor);
        reset(destroyFulltextProcessor);

        treeIndexer = new RepositoryObjectTreeIndexer(queryModel, fcrepoClient);
        premisLoggerFactory.setBinaryTransferService(transferService);

        when(pathFactory.getPath(any(PID.class))).thenReturn(path);
        when(path.toNamePath()).thenReturn("path/to/object");
        when(path.toIdPath()).thenReturn("pid0/pid1/pid2/pid3");
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
        TestRepositoryDeinitializer.cleanup(fcrepoClient);
    }

    @Test
    public void destroyImageTest() throws Exception {
        WorkObject work = repoObjectFactory.createWorkObject(null);
        FileObject fileObj = addFileToWork(work, "image/png");
        work.addMember(fileObj);

        treeIndexer.indexAll(baseAddress);

        markForDeletion(fileObj.getPid());
        initializeDestroyJob(Collections.singletonList(fileObj.getPid()));
        destroyJob.run();

        verify(destroySmallThumbnailProcessor).process(any(Exchange.class));
        verify(destroyLargeThumbnailProcessor).process(any(Exchange.class));
        verify(destroyCollectionSrcImgProcessor, never()).process(any(Exchange.class));
        verify(destroyAccessCopyProcessor).process(any(Exchange.class));
        verify(destroyFulltextProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void destroyCollectionImageTest() throws Exception {
        CollectionObject collectionWithImg = repoObjectFactory.createCollectionObject(null);
        adminUnit.addMember(collectionWithImg);

        treeIndexer.indexAll(baseAddress);

        // Create collection thumbnail
        PID collPid = collectionWithImg.getPid();
        String uuid = collPid.getId();
        String binarySubPath = idToPath(uuid, HASHED_PATH_DEPTH, HASHED_PATH_SIZE);
        Path existingFileDirs = Files.createDirectories(new File("target/" + binarySubPath).toPath());
        File existingFile = new File(existingFileDirs.toString(), uuid);
        FileUtils.writeStringToFile(existingFile, "thumbnail", "UTF-8");

        markForDeletion(collPid);
        initializeDestroyJob(Collections.singletonList(collPid));

        destroyJob.run();

        verify(destroySmallThumbnailProcessor).process(any(Exchange.class));
        verify(destroyLargeThumbnailProcessor).process(any(Exchange.class));
        verify(destroyCollectionSrcImgProcessor).process(any(Exchange.class));
        verify(destroyAccessCopyProcessor, never()).process(any(Exchange.class));
        verify(destroyFulltextProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void destroyCollectionNoImageTest() throws Exception {
        CollectionObject collectionWithImg = repoObjectFactory.createCollectionObject(null);
        adminUnit.addMember(collectionWithImg);

        treeIndexer.indexAll(baseAddress);

        markForDeletion(collectionWithImg.getPid());
        initializeDestroyJob(Collections.singletonList(collectionWithImg.getPid()));
        destroyJob.run();

        verify(destroySmallThumbnailProcessor, never()).process(any(Exchange.class));
        verify(destroyLargeThumbnailProcessor, never()).process(any(Exchange.class));
        verify(destroyCollectionSrcImgProcessor, never()).process(any(Exchange.class));
        verify(destroyAccessCopyProcessor, never()).process(any(Exchange.class));
        verify(destroyFulltextProcessor, never()).process(any(Exchange.class));
    }

    @Test
    public void destroyTextTest() throws Exception {
        WorkObject work = repoObjectFactory.createWorkObject(null);
        String mimetype = "text/plain";
        FileObject fileObj = addFileToWork(work, mimetype);
        work.addMember(fileObj);

        treeIndexer.indexAll(baseAddress);

        markForDeletion(work.getPid());
        initializeDestroyJob(Collections.singletonList(fileObj.getPid()));
        destroyJob.run();

        verify(destroySmallThumbnailProcessor, never()).process(any(Exchange.class));
        verify(destroyLargeThumbnailProcessor, never()).process(any(Exchange.class));
        verify(destroyCollectionSrcImgProcessor, never()).process(any(Exchange.class));
        verify(destroyAccessCopyProcessor, never()).process(any(Exchange.class));
        verify(destroyFulltextProcessor).process(any(Exchange.class));
    }

    @Test
    public void invalidTypeTest() throws Exception {
        WorkObject work = repoObjectFactory.createWorkObject(null);
        FileObject fileObj = addFileToWork(work, "application/octet-stream");
        work.addMember(fileObj);

        treeIndexer.indexAll(baseAddress);

        markForDeletion(fileObj.getPid());
        initializeDestroyJob(Collections.singletonList(fileObj.getPid()));
        destroyJob.run();

        verify(destroySmallThumbnailProcessor, never()).process(any(Exchange.class));
        verify(destroyLargeThumbnailProcessor, never()).process(any(Exchange.class));
        verify(destroyCollectionSrcImgProcessor, never()).process(any(Exchange.class));
        verify(destroyAccessCopyProcessor, never()).process(any(Exchange.class));
        verify(destroyFulltextProcessor, never()).process(any(Exchange.class));
    }

    private FileObject addFileToWork(WorkObject work, String mimetype) throws Exception {
        collection.addMember(work);

        String bodyString = "Content";
        Path storagePath = Paths.get(locationManager.getStorageLocationById(LOC1_ID).getNewStorageUri(work.getPid()));
        Files.createDirectories(storagePath);
        File contentFile = Files.createTempFile(storagePath, "file", ".txt").toFile();
        String sha1 = "4f9be057f0ea5d2ba72fd2c810e8d7b9aa98b469";
        String filename = contentFile.getName();
        FileUtils.writeStringToFile(contentFile, bodyString, "UTF-8");

        return work.addDataFile(contentFile.toURI(), filename, mimetype, sha1, null);
    }

    private void markForDeletion(PID pid) {
        new MarkForDeletionJob(pid, "", agent, repoObjLoader,
                sparqlUpdateService, aclService, premisLoggerFactory).run();
    }

    private void initializeDestroyJob(List<PID> objsToDestroy) {
        DestroyObjectsRequest request = new DestroyObjectsRequest("jobid", agent,
                objsToDestroy.stream().map(PID::getId).toArray(String[]::new));
        destroyJob = new DestroyObjectsJob(request);
        destroyJob.setPathFactory(pathFactory);
        destroyJob.setRepoObjFactory(repoObjectFactory);
        destroyJob.setRepoObjLoader(repoObjLoader);
        destroyJob.setTransactionManager(txManager);
        destroyJob.setFcrepoClient(fcrepoClient);
        destroyJob.setAclService(aclService);
        destroyJob.setInheritedAclFactory(inheritedAclFactory);
        destroyJob.setBinaryTransferService(transferService);
        destroyJob.setStorageLocationManager(locationManager);
        destroyJob.setIndexingMessageSender(indexingMessageSender);
        destroyJob.setBinaryDestroyedMessageSender(binaryDestroyedMessageSender);
        destroyJob.setPremisLoggerFactory(premisLoggerFactory);
        destroyJob.setMemberOrderRequestSender(memberOrderRequestSender);

    }
}

package edu.unc.lib.boxc.operations.impl.destroy;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.InheritedAclFactory;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.Tombstone;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.Ebucore;
import edu.unc.lib.boxc.model.api.rdf.PcdmModels;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.api.sparql.SparqlUpdateService;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.AclModelBuilder;
import edu.unc.lib.boxc.model.fcrepo.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.order.MemberOrderRequestSender;
import edu.unc.lib.boxc.operations.jms.order.MultiParentOrderRequest;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationManagerImpl;
import edu.unc.lib.boxc.search.api.models.ObjectPath;
import edu.unc.lib.boxc.search.solr.services.ObjectPathFactory;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.jdom2.Document;
import org.jdom2.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static edu.unc.lib.boxc.model.api.rdf.CdrAcl.markedForDeletion;
import static edu.unc.lib.boxc.model.api.sparql.SparqlUpdateHelper.createSparqlReplace;
import static edu.unc.lib.boxc.model.api.xml.JDOMNamespaceUtil.CDR_MESSAGE_NS;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.DELETE_SOLR_TREE;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 *
 * @author harring
 *
 */
@ExtendWith(SpringExtension.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/spring-test/acl-service-context.xml")
})
public class DestroyObjectsJobIT {
    private final static String LOC1_ID = "loc1";
    private static final String USER_NAME = "user";
    private static final String USER_GROUPS = "edu:lib:staff_grp";

    @TempDir
    public Path tmpFolder;

    @Autowired
    private String baseAddress;
    @Autowired
    private RepositoryObjectFactory repoObjFactory;
    @Autowired
    private RepositoryObjectLoader repoObjLoader;
    @Autowired
    private TransactionManager txManager;
    @Autowired
    private ObjectPathFactory pathFactory;
    @Mock
    private ObjectPath path;
    @Autowired
    private SparqlUpdateService sparqlUpdateService;
    @Autowired
    private Model queryModel;
    @Autowired
    private FcrepoClient fcrepoClient;
    @Autowired
    private AccessControlService aclService;
    @Autowired
    private InheritedAclFactory inheritedAclFactory;
    @Autowired
    private RepositoryInitializer repoInitializer;
    @Autowired
    private PremisLoggerFactory premisLoggerFactory;
    @Mock
    private IndexingMessageSender indexingMessageSender;
    @Mock
    private MessageSender binaryDestroyedMessageSender;
    @Captor
    private ArgumentCaptor<Document> docCaptor;

    @Autowired
    private StorageLocationManagerImpl locationManager;
    @Autowired
    private BinaryTransferService transferService;
    @Autowired
    private PIDMinter pidMinter;

    private AgentPrincipals agent;

    private RepositoryObjectTreeIndexer treeIndexer;

    private List<PID> objsToDestroy = new ArrayList<>();

    private DestroyObjectsJob job;

    @Mock
    private MemberOrderRequestSender memberOrderRequestSender;
    @Captor
    private ArgumentCaptor<MultiParentOrderRequest> requestCaptor;

    @BeforeEach
    public void init() throws Exception {
        initMocks(this);
        TestHelper.setContentBase(baseAddress);

        AccessGroupSet testPrincipals = new AccessGroupSetImpl(USER_GROUPS);
        agent = new AgentPrincipalsImpl(USER_NAME, testPrincipals);

        treeIndexer = new RepositoryObjectTreeIndexer(queryModel, fcrepoClient);

        objsToDestroy = createContentTree();

        when(pathFactory.getPath(any(PID.class))).thenReturn(path);
        when(path.toNamePath()).thenReturn("path/to/object");
        when(path.toIdPath()).thenReturn("pid0/pid1/pid2/pid3");
    }

    @Test
    public void destroySingleFileObjectTest() throws Exception {
        PID fileObjPid = objsToDestroy.get(2);
        initializeJob(asList(fileObjPid));

        FileObject fileObj = repoObjLoader.getFileObject(fileObjPid);
        Map<URI, Map<String, String>> filesToCleanup = derivativesToCleanup(fileObj);

        URI contentUri = fileObj.getOriginalFile().getContentUri();
        assertTrue(Files.exists(Paths.get(contentUri)));

        job.run();

        Model logParentModel = fileObj.getParent().getPremisLog().getEventsModel();
        assertTrue(logParentModel.contains(null, RDF.type, Premis.Deletion));
        assertTrue(logParentModel.contains(null, Premis.note,
                "1 object(s) were destroyed"));

        Tombstone stoneFile = repoObjLoader.getTombstone(fileObjPid);
        Resource stoneResc = stoneFile.getResource();
        assertTrue(stoneResc.hasProperty(RDF.type, Cdr.Tombstone));
        assertTrue(stoneResc.hasProperty(RDF.type, Cdr.FileObject));
        // check to make sure metadata from binary was retained by file obj's tombstone
        assertTrue(stoneResc.hasProperty(Ebucore.filename));
        assertTrue(stoneResc.hasProperty(Cdr.hasMessageDigest));
        assertTrue(stoneResc.hasProperty(Ebucore.hasMimeType));
        assertTrue(stoneResc.hasProperty(Cdr.hasSize));

        assertFalse(Files.exists(Paths.get(contentUri)), "Original file must be deleted");

        verify(indexingMessageSender).sendIndexingOperation(anyString(), eq(fileObjPid), eq(DELETE_SOLR_TREE));

        verify(binaryDestroyedMessageSender).sendMessage(docCaptor.capture());

        assertMessagePresent(docCaptor.getAllValues(), filesToCleanup, null);

        // assert that the remove order request was sent, and has the right map
        verify(memberOrderRequestSender).sendToQueue(requestCaptor.capture());
        var parentId = fileObj.getParentPid().getId();
        var map = Map.of(parentId, Collections.singletonList(fileObjPid.getId()));
        assertEquals(map, requestCaptor.getValue().getParentToOrdered());
    }

    @Test
    public void destroyPrimaryObjectInMultiFileWorkTest() throws Exception {
        PID fileObjPid = objsToDestroy.get(2);
        var workObj = repoObjLoader.getWorkObject(objsToDestroy.get(1));
        workObj.setPrimaryObject(fileObjPid);
        // Add a second file
        var fileObj2 = addFileToWork(workObj);
        treeIndexer.indexAll(baseAddress);

        initializeJob(asList(fileObjPid));

        FileObject fileObj = repoObjLoader.getFileObject(fileObjPid);
        Map<URI, Map<String, String>> filesToCleanup = derivativesToCleanup(fileObj);

        URI contentUri = fileObj.getOriginalFile().getContentUri();
        assertTrue(Files.exists(Paths.get(contentUri)));

        job.run();

        Model logParentModel = fileObj.getParent().getPremisLog().getEventsModel();
        assertTrue(logParentModel.contains(null, RDF.type, Premis.Deletion));
        assertTrue(logParentModel.contains(null, Premis.note,
                "1 object(s) were destroyed"));

        Tombstone stoneFile = repoObjLoader.getTombstone(fileObjPid);
        Resource stoneResc = stoneFile.getResource();
        assertTrue(stoneResc.hasProperty(RDF.type, Cdr.Tombstone));
        assertTrue(stoneResc.hasProperty(RDF.type, Cdr.FileObject));
        // check to make sure metadata from binary was retained by file obj's tombstone
        assertTrue(stoneResc.hasProperty(Ebucore.filename));
        assertTrue(stoneResc.hasProperty(Cdr.hasMessageDigest));
        assertTrue(stoneResc.hasProperty(Ebucore.hasMimeType));
        assertTrue(stoneResc.hasProperty(Cdr.hasSize));

        assertFalse(Files.exists(Paths.get(contentUri)), "Original file must be deleted");

        verify(indexingMessageSender).sendIndexingOperation(anyString(), eq(fileObjPid), eq(DELETE_SOLR_TREE));

        verify(binaryDestroyedMessageSender).sendMessage(docCaptor.capture());

        assertMessagePresent(docCaptor.getAllValues(), filesToCleanup, null);

        // assert that the remove order request was sent, and has the right map
        verify(memberOrderRequestSender).sendToQueue(requestCaptor.capture());
        var parentId = fileObj.getParentPid().getId();
        var map = Map.of(parentId, Collections.singletonList(fileObjPid.getId()));
        assertEquals(map, requestCaptor.getValue().getParentToOrdered());

        workObj.shouldRefresh();

        assertNull(workObj.getPrimaryObject(), "Primary object of work must now be null");
        var members = workObj.getMembers();
        assertTrue(members.stream().anyMatch(m -> m.getPid().equals(fileObj2.getPid())),
                "Second file must still be present");
        assertEquals(2, members.size());
        assertFalse(workObj.getModel().contains(null, Cdr.primaryObject, (RDFNode) null),
                "Primary object must be unset");
    }

    @Test
    public void destroyObjectsInSameTreeTest() throws Exception {
        //remove unrelated folder obj before running job
        objsToDestroy.remove(3);

        initializeJob(objsToDestroy);

        PID fileObjPid = objsToDestroy.get(2);
        PID workObjPid = objsToDestroy.get(1);
        PID folderObjPid = objsToDestroy.get(0);

        FileObject fileObj = repoObjLoader.getFileObject(fileObjPid);
        WorkObject workObj = repoObjLoader.getWorkObject(workObjPid);
        FolderObject folderObj = repoObjLoader.getFolderObject(folderObjPid);
        Map<URI, Map<String, String>> filesToCleanup = derivativesToCleanup(fileObj);
        Map<URI, Map<String, String>> nonBinariesToCleanup = new HashMap<>();
        nonBinariesToCleanup.put(folderObj.getUri(), nonBinaryToCleanup(folderObj));
        nonBinariesToCleanup.put(workObj.getUri(), nonBinaryToCleanup(workObj));

        job.run();

        Tombstone stoneFile = repoObjLoader.getTombstone(fileObjPid);
        Resource fileResc = stoneFile.getResource();
        Tombstone stoneWork = repoObjLoader.getTombstone(workObjPid);
        Resource workResc = stoneWork.getResource();
        Tombstone stoneFolder = repoObjLoader.getTombstone(folderObjPid);
        Resource folderResc = stoneFolder.getResource();
        assertTrue(fileResc.hasProperty(RDF.type, Cdr.Tombstone));
        assertTrue(fileResc.hasProperty(RDF.type, Cdr.FileObject));
        assertTrue(fileResc.hasProperty(PcdmModels.memberOf, workResc));
        assertTrue(workResc.hasProperty(RDF.type, Cdr.Tombstone));
        assertTrue(workResc.hasProperty(RDF.type, Cdr.Work));
        assertTrue(workResc.hasProperty(PcdmModels.memberOf, folderResc));
        assertTrue(folderResc.hasProperty(RDF.type, Cdr.Tombstone));
        assertTrue(folderResc.hasProperty(RDF.type, Cdr.Folder));

        Model logModel = stoneFolder.getPremisLog().getEventsModel();
        assertTrue(logModel.contains(null, RDF.type, Premis.Deletion));
        assertTrue(logModel.contains(null, Premis.note,
                "Item deleted from repository and replaced by tombstone"));

        verify(indexingMessageSender).sendIndexingOperation(anyString(), eq(folderObjPid), eq(DELETE_SOLR_TREE));

        verify(binaryDestroyedMessageSender, times(3)).sendMessage(docCaptor.capture());

        assertMessagePresent(docCaptor.getAllValues(), filesToCleanup, nonBinariesToCleanup);
    }

    @Test
    public void destroyObjectsInDifferentTreesTest() {
        initializeJob(objsToDestroy);

        job.run();

        PID folderObj2Pid = objsToDestroy.get(3);
        PID fileObjPid = objsToDestroy.get(2);
        PID workObjPid = objsToDestroy.get(1);
        PID folderObj1Pid = objsToDestroy.get(0);

        Tombstone stoneFile = repoObjLoader.getTombstone(fileObjPid);
        Tombstone stoneWork = repoObjLoader.getTombstone(workObjPid);
        Tombstone stoneFolder1 = repoObjLoader.getTombstone(folderObj1Pid);
        Tombstone stoneFolder2 = repoObjLoader.getTombstone(folderObj2Pid);
        assertTrue(stoneFile.getModel().contains(stoneFile.getResource(), RDF.type, Cdr.Tombstone));
        assertTrue(stoneWork.getModel().contains(stoneWork.getResource(), RDF.type, Cdr.Tombstone));
        assertTrue(stoneFolder1.getModel().contains(stoneFolder1.getResource(), RDF.type, Cdr.Tombstone));
        assertTrue(stoneFolder2.getModel().contains(stoneFolder2.getResource(), RDF.type, Cdr.Tombstone));

        Model logModel1 = stoneFolder1.getPremisLog().getEventsModel();
        assertTrue(logModel1.contains(null, RDF.type, Premis.Deletion));
        assertTrue(logModel1.contains(null, Premis.note,
                "Item deleted from repository and replaced by tombstone"));

        Model logModel2 = stoneFolder2.getPremisLog().getEventsModel();
        assertTrue(logModel2.contains(null, RDF.type, Premis.Deletion));
        assertTrue(logModel2.contains(null, Premis.note,
                "Item deleted from repository and replaced by tombstone"));

        verify(indexingMessageSender).sendIndexingOperation(anyString(), eq(folderObj1Pid), eq(DELETE_SOLR_TREE));
        verify(indexingMessageSender).sendIndexingOperation(anyString(), eq(folderObj2Pid), eq(DELETE_SOLR_TREE));
    }

    @Test
    public void destroyFolderTest() throws Exception {
        PID folderObjPid = objsToDestroy.get(0);
        initializeJob(Arrays.asList(folderObjPid));
        FolderObject folderObj = repoObjLoader.getFolderObject(folderObjPid);
        WorkObject workObj = (WorkObject) folderObj.getMembers().get(0);
        FileObject fileObj = (FileObject) workObj.getMembers().get(0);

        Map<URI, Map<String, String>> filesToCleanup = derivativesToCleanup(fileObj);
        Map<URI, Map<String, String>> nonBinariesToCleanup = new HashMap<>();
        nonBinariesToCleanup.put(folderObj.getUri(), nonBinaryToCleanup(folderObj));
        nonBinariesToCleanup.put(workObj.getUri(), nonBinaryToCleanup(workObj));

        job.run();

        RepositoryObject folderObjParent = folderObj.getParent();
        Model logParentModel = folderObjParent.getPremisLog().getEventsModel();
        assertTrue(logParentModel.contains(null, RDF.type, Premis.Deletion));
        assertTrue(logParentModel.contains(null, Premis.note, "3 object(s) were destroyed"));

        Resource fileResc = fileObj.getResource(true);
        Resource workResc = workObj.getResource(true);
        Resource folderResc = folderObj.getResource(true);
        assertTrue(fileResc.hasProperty(RDF.type, Cdr.Tombstone));
        assertTrue(fileResc.hasProperty(PcdmModels.memberOf, workResc));
        assertTrue(workResc.hasProperty(RDF.type, Cdr.Tombstone));
        assertTrue(workResc.hasProperty(PcdmModels.memberOf, folderResc));
        assertTrue(folderResc.hasProperty(RDF.type, Cdr.Tombstone));

        verify(indexingMessageSender).sendIndexingOperation(anyString(), eq(folderObjPid), eq(DELETE_SOLR_TREE));

        verify(binaryDestroyedMessageSender, times(3)).sendMessage(docCaptor.capture());
        List<Document> values = docCaptor.getAllValues();
        assertMessagePresent(values, filesToCleanup, nonBinariesToCleanup);
    }

    @Test
    public void destroySingleObjectWithPreexistingPremisEventTest() {
        PID fileObjPid = objsToDestroy.get(2);
        FileObject fileObj = repoObjLoader.getFileObject(fileObjPid);
        Resource event = premisLoggerFactory.createPremisLogger(fileObj)
                .buildEvent(null, Premis.Ingestion, new Date(1L)).write();

        initializeJob(Collections.singletonList(fileObjPid));

        job.run();

        Model logParentModel = fileObj.getParent().getPremisLog().getEventsModel();
        assertTrue(logParentModel.contains(null, RDF.type, Premis.Deletion));
        assertTrue(logParentModel.contains(null, Premis.note, "1 object(s) were destroyed"));

        Tombstone stoneFile = repoObjLoader.getTombstone(fileObjPid);
        assertTrue(stoneFile.getModel().contains(stoneFile.getResource(), RDF.type, Cdr.Tombstone));

        Model logModel = stoneFile.getPremisLog().getEventsModel();
        assertTrue(logModel.contains(null, RDF.type, Premis.Deletion));
        assertTrue(logModel.contains(event, RDF.type, Premis.Ingestion));
    }

    private List<PID> createContentTree() throws Exception {
        PID contentRootPid = getContentRootPid();
        repoInitializer.initializeRepository();
        ContentRootObject contentRoot = repoObjLoader.getContentRootObject(contentRootPid);

        AdminUnit adminUnit = repoObjFactory.createAdminUnit(new AclModelBuilder("Unit")
                .addUnitOwner(agent.getUsernameUri())
                .model);
        contentRoot.addMember(adminUnit);

        CollectionObject collection = repoObjFactory.createCollectionObject(null);
        adminUnit.addMember(collection);
        FolderObject folder = repoObjFactory.createFolderObject(null);
        FolderObject folder2 = repoObjFactory.createFolderObject(null);
        collection.addMember(folder);
        collection.addMember(folder2);
        WorkObject work = repoObjFactory.createWorkObject(null);
        folder.addMember(work);
        var file = addFileToWork(work);

        treeIndexer.indexAll(baseAddress);

        objsToDestroy.add(folder.getPid());
        objsToDestroy.add(work.getPid());
        objsToDestroy.add(file.getPid());
        objsToDestroy.add(folder2.getPid());
        markObjsForDeletion(objsToDestroy);

        return objsToDestroy;
    }

    private FileObject addFileToWork(WorkObject workObj) throws Exception {
        String bodyString = "Content";
        String mimetype = "text/plain";
        PID filePid = pidMinter.mintContentPid();
        Path storagePath = Paths.get(locationManager.getStorageLocationById(LOC1_ID)
                .getNewStorageUri(filePid));
        Files.createDirectories(storagePath);
        File contentFile = Files.createTempFile(storagePath, "file", ".txt").toFile();
        String sha1 = "4f9be057f0ea5d2ba72fd2c810e8d7b9aa98b469";
        String filename = contentFile.getName();
        FileUtils.writeStringToFile(contentFile, bodyString, "UTF-8");
        return workObj.addDataFile(filePid, contentFile.toPath().toUri(), filename, mimetype, sha1, null, null);
    }

    private void initializeJob(List<PID> objsToDestroy) {
        DestroyObjectsRequest request = new DestroyObjectsRequest("jobid", agent,
                objsToDestroy.stream().map(PID::getId).toArray(String[]::new));
        job = new DestroyObjectsJob(request);
        job.setPathFactory(pathFactory);
        job.setRepoObjFactory(repoObjFactory);
        job.setRepoObjLoader(repoObjLoader);
        job.setTransactionManager(txManager);
        job.setFcrepoClient(fcrepoClient);
        job.setAclService(aclService);
        job.setInheritedAclFactory(inheritedAclFactory);
        job.setBinaryTransferService(transferService);
        job.setStorageLocationManager(locationManager);
        job.setIndexingMessageSender(indexingMessageSender);
        job.setBinaryDestroyedMessageSender(binaryDestroyedMessageSender);
        job.setPremisLoggerFactory(premisLoggerFactory);
        job.setMemberOrderRequestSender(memberOrderRequestSender);
    }

    private void assertMessagePresent(List<Document> returnedDocs, Map<URI, Map<String, String>> filesToCleanup,
            Map<URI, Map<String, String>> nonBinariesToCleanup) {

        for (Document returnedDoc : returnedDocs) {
            Element root = returnedDoc.getRootElement();
            Element info = root.getChild("objToDestroy", CDR_MESSAGE_NS);
            String pidId = info.getChildTextTrim("pidId", CDR_MESSAGE_NS);
            String msgObjType = info.getChildTextTrim("objType", CDR_MESSAGE_NS);
            String msgMimetype = info.getChildTextTrim("mimeType", CDR_MESSAGE_NS);

            if (msgObjType.equals(Cdr.FileObject.getURI())) {
                URI uri = URI.create(info.getChildTextTrim("contentUri", CDR_MESSAGE_NS));
                Map<String, String> cleanupFile = filesToCleanup.get(uri);
                assertEquals(cleanupFile.get("objType"), msgObjType);
                assertEquals(cleanupFile.get("mimeType"), msgMimetype);
                assertEquals(cleanupFile.get("pid"), pidId);
            } else {
                Map<String, String> cleanupObj = nonBinariesToCleanup.get(PIDs.get(pidId).getRepositoryUri());
                assertEquals(msgObjType, cleanupObj.get("objType"));
                assertNull(msgMimetype);
            }
        }
    }

    private void markObjsForDeletion(List<PID> objsToDestroy) {
        for (PID pid : objsToDestroy) {
            String updateString = createSparqlReplace(pid.getRepositoryPath(), markedForDeletion,
                    true);
            sparqlUpdateService.executeUpdate(pid.getRepositoryUri().toString(), updateString);
        }
    }

    private Map<String, String> nonBinaryToCleanup(RepositoryObject obj) {
        Map<String, String> contentMetadata = new HashMap<>();
        contentMetadata.put("objType", ResourceType.getResourceTypeForUris(obj.getTypes()).getUri());
        PID pid = obj.getPid();
        contentMetadata.put("pid", pid.getQualifiedId());

        return contentMetadata;
    }

    private Map<URI, Map<String, String>> derivativesToCleanup(FileObject fileObj) {
        HashMap<URI, Map<String, String>> cleanupBinaryUris = new HashMap<>();

        BinaryObject binary = fileObj.getOriginalFile();
        PID binaryPid = binary.getPid();
        Map<String, String> contentMetadata = new HashMap<>();
        contentMetadata.put("objType", Cdr.FileObject.getURI());
        contentMetadata.put("pid", binaryPid.getQualifiedId());
        contentMetadata.put("mimeType", binary.getMimetype());
        cleanupBinaryUris.put(binary.getContentUri(), contentMetadata);

        return cleanupBinaryUris;
    }
}

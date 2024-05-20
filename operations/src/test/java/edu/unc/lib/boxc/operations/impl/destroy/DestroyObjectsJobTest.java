package edu.unc.lib.boxc.operations.impl.destroy;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.InheritedAclFactory;
import edu.unc.lib.boxc.common.test.SelfReturningAnswer;
import edu.unc.lib.boxc.fcrepo.utils.FedoraTransaction;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.Tombstone;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.Premis;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.api.events.PremisEventBuilder;
import edu.unc.lib.boxc.operations.api.events.PremisLogger;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.order.MemberOrderRequestSender;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.persist.api.transfer.MultiDestinationTransferSession;
import edu.unc.lib.boxc.search.api.models.ObjectPath;
import edu.unc.lib.boxc.search.solr.services.ObjectPathFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.fcrepo.client.FcrepoClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
public class DestroyObjectsJobTest {
    private AutoCloseable closeable;
    @Mock
    protected MultiDestinationTransferSession transferSession;
    @Mock
    protected RepositoryObjectFactory repoObjFactory;
    @Mock
    protected RepositoryObjectLoader repoObjLoader;
    @Mock
    protected IndexingMessageSender indexingMessageSender;
    @Mock
    protected FcrepoClient fcrepoClient;
    @Mock
    protected MessageSender binaryDestroyedMessageSender;
    @Mock
    protected AccessControlService aclService;
    @Mock
    protected StorageLocationManager locManager;
    @Mock
    protected BinaryTransferService transferService;
    @Mock
    protected PremisLoggerFactory premisLoggerFactory;
    @Mock
    protected PremisLogger premisLogger;
    @Mock
    protected TransactionManager txManager;
    @Mock
    protected FedoraTransaction transaction;
    @Mock
    protected BinaryTransferService binaryTransferService;
    @Mock
    private ObjectPathFactory pathFactory;
    @Mock
    private InheritedAclFactory inheritedAclFactory;
    @Mock
    private MemberOrderRequestSender memberOrderRequestSender;
    private AgentPrincipals agent;
    private DestroyObjectsJob job;

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);

        AccessGroupSet testPrincipals = new AccessGroupSetImpl("group");
        agent = new AgentPrincipalsImpl("user", testPrincipals);

        when(txManager.startTransaction()).thenReturn(transaction);
        var eventBuilder = mock(PremisEventBuilder.class, new SelfReturningAnswer());
        when(premisLoggerFactory.createPremisLogger(any())).thenReturn(premisLogger);
        when(premisLogger.buildEvent(eq(Premis.Deletion))).thenReturn(eventBuilder);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    private void setupJob(DestroyObjectsRequest request) {
        job = new DestroyObjectsJob(request);
        job.setMemberOrderRequestSender(memberOrderRequestSender);
        job.setPathFactory(pathFactory);
        job.setInheritedAclFactory(inheritedAclFactory);
        job.setBinaryTransferService(binaryTransferService);
        job.setRepoObjFactory(repoObjFactory);
        job.setRepoObjLoader(repoObjLoader);
        job.setIndexingMessageSender(indexingMessageSender);
        job.setFcrepoClient(fcrepoClient);
        job.setBinaryDestroyedMessageSender(binaryDestroyedMessageSender);
        job.setAclService(aclService);
        job.setStorageLocationManager(locManager);
        job.setBinaryTransferService(transferService);
        job.setPremisLoggerFactory(premisLoggerFactory);
        job.setTransactionManager(txManager);
    }

    @Test
    public void testDestroyFolderWithTombstonMember() throws Exception {
        Model model = ModelFactory.createDefaultModel();

        var parentFolder = mock(FolderObject.class);

        var folder = mock(FolderObject.class);
        var folderPid = TestHelper.makePid();
        when(folder.getPid()).thenReturn(folderPid);
        when(folder.getParent()).thenReturn(parentFolder);
        when(folder.getUri()).thenReturn(folderPid.getRepositoryUri());
        when(repoObjLoader.getRepositoryObject(folderPid)).thenReturn(folder);
        var folderResc = model.getResource(folderPid.getRepositoryPath());
        folderResc.addProperty(RDF.type, Cdr.Folder);
        when(folder.getResource(true)).thenReturn(folderResc);
        when(folder.getTypes()).thenReturn(List.of(Cdr.Folder.getURI()));

        var memberTombstone = mock(Tombstone.class);
        var tombstonePid = TestHelper.makePid();
        when(memberTombstone.getPid()).thenReturn(tombstonePid);
        when(memberTombstone.getUri()).thenReturn(tombstonePid.getRepositoryUri());
        when(folder.getMembers()).thenReturn(List.of(memberTombstone));

        var objPath = mock(ObjectPath.class);
        when(pathFactory.getPath(folderPid)).thenReturn(objPath);
        when(objPath.toNamePath()).thenReturn("/path/to/stuff");
        when(objPath.toIdPath()).thenReturn(folderPid.getId());

        when(inheritedAclFactory.isMarkedForDeletion(folderPid)).thenReturn(true);

        setupJob(new DestroyObjectsRequest("1", agent, folderPid.getId()));

        job.run();

        verify(repoObjFactory).createOrTransformObject(eq(folderPid.getRepositoryUri()), any(Model.class));
        verify(repoObjFactory, never()).createOrTransformObject(eq(tombstonePid.getRepositoryUri()), any(Model.class));
    }
}

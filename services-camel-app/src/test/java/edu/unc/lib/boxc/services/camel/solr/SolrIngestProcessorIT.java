package edu.unc.lib.boxc.services.camel.solr;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.fcrepo.FcrepoJmsConstants.RESOURCE_TYPE;
import static edu.unc.lib.boxc.model.api.DatastreamType.MD_DESCRIPTIVE;
import static edu.unc.lib.boxc.model.api.DatastreamType.ORIGINAL_FILE;
import static edu.unc.lib.boxc.model.api.DatastreamType.MD_EVENTS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.auth.fcrepo.services.InheritedAclFactory;
import edu.unc.lib.boxc.fcrepo.utils.TransactionManager;
import edu.unc.lib.boxc.model.api.sparql.SparqlUpdateService;
import edu.unc.lib.boxc.operations.api.events.PremisLoggerFactory;
import edu.unc.lib.boxc.operations.impl.delete.MarkForDeletionJob;
import edu.unc.lib.boxc.operations.impl.destroy.DestroyObjectsJob;
import edu.unc.lib.boxc.operations.jms.MessageSender;
import edu.unc.lib.boxc.operations.jms.destroy.DestroyObjectsRequest;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.operations.jms.order.MemberOrderRequestSender;
import edu.unc.lib.boxc.persist.api.transfer.BinaryTransferService;
import edu.unc.lib.boxc.search.solr.services.ObjectPathFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.fcrepo.client.FcrepoClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPipeline;
import edu.unc.lib.boxc.model.api.DatastreamType;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.objects.BinaryObject;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.rdf.Cdr;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;
import edu.unc.lib.boxc.model.api.rdf.Fcrepo4Repository;
import edu.unc.lib.boxc.model.fcrepo.services.DerivativeService;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService;
import edu.unc.lib.boxc.operations.impl.edit.UpdateDescriptionService.UpdateDescriptionRequest;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;

/**
 *
 * @author bbpennel
 *
 */
public class SolrIngestProcessorIT extends AbstractSolrProcessorIT {

    private SolrIngestProcessor processor;
    private AutoCloseable closeable;

    private static final String CONTENT_TEXT = "Content";
    private static final String TEXT_EXTRACT = "Cone Tent";

    @Autowired
    private DocumentIndexingPipeline solrFullUpdatePipeline;
    @Autowired
    private UpdateDescriptionService updateDescriptionService;
    @javax.annotation.Resource(name = "repositoryObjectLoader")
    private RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    private DerivativeService derivativeService;
    private AgentPrincipals agent;
    @Autowired
    private MessageSender updateWorkSender;
    @Autowired
    private AccessControlService aclService;
    @Autowired
    private TransactionManager txManager;
    @Autowired
    private ObjectPathFactory pathFactory;
    @Autowired
    private FcrepoClient fcrepoClient;
    @Autowired
    private InheritedAclFactory inheritedAclFactory;
    @Autowired
    private BinaryTransferService transferService;
    @Mock
    private IndexingMessageSender indexingMessageSender;
    @Mock
    private MessageSender binaryDestroyedMessageSender;
    @Autowired
    private PremisLoggerFactory premisLoggerFactory;
    @Autowired
    private SparqlUpdateService sparqlUpdateService;
    @Mock
    private MemberOrderRequestSender memberOrderRequestSender;

    @Before
    public void setUp() throws Exception {
        closeable = openMocks(this);

        agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("group"));
        TestHelper.setContentBase(baseAddress);

        processor = new SolrIngestProcessor(dipFactory, solrFullUpdatePipeline, driver, repositoryObjectLoader);
        processor.setUpdateWorkSender(updateWorkSender);

        when(exchange.getIn()).thenReturn(message);

        generateBaseStructure();
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testIndexDescribedWork() throws Exception {
        WorkObject workObj = repositoryObjectFactory.createWorkObject(null);
        collObj.addMember(workObj);

        repositoryObjectSolrIndexer.index(unitObj.getPid(), collObj.getPid());

        FileObject fileObj = workObj.addDataFile(makeContentUri(CONTENT_TEXT),
                "text.txt", "text/plain", null, null);
        workObj.setPrimaryObject(fileObj.getPid());
        InputStream modsStream = streamResource("/datastreams/simpleMods.xml");
        updateDescriptionService.updateDescription(new UpdateDescriptionRequest(agent, workObj.getPid(), modsStream));

        indexObjectsInTripleStore();

        setMessageTarget(fileObj);
        when(message.getHeader(RESOURCE_TYPE)).thenReturn(Cdr.FileObject.getURI());
        processor.process(exchange);
        server.commit();

        setMessageTarget(workObj);
        processor.process(exchange);
        server.commit();

        SimpleIdRequest idRequest = new SimpleIdRequest(workObj.getPid(), accessGroups);
        ContentObjectRecord workMd = solrSearchService.getObjectById(idRequest);

        assertEquals("Work", workMd.getResourceType());

        assertNotNull("Date added must be set", workMd.getDateAdded());
        assertNotNull("Date updated must be set", workMd.getDateUpdated());

        assertEquals("Object title", workMd.getTitle());
        assertEquals("Boxy", workMd.getCreator().get(0));

        assertAncestorIds(workMd, rootObj, unitObj, collObj, workObj);

        assertNotNull(workMd.getContentStatus());

        assertEquals(2, workMd.getDatastream().size());
        assertNotNull(workMd.getDatastreamObject(ORIGINAL_FILE.getId()));
        assertNotNull(workMd.getDatastreamObject(MD_DESCRIPTIVE.getId()));

        assertTrue("Content type was not set to text", workMd.getFileFormatCategory().get(0).contains("Text"));

        assertTrue("Read groups did not contain assigned group", workMd.getReadGroup().contains(AUTHENTICATED_PRINC));
        assertTrue("Admin groups did not contain assigned group", workMd.getAdminGroup().contains("admin"));
    }

    @Test
    public void testIndexCollection() throws Exception {
        indexObjectsInTripleStore();
        repositoryObjectSolrIndexer.index(unitObj.getPid());

        setMessageTarget(collObj);
        processor.process(exchange);
        server.commit();

        SimpleIdRequest idRequest = new SimpleIdRequest(collObj.getPid(), accessGroups);
        ContentObjectRecord collMd = solrSearchService.getObjectById(idRequest);

        assertEquals("Collection", collMd.getResourceType());

        assertNotNull("Date added must be set", collMd.getDateAdded());
        assertNotNull("Date updated must be set", collMd.getDateUpdated());

        assertEquals(collObj.getPid().getId(), collMd.getTitle());

        assertAncestorIds(collMd, rootObj, unitObj, collObj);

        assertNotNull(collMd.getContentStatus());

        assertNull(collMd.getDatastream());

        assertTrue(CollectionUtils.isEmpty(collMd.getFileFormatCategory()));

        assertTrue("Read groups did not contain assigned group", collMd.getReadGroup().contains(AUTHENTICATED_PRINC));
        assertTrue("Admin groups did not contain assigned group", collMd.getAdminGroup().contains("admin"));
    }

    @Test
    public void testIndexFileObject() throws Exception {
        repositoryObjectSolrIndexer.index(unitObj.getPid(), collObj.getPid());

        WorkObject workObj = repositoryObjectFactory.createWorkObject(null);
        collObj.addMember(workObj);

        // Revoking patron access on the file
        Model fileModel = ModelFactory.createDefaultModel();
        Resource fileResc = fileModel.getResource("");
        fileResc.addProperty(CdrAcl.none, AUTHENTICATED_PRINC);
        FileObject fileObj = workObj.addDataFile(makeContentUri(CONTENT_TEXT),
                "text.txt", "text/plain", null, null, fileModel);

        Path derivPath = derivativeService.getDerivativePath(fileObj.getPid(), DatastreamType.FULLTEXT_EXTRACTION);
        FileUtils.writeStringToFile(derivPath.toFile(), TEXT_EXTRACT, UTF_8);

        indexObjectsInTripleStore();

        setMessageTarget(fileObj);
        processor.process(exchange);
        server.commit();

        List<String> allFields = Arrays.stream(SearchFieldKey.values())
                .map(SearchFieldKey::name).collect(Collectors.toList());
        SimpleIdRequest idRequest = new SimpleIdRequest(fileObj.getPid(), allFields, accessGroups);
        ContentObjectRecord fileMd = solrSearchService.getObjectById(idRequest);

        assertEquals(ResourceType.File.name(), fileMd.getResourceType());

        assertNotNull("Date added must be set", fileMd.getDateAdded());
        assertNotNull("Date updated must be set", fileMd.getDateUpdated());

        assertEquals("text.txt", fileMd.getTitle());

        assertAncestorIds(fileMd, rootObj, unitObj, collObj, workObj);

        assertNotNull(fileMd.getContentStatus());

        assertEquals(2, fileMd.getDatastream().size());
        assertNotNull(fileMd.getDatastreamObject(ORIGINAL_FILE.getId()));
        assertNotNull(fileMd.getDatastreamObject(DatastreamType.FULLTEXT_EXTRACTION.getId()));

        assertTrue("Content type was not set to text:" + fileMd.getFileFormatCategory(),
                fileMd.getFileFormatCategory().get(0).contains("Text"));

        assertFalse("Read group should not be assigned", fileMd.getReadGroup().contains(AUTHENTICATED_PRINC));
        assertTrue("Admin groups did not contain assigned group", fileMd.getAdminGroup().contains("admin"));

        assertEquals(TEXT_EXTRACT, fileMd.getFullText());
    }

    @Test
    public void testIndexBinaryInWork() throws Exception {
        repositoryObjectSolrIndexer.index(unitObj.getPid(), collObj.getPid());

        WorkObject workObj = repositoryObjectFactory.createWorkObject(null);
        collObj.addMember(workObj);

        FileObject fileObj = workObj.addDataFile(makeContentUri(CONTENT_TEXT),
                "text.txt", "text/plain", null, null);
        workObj.setPrimaryObject(fileObj.getPid());

        BinaryObject binObj = fileObj.getOriginalFile();

        indexObjectsInTripleStore();

        setMessageTarget(binObj);
        when(message.getHeader(RESOURCE_TYPE)).thenReturn(Fcrepo4Repository.Binary.getURI());
        processor.process(exchange);
        server.commit();

        setMessageTarget(workObj);
        when(message.getHeader(RESOURCE_TYPE)).thenReturn(Cdr.Work.getURI());
        processor.process(exchange);
        server.commit();

        SimpleIdRequest idRequest = new SimpleIdRequest(workObj.getPid(), accessGroups);
        ContentObjectRecord workMd = solrSearchService.getObjectById(idRequest);

        assertEquals("Work", workMd.getResourceType());

        assertEquals(1, workMd.getDatastream().size());
        assertNotNull(workMd.getDatastreamObject(ORIGINAL_FILE.getId()));

        assertTrue("Content type was not set to text", workMd.getFileFormatCategory().get(0).contains("Text"));

        idRequest = new SimpleIdRequest(fileObj.getPid(), accessGroups);
        ContentObjectRecord fileMd = solrSearchService.getObjectById(idRequest);

        assertEquals(ResourceType.File.name(), fileMd.getResourceType());

        assertEquals("text.txt", fileMd.getTitle());

        assertEquals(1, fileMd.getDatastream().size());
        assertNotNull(fileMd.getDatastreamObject(ORIGINAL_FILE.getId()));
    }

    // Relates to bug: BXC-3676
    @Test
    public void testWorkWithTombstonePrimaryObject() throws Exception {
        repositoryObjectSolrIndexer.index(unitObj.getPid(), collObj.getPid());

        WorkObject workObj = repositoryObjectFactory.createWorkObject(null);
        InputStream modsStream = streamResource("/datastreams/simpleMods.xml");
        updateDescriptionService.updateDescription(new UpdateDescriptionRequest(agent, workObj.getPid(), modsStream));
        collObj.addMember(workObj);

        FileObject fileObj = workObj.addDataFile(makeContentUri(CONTENT_TEXT),
                "text.txt", "text/plain", null, null);
        workObj.setPrimaryObject(fileObj.getPid());

        indexObjectsInTripleStore();

        setMessageTarget(fileObj);
        when(message.getHeader(RESOURCE_TYPE)).thenReturn(Cdr.FileObject.getURI());
        processor.process(exchange);
        server.commit();

        setMessageTarget(workObj);
        processor.process(exchange);
        server.commit();

        // Replace primary object with tombstone
        deleteAndDestroyObject(fileObj);

        indexObjectsInTripleStore();

        setMessageTarget(workObj);
        processor.process(exchange);
        server.commit();

        SimpleIdRequest idRequest = new SimpleIdRequest(workObj.getPid(), accessGroups);
        ContentObjectRecord workMd = solrSearchService.getObjectById(idRequest);

        assertEquals("Work", workMd.getResourceType());

        assertNotNull("Date added must be set", workMd.getDateAdded());
        assertNotNull("Date updated must be set", workMd.getDateUpdated());

        assertEquals("Object title", workMd.getTitle());
        assertEquals("Boxy", workMd.getCreator().get(0));

        assertAncestorIds(workMd, rootObj, unitObj, collObj, workObj);

        // Should not have an original file, but other datastreams should still be present
        assertEquals(2, workMd.getDatastream().size());
        assertNull(workMd.getDatastreamObject(ORIGINAL_FILE.getId()));
        assertNotNull(workMd.getDatastreamObject(MD_DESCRIPTIVE.getId()));
        assertNotNull(workMd.getDatastreamObject(MD_EVENTS.getId()));
    }

    private void deleteAndDestroyObject(RepositoryObject repoObj) {
        // Object must be marked for deletion before destroying it
        var markForDeleteJob = new MarkForDeletionJob(repoObj.getPid(), "delete me", agent, repositoryObjectLoader,
                sparqlUpdateService, aclService, premisLoggerFactory);
        markForDeleteJob.run();

        // Now destroy it
        var destroyRequest = new DestroyObjectsRequest("job", agent, repoObj.getPid().getId());
        var destroyJob = new DestroyObjectsJob(destroyRequest);
        destroyJob.setPathFactory(pathFactory);
        destroyJob.setInheritedAclFactory(inheritedAclFactory);
        destroyJob.setBinaryDestroyedMessageSender(binaryDestroyedMessageSender);
        destroyJob.setAclService(aclService);
        destroyJob.setFcrepoClient(fcrepoClient);
        destroyJob.setPremisLoggerFactory(premisLoggerFactory);
        destroyJob.setRepoObjFactory(repositoryObjectFactory);
        destroyJob.setRepoObjLoader(repositoryObjectLoader);
        destroyJob.setIndexingMessageSender(indexingMessageSender);
        destroyJob.setStorageLocationManager(locManager);
        destroyJob.setTransactionManager(txManager);
        destroyJob.setBinaryTransferService(transferService);
        destroyJob.setMemberOrderRequestSender(memberOrderRequestSender);
        destroyJob.run();
    }

    private void assertAncestorIds(ContentObjectRecord md, RepositoryObject... ancestorObjs) {
        String joinedIds = "/" + Arrays.stream(ancestorObjs)
                .map(obj -> obj.getPid().getId())
                .collect(Collectors.joining("/"));

        assertEquals(joinedIds, md.getAncestorIds());
    }
}

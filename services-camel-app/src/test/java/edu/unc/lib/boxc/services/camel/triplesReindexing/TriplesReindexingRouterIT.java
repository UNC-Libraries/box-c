package edu.unc.lib.boxc.services.camel.triplesReindexing;

import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.DepositRecord;
import edu.unc.lib.boxc.model.api.objects.FileObject;
import edu.unc.lib.boxc.model.api.objects.FolderObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.api.sparql.SparqlQueryService;
import edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.model.fcrepo.test.TestRepositoryDeinitializer;
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationTestHelper;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.ResultSet;
import org.fcrepo.client.FcrepoClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;
import static edu.unc.lib.boxc.operations.jms.indexing.IndexingActionType.RECURSIVE_REINDEX;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 *
 * @author bbpennel
 *
 */
public class TriplesReindexingRouterIT extends CamelSpringTestSupport {
    private AutoCloseable closeable;

    private String baseAddress;
    private String fusekiPort;
    private CamelContext camelContext;

    private RepositoryObjectLoader repositoryObjectLoader;
    private RepositoryObjectFactory repositoryObjectFactory;
    private RepositoryInitializer repoInitializer;
    private IndexingMessageSender messageSender;
    private AccessControlService aclService;
    private SparqlQueryService sparqlQueryService;
    private StorageLocationTestHelper storageLocationTestHelper;
    private FcrepoClient fcrepoClient;

    private String indexingEndpoint;

    @Mock
    private Exchange exchange;
    @Mock
    private Message message;

    private ContentRootObject rootObj;
    private AdminUnit unitObj;
    private CollectionObject collObj;
    private FolderObject folderObj1;
    private FolderObject folderObj2;
    private WorkObject workObj;
    private FileObject fileObj;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

//        camelContext = applicationContext.getBean("cdrServiceTriplesReindexing", CamelContext.class);
//        indexingEndpoint = applicationContext.getBean("indexingEndpoint", String.class);
//        fusekiPort = applicationContext.getBean("fusekiPort", String.class);
        baseAddress = applicationContext.getBean("baseAddress", String.class);
        repositoryObjectLoader = applicationContext.getBean("repositoryObjectLoader", RepositoryObjectLoader.class);
        repositoryObjectFactory = applicationContext.getBean("repositoryObjectFactory", RepositoryObjectFactory.class);
        repoInitializer = applicationContext.getBean("repositoryInitializer", RepositoryInitializer.class);
//        messageSender = applicationContext.getBean("triplesIndexingMessageSender", IndexingMessageSender.class);
//        aclService = applicationContext.getBean("aclService", AccessControlService.class);
//        sparqlQueryService = applicationContext.getBean("sparqlQueryService", SparqlQueryService.class);

        TestHelper.setContentBase(baseAddress);

//        when(aclService.hasAccess(any(PID.class), any(AccessGroupSetImpl.class),
//                any(Permission.class))).thenReturn(true);
//
//        when(exchange.getIn()).thenReturn(message);

        generateBaseStructure();
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
        TestRepositoryDeinitializer.cleanup(fcrepoClient);
        storageLocationTestHelper.cleanupStorageLocations();
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext(
//                "/spring-test/test-fedora-container.xml",
                "/spring-test/cdr-client-container.xml",
//                "/spring-test/jms-context.xml",
                "/triples-reindexing-it-context.xml");
    }

    private void generateBaseStructure() throws Exception {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        var getRequest = new HttpGet(baseAddress + "/fcrepo/rest");
        var response = httpClient.execute(getRequest);
        System.out.println(response.getStatusLine().getStatusCode());

        repoInitializer.initializeRepository();
        rootObj = repositoryObjectLoader.getContentRootObject(getContentRootPid());

        unitObj = repositoryObjectFactory.createAdminUnit(null);
        rootObj.addMember(unitObj);

        collObj = repositoryObjectFactory.createCollectionObject(null);
        unitObj.addMember(collObj);

        folderObj1 = repositoryObjectFactory.createFolderObject(null);
        folderObj2 = repositoryObjectFactory.createFolderObject(null);
        collObj.addMember(folderObj1);
        collObj.addMember(folderObj2);

        workObj = repositoryObjectFactory.createWorkObject(null);
        folderObj1.addMember(workObj);

        PID filePid = TestHelper.makePid();
        var storageUri = storageLocationTestHelper.makeTestStorageUri(filePid);
        FileUtils.write(new File(storageUri), "content", UTF_8);
        fileObj = workObj.addDataFile(filePid, storageUri, "file.txt", null, null, null, null);
    }

    @Test
    public void testIndexingSingle() throws Exception {
        messageSender.sendIndexingOperation("user", folderObj2.getPid(), RECURSIVE_REINDEX);

        // 3 resources compose the folder
        NotifyBuilder notify = new NotifyBuilder(fcrepoTriplestoreIndexer)
                .from(indexingEndpoint)
                .whenDone(2)
                .create();

        notify.matches(5l, TimeUnit.SECONDS);

        assertIndexed(folderObj2);
    }

    @Test
    public void testIndexingFromContentRoot() throws Exception {
        PID contentPid = RepositoryPaths.getContentBasePid();
        messageSender.sendIndexingOperation("user", contentPid, RECURSIVE_REINDEX);

        // Wait for roughly all of the objects to be indexed
        NotifyBuilder notify = new NotifyBuilder(fcrepoTriplestoreIndexer)
                .from(indexingEndpoint)
                .whenCompleted(15)
                .create();

        notify.matches(25l, TimeUnit.SECONDS);

        assertIndexed(rootObj);
        assertIndexed(unitObj);
        assertIndexed(collObj);
        assertIndexed(folderObj1);
        assertIndexed(folderObj2);
        assertIndexed(workObj);
        assertIndexed(fileObj);
    }

    @Test
    public void testIndexingFromRepoRoot() throws Exception {
        // Create a deposit record
        DepositRecord depositRec = repositoryObjectFactory.createDepositRecord(null);

        PID contentPid = RepositoryPaths.getRootPid();
        messageSender.sendIndexingOperation("user", contentPid, RECURSIVE_REINDEX);

        // Wait for roughly all of the objects to be indexed
        NotifyBuilder notify = new NotifyBuilder(fcrepoTriplestoreIndexer)
                .from(indexingEndpoint)
                .whenCompleted(59)
                .create();

        notify.matches(25l, TimeUnit.SECONDS);

        assertIndexed(rootObj);
        assertIndexed(unitObj);
        assertIndexed(collObj);
        assertIndexed(folderObj1);
        assertIndexed(folderObj2);
        assertIndexed(workObj);
        assertIndexed(fileObj);

        assertIndexed(depositRec);
    }
//
//    @Test
//    public void testIndexingFromContentRoot() throws Exception {
//        PID contentPid = RepositoryPaths.getContentBasePid();
//        messageSender.sendIndexingOperation("user", contentPid, RECURSIVE_REINDEX);
//
//        // Wait for roughly all of the objects to be indexed
//        NotifyBuilder notify = new NotifyBuilder(camelContext)
//                .from(indexingEndpoint)
//                .whenCompleted(15)
//                .create();
//
//        notify.matches(25l, TimeUnit.SECONDS);
//
//        assertIndexed(rootObj);
//        assertIndexed(unitObj);
//        assertIndexed(collObj);
//        assertIndexed(folderObj1);
//        assertIndexed(folderObj2);
//        assertIndexed(workObj);
//        assertIndexed(fileObj);
//    }
//
//    @Test
//    public void testIndexingFromRepoRoot() throws Exception {
//        // Create a deposit record
//        DepositRecord depositRec = repositoryObjectFactory.createDepositRecord(null);
//
//        PID contentPid = RepositoryPaths.getRootPid();
//        messageSender.sendIndexingOperation("user", contentPid, RECURSIVE_REINDEX);
//
//        // Wait for roughly all of the objects to be indexed
//        NotifyBuilder notify = new NotifyBuilder(camelContext)
//                .from(indexingEndpoint)
//                .whenCompleted(16)
//                .create();
//
//        notify.matches(25l, TimeUnit.SECONDS);
//
//        assertIndexed(rootObj);
//        assertIndexed(unitObj);
//        assertIndexed(collObj);
//        assertIndexed(folderObj1);
//        assertIndexed(folderObj2);
//        assertIndexed(workObj);
//        assertIndexed(fileObj);
//
//        assertIndexed(depositRec);
//    }

    private void assertIndexed(RepositoryObject repoObj) {
        String query = String.format("select ?pred ?obj where { <%s> ?pred ?obj } limit 1",
                repoObj.getPid().getRepositoryPath());

        try (QueryExecution qExecution = sparqlQueryService.executeQuery(query)) {
            ResultSet resultSet = qExecution.execSelect();

            assertTrue("Object " + repoObj.getPid() + " was not indexed", resultSet.hasNext());
            return;
        }
    }
}

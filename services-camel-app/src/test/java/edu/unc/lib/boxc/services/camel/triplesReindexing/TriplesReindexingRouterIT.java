package edu.unc.lib.boxc.services.camel.triplesReindexing;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
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
import edu.unc.lib.boxc.operations.jms.indexing.IndexingMessageSender;
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
import org.apache.jena.rdf.model.Model;
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
import static org.apache.jena.rdf.model.ModelFactory.createDefaultModel;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 *
 * @author bbpennel
 *
 */
//@RunWith(CamelSpringRunner.class)
//@BootstrapWith(CamelTestContextBootstrapper.class)
//@ContextHierarchy({
//    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
//    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
//    @ContextConfiguration("/spring-test/jms-context.xml"),
//    @ContextConfiguration("/triples-reindexing-it-context.xml")
//})
//@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
//@WireMockTest(httpPort = 46887)
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

    private Model fusekiModel;
//    private FusekiServer fusekiServer;
    private FcrepoJettyServer fcrepoServer;

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

        fusekiModel = createDefaultModel();

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

        fcrepoServer = new FcrepoJettyServer();
        fcrepoServer.start();

//        final Dataset ds = new DatasetImpl(fusekiModel);
//        fusekiServer = FusekiServer.create()
//                .port(Integer.parseInt(fusekiPort))
//                .contextPath("/fuseki")
//                .add("/test", ds)
//                .build();
//        fusekiServer.start();

        TestHelper.setContentBase(baseAddress);

//        when(aclService.hasAccess(any(PID.class), any(AccessGroupSetImpl.class),
//                any(Permission.class))).thenReturn(true);
//
//        when(exchange.getIn()).thenReturn(message);

        generateBaseStructure();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (closeable != null) {
            closeable.close();
        }
//        if (fusekiServer != null) {
//            fusekiServer.stop();
//        }
        if (fcrepoServer != null) {
            fcrepoServer.stop();
        }
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext(
//                "/spring-test/test-fedora-container.xml",
                "/spring-test/cdr-client-container.xml");
//                "/spring-test/jms-context.xml",
//                "/triples-reindexing-it-context.xml");
    }

    private void generateBaseStructure() throws Exception {
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        var getRequest = new HttpGet(baseAddress + "/fcrepo/rest");
        var response = httpClient.execute(getRequest);
        System.out.println(response.getStatusLine().getStatusCode());

        repoInitializer.initializeRepository();
//        rootObj = repositoryObjectLoader.getContentRootObject(getContentRootPid());
//
//        unitObj = repositoryObjectFactory.createAdminUnit(null);
//        rootObj.addMember(unitObj);
//
//        collObj = repositoryObjectFactory.createCollectionObject(null);
//        unitObj.addMember(collObj);
//
//        folderObj1 = repositoryObjectFactory.createFolderObject(null);
//        folderObj2 = repositoryObjectFactory.createFolderObject(null);
//        collObj.addMember(folderObj1);
//        collObj.addMember(folderObj2);
//
//        workObj = repositoryObjectFactory.createWorkObject(null);
//        folderObj1.addMember(workObj);
//
//        File contentFile = File.createTempFile("test", ".txt");
//        FileUtils.write(contentFile, "content", UTF_8);
//        fileObj = workObj.addDataFile(contentFile.toPath().toUri(), "file.txt", null, null, null);
    }

    @Test
    public void testIndexingSingle() throws Exception {
//        messageSender.sendIndexingOperation("user", folderObj2.getPid(), RECURSIVE_REINDEX);
//
//        // 3 resources compose the folder
//        NotifyBuilder notify = new NotifyBuilder(camelContext)
//                .from(indexingEndpoint)
//                .whenDone(2)
//                .create();
//
//        notify.matches(5l, TimeUnit.SECONDS);
//
//        assertIndexed(folderObj2);
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

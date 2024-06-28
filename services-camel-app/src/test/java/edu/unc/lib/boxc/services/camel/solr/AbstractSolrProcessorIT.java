package edu.unc.lib.boxc.services.camel.solr;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.boxc.indexing.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.boxc.indexing.solr.test.RepositoryObjectSolrIndexer;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.ids.PIDMinter;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.ContentRootObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObject;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.api.rdf.CdrAcl;
import edu.unc.lib.boxc.model.api.services.RepositoryObjectFactory;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import edu.unc.lib.boxc.model.fcrepo.test.RepositoryObjectTreeIndexer;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationTestHelper;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.fcrepo.client.FcrepoClient;
import org.mockito.Mock;

import java.io.File;
import java.io.InputStream;
import java.net.URI;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 *
 * @author bbpennel
 *
 */
public abstract class AbstractSolrProcessorIT extends CamelSpringTestSupport {

    protected String baseAddress;
    protected File solrDataDir;
    protected EmbeddedSolrServer server;
    protected SolrUpdateDriver driver;
    protected SolrSearchService solrSearchService;
    protected AccessGroupSet accessGroups;
    protected Model queryModel;
    protected RepositoryObjectLoader repositoryObjectLoader;
    protected RepositoryObjectFactory repositoryObjectFactory;
    protected DocumentIndexingPackageFactory dipFactory;
    protected PIDMinter pidMinter;
    private RepositoryInitializer repoInitializer;
    protected RepositoryObjectTreeIndexer treeIndexer;
    protected RepositoryObjectSolrIndexer repositoryObjectSolrIndexer;
    protected StorageLocationManager locManager;
    protected StorageLocationTestHelper storageLocationTestHelper;
    protected FcrepoClient fcrepoClient;

    protected ContentRootObject rootObj;
    protected AdminUnit unitObj;
    protected CollectionObject collObj;

    @Mock
    protected Exchange exchange;
    @Mock
    protected Message message;

    protected void initCommon() {
        baseAddress = applicationContext.getBean("baseAddress", String.class);
        fcrepoClient = applicationContext.getBean(FcrepoClient.class);
        solrDataDir = applicationContext.getBean("solrDataDir", File.class);
        server = applicationContext.getBean(EmbeddedSolrServer.class);
        driver = applicationContext.getBean(SolrUpdateDriver.class);
        solrSearchService = applicationContext.getBean(SolrSearchService.class);
        accessGroups = applicationContext.getBean("accessGroups", AccessGroupSet.class);
        queryModel = applicationContext.getBean("queryModel", Model.class);
        repositoryObjectLoader = applicationContext.getBean("repositoryObjectLoader", RepositoryObjectLoader.class);
        repositoryObjectFactory = applicationContext.getBean(RepositoryObjectFactory.class);
        dipFactory = applicationContext.getBean(DocumentIndexingPackageFactory.class);
        pidMinter = applicationContext.getBean(PIDMinter.class);
        repoInitializer = applicationContext.getBean(RepositoryInitializer.class);
        treeIndexer = applicationContext.getBean(RepositoryObjectTreeIndexer.class);
        repositoryObjectSolrIndexer = applicationContext.getBean(RepositoryObjectSolrIndexer.class);
        locManager = applicationContext.getBean(StorageLocationManager.class);
        storageLocationTestHelper = applicationContext.getBean(StorageLocationTestHelper.class);
    }

    protected void generateBaseStructure() throws Exception {
        repoInitializer.initializeRepository();
        rootObj = repositoryObjectLoader.getContentRootObject(getContentRootPid());

        PID unitPid = pidMinter.mintContentPid();
        Model unitModel = ModelFactory.createDefaultModel();
        Resource unitResc = unitModel.getResource(unitPid.getRepositoryPath());
        unitResc.addProperty(CdrAcl.unitOwner, "admin");
        unitObj = repositoryObjectFactory.createAdminUnit(unitPid, unitModel);
        rootObj.addMember(unitObj);

        PID collPid = pidMinter.mintContentPid();
        Model collModel = ModelFactory.createDefaultModel();
        Resource collResc = collModel.getResource(collPid.getRepositoryPath());
        collResc.addProperty(CdrAcl.canViewOriginals, AUTHENTICATED_PRINC);
        collObj = repositoryObjectFactory.createCollectionObject(collPid, collModel);
        unitObj.addMember(collObj);
    }

    protected void setMessageTarget(RepositoryObject obj) {
        when(message.getHeader(eq(FCREPO_URI)))
                .thenReturn(obj.getPid().getRepositoryPath());
    }

    protected void indexObjectsInTripleStore() throws Exception {
        treeIndexer.indexAll(baseAddress);
    }

    protected URI makeContentUri(String content) throws Exception {
        var pid = TestHelper.makePid();
        var storageUri = storageLocationTestHelper.makeTestStorageUri(pid);
        var contentFile = new File(storageUri);
        FileUtils.write(contentFile, content, UTF_8);
        return storageUri;
    }

    protected InputStream streamResource(String resourcePath) throws Exception {
        return getClass().getResourceAsStream(resourcePath);
    }
}

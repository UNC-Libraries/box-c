package edu.unc.lib.boxc.services.camel.solr;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.AUTHENTICATED_PRINC;
import static edu.unc.lib.boxc.model.fcrepo.ids.RepositoryPaths.getContentRootPid;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.fcrepo.camel.FcrepoHeaders.FCREPO_URI;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.UUID;

import edu.unc.lib.boxc.indexing.solr.test.RepositoryObjectSolrIndexer;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.persist.api.storage.StorageLocationManager;
import edu.unc.lib.boxc.persist.impl.storage.StorageLocationTestHelper;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.test.spring.CamelSpringRunner;
import org.apache.camel.test.spring.CamelTestContextBootstrapper;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.indexing.solr.indexing.DocumentIndexingPackageFactory;
import edu.unc.lib.boxc.indexing.solr.indexing.SolrUpdateDriver;
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
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;

/**
 *
 * @author bbpennel
 *
 */
@RunWith(CamelSpringRunner.class)
@BootstrapWith(CamelTestContextBootstrapper.class)
@ContextHierarchy({
    @ContextConfiguration("/spring-test/test-fedora-container.xml"),
    @ContextConfiguration("/spring-test/cdr-client-container.xml"),
    @ContextConfiguration("/spring-test/solr-indexing-context.xml"),
    @ContextConfiguration("/solr-indexing-it-context.xml")
})
public abstract class AbstractSolrProcessorIT {

    @Autowired
    protected String baseAddress;

    @Autowired
    protected File solrDataDir;
    @Autowired
    protected EmbeddedSolrServer server;
    @Autowired
    protected SolrUpdateDriver driver;
    @Autowired
    protected SolrSearchService solrSearchService;
    @javax.annotation.Resource(name = "accessGroups")
    protected AccessGroupSet accessGroups;

    @Autowired
    protected Model queryModel;
    @javax.annotation.Resource(name = "repositoryObjectLoader")
    protected RepositoryObjectLoader repositoryObjectLoader;
    @Autowired
    protected RepositoryObjectFactory repositoryObjectFactory;
    @Autowired
    protected DocumentIndexingPackageFactory dipFactory;
    @Autowired
    protected PIDMinter pidMinter;
    @Autowired
    private RepositoryInitializer repoInitializer;
    @Autowired
    protected RepositoryObjectTreeIndexer treeIndexer;
    @Autowired
    protected RepositoryObjectSolrIndexer repositoryObjectSolrIndexer;
    @Autowired
    protected StorageLocationManager locManager;

    protected ContentRootObject rootObj;
    protected AdminUnit unitObj;
    protected CollectionObject collObj;

    @Mock
    protected Exchange exchange;
    @Mock
    protected Message message;

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
        var loc1 = locManager.getStorageLocationById(StorageLocationTestHelper.LOC1_ID);
        var storageUri = loc1.getNewStorageUri(PIDs.get(UUID.randomUUID().toString()));
        var contentFile = new File(storageUri);
        contentFile.deleteOnExit();
        FileUtils.write(contentFile, content, UTF_8);
        return storageUri;
    }

    protected InputStream streamResource(String resourcePath) throws Exception {
        return getClass().getResourceAsStream(resourcePath);
    }
}

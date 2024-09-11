package edu.unc.lib.boxc.search.solr.test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.CoreContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;

public class BaseEmbeddedSolrTest {
    private static final Logger log = LoggerFactory.getLogger(BaseEmbeddedSolrTest.class);

    protected SolrSettings solrSettings;

    protected SearchSettings searchSettings;

    protected EmbeddedSolrServer server;

    protected CoreContainer container;

    @TempDir
    public Path dataBaseDir;

    private File dataDir;

    @BeforeEach
    public void setUp() throws Exception {
        dataDir = dataBaseDir.resolve("dataDir").toFile();
        Files.createDirectory(dataBaseDir.resolve("dataDir"));

        System.setProperty("solr.data.dir", dataDir.getAbsolutePath());
        container = CoreContainer.createAndLoad(Paths.get("../etc/solr-config").toAbsolutePath(),
                Paths.get("../etc/solr-config/solr.xml").toAbsolutePath());

        server = new EmbeddedSolrServer(container, "access");

        Properties solrProps = new Properties();
        solrProps.load(this.getClass().getResourceAsStream("/solr.properties"));
        solrSettings = new SolrSettings();
        solrSettings.setProperties(solrProps);

        Properties searchProps = new Properties();
        searchProps.load(this.getClass().getResourceAsStream("/search.properties"));
        searchSettings = new SearchSettings();
        searchSettings.setProperties(searchProps);
    }

    protected SolrDocumentList getDocumentList(String query, String fieldList) throws Exception {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("q", query);
        params.set("fl", fieldList);
        QueryResponse qResp = server.query(params);
        return qResp.getResults();
    }

    protected SolrDocumentList getDocumentList() throws Exception {
        return getDocumentList("*:*", "id,resourceType,timestamp,_version_");
    }

    protected void index(SolrInputDocument doc) throws Exception {
        server.add(doc);
        server.commit();
    }

    protected void index(List<SolrInputDocument> docs) throws Exception {
        server.add(docs);
        server.commit();
    }

    @AfterEach
    public void tearDown() throws Exception {
        container.shutdown();
        server.close();
        log.debug("Cleaning up data directory");
        FileUtils.deleteDirectory(dataDir);
    }
}

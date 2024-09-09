package edu.unc.lib.boxc.indexing.solr.action;

import java.io.File;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.CoreContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.indexing.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;

public class BaseEmbeddedSolrTest extends Assertions {
    private static final Logger log = LoggerFactory.getLogger(BaseEmbeddedSolrTest.class);

    protected SolrSettings solrSettings;

    protected EmbeddedSolrServer server;

    protected CoreContainer container;

    protected SolrUpdateDriver driver;

    private File dataDir;

    @BeforeEach
    public void setUp() throws Exception {
        dataDir = new File("target/solr_data/");
        dataDir.mkdir();

        System.setProperty("solr.data.dir", dataDir.getAbsolutePath());
        container = CoreContainer.createAndLoad(Paths.get("../etc/solr-config").toAbsolutePath(),
                Paths.get("../etc/solr-config/solr.xml").toAbsolutePath());

        server = new EmbeddedSolrServer(container, "access");

        Properties solrProps = new Properties();
        solrProps.load(this.getClass().getResourceAsStream("/solr.properties"));
        solrSettings = new SolrSettings();
        solrSettings.setProperties(solrProps);

        driver = new SolrUpdateDriver();
        driver.setSolrClient(server);
        driver.setUpdateSolrClient(server);
        driver.setSolrSettings(solrSettings);
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

    @AfterEach
    public void tearDown() throws Exception {
        server.close();
        container.shutdown();
        log.debug("Cleaning up data directory");
        FileUtils.deleteDirectory(dataDir);
    }
}

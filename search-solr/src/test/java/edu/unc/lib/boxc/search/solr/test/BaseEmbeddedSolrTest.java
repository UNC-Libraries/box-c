package edu.unc.lib.boxc.search.solr.test;

import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;

public class BaseEmbeddedSolrTest {
    private static final Logger log = LoggerFactory.getLogger(BaseEmbeddedSolrTest.class);

    protected SolrSettings solrSettings;

    protected SearchSettings searchSettings;

    protected SolrClient server;

    @BeforeEach
    public void setUp() throws Exception {
        Properties solrProps = new Properties();
        solrProps.load(this.getClass().getResourceAsStream("/solr.properties"));
        solrSettings = new SolrSettings();
        solrSettings.setProperties(solrProps);

        Properties searchProps = new Properties();
        searchProps.load(this.getClass().getResourceAsStream("/search.properties"));
        searchSettings = new SearchSettings();
        searchSettings.setProperties(searchProps);

        server = solrSettings.getSolrClient();
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
        log.debug("Tearing down Solr server and cleaning up records");
        server.deleteByQuery("*:*");
        server.commit();
        server.close();
    }
}

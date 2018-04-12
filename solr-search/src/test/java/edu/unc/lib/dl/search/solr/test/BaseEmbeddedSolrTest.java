/**
 * Copyright 2008 The University of North Carolina at Chapel Hill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.unc.lib.dl.search.solr.test;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.CoreContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

public class BaseEmbeddedSolrTest {
    private static final Logger log = LoggerFactory.getLogger(BaseEmbeddedSolrTest.class);

    protected SolrSettings solrSettings;

    protected SearchSettings searchSettings;

    protected EmbeddedSolrServer server;

    protected CoreContainer container;

    @Rule
    public TemporaryFolder dataBaseDir = new TemporaryFolder();

    private File dataDir;

    @Before
    public void setUp() throws Exception {
        dataDir = dataBaseDir.newFolder();

        System.setProperty("solr.data.dir", dataDir.getAbsolutePath());
        container = new CoreContainer("../etc/solr-config");
        container.load();

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

    @After
    public void tearDown() throws Exception {
        server.close();
        log.debug("Cleaning up data directory");
        FileUtils.deleteDirectory(dataDir);
    }
}

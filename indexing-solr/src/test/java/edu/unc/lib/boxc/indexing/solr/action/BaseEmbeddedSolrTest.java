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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.boxc.indexing.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;

public class BaseEmbeddedSolrTest extends Assert {
    private static final Logger log = LoggerFactory.getLogger(BaseEmbeddedSolrTest.class);

    protected SolrSettings solrSettings;

    protected EmbeddedSolrServer server;

    protected CoreContainer container;

    protected SolrUpdateDriver driver;

    private File dataDir;

    @Before
    public void setUp() throws Exception {
        dataDir = new File("target/solr_data/");
        dataDir.mkdir();

        System.setProperty("solr.data.dir", dataDir.getAbsolutePath());
        container = CoreContainer.createAndLoad(Paths.get("../etc/solr-config"),
                Paths.get("../etc/solr-config/solr.xml"));
        container.load();

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

    @After
    public void tearDown() throws Exception {
        server.close();
        container.shutdown();
        log.debug("Cleaning up data directory");
        FileUtils.deleteDirectory(dataDir);
    }
}

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
package edu.unc.lib.dl.ui.service;

import static edu.unc.lib.dl.test.TestHelpers.setField;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.fcrepo4.GlobalPermissionEvaluator;
import edu.unc.lib.dl.fcrepo4.PIDs;
import edu.unc.lib.dl.fedora.PID;
import edu.unc.lib.dl.rdf.Cdr;
import edu.unc.lib.dl.search.solr.service.SearchStateFactory;
import edu.unc.lib.dl.search.solr.util.FacetFieldUtil;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

/**
 * @author bbpennel
 * @date Jan 23, 2015
 */
public class AbstractSolrQueryLayerTest {
    protected EmbeddedSolrServer server;

    protected CoreContainer container;

    protected SolrQueryLayerService queryLayer;

    @Mock
    protected GlobalPermissionEvaluator globalPermissionEvaluator;
    protected SearchStateFactory stateFactory;
    protected SearchSettings searchSettings;
    protected SolrSettings solrSettings;
    protected FacetFieldUtil facetUtil;

    private File dataDir;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    protected PID rootPid;
    protected PID coll1Pid;
    protected PID coll2Pid;
    protected PID folderPid;
    protected PID workPid;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        dataDir = new File("target/solr_data/");
        dataDir.mkdir();

        System.setProperty("solr.data.dir", dataDir.getAbsolutePath());
        container = new CoreContainer("src/test/resources/config");
        container.load();

        server = new EmbeddedSolrServer(container, "access");

        searchSettings = new SearchSettings();
        Properties properties = new Properties();
        properties.load(new FileInputStream(new File("src/test/resources/search.properties")));
        searchSettings.setProperties(properties);

        solrSettings = new SolrSettings();
        Properties solrProperties = new Properties();
        solrProperties.load(new FileInputStream(new File("src/test/resources/solr.properties")));
        solrSettings.setProperties(solrProperties);

        stateFactory = new SearchStateFactory();
        stateFactory.setSearchSettings(searchSettings);

        facetUtil = new FacetFieldUtil();
        facetUtil.setSearchSettings(searchSettings);
        facetUtil.setSolrSettings(solrSettings);

        queryLayer = new SolrQueryLayerService();
        queryLayer.setSearchSettings(searchSettings);
        queryLayer.setSolrSettings(solrSettings);
        queryLayer.setSearchStateFactory(stateFactory);
        queryLayer.setFacetFieldUtil(facetUtil);
        queryLayer.setGlobalPermissionEvaluator(globalPermissionEvaluator);
        setField(queryLayer, "solrClient", server);
    }

    @After
    public void tearDown() throws Exception {
        server.close();
        FileUtils.forceDelete(dataDir);
    }

    protected List<SolrInputDocument> populate() {
        List<SolrInputDocument> docs = new ArrayList<>();

        SolrInputDocument newDoc = new SolrInputDocument();
        rootPid = makePid();
        String rootId = rootPid.getId();
        newDoc.addField("title", "Collections Root");
        addAccessFields(newDoc, Cdr.ContentRoot.getLocalName(), rootId);
        addAncestors(newDoc, true, rootId);
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        coll1Pid = makePid();
        String coll1Id = coll1Pid.getId();
        newDoc.addField("title", "coll 1");
        addAccessFields(newDoc, "Collection", coll1Id);
        addAncestors(newDoc, true, rootId, coll1Id);
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        coll2Pid = makePid();
        String coll2Id = coll2Pid.getId();
        newDoc.addField("title", "coll 2");
        addAccessFields(newDoc, "Collection", coll2Id);
        addAncestors(newDoc, true, rootId, coll2Id);
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        folderPid = makePid();
        String folderId = folderPid.getId();
        newDoc.addField("title", "folder 1 coll 1");
        addAccessFields(newDoc, "Folder", folderId);
        addAncestors(newDoc, true, rootId, coll1Id, folderId);
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        workPid = makePid();
        String workId = workPid.getId();
        newDoc.addField("title", "work 1 folder 1 coll 1");
        addAccessFields(newDoc, "Work", workId);
        addAncestors(newDoc, true, rootId, coll1Id, folderId, workId);
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        PID file1Pid = makePid();
        String file1Id = file1Pid.getId();
        newDoc.addField("title", "file1 work 1 folder 1 coll 1");
        addAccessFields(newDoc, "File", file1Id, workId);
        addAncestors(newDoc, false, rootId, coll1Id, folderId, workId, file1Id);
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        PID file2Pid = makePid();
        String file2Id = file2Pid.getId();
        newDoc.addField("title", "file2 work 1 folder 1 coll 1");
        addAccessFields(newDoc, "File", file2Id, workId);
        addAncestors(newDoc, false, rootId, coll1Id, folderId, workId, file2Id);
        docs.add(newDoc);

        return docs;
    }

    private void addAncestors(SolrInputDocument doc, boolean isContainer, String... ids) {
        List<String> ancestorPath = new ArrayList<>();
        String ancestorIds = "";
        for (int i = 0; i < ids.length; i++) {
            if (i < ids.length - 1) {
                ancestorPath.add((i + 1) + "," + ids[i]);
            }
            if (i < ids.length - 1 || isContainer) {
                ancestorIds += "/" + ids[i];
            }
        }
        doc.addField("ancestorIds", ancestorIds);
        doc.addField("ancestorPath", ancestorPath);
    }

    protected void addAccessFields(SolrInputDocument doc, String type, String id) {
        addAccessFields(doc, type, id, id);
    }

    protected void addAccessFields(SolrInputDocument doc, String type, String id, String rollup) {
        doc.addField("id", id);
        doc.addField("rollup", rollup);
        doc.addField("resourceType", type);
        doc.addField("roleGroup", "public admin");
        doc.addField("readGroup", "public");
    }

    protected PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }
}

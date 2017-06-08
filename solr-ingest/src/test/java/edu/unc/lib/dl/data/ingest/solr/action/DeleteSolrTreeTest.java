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
package edu.unc.lib.dl.data.ingest.solr.action;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.model.SimpleIdRequest;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.util.SolrSettings;
import edu.unc.lib.dl.util.IndexingActionType;

public class DeleteSolrTreeTest extends BaseEmbeddedSolrTest {

    @Mock
    private SearchSettings searchSettings;
    @Mock
    private SolrSettings solrSettings;
    @Mock
    private SolrSearchService solrSearchService;

    private DeleteSolrTreeAction action;

    @Mock
    private BriefObjectMetadataBean metadata;

    @Before
    public void setup() throws SolrServerException, IOException {
        initMocks(this);

        when(searchSettings.getResourceTypeCollection()).thenReturn("Collection");
        when(searchSettings.getResourceTypeFolder()).thenReturn("Folder");

        when(solrSettings.getFieldName(eq(SearchFieldKeys.ANCESTOR_PATH.name()))).thenReturn("ancestorPath");
        when(solrSettings.getFieldName(eq(SearchFieldKeys.ID.name()))).thenReturn("id");

        when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(metadata);

        action = new DeleteSolrTreeAction();

        action.setSolrUpdateDriver(driver);
        action.setSolrSettings(solrSettings);
        action.setSearchSettings(searchSettings);
        action.setSolrSearchService(solrSearchService);
        action.setAccessGroups(new AccessGroupSet("admin"));
        action.setSolrSearchService(solrSearchService);

        server.add(populate());
        server.commit();
    }

    @Test
    public void deleteTree() throws Exception {

        when(metadata.getId()).thenReturn("uuid:2");
        when(metadata.getAncestorPath()).thenReturn(Arrays.asList("1,uuid:1"));
        when(metadata.getResourceType()).thenReturn("Collection");
        CutoffFacet path = mock(CutoffFacet.class);
        when(path.getSearchValue()).thenReturn("2,uuid:2");
        when(metadata.getPath()).thenReturn(path);

        action.performAction(new SolrUpdateRequest("uuid:2", IndexingActionType.DELETE_SOLR_TREE));
        server.commit();

        SolrDocumentList docListAfter = getDocumentList();

        assertEquals(2, docListAfter.getNumFound());

        for (SolrDocument docAfter : docListAfter) {
            String id = (String) docAfter.getFieldValue("id");
            if ("uuid:2".equals(id) || "uuid:6".equals(id))
                fail("Object was not deleted: " + id);
        }
    }

    @Test
    public void deleteNonexistent() throws Exception {

        when(solrSearchService.getObjectById(any(SimpleIdRequest.class))).thenReturn(null);

        action.performAction(new SolrUpdateRequest("uuid:doesnotexist", IndexingActionType.DELETE_SOLR_TREE));
        server.commit();

        SolrDocumentList docListAfter = getDocumentList();

        assertEquals(4, docListAfter.getNumFound());
    }

    @Test
    public void deleteSimple() throws Exception {

        when(metadata.getResourceType()).thenReturn("File");

        action.performAction(new SolrUpdateRequest("uuid:6", IndexingActionType.DELETE_SOLR_TREE));
        server.commit();

        SolrDocumentList docListAfter = getDocumentList();

        assertEquals("One object should have been removed", 3, docListAfter.getNumFound());

        for (SolrDocument docAfter : docListAfter) {
            String id = (String) docAfter.getFieldValue("id");
            if ("uuid:6".equals(id))
                fail("Object was not deleted: " + id);
        }
    }

    @Test
    public void deleteEverything() throws Exception {

        action.performAction(new SolrUpdateRequest(AbstractIndexingAction.TARGET_ALL,
                IndexingActionType.DELETE_SOLR_TREE));
        server.commit();

        SolrDocumentList docListAfter = getDocumentList();

        assertEquals("Index should be empty", 0, docListAfter.getNumFound());
    }

    protected List<SolrInputDocument> populate() {
        List<SolrInputDocument> docs = new ArrayList<SolrInputDocument>();

        SolrInputDocument newDoc = new SolrInputDocument();
        newDoc.addField("title", "Collections");
        newDoc.addField("id", "uuid:1");
        newDoc.addField("rollup", "uuid:1");
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", "/uuid:1");
        newDoc.addField("resourceType", "Folder");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "A collection");
        newDoc.addField("id", "uuid:2");
        newDoc.addField("rollup", "uuid:2");
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", "/uuid:1/uuid:2");
        newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1"));
        newDoc.addField("resourceType", "Collection");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "File");
        newDoc.addField("id", "uuid:6");
        newDoc.addField("rollup", "uuid:6");
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", "/uuid:1/uuid:2");
        newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1", "2,uuid:2"));
        newDoc.addField("resourceType", "File");
        docs.add(newDoc);

        newDoc = new SolrInputDocument();
        newDoc.addField("title", "Second collection");
        newDoc.addField("id", "uuid:3");
        newDoc.addField("rollup", "uuid:3");
        newDoc.addField("roleGroup", "public admin");
        newDoc.addField("readGroup", "public");
        newDoc.addField("adminGroup", "admin");
        newDoc.addField("ancestorIds", "/uuid:1/uuid:3");
        newDoc.addField("ancestorPath", Arrays.asList("1,uuid:1"));
        newDoc.addField("resourceType", "Collection");
        docs.add(newDoc);

        return docs;
    }
}

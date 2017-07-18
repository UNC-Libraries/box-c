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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.data.ingest.solr.ProcessingStatus;
import edu.unc.lib.dl.data.ingest.solr.SolrUpdateRequest;
import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import edu.unc.lib.dl.search.solr.service.SolrSearchService;
import edu.unc.lib.dl.search.solr.util.SearchFieldKeys;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.util.SolrSettings;
import edu.unc.lib.dl.test.TestHelpers;
import edu.unc.lib.dl.util.IndexingActionType;

public class IndexTreeInplaceActionTest extends UpdateTreeActionTest {
    private static final Logger log = LoggerFactory.getLogger(IndexTreeInplaceActionTest.class);

    @Mock
    private SearchSettings searchSettings;
    @Mock
    private SolrSettings solrSettings;
    private SolrSearchService solrSearchService;
    @Mock
    private BriefObjectMetadataBean metadata;
    @Mock
    CutoffFacet path;

    @Override
    @Before
    public void setup() throws Exception {
        super.setup();

        when(solrSettings.getFieldName(eq(SearchFieldKeys.ID.name()))).thenReturn("id");
        when(solrSettings.getFieldName(eq(SearchFieldKeys.ANCESTOR_PATH.name()))).thenReturn("ancestorPath");
        when(solrSettings.getFieldName(eq(SearchFieldKeys.TIMESTAMP.name()))).thenReturn("timestamp");
        ((IndexTreeInplaceAction) action).setSolrSettings(solrSettings);

        when(path.getSearchValue()).thenReturn("");
        when(metadata.getPath()).thenReturn(path);

        solrSearchService = new SolrSearchService();
        solrSearchService.setSolrSettings(solrSettings);
        solrSearchService.setSearchSettings(searchSettings);
        TestHelpers.setField(solrSearchService, "server", server);
        action.setSolrSearchService(solrSearchService);
        action.setAccessGroups(new AccessGroupSet("admin"));
    }

    @Override
    protected UpdateTreeAction getAction() {
        return new IndexTreeInplaceAction();
    }

    @Test
    public void verifyOrphanCleanup() throws Exception {

        when(metadata.getId()).thenReturn("uuid:2");
        when(metadata.getAncestorPath()).thenReturn(Arrays.asList("1,uuid:1"));
        when(path.getSearchValue()).thenReturn("2,uuid:2");

        SolrDocumentList docListBefore = getDocumentList();

        SolrUpdateRequest request = new SolrUpdateRequest("uuid:2", IndexingActionType.RECURSIVE_ADD);
        request.setStatus(ProcessingStatus.ACTIVE);

        action.performAction(request);
        server.commit();

        SolrDocumentList docListAfter = getDocumentList();

        log.debug("Docs: " + docListBefore);
        log.debug("Docs: " + docListAfter);

        // Verify that the number of results has decreased
        assertEquals(6, docListBefore.getNumFound());
        assertEquals(5, docListAfter.getNumFound());

        // Verify that the orphan is not in the new result set
        for (SolrDocument docAfter : docListAfter) {
            String id = (String) docAfter.getFieldValue("id");
            assertFalse("uuid:5".equals(id));
        }

    }

    @Test
    public void testIndexAll() throws Exception {

        SolrDocumentList docListBefore = getDocumentList();

        SolrUpdateRequest request = new SolrUpdateRequest(UpdateTreeAction.TARGET_ALL,
                IndexingActionType.RECURSIVE_ADD);
        request.setStatus(ProcessingStatus.ACTIVE);

        action.performAction(request);
        server.commit();

        SolrDocumentList docListAfter = getDocumentList();

        log.debug("Docs: " + docListBefore);
        log.debug("Docs: " + docListAfter);

        // Verify that the number of results has decreased
        assertEquals(6, docListBefore.getNumFound());
        assertEquals(5, docListAfter.getNumFound());

        // Verify that the orphan is not in the new result set
        for (SolrDocument docAfter : docListAfter) {
            String id = (String) docAfter.getFieldValue("id");
            assertFalse("uuid:5".equals(id));
        }
    }

    @Test(expected = IndexingException.class)
    public void testNoAncestorBean() throws Exception {

        server.deleteById("uuid:2");
        server.commit();

        SolrUpdateRequest request = new SolrUpdateRequest("uuid:2", IndexingActionType.RECURSIVE_ADD);
        request.setStatus(ProcessingStatus.ACTIVE);

        action.performAction(request);
    }
}

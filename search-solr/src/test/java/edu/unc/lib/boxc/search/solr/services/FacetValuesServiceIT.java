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
package edu.unc.lib.boxc.search.solr.services;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.ContentCategory;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.facets.FacetFieldObject;
import edu.unc.lib.boxc.search.api.facets.SearchFacet;
import edu.unc.lib.boxc.search.api.requests.FacetValuesRequest;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import edu.unc.lib.boxc.search.solr.test.BaseEmbeddedSolrTest;
import edu.unc.lib.boxc.search.solr.test.TestCorpus;
import edu.unc.lib.boxc.search.solr.utils.AccessRestrictionUtil;
import edu.unc.lib.boxc.search.solr.utils.FacetFieldUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author bbpennel
 */
public class FacetValuesServiceIT extends BaseEmbeddedSolrTest {
    private final static String SUBJECT_1 = "Oral History";
    private final static String SUBJECT_2 = "North Carolina";
    private final static String SUBJECT_3 = "Digital Repositories";
    private final static String SUBJECT_4 = "Voting--North Carolina";
    private final static String SUBJECT_5 = "Biography";
    private final static String SUBJECT_6 = "University of North Carolina";
    private final static String SUBJECT_7 = "Chapel Hill";
    private final static String SUBJECT_8 = "Boxy";

    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;
    private FacetFieldFactory facetFieldFactory;
    private FacetFieldUtil facetFieldUtil;
    private AccessRestrictionUtil accessRestrictionUtil;
    private SolrSearchService searchService;

    private FacetValuesService facetValuesService;

    private FacetValueTestCorpus testCorpus;
    private boolean corpusLoaded;
    private AccessGroupSet accessGroups;

    public FacetValuesServiceIT() {
        testCorpus = new FacetValueTestCorpus();
    }

    @Before
    public void setupFacets() throws Exception {
        initMocks(this);

        accessRestrictionUtil = new AccessRestrictionUtil();
        accessRestrictionUtil.setSearchSettings(searchSettings);
        accessRestrictionUtil.setGlobalPermissionEvaluator(globalPermissionEvaluator);

        facetFieldFactory = new FacetFieldFactory();
        facetFieldFactory.setSearchSettings(searchSettings);
        facetFieldFactory.setSolrSettings(solrSettings);

        facetFieldUtil = new FacetFieldUtil();
        facetFieldUtil.setSearchSettings(searchSettings);
        facetFieldUtil.setSolrSettings(solrSettings);

        searchService = new SolrSearchService();
        searchService.setSolrSettings(solrSettings);
        searchService.setSearchSettings(searchSettings);
        searchService.setFacetFieldUtil(facetFieldUtil);
        searchService.setAccessRestrictionUtil(accessRestrictionUtil);
        searchService.setFacetFieldFactory(facetFieldFactory);
        searchService.setSolrClient(server);

        facetValuesService = new FacetValuesService();
        facetValuesService.setSearchService(searchService);
        facetValuesService.setFacetFieldFactory(facetFieldFactory);

        accessGroups = new AccessGroupSetImpl("unitOwner", PUBLIC_PRINC);

        if (!corpusLoaded) {
            corpusLoaded = true;
            index(testCorpus.populate());
        }
    }

    @Test
    public void singlePageOfResultsTest() throws Exception {
        var request = buildFacetRequest();

        var result = facetValuesService.listValues(request);
        assertUnfilteredCountSort(result);
    }

    private void assertUnfilteredCountSort(FacetFieldObject result) throws Exception {
        assertEquals(SearchFieldKey.SUBJECT.getSolrField(), result.getName());
        assertEquals(8, result.getValues().size());

        assertValuePresent(result, 0, SUBJECT_5, 4);
        assertValuePresent(result, 1, SUBJECT_1, 3);
        assertValuePresent(result, 2, SUBJECT_7, 2);
        assertValuePresent(result, 3, SUBJECT_2, 2);
        assertValuePresent(result, 4, SUBJECT_8, 1);
        assertValuePresent(result, 5, SUBJECT_3, 1);
        assertValuePresent(result, 6, SUBJECT_6, 1);
        assertValuePresent(result, 7, SUBJECT_4, 1);
    }

    @Test
    public void pagedResultsTest() throws Exception {
        var request = buildFacetRequest();
        request.setRows(3);

        var result = facetValuesService.listValues(request);
        assertEquals(3, result.getValues().size());
        assertValuePresent(result, 0, SUBJECT_5, 4);
        assertValuePresent(result, 1, SUBJECT_1, 3);
        assertValuePresent(result, 2, SUBJECT_7, 2);

        request.setStart(3);
        var result2 = facetValuesService.listValues(request);
        assertEquals(3, result2.getValues().size());
        assertValuePresent(result2, 0, SUBJECT_2, 2);
        assertValuePresent(result2, 1, SUBJECT_8, 1);
        assertValuePresent(result2, 2, SUBJECT_3, 1);

        request.setStart(6);
        var result3 = facetValuesService.listValues(request);
        assertEquals(2, result3.getValues().size());
        assertValuePresent(result3, 0, SUBJECT_6, 1);
        assertValuePresent(result3, 1, SUBJECT_4, 1);

        request.setStart(9);
        var result4 = facetValuesService.listValues(request);
        assertTrue(result4.getValues().isEmpty());
    }

    @Test
    public void singlePageSortByIndexTest() throws Exception {
        var request = buildFacetRequest();
        request.setSort("index");

        var result = facetValuesService.listValues(request);
        assertEquals(SearchFieldKey.SUBJECT.getSolrField(), result.getName());
        assertEquals(8, result.getValues().size());

        assertValuePresent(result, 0, SUBJECT_5, 4);
        assertValuePresent(result, 1, SUBJECT_8, 1);
        assertValuePresent(result, 2, SUBJECT_7, 2);
        assertValuePresent(result, 3, SUBJECT_3, 1);
        assertValuePresent(result, 4, SUBJECT_2, 2);
        assertValuePresent(result, 5, SUBJECT_1, 3);
        assertValuePresent(result, 6, SUBJECT_6, 1);
        assertValuePresent(result, 7, SUBJECT_4, 1);
    }

    @Test
    public void filterByRootIdTest() throws Exception {
        var request = buildFacetRequest();
        request.getBaseSearchRequest().setRootPid(testCorpus.coll1Pid);

        var result = facetValuesService.listValues(request);
        assertEquals(SearchFieldKey.SUBJECT.getSolrField(), result.getName());
        assertEquals(5, result.getValues().size());

        assertValuePresent(result, 0, SUBJECT_2, 2);
        assertValuePresent(result, 1, SUBJECT_1, 2);
        assertValuePresent(result, 2, SUBJECT_5, 1);
        assertValuePresent(result, 3, SUBJECT_3, 1);
        assertValuePresent(result, 4, SUBJECT_4, 1);
    }

    @Test
    public void searchForTermTest() throws Exception {
        var request = buildFacetRequest();
        request.getBaseSearchRequest().getSearchState().setSearchFields(
                Map.of(SearchFieldKey.DEFAULT_INDEX.name(), "north"));

        var result = facetValuesService.listValues(request);
        assertEquals(SearchFieldKey.SUBJECT.getSolrField(), result.getName());
        assertEquals(3, result.getValues().size());

        assertValuePresent(result, 0, SUBJECT_2, 2);
        assertValuePresent(result, 1, SUBJECT_6, 1);
        assertValuePresent(result, 2, SUBJECT_4, 1);
    }

    @Test
    public void facetBeingRetrievedIsSelectedTest() throws Exception {
        var request = buildFacetRequest();
        request.getBaseSearchRequest().getSearchState().setFacet(new GenericFacet(SearchFieldKey.SUBJECT, SUBJECT_1));

        var result = facetValuesService.listValues(request);
        assertUnfilteredCountSort(result);
    }

    @Test
    public void sortByCountTest() throws Exception {
        var request = buildFacetRequest();
        request.setSort("count");

        var result = facetValuesService.listValues(request);
        assertUnfilteredCountSort(result);
    }

    @Test
    public void invalidSortTest() throws Exception {
        var request = buildFacetRequest();
        request.setSort("boxy");

        var result = facetValuesService.listValues(request);
        // Reverts to default sort
        assertUnfilteredCountSort(result);
    }

    @Test
    public void invalidStartTest() throws Exception {
        var request = buildFacetRequest();
        request.setStart(-1);

        var result = facetValuesService.listValues(request);
        // Reverts to 0 start
        assertUnfilteredCountSort(result);
    }

    @Test
    public void invalidRowsTest() throws Exception {
        var request = buildFacetRequest();
        request.setRows(0);

        var result = facetValuesService.listValues(request);
        // Reverts to default rows
        assertUnfilteredCountSort(result);
    }

    @Test
    public void withoutAccessToPrivateFoldersTest() throws Exception {
        var request = buildFacetRequest();
        request.getBaseSearchRequest().setAccessGroups(new AccessGroupSetImpl(PUBLIC_PRINC));

        var result = facetValuesService.listValues(request);
        assertEquals(7, result.getValues().size());

        assertValuePresent(result, 0, SUBJECT_5, 4);
        assertValuePresent(result, 1, SUBJECT_1, 3);
        assertValuePresent(result, 2, SUBJECT_2, 2);
        assertValuePresent(result, 3, SUBJECT_7, 1);
        assertValuePresent(result, 4, SUBJECT_3, 1);
        assertValuePresent(result, 5, SUBJECT_6, 1);
        assertValuePresent(result, 6, SUBJECT_4, 1);
    }

    private FacetValuesRequest buildFacetRequest() {
        var request = new FacetValuesRequest(SearchFieldKey.SUBJECT);
        var baseRequest = new SearchRequest();
        baseRequest.setSearchState(new SearchState());
        baseRequest.setAccessGroups(accessGroups);
        request.setBaseSearchRequest(baseRequest);
        return request;
    }

    private void assertValuePresent(FacetFieldObject result, int index, String expectedValue, int expectedCount) {
        var values = result.getValues();
        if (index >= values.size()) {
            var vals = values.stream().map(SearchFacet::getSearchValue).collect(Collectors.joining(", "));
            fail("Index " + index + " out of bounds, expecting " + expectedValue + " but values were " + vals);
        }
        var value = values.get(index);
        assertEquals(expectedValue, value.getSearchValue());
        assertEquals(expectedCount, value.getCount());
    }

    public class FacetValueTestCorpus extends TestCorpus {
        @Override
        public List<SolrInputDocument> populate() {
            var docs = super.populate();

            // Add a bunch of extra works, distributed across multiple subjects and two collections
            addWorkWithFile(docs, SUBJECT_1, true, rootPid, unitPid, coll1Pid);
            addWorkWithFile(docs, SUBJECT_2, true, rootPid, unitPid, coll1Pid);
            addWorkWithFile(docs, SUBJECT_2, true, rootPid, unitPid, coll1Pid);
            addWorkWithFile(docs, SUBJECT_3, true, rootPid, unitPid, coll1Pid);
            addWorkWithFile(docs, SUBJECT_4, true, rootPid, unitPid, coll1Pid, folder1Pid);
            addWorkWithFile(docs, SUBJECT_5, true, rootPid, unitPid, coll1Pid);
            addWorkWithFile(docs, SUBJECT_1, true, rootPid, unitPid, coll2Pid);
            addWorkWithFile(docs, SUBJECT_5, true, rootPid, unitPid, coll2Pid);
            addWorkWithFile(docs, SUBJECT_5, true, rootPid, unitPid, coll2Pid);
            addWorkWithFile(docs, SUBJECT_5, true, rootPid, unitPid, coll2Pid);
            addWorkWithFile(docs, SUBJECT_6, true, rootPid, unitPid, coll2Pid);
            addWorkWithFile(docs, SUBJECT_7, true, rootPid, unitPid, coll2Pid);
            addWorkWithFile(docs, SUBJECT_7, false, rootPid, unitPid, coll2Pid, privateFolderPid);
            addWorkWithFile(docs, SUBJECT_8, false, rootPid, unitPid, coll2Pid, privateFolderPid);

            // Add a work that contains a file with a subject
            var docsFileWithSubj = addWorkWithFile(docs, SUBJECT_1, true, rootPid, unitPid, coll1Pid);
            addSubject(docsFileWithSubj[1], SUBJECT_3);

            return docs;
        }

        private SolrInputDocument[] addWorkWithFile(List<SolrInputDocument> docs, String subject, boolean publicAccess,
                                                    PID... ancestors) {
            var workPid = PIDs.get(UUID.randomUUID().toString());
            var timestamp = System.nanoTime();
            var workDoc = makeContainerDocument(workPid, "Work " + timestamp, ResourceType.Work,
                    ancestors);
            if (publicAccess) {
                addAclProperties(workDoc, PUBLIC_PRINC, "unitOwner", "manager");
            } else {
                addAclProperties(workDoc, null, "unitOwner", "manager");
            }
            addFileProperties(workDoc, ContentCategory.text, "text/plain", "Plain Text");
            addSubject(workDoc, subject);
            docs.add(workDoc);

            var fileAncestors = ArrayUtils.add(ancestors, workPid);
            var filePid = PIDs.get(UUID.randomUUID().toString());
            var fileDoc = makeFileDocument(filePid, "File " + timestamp, fileAncestors);
            addFileProperties(fileDoc, ContentCategory.text, "text/plain", "Plain Text");
            if (publicAccess) {
                addAclProperties(fileDoc, PUBLIC_PRINC, "unitOwner", "manager");
            } else {
                addAclProperties(fileDoc, null, "unitOwner", "manager");
            }
            docs.add(fileDoc);

            return new SolrInputDocument[] { workDoc, fileDoc };
        }

        private void addSubject(SolrInputDocument doc, String subject) {
            if (subject != null) {
                doc.setField(SearchFieldKey.SUBJECT.getSolrField(), subject);
            }
        }
    }
}

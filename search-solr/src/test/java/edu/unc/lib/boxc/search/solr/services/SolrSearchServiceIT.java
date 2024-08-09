package edu.unc.lib.boxc.search.solr.services;

import edu.unc.lib.boxc.auth.api.Permission;
import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.api.exceptions.SolrRuntimeException;
import edu.unc.lib.boxc.search.api.requests.IdListRequest;
import edu.unc.lib.boxc.search.api.requests.SearchRequest;
import edu.unc.lib.boxc.search.api.requests.SearchState;
import edu.unc.lib.boxc.search.api.requests.SimpleIdRequest;
import edu.unc.lib.boxc.search.solr.models.GroupedContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.test.BaseEmbeddedSolrTest;
import edu.unc.lib.boxc.search.solr.test.TestCorpus;
import edu.unc.lib.boxc.search.solr.utils.AccessRestrictionUtil;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * @author bbpennel
 */
public class SolrSearchServiceIT extends BaseEmbeddedSolrTest {
    private TestCorpus testCorpus;
    private SolrSearchService solrSearchService;
    private AutoCloseable closeable;
    @Mock
    private AccessRestrictionUtil restrictionUtil;
    private AccessGroupSet accessGroups;

    public SolrSearchServiceIT() {
        testCorpus = new TestCorpus();
    }

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

        index(testCorpus.populate());

        solrSearchService = new SolrSearchService();
        solrSearchService.setSolrSettings(solrSettings);
        solrSearchService.setAccessRestrictionUtil(restrictionUtil);
        solrSearchService.setSolrClient(server);

        accessGroups = new AccessGroupSetImpl("adminGroup");
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void getObjectByIdTest() {
        var request = new SimpleIdRequest(testCorpus.work1Pid, accessGroups);
        var record = solrSearchService.getObjectById(request);
        assertNotNull(record);
        assertEquals(testCorpus.work1Pid, record.getPid());
    }

    @Test
    public void getObjectByIdSolrExceptionTest() throws Exception {
        mockSolrClientThrowsException();
        var request = new SimpleIdRequest(testCorpus.work1Pid, accessGroups);
        assertThrows(SolrRuntimeException.class, () -> solrSearchService.getObjectById(request));
    }

    @Test
    public void getObjectByIdAccessExceptionTest() {
        doThrow(new AccessRestrictionException("Access error")).when(restrictionUtil).add(any(), any());
        var request = new SimpleIdRequest(testCorpus.work1Pid, accessGroups);
        var record = solrSearchService.getObjectById(request);
        assertNull(record);
    }

    @Test
    public void getObjectsByIdTest() {
        var idList = Arrays.asList(testCorpus.work1Pid.getId(), testCorpus.folder1Pid.getId());
        var resultFields = Arrays.asList(SearchFieldKey.ID.name(), SearchFieldKey.TITLE.name());
        var request = new IdListRequest(idList, resultFields, accessGroups);
        var records = solrSearchService.getObjectsById(request);
        assertEquals(2, records.size());
        assertEquals(testCorpus.folder1Pid.getId(), records.get(0).getId());
        assertEquals(testCorpus.work1Pid.getId(), records.get(1).getId());
    }

    @Test
    public void getObjectsByIdSolrExceptionTest() throws Exception {
        mockSolrClientThrowsException();
        var idList = Arrays.asList(testCorpus.work1Pid.getId(), testCorpus.folder1Pid.getId());
        var resultFields = Arrays.asList(SearchFieldKey.ID.name(), SearchFieldKey.TITLE.name());
        var request = new IdListRequest(idList, resultFields, accessGroups);
        assertThrows(SolrRuntimeException.class, () -> solrSearchService.getObjectsById(request));
    }

    @Test
    public void getObjectsByIdAccessExceptionTest() {
        doThrow(new AccessRestrictionException("Access error")).when(restrictionUtil).add(any(), any());

        var idList = Arrays.asList(testCorpus.work1Pid.getId(), testCorpus.folder1Pid.getId());
        var resultFields = Arrays.asList(SearchFieldKey.ID.name(), SearchFieldKey.TITLE.name());
        var request = new IdListRequest(idList, resultFields, accessGroups);
        var record = solrSearchService.getObjectsById(request);
        assertNull(record);
    }

    @Test
    public void getSearchResultsTest() {
        var searchState = new SearchState();
        searchState.setSearchFields(Map.of(SearchFieldKey.TITLE_INDEX.name(), "Private"));
        searchState.setSortType("title");
        var searchRequest = new SearchRequest(searchState, accessGroups);
        var resp = solrSearchService.getSearchResults(searchRequest);
        var results = resp.getResultList();
        assertEquals(3, results.size());
        assertEquals(testCorpus.privateWorkFile1Pid.getId(), results.get(0).getId());
        assertEquals(testCorpus.privateFolderPid.getId(), results.get(1).getId());
        assertEquals(testCorpus.privateWorkPid.getId(), results.get(2).getId());
    }

    @Test
    public void getSearchResultsSolrExceptionTest() throws Exception {
        mockSolrClientThrowsException();
        var searchState = new SearchState();
        searchState.setSearchFields(Map.of(SearchFieldKey.TITLE_INDEX.name(), "Work"));
        var searchRequest = new SearchRequest(searchState, accessGroups);
        assertThrows(SolrRuntimeException.class, () -> solrSearchService.getSearchResults(searchRequest));
    }

    @Test
    public void getSearchResultsRollupQueryTest() {
        var searchState = new SearchState();
        searchState.setRollup(true);
        searchState.setSearchFields(Map.of(SearchFieldKey.TITLE_INDEX.name(), "Private"));
        searchState.setSortType("title");
        var searchRequest = new SearchRequest(searchState, accessGroups);
        var resp = solrSearchService.getSearchResults(searchRequest);
        var results = resp.getResultList();
        assertEquals(2, results.size());
        assertTrue(results.get(0) instanceof GroupedContentObjectSolrRecord);
        assertEquals(testCorpus.privateWorkPid.getId(), results.get(0).getId());
        assertEquals(testCorpus.privateFolderPid.getId(), results.get(1).getId());
        // File object not returned since it was rolled up into the work record
        var workGroup = (GroupedContentObjectSolrRecord) results.get(0);
        assertEquals(2, workGroup.getItemCount());
    }

    @Test
    public void getSearchResultsByPermissionLimitsTest() {
        var publicGroups = new AccessGroupSetImpl("everyone");
        var searchState = new SearchState();
        searchState.setPermissionLimits(Arrays.asList(Permission.viewAccessCopies, Permission.viewMetadata));
        searchState.setSortType("title");
        var searchRequest = new SearchRequest(searchState, publicGroups);
        var resp = solrSearchService.getSearchResults(searchRequest);
        var results = resp.getResultList();
        assertEquals(10, results.size());
    }

    private void mockSolrClientThrowsException() throws Exception {
        var mockSolrClient = mock(SolrClient.class);
        when(mockSolrClient.query(any(SolrQuery.class), any(SolrRequest.METHOD.class)))
                .thenThrow(new SolrServerException("Connect error"));
        solrSearchService.setSolrClient(mockSolrClient);
    }
}

package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.model.api.exceptions.NotFoundException;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import edu.unc.lib.boxc.search.api.facets.CutoffFacet;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;
import edu.unc.lib.boxc.search.solr.services.ChildrenCountService;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static edu.unc.lib.boxc.auth.api.Permission.ingest;
import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants.CONTENT_ROOT_ID;
import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class ExportCsvServiceTest {
    private static final String BASE_URL = "http://example.com/";
    @Mock
    private SolrQueryLayerService solrQueryLayerService;
    @Mock
    private AccessControlService accessControlService;
    @Mock
    private ChildrenCountService childrenCountService;
    @Mock
    private RepositoryObjectLoader repositoryObjectLoader;
    @Mock
    private AgentPrincipals agent;
    @Mock
    private AccessGroupSet principals;
    @Mock
    private ContentObjectRecord object;
    @Mock
    private OutputStream outputStream;
    @Mock
    private SearchResultResponse response;
    private AutoCloseable closeable;
    private PID pid;
    private ExportCsvService exportCsvService;
    private List<ContentObjectRecord> resultList;
    private List<PID> pids;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        pid = makePid();
        pids = List.of(pid);
        exportCsvService = new ExportCsvService();
        exportCsvService.setAclService(accessControlService);
        exportCsvService.setChildrenCountService(childrenCountService);
        exportCsvService.setQueryLayer(solrQueryLayerService);
        exportCsvService.setRepositoryObjectLoader(repositoryObjectLoader);
        exportCsvService.setBaseUrl(BASE_URL);
        resultList = new ArrayList<>();

        when(agent.getPrincipals()).thenReturn(principals);
        when(response.getResultList()).thenReturn(resultList);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testStreamCsv() {
        var searchResponse = mock(SearchResultResponse.class);
        var facet = mock(CutoffFacet.class);

        when(solrQueryLayerService.addSelectedContainer(
                any(), any(), anyBoolean(), any())).thenReturn(object);
        when(solrQueryLayerService.getSearchResults(any())).thenReturn(searchResponse);

        when(object.getResourceType()).thenReturn(ResourceType.Folder.name());
        when(object.getId()).thenReturn(pid.getId());
        when(object.getTitle()).thenReturn("a good title");
        when(object.getAncestorNames()).thenReturn("name");
        when(object.getAncestorPathFacet()).thenReturn(facet);
        when(facet.getHighestTier()).thenReturn(1);

        exportCsvService.streamCsv(pids, agent, outputStream);
        verify(accessControlService).assertHasAccess(
                any(), eq(pid), eq(principals), eq(viewHidden));
        verify(childrenCountService).addChildrenCounts(any(), any());
    }

    @Test
    public void testStreamCsvContentRoot() {
        assertThrows(IllegalArgumentException.class, () -> {
            var contentRootPid = PIDs.get(CONTENT_ROOT_ID);
            var list = List.of(contentRootPid);
            exportCsvService.streamCsv(list, agent, outputStream);
        });
    }

    @Test
    public void testStreamCsvNoAccess() {
        assertThrows(AccessRestrictionException.class, () -> {
            doThrow(new AccessRestrictionException()).when(accessControlService)
                    .assertHasAccess(anyString(), eq(pid), any(), eq(viewHidden));
            exportCsvService.streamCsv(pids, agent, outputStream);
        });
    }

    @Test
    public void testStreamCsvNoContentObjectRecord() {
        assertThrows(NotFoundException.class, () -> {
            when(solrQueryLayerService.addSelectedContainer(
                    any(), any(), anyBoolean(), any())).thenReturn(null);
            exportCsvService.streamCsv(pids, agent, outputStream);
        });
    }
}

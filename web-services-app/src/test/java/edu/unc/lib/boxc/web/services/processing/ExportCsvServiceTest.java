package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.model.api.objects.RepositoryObjectLoader;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.services.ChildrenCountService;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import edu.unc.lib.boxc.web.common.services.SolrQueryLayerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.io.OutputStream;
import java.util.List;

import static edu.unc.lib.boxc.auth.api.Permission.viewHidden;
import static edu.unc.lib.boxc.model.fcrepo.test.TestHelper.makePid;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private AutoCloseable closeable;
    private ExportCsvService exportCsvService;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        exportCsvService = new ExportCsvService();
        exportCsvService.setAclService(accessControlService);
        exportCsvService.setChildrenCountService(childrenCountService);
        exportCsvService.setQueryLayer(solrQueryLayerService);
        exportCsvService.setRepositoryObjectLoader(repositoryObjectLoader);
        exportCsvService.setBaseUrl(BASE_URL);

        when(agent.getPrincipals()).thenReturn(principals);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testStreamCsv() {
        var pid = makePid();
        var pids = List.of(pid);
        var outputStream = mock(OutputStream.class);
        var object = mock(ContentObjectRecord.class);

        when(solrQueryLayerService.addSelectedContainer(
                any(), any(), any(), any())).thenReturn(object);
        exportCsvService.streamCsv(pids, agent, outputStream);
        verify(accessControlService).assertHasAccess(eq(pid), eq(principals), eq(viewHidden));
    }
}

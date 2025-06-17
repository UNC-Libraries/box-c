package edu.unc.lib.boxc.web.services.processing;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.api.services.AccessControlService;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.search.solr.services.SolrSearchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import static org.mockito.MockitoAnnotations.openMocks;

public class BulkRefIdCsvExporterTest {
    private AutoCloseable closeable;

    @Mock
    private SolrSearchService solrSearchService;
    @Mock
    private AccessControlService aclService;
    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private BulkRefIdCsvExporter exporter;

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        exporter = new BulkRefIdCsvExporter();
        exporter.setAclService(aclService);
        exporter.setSolrSearchService(solrSearchService);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }
}

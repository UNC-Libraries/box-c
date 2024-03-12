package edu.unc.lib.boxc.search.solr.services;

import edu.unc.lib.boxc.search.solr.utils.AccessRestrictionUtil;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.MockitoAnnotations.openMocks;

public class SolrSearchServiceTest {
    private AutoCloseable closeable;
    private SolrSearchService solrSearchService = new SolrSearchService();
    @Mock
    private AccessRestrictionUtil restrictionUtil;
    @Mock
    private SolrQuery solrQuery;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);
        solrSearchService.setAccessRestrictionUtil(restrictionUtil);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testAddAccessRestrictionsNullAccessGroupReturnsQuery() {
        var query = solrSearchService.addAccessRestrictions(solrQuery, null);
        assertNotNull(query);
    }
}

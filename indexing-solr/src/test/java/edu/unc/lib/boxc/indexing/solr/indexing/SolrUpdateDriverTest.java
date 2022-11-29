package edu.unc.lib.boxc.indexing.solr.indexing;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.indexing.solr.indexing.SolrUpdateDriver;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;

/**
 *
 * @author lfarrell
 *
 */
public class SolrUpdateDriverTest {
    @Mock
    private SolrClient solrClient;
    @Mock
    private SolrClient updateSolrClient;
    @Mock
    private IndexDocumentBean idb;
    @Mock
    private SolrInputDocument sid;
    @Mock
    private SolrSettings solrSettings;

    private SolrUpdateDriver driver;

    private Map<String, Object> missingFields = new HashMap<>();
    private Map<String, Object> allFields = new HashMap<>();

    private static String[] REQUIRED_INDEXING_FIELDS = new String[] {
        "adminGroup",
        "id",
        "readGroup",
        "resourceType",
        "roleGroup",
        "rollup",
        "title"
    };

    @Before
    public void setup() throws Exception {
        initMocks(this);

        driver = new SolrUpdateDriver();
        driver.setUpdateSolrClient(updateSolrClient);
        driver.setSolrClient(solrClient);
        driver.setSolrSettings(solrSettings);

        when(solrSettings.getRequiredFields()).thenReturn(REQUIRED_INDEXING_FIELDS);
    }

    @Test(expected = IndexingException.class)
    public void testRequiredIndexingFieldsMissing() throws Exception {
        when(idb.getFields()).thenReturn(missingFields);

        driver.addDocument(idb);
    }

    @Test
    public void testRequiredIndexingFieldsSet() throws Exception {
        for (String field : REQUIRED_INDEXING_FIELDS) {
            allFields.put(field, field);
        }

        when(idb.getFields()).thenReturn(allFields);

        driver.addDocument(idb);
        verify(solrClient).addBean(any(IndexDocumentBean.class));
    }
}

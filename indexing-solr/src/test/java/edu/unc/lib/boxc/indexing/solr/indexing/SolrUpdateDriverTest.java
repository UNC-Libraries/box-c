package edu.unc.lib.boxc.indexing.solr.indexing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.HashMap;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.indexing.solr.exception.IndexingException;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;
import edu.unc.lib.boxc.search.solr.models.IndexDocumentBean;

/**
 *
 * @author lfarrell
 *
 */
public class SolrUpdateDriverTest {
    private AutoCloseable closeable;

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

    @BeforeEach
    public void setup() throws Exception {
        closeable = openMocks(this);

        driver = new SolrUpdateDriver();
        driver.setUpdateSolrClient(updateSolrClient);
        driver.setSolrClient(solrClient);
        driver.setSolrSettings(solrSettings);

        when(solrSettings.getRequiredFields()).thenReturn(REQUIRED_INDEXING_FIELDS);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void testRequiredIndexingFieldsMissing() throws Exception {
        Assertions.assertThrows(IndexingException.class, () -> {
            when(idb.getFields()).thenReturn(missingFields);

            driver.addDocument(idb);
        });
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

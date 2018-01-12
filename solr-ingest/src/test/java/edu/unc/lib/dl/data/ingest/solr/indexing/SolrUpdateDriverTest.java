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
package edu.unc.lib.dl.data.ingest.solr.indexing;

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

import edu.unc.lib.dl.data.ingest.solr.exception.IndexingException;
import edu.unc.lib.dl.search.solr.model.IndexDocumentBean;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

/**
 *
 * @author lfarrell
 *
 */
public class SolrUpdateDriverTest {
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
        "title",
        "title_lc"
    };

    @Before
    public void setup() throws Exception {
        initMocks(this);

        driver = new SolrUpdateDriver();
        driver.setUpdateSolrClient(updateSolrClient);
        driver.setSolrSettings(solrSettings);

        when(solrSettings.getRequiredFields()).thenReturn(REQUIRED_INDEXING_FIELDS);
    }

    @Test(expected = IndexingException.class)
    public void testRequiredIndexingFieldsMissing() throws Exception {
        when(idb.getFields()).thenReturn(missingFields);

        driver.updateDocument(idb);
    }

    @Test
    public void testRequiredIndexingFieldsSet() throws Exception {
        for (String field : REQUIRED_INDEXING_FIELDS) {
            allFields.put(field, field);
        }

        when(idb.getFields()).thenReturn(allFields);

        driver.updateDocument(idb);
        verify(updateSolrClient).add(any(SolrInputDocument.class));
    }
}

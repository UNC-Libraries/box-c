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
package edu.unc.lib.dl.search.solr.util;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Assert;
import org.junit.Test;

import edu.unc.lib.dl.search.solr.model.CutoffFacet;
import static org.mockito.Mockito.*;

public class FacetFieldUtilTest extends Assert {

    @Test
    public void addFacetCutoffToQuery() {
        SolrQuery query = new SolrQuery();

        CutoffFacet facet = new CutoffFacet("ANCESTOR_PATH", "2,uuid:test!3");

        SolrSettings solrSettings = mock(SolrSettings.class);
        when(solrSettings.getFieldName(anyString())).thenReturn("ancestorPath");

        FacetFieldUtil facetFieldUtil = new FacetFieldUtil();
        facetFieldUtil.setSolrSettings(solrSettings);

        facetFieldUtil.addToSolrQuery(facet, query);

        String[] filterQueries = query.getFilterQueries();
        assertEquals(2, filterQueries.length);

        assertEquals("ancestorPath:2,uuid\\:test", filterQueries[0]);
        assertEquals("!ancestorPath:3,*", filterQueries[1]);
    }
}

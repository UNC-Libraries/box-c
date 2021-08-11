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
package edu.unc.lib.boxc.search.solr.utils;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import edu.unc.lib.boxc.search.solr.config.SolrSettings;
import edu.unc.lib.boxc.search.solr.facets.CaseInsensitiveFacet;
import edu.unc.lib.boxc.search.solr.facets.CutoffFacetImpl;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import edu.unc.lib.boxc.search.solr.facets.MultivaluedHierarchicalFacet;
import edu.unc.lib.boxc.search.solr.facets.RoleGroupFacet;
import edu.unc.lib.boxc.search.solr.utils.FacetFieldUtil;

public class FacetFieldUtilTest {

    private static SolrSettings solrSettings;
    private FacetFieldUtil facetFieldUtil;

    @BeforeClass
    public static void setupOnce() throws Exception {
        Properties properties = new Properties();
        properties.load(new FileInputStream("src/test/resources/solr.properties"));
        solrSettings = new SolrSettings();
        solrSettings.setProperties(properties);
    }

    @Before
    public void setup() throws Exception {
        facetFieldUtil = new FacetFieldUtil();
        facetFieldUtil.setSolrSettings(solrSettings);
    }

    @Test
    public void addFacetCutoffToQuery() {
        SolrQuery query = new SolrQuery();

        CutoffFacetImpl facet = new CutoffFacetImpl("ANCESTOR_PATH", "2,test");

        facetFieldUtil.addToSolrQuery(facet, query);

        String[] filterQueries = query.getFilterQueries();
        assertEquals(1, filterQueries.length);

        assertEquals("(ancestorPath:2,test)", filterQueries[0]);
    }

    @Test
    public void addFacetCutoffWithCutoffToQuery() {
        SolrQuery query = new SolrQuery();

        CutoffFacetImpl facet = new CutoffFacetImpl("ANCESTOR_PATH", "2,test!3");

        facetFieldUtil.addToSolrQuery(facet, query);

        String[] filterQueries = query.getFilterQueries();
        assertEquals(1, filterQueries.length);

        assertEquals("(ancestorPath:2,test AND !ancestorPath:3,*)", filterQueries[0]);
    }

    @Test
    public void addMultipleCutoffToQuery() {
        SolrQuery query = new SolrQuery();

        CutoffFacetImpl facet = new CutoffFacetImpl("ANCESTOR_PATH", "2,test1");
        CutoffFacetImpl facet2 = new CutoffFacetImpl("ANCESTOR_PATH", "3,test2!4");

        facetFieldUtil.addToSolrQuery(Arrays.asList(facet, facet2), query);

        String[] filterQueries = query.getFilterQueries();
        assertEquals(1, filterQueries.length);

        assertEquals("(ancestorPath:2,test1) OR (ancestorPath:3,test2 AND !ancestorPath:4,*)", filterQueries[0]);
    }

    @Test
    public void addMultivaluedHierarchicalFacetToQuery() {
        SolrQuery query = new SolrQuery();

        MultivaluedHierarchicalFacet facet = new MultivaluedHierarchicalFacet("CONTENT_TYPE", "image/jpg");

        facetFieldUtil.addToSolrQuery(facet, query);

        String[] filterQueries = query.getFilterQueries();
        assertEquals(1, filterQueries.length);

        assertEquals("contentType:\\/image\\^jpg,*", filterQueries[0]);
    }

    @Test
    public void addMultivaluedHierarchicalParentAndChildrenToQuery() {
        SolrQuery query = new SolrQuery();

        MultivaluedHierarchicalFacet facet1 = new MultivaluedHierarchicalFacet("CONTENT_TYPE", "text");
        MultivaluedHierarchicalFacet facet2 = new MultivaluedHierarchicalFacet("CONTENT_TYPE", "text^pdf");
        MultivaluedHierarchicalFacet facet3 = new MultivaluedHierarchicalFacet("CONTENT_TYPE", "text^txt");

        facetFieldUtil.addToSolrQuery(Arrays.asList(facet1, facet2, facet3), query);

        String[] filterQueries = query.getFilterQueries();
        assertEquals(1, filterQueries.length);

        assertEquals("contentType:\\/text\\^pdf,* OR contentType:\\/text\\^txt,*", filterQueries[0]);
    }

    @Test
    public void addMultivaluedHierarchicalMultipleParentAndChildToQuery() {
        SolrQuery query = new SolrQuery();

        MultivaluedHierarchicalFacet facet1 = new MultivaluedHierarchicalFacet("CONTENT_TYPE", "text");
        MultivaluedHierarchicalFacet facet2 = new MultivaluedHierarchicalFacet("CONTENT_TYPE", "text^txt");
        MultivaluedHierarchicalFacet facet3 = new MultivaluedHierarchicalFacet("CONTENT_TYPE", "image");
        MultivaluedHierarchicalFacet facet4 = new MultivaluedHierarchicalFacet("CONTENT_TYPE", "image^png");

        facetFieldUtil.addToSolrQuery(Arrays.asList(facet1, facet2, facet3, facet4), query);

        String[] filterQueries = query.getFilterQueries();
        assertEquals(1, filterQueries.length);

        assertEquals("contentType:\\/text\\^txt,* OR contentType:\\/image\\^png,*", filterQueries[0]);
    }

    @Test
    public void addMultivaluedHierarchicalMultipleParentOneChildToQuery() {
        SolrQuery query = new SolrQuery();

        MultivaluedHierarchicalFacet facet1 = new MultivaluedHierarchicalFacet("CONTENT_TYPE", "text");
        MultivaluedHierarchicalFacet facet2 = new MultivaluedHierarchicalFacet("CONTENT_TYPE", "image");
        MultivaluedHierarchicalFacet facet3 = new MultivaluedHierarchicalFacet("CONTENT_TYPE", "image^png");

        facetFieldUtil.addToSolrQuery(Arrays.asList(facet1, facet2, facet3), query);

        String[] filterQueries = query.getFilterQueries();
        assertEquals(1, filterQueries.length);

        assertEquals("contentType:\\^text,* OR contentType:\\/image\\^png,*", filterQueries[0]);
    }

    @Test
    public void addCaseInsensitiveFacetToQuery() {
        SolrQuery query = new SolrQuery();

        CaseInsensitiveFacet facet = new CaseInsensitiveFacet("DEPARTMENT", "department of boxy");

        facetFieldUtil.addToSolrQuery(facet, query);

        String[] filterQueries = query.getFilterQueries();
        assertEquals(1, filterQueries.length);

        assertEquals("department_lc:\"department\\ of\\ boxy\"", filterQueries[0]);
    }

    @Test
    public void addGenericFacetToQuery() {
        SolrQuery query = new SolrQuery();

        GenericFacet facet = new RoleGroupFacet("ROLE_GROUP", "canManage|some_group");

        facetFieldUtil.addToSolrQuery(facet, query);

        String[] filterQueries = query.getFilterQueries();
        assertEquals(1, filterQueries.length);

        assertEquals("roleGroup:\"canManage\\|some_group\"", filterQueries[0]);
    }

    @Test
    public void addMultipleFacets() {
        SolrQuery query = new SolrQuery();

        CutoffFacetImpl facet = new CutoffFacetImpl("ANCESTOR_PATH", "2,test");
        facetFieldUtil.addToSolrQuery(facet, query);

        MultivaluedHierarchicalFacet facet2 = new MultivaluedHierarchicalFacet("CONTENT_TYPE", "image/jpg");
        facetFieldUtil.addToSolrQuery(facet2, query);

        String[] filterQueries = query.getFilterQueries();
        assertEquals(2, filterQueries.length);

        assertEquals("(ancestorPath:2,test)", filterQueries[0]);
        assertEquals("contentType:\\/image\\^jpg,*", filterQueries[1]);
    }
}

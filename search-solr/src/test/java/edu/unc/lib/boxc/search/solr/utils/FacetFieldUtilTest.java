package edu.unc.lib.boxc.search.solr.utils;

import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;
import edu.unc.lib.boxc.search.solr.facets.CutoffFacetImpl;
import edu.unc.lib.boxc.search.solr.facets.GenericFacet;
import edu.unc.lib.boxc.search.solr.facets.RoleGroupFacet;
import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FacetFieldUtilTest {

    private static SolrSettings solrSettings;
    private FacetFieldUtil facetFieldUtil;

    @BeforeAll
    public static void setupOnce() throws Exception {
        Properties properties = new Properties();
        properties.load(new FileInputStream("src/test/resources/solr.properties"));
        solrSettings = new SolrSettings();
        solrSettings.setProperties(properties);
    }

    @BeforeEach
    public void setup() throws Exception {
        facetFieldUtil = new FacetFieldUtil();
        facetFieldUtil.setSolrSettings(solrSettings);
    }

    @Test
    public void addFacetCutoffToQuery() {
        SolrQuery query = new SolrQuery();

        CutoffFacetImpl facet = new CutoffFacetImpl("ANCESTOR_PATH", "2,test");

        facetFieldUtil.addToSolrQuery(facet, true, query);

        String[] filterQueries = query.getFilterQueries();
        assertEquals(1, filterQueries.length);

        assertEquals("(ancestorPath:2,test)", filterQueries[0]);
    }

    @Test
    public void addFacetCutoffWithCutoffToQuery() {
        SolrQuery query = new SolrQuery();

        CutoffFacetImpl facet = new CutoffFacetImpl("ANCESTOR_PATH", "2,test!3");

        facetFieldUtil.addToSolrQuery(facet, true, query);

        String[] filterQueries = query.getFilterQueries();
        assertEquals(1, filterQueries.length);

        assertEquals("(ancestorPath:2,test AND !ancestorPath:3,*)", filterQueries[0]);
    }

    @Test
    public void addFacetCutoffWithCutoffToQueryDisableApplyCutoffs() {
        SolrQuery query = new SolrQuery();

        CutoffFacetImpl facet = new CutoffFacetImpl("ANCESTOR_PATH", "2,test!3");

        facetFieldUtil.addToSolrQuery(facet, false, query);

        String[] filterQueries = query.getFilterQueries();
        assertEquals(1, filterQueries.length);

        assertEquals("(ancestorPath:2,test)", filterQueries[0]);
    }

    @Test
    public void addMultipleCutoffToQuery() {
        SolrQuery query = new SolrQuery();

        CutoffFacetImpl facet = new CutoffFacetImpl("ANCESTOR_PATH", "2,test1");
        CutoffFacetImpl facet2 = new CutoffFacetImpl("ANCESTOR_PATH", "3,test2!4");

        facetFieldUtil.addToSolrQuery(Arrays.asList(facet, facet2), true, query);

        String[] filterQueries = query.getFilterQueries();
        assertEquals(1, filterQueries.length);

        assertEquals("(ancestorPath:2,test1) OR (ancestorPath:3,test2 AND !ancestorPath:4,*)", filterQueries[0]);
    }

    @Test
    public void addGenericFacetToQuery() {
        SolrQuery query = new SolrQuery();

        GenericFacet facet = new RoleGroupFacet("ROLE_GROUP", "canManage|some_group");

        facetFieldUtil.addToSolrQuery(facet, true, query);

        String[] filterQueries = query.getFilterQueries();
        assertEquals(1, filterQueries.length);

        assertEquals("roleGroup:\"canManage\\|some_group\"", filterQueries[0]);
    }

    @Test
    public void addMultipleFacets() {
        SolrQuery query = new SolrQuery();

        CutoffFacetImpl facet = new CutoffFacetImpl("ANCESTOR_PATH", "2,test");
        facetFieldUtil.addToSolrQuery(facet, true, query);

        var facet2 = new GenericFacet(SearchFieldKey.FILE_FORMAT_CATEGORY, "Image");
        facetFieldUtil.addToSolrQuery(facet2, true, query);

        String[] filterQueries = query.getFilterQueries();
        assertEquals(2, filterQueries.length);

        assertEquals("(ancestorPath:2,test)", filterQueries[0]);
        assertEquals("fileFormatCategory:\"Image\"", filterQueries[1]);
    }
}

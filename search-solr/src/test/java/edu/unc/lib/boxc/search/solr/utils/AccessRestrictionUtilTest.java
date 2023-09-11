package edu.unc.lib.boxc.search.solr.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import edu.unc.lib.boxc.auth.api.exceptions.AccessRestrictionException;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;

/**
 *
 * @author bbpennel
 *
 */
public class AccessRestrictionUtilTest {

    private final static String BASE_QUERY = "*:*";
    private final static String ACCESS_GROUP = "group";

    private AutoCloseable closeable;

    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;
    @Mock
    private SearchSettings searchSettings;

    private AccessGroupSet accessGroups;

    private AccessRestrictionUtil restrictionUtil;

    @BeforeEach
    public void init() throws Exception {
        closeable = openMocks(this);

        restrictionUtil = new AccessRestrictionUtil();
        restrictionUtil.setGlobalPermissionEvaluator(globalPermissionEvaluator);
        restrictionUtil.setSearchSettings(searchSettings);

        accessGroups = new AccessGroupSetImpl(ACCESS_GROUP);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void addAccessDisabledTest() {
        restrictionUtil.setDisablePermissionFiltering(true);

        SolrQuery query = new SolrQuery(BASE_QUERY);
        restrictionUtil.add(query, accessGroups);

        assertEquals("q=" + BASE_QUERY, query.toString());
    }

    @Test
    public void addAccessNoGroupsTest() {
        Assertions.assertThrows(AccessRestrictionException.class, () -> {
            accessGroups = new AccessGroupSetImpl();

            SolrQuery query = new SolrQuery(BASE_QUERY);
            restrictionUtil.add(query, accessGroups);
        });
    }

    @Test
    public void addAccessGlobalPermissionsTest() {
        when(globalPermissionEvaluator.hasGlobalPrincipal(anySetOf(String.class))).thenReturn(true);

        SolrQuery query = new SolrQuery(BASE_QUERY);
        restrictionUtil.add(query, accessGroups);

        assertEquals("q=" + BASE_QUERY, query.toString());
    }

    @Test
    public void addAccessAllowPatronAccessTest() {
        when(searchSettings.getAllowPatronAccess()).thenReturn(true);

        SolrQuery query = new SolrQuery(BASE_QUERY);
        restrictionUtil.add(query, accessGroups);

        assertEquals(BASE_QUERY, query.getQuery());
        assertEquals("readGroup:(" + ACCESS_GROUP + ") OR adminGroup:(" + ACCESS_GROUP + ")",
                query.getFilterQueries()[0]);
    }

    @Test
    public void addAccessAdminOnlyTest() {
        when(searchSettings.getAllowPatronAccess()).thenReturn(false);

        SolrQuery query = new SolrQuery(BASE_QUERY);
        restrictionUtil.add(query, accessGroups);

        assertEquals(BASE_QUERY, query.getQuery());
        assertEquals("adminGroup:(" + ACCESS_GROUP + ")", query.getFilterQueries()[0]);
    }
}

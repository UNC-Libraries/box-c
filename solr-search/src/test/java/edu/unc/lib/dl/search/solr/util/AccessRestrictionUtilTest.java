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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import edu.unc.lib.dl.acl.exception.AccessRestrictionException;
import edu.unc.lib.dl.acl.fcrepo4.GlobalPermissionEvaluator;
import edu.unc.lib.dl.acl.util.AccessGroupSet;

/**
 *
 * @author bbpennel
 *
 */
public class AccessRestrictionUtilTest {

    private final static String BASE_QUERY = "*:*";
    private final static String ACCESS_GROUP = "group";

    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;
    @Mock
    private SearchSettings searchSettings;

    private AccessGroupSet accessGroups;

    private AccessRestrictionUtil restrictionUtil;

    @Before
    public void init() throws Exception {
        initMocks(this);

        restrictionUtil = new AccessRestrictionUtil();
        restrictionUtil.setGlobalPermissionEvaluator(globalPermissionEvaluator);
        restrictionUtil.setSearchSettings(searchSettings);

        accessGroups = new AccessGroupSet(ACCESS_GROUP);
    }

    @Test
    public void addAccessDisabledTest() {
        restrictionUtil.setDisablePermissionFiltering(true);

        StringBuilder query = new StringBuilder(BASE_QUERY);
        restrictionUtil.add(query, accessGroups);

        assertEquals(BASE_QUERY, query.toString());
    }

    @Test(expected = AccessRestrictionException.class)
    public void addAccessNoGroupsTest() {
        accessGroups = new AccessGroupSet();

        StringBuilder query = new StringBuilder(BASE_QUERY);
        restrictionUtil.add(query, accessGroups);
    }

    @Test
    public void addAccessGlobalPermissionsTest() {
        when(globalPermissionEvaluator.hasGlobalPrincipal(anySetOf(String.class))).thenReturn(true);

        StringBuilder query = new StringBuilder(BASE_QUERY);
        restrictionUtil.add(query, accessGroups);

        assertEquals(BASE_QUERY, query.toString());
    }

    @Test
    public void addAccessAllowPatronAccessTest() {
        when(searchSettings.getAllowPatronAccess()).thenReturn(true);

        StringBuilder query = new StringBuilder(BASE_QUERY);
        restrictionUtil.add(query, accessGroups);

        assertEquals(BASE_QUERY + " AND (readGroup:(" + ACCESS_GROUP
                + ") OR adminGroup:(" + ACCESS_GROUP + "))", query.toString());
    }

    @Test
    public void addAccessAdminOnlyTest() {
        when(searchSettings.getAllowPatronAccess()).thenReturn(false);

        StringBuilder query = new StringBuilder(BASE_QUERY);
        restrictionUtil.add(query, accessGroups);

        assertEquals(BASE_QUERY + " AND adminGroup:(" + ACCESS_GROUP + ")", query.toString());
    }
}

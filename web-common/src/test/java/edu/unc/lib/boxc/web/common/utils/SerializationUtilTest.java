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
package edu.unc.lib.boxc.web.common.utils;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;

import edu.unc.lib.boxc.auth.api.UserRole;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.api.services.GlobalPermissionEvaluator;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.search.api.models.ContentObjectRecord;
import edu.unc.lib.boxc.search.solr.config.SearchSettings;
import edu.unc.lib.boxc.search.solr.config.SolrSettings;
import edu.unc.lib.boxc.search.solr.models.ContentObjectSolrRecord;
import edu.unc.lib.boxc.search.solr.responses.HierarchicalBrowseResultResponse;
import edu.unc.lib.boxc.search.solr.responses.SearchResultResponse;

/**
 *
 * @author bbpennel
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SerializationUtilTest extends Assert {
    private static final List<String> DATASTREAMS =
            singletonList("datastream|image/jpeg|image.jpg|jpg|orig|582753|");

    private ObjectMapper mapper;

    @Mock
    private SearchSettings searchSettings;
    @Mock
    private SolrSettings solrSettings;
    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;

    private ContentObjectSolrRecord md;

    @Before
    public void init() {
        mapper = new ObjectMapper();
        SerializationUtil.injectSettings(searchSettings, solrSettings,
                globalPermissionEvaluator);
        when(globalPermissionEvaluator.getGlobalUserRoles(any(AccessGroupSetImpl.class)))
                .thenReturn(Collections.emptySet());

        md = new ContentObjectSolrRecord();
        md.setId("48aeb594-6d95-45e9-bb20-dd631ecc93e9");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void briefMetadataToJSONTest() throws Exception {
        md.setTitle("Test Item");
        md.setDatastream(DATASTREAMS);

        String json = SerializationUtil.metadataToJSON(md, null);
        Map<String, Object> jsonMap = getResultMap(json);

        assertEquals("48aeb594-6d95-45e9-bb20-dd631ecc93e9", jsonMap.get("id"));
        assertEquals("Test Item", jsonMap.get("title"));
        assertEquals(1, ((List<String>) jsonMap.get("datastream")).size());
    }

    @Test
    public void briefMetadataListToJSONTest() throws Exception {
        md.setTitle("Test Item");

        ContentObjectSolrRecord md2 = new ContentObjectSolrRecord();
        md2.setId("9ef8d1c5-14a1-4ed3-b0c0-6da67fa5f6d1");
        md2.setTitle("Test Item 2");
        md.setDatastream(DATASTREAMS);

        SearchResultResponse response = new SearchResultResponse();
        response.setResultList(asList(md, md2));

        List<Map<String, Object>> resultList = SerializationUtil.resultsToList(response, null);

        assertEquals(2, resultList.size());
        assertEquals("48aeb594-6d95-45e9-bb20-dd631ecc93e9", resultList.get(0).get("id"));
        assertEquals("9ef8d1c5-14a1-4ed3-b0c0-6da67fa5f6d1", resultList.get(1).get("id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStructureToJSON() throws Exception {
        ContentObjectSolrRecord rootMd = new ContentObjectSolrRecord();
        rootMd.setId("48aeb594-6d95-45e9-bb20-dd631ecc93e9");
        rootMd.setAncestorPath(singletonList("1,48aeb594-6d95-45e9-bb20-dd631ecc93e9"));

        ContentObjectSolrRecord childMd = new ContentObjectSolrRecord();
        childMd.setId("7c73296f-54ae-438e-b8d5-1890eba41676");
        childMd.setAncestorPath(asList("1,48aeb594-6d95-45e9-bb20-dd631ecc93e9"));

        List<ContentObjectRecord> mdList = asList(rootMd, childMd);

        HierarchicalBrowseResultResponse response = new HierarchicalBrowseResultResponse();
        response.setResultList(mdList);
        response.setSelectedContainer(rootMd);
        response.generateResultTree();

        String json = SerializationUtil.structureToJSON(response, null);
        Map<String, Object> jsonMap = getResultMap(json);

        // Verify that we got a root node containing the child node
        Map<String, Object> rootObj = (Map<String, Object>) jsonMap.get("root");
        Map<String, Object> rootEntry = (Map<String, Object>) rootObj.get("entry");
        assertEquals("48aeb594-6d95-45e9-bb20-dd631ecc93e9", rootEntry.get("id"));

        List<Object> childrenList = (List<Object>) rootObj.get("children");
        assertEquals(1, childrenList.size());

        Map<String, Object> childObj = (Map<String, Object>) childrenList.get(0);
        Map<String, Object> childEntry = (Map<String, Object>) childObj.get("entry");
        assertEquals("7c73296f-54ae-438e-b8d5-1890eba41676", childEntry.get("id"));
    }

    @Test
    public void testPermissionSerialization() throws Exception {
        md.setRoleGroup(Arrays.asList(UserRole.canManage.name() + "|group1",
                UserRole.canViewOriginals.name() + "|group2"));

        // Verify principal with administrative permissions
        assertHasRolePermissions(md, new AccessGroupSetImpl("group1"), UserRole.canManage);

        // Verify principal with patron permissions
        assertHasRolePermissions(md, new AccessGroupSetImpl("group2"), UserRole.canViewOriginals);

        // Verify group with no permissions
        assertHasRolePermissions(md, new AccessGroupSetImpl("group3"), null);

        // Verify that highest permissions out of set of principals are assigned
        assertHasRolePermissions(md, new AccessGroupSetImpl("group2;group3"), UserRole.canViewOriginals);
    }

    // Check for only global match
    @Test
    public void testGlobalPermissionSerialization1() throws Exception {
        md.setRoleGroup(Arrays.asList(UserRole.canManage.name() + "|group1"));
        AccessGroupSet adminPrincipals = new AccessGroupSetImpl("adminGrp");

        // Check for exclusive global match
        when(globalPermissionEvaluator.getGlobalUserRoles(any(AccessGroupSetImpl.class)))
                .thenReturn(new HashSet<>(Arrays.asList(UserRole.administrator)));

        assertHasRolePermissions(md, adminPrincipals, UserRole.administrator);
    }

    // Check for global and local, with global winning
    @Test
    public void testGlobalPermissionSerialization2() throws Exception {
        md.setRoleGroup(Arrays.asList(UserRole.canManage.name() + "|group1"));
        AccessGroupSet mixedPrincipals = new AccessGroupSetImpl("adminGrp;group1");

        when(globalPermissionEvaluator.getGlobalUserRoles(any(AccessGroupSetImpl.class)))
                .thenReturn(new HashSet<>(Arrays.asList(UserRole.canAccess)));

        assertHasRolePermissions(md, mixedPrincipals, UserRole.canManage);
    }

    // Check for global and local, with local winning
    @Test
    public void testGlobalPermissionSerialization3() throws Exception {
        md.setRoleGroup(Arrays.asList(UserRole.canManage.name() + "|group1"));
        AccessGroupSet mixedPrincipals = new AccessGroupSetImpl("adminGrp;group1");

        when(globalPermissionEvaluator.getGlobalUserRoles(any(AccessGroupSetImpl.class)))
        .thenReturn(new HashSet<>(Arrays.asList(UserRole.canAccess)));

        assertHasRolePermissions(md, mixedPrincipals, UserRole.canManage);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> assertHasRolePermissions(ContentObjectSolrRecord md, AccessGroupSet principals,
            UserRole expectedRole) throws Exception {
        String groupJson = SerializationUtil.metadataToJSON(md, principals);
        Map<String, Object> groupMap = getResultMap(groupJson);
        List<String> permissions = (List<String>) groupMap.get("permissions");
        if (expectedRole == null) {
            assertTrue(permissions.isEmpty());
        } else {
            assertTrue(permissions.containsAll(expectedRole.getPermissionNames()));
            assertEquals("Unexpected additional permissions present",
                    expectedRole.getPermissions().size(), permissions.size());
        }

        return groupMap;
    }

    private Map<String, Object> getResultMap(String json) throws Exception {
        MapType type = defaultInstance().constructMapType(HashMap.class, String.class, Object.class);
        return mapper.readValue(json, type);
    }
}

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
package edu.unc.lib.dl.ui.util;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import edu.unc.lib.dl.acl.fcrepo4.GlobalPermissionEvaluator;
import edu.unc.lib.dl.acl.util.AccessGroupSet;
import edu.unc.lib.dl.acl.util.UserRole;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadata;
import edu.unc.lib.dl.search.solr.model.BriefObjectMetadataBean;
import edu.unc.lib.dl.search.solr.model.HierarchicalBrowseResultResponse;
import edu.unc.lib.dl.search.solr.model.SearchResultResponse;
import edu.unc.lib.dl.search.solr.util.SearchSettings;
import edu.unc.lib.dl.search.solr.util.SolrSettings;

/**
 *
 * @author bbpennel
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SerializationUtilTest extends Assert {
    private static final List<String> DATASTREAMS =
            singletonList("datastream|image/jpeg|image.jpg|jpg|orig|582753|");

    private static final String API_PATH = "http://example.com/api/";

    private ObjectMapper mapper;

    @Mock
    private ApplicationPathSettings applicationPathSettings;
    @Mock
    private SearchSettings searchSettings;
    @Mock
    private SolrSettings solrSettings;
    @Mock
    private GlobalPermissionEvaluator globalPermissionEvaluator;

    private BriefObjectMetadataBean md;

    @Before
    public void init() {
        mapper = new ObjectMapper();
        when(applicationPathSettings.getApiRecordPath()).thenReturn(API_PATH);
        SerializationUtil.injectSettings(applicationPathSettings, searchSettings, solrSettings,
                globalPermissionEvaluator);
        when(globalPermissionEvaluator.getGlobalUserRoles(any(AccessGroupSet.class)))
                .thenReturn(Collections.emptySet());

        md = new BriefObjectMetadataBean();
        md.setId("uuid:test");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void briefMetadataToJSONTest() throws Exception {
        md.setTitle("Test Item");
        md.setIsPart(Boolean.FALSE);
        md.setDatastream(DATASTREAMS);

        String json = SerializationUtil.metadataToJSON(md, null);
        Map<String, Object> jsonMap = getResultMap(json);

        assertEquals("uuid:test", jsonMap.get("id"));
        assertEquals(API_PATH + "uuid:test", jsonMap.get("uri"));
        assertEquals("Test Item", jsonMap.get("title"));
        assertEquals(false, jsonMap.get("isPart"));
        assertEquals(1, ((List<String>) jsonMap.get("datastream")).size());
    }

    @Test
    public void briefMetadataListToJSONTest() throws Exception {
        md.setTitle("Test Item");

        BriefObjectMetadataBean md2 = new BriefObjectMetadataBean();
        md2.setId("uuid:test2");
        md2.setTitle("Test Item 2");
        md.setDatastream(DATASTREAMS);

        SearchResultResponse response = new SearchResultResponse();
        response.setResultList(asList(md, md2));

        List<Map<String, Object>> resultList = SerializationUtil.resultsToList(response, null);

        assertEquals(2, resultList.size());
        assertEquals("uuid:test", resultList.get(0).get("id"));
        assertEquals("uuid:test2", resultList.get(1).get("id"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStructureToJSON() throws Exception {
        BriefObjectMetadataBean rootMd = new BriefObjectMetadataBean();
        rootMd.setId("uuid:test");
        rootMd.setAncestorPath(singletonList("1,uuid:test"));

        BriefObjectMetadataBean childMd = new BriefObjectMetadataBean();
        childMd.setId("uuid:child");
        childMd.setAncestorPath(asList("1,uuid:test"));

        List<BriefObjectMetadata> mdList = asList(rootMd, childMd);

        HierarchicalBrowseResultResponse response = new HierarchicalBrowseResultResponse();
        response.setResultList(mdList);
        response.setSelectedContainer(rootMd);
        response.generateResultTree();

        String json = SerializationUtil.structureToJSON(response, null);
        Map<String, Object> jsonMap = getResultMap(json);

        // Verify that we got a root node containing the child node
        Map<String, Object> rootObj = (Map<String, Object>) jsonMap.get("root");
        Map<String, Object> rootEntry = (Map<String, Object>) rootObj.get("entry");
        assertEquals("uuid:test", rootEntry.get("id"));

        List<Object> childrenList = (List<Object>) rootObj.get("children");
        assertEquals(1, childrenList.size());

        Map<String, Object> childObj = (Map<String, Object>) childrenList.get(0);
        Map<String, Object> childEntry = (Map<String, Object>) childObj.get("entry");
        assertEquals("uuid:child", childEntry.get("id"));
    }

    @Test
    public void testPermissionSerialization() throws Exception {
        md.setRoleGroup(Arrays.asList(UserRole.canManage.name() + "|group1",
                UserRole.canViewOriginals.name() + "|group2"));

        // Verify principal with administrative permissions
        assertHasRolePermissions(md, new AccessGroupSet("group1"), UserRole.canManage);

        // Verify principal with patron permissions
        assertHasRolePermissions(md, new AccessGroupSet("group2"), UserRole.canViewOriginals);

        // Verify group with no permissions
        assertHasRolePermissions(md, new AccessGroupSet("group3"), null);

        // Verify that highest permissions out of set of principals are assigned
        assertHasRolePermissions(md, new AccessGroupSet("group2;group3"), UserRole.canViewOriginals);
    }

    // Check for only global match
    @Test
    public void testGlobalPermissionSerialization1() throws Exception {
        md.setRoleGroup(Arrays.asList(UserRole.canManage.name() + "|group1"));
        AccessGroupSet adminPrincipals = new AccessGroupSet("adminGrp");

        // Check for exclusive global match
        when(globalPermissionEvaluator.getGlobalUserRoles(any(AccessGroupSet.class)))
                .thenReturn(new HashSet<>(Arrays.asList(UserRole.administrator)));

        assertHasRolePermissions(md, adminPrincipals, UserRole.administrator);
    }

    // Check for global and local, with global winning
    @Test
    public void testGlobalPermissionSerialization2() throws Exception {
        md.setRoleGroup(Arrays.asList(UserRole.canManage.name() + "|group1"));
        AccessGroupSet mixedPrincipals = new AccessGroupSet("adminGrp;group1");

        when(globalPermissionEvaluator.getGlobalUserRoles(any(AccessGroupSet.class)))
                .thenReturn(new HashSet<>(Arrays.asList(UserRole.canAccess)));

        assertHasRolePermissions(md, mixedPrincipals, UserRole.canManage);
    }

    // Check for global and local, with local winning
    @Test
    public void testGlobalPermissionSerialization3() throws Exception {
        md.setRoleGroup(Arrays.asList(UserRole.canManage.name() + "|group1"));
        AccessGroupSet mixedPrincipals = new AccessGroupSet("adminGrp;group1");

        when(globalPermissionEvaluator.getGlobalUserRoles(any(AccessGroupSet.class)))
        .thenReturn(new HashSet<>(Arrays.asList(UserRole.canAccess)));

        assertHasRolePermissions(md, mixedPrincipals, UserRole.canManage);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> assertHasRolePermissions(BriefObjectMetadataBean md, AccessGroupSet principals,
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
        return mapper.readValue(json,
                new TypeReference<Map<String, Object>>(){});
    }
}

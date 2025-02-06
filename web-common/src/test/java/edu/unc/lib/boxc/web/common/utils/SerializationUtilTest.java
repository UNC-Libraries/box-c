package edu.unc.lib.boxc.web.common.utils;

import static com.fasterxml.jackson.databind.type.TypeFactory.defaultInstance;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import edu.unc.lib.boxc.common.util.DateTimeUtil;
import edu.unc.lib.boxc.model.api.ResourceType;
import edu.unc.lib.boxc.search.api.FacetConstants;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
@MockitoSettings(strictness = Strictness.LENIENT)
public class SerializationUtilTest extends Assertions {
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

    @BeforeEach
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

    @Test
    public void testMetadataToMapWithAltText() throws Exception {
        md.setAltText("This is the alt text");
        md.setContentStatus(List.of(FacetConstants.HAS_ALT_TEXT));

        String groupJson = SerializationUtil.metadataToJSON(md, new AccessGroupSetImpl("group1"));
        Map<String, Object> groupMap = getResultMap(groupJson);
        assertEquals("This is the alt text", groupMap.get("altText"));
        assertEquals(List.of(FacetConstants.HAS_ALT_TEXT), groupMap.get("contentStatus"));
    }

    @Test
    public void testMetadataToMapWithStreaming() throws Exception {
        md.setStreamingUrl("http://example.com/streaming");
        md.setStreamingType("video");
        md.setContentStatus(List.of(FacetConstants.HAS_STREAMING));

        String groupJson = SerializationUtil.metadataToJSON(md, new AccessGroupSetImpl("group1"));
        Map<String, Object> groupMap = getResultMap(groupJson);
        assertEquals("http://example.com/streaming", groupMap.get("streamingUrl"));
        assertEquals(List.of(FacetConstants.HAS_STREAMING), groupMap.get("contentStatus"));
        assertEquals("video", groupMap.get("streamingType"));
    }

    @Test
    public void testMetadataToMapFileObject() throws Exception {
        md.setFileFormatCategory(List.of("image"));
        md.setFileFormatType(List.of("jpeg"));
        md.setFileFormatDescription(List.of("JPEG"));
        md.setFilesizeSort(582753L);
        md.setResourceType(ResourceType.File.name());
        md.setDateAdded("2019-01-01T00:00:00Z");
        md.setDateUpdated("2019-01-01T00:01:00Z");
        md.setDateCreated(DateTimeUtil.parseUTCToDate("2019-01-01T00:02:00Z"));
        Date timestamp = new Date();
        md.setTimestamp(timestamp);
        md.setRollup("parentid");
        md.set_version_(1L);

        String groupJson = SerializationUtil.metadataToJSON(md, new AccessGroupSetImpl("group1"));
        Map<String, Object> groupMap = getResultMap(groupJson);
        assertEquals(List.of("image"), groupMap.get(SearchFieldKey.FILE_FORMAT_CATEGORY.getUrlParam()));
        assertEquals(List.of("jpeg"), groupMap.get(SearchFieldKey.FILE_FORMAT_TYPE.getUrlParam()));
        assertEquals(List.of("JPEG"), groupMap.get(SearchFieldKey.FILE_FORMAT_DESCRIPTION.getUrlParam()));
        assertEquals(582753, groupMap.get(SearchFieldKey.FILESIZE.getUrlParam())); // using the incorrect name in the class
        assertEquals(ResourceType.File.name(), groupMap.get(SearchFieldKey.RESOURCE_TYPE.getUrlParam()));
        assertEquals("2019-01-01T00:00:00.000Z", groupMap.get(SearchFieldKey.DATE_ADDED.getUrlParam()));
        assertEquals("2019-01-01T00:01:00.000Z", groupMap.get(SearchFieldKey.DATE_UPDATED.getUrlParam()));
        assertEquals("2019-01-01T00:02:00.000Z", groupMap.get(SearchFieldKey.DATE_CREATED.getUrlParam()));
        assertEquals(timestamp.getTime(), groupMap.get(SearchFieldKey.TIMESTAMP.getUrlParam()));
        assertEquals("parentid", groupMap.get("rollup"));
        assertEquals(1, groupMap.get("_version_"));
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
            assertEquals(expectedRole.getPermissions().size(), permissions.size(),
                    "Unexpected additional permissions present");
        }

        return groupMap;
    }

    private Map<String, Object> getResultMap(String json) throws Exception {
        MapType type = defaultInstance().constructMapType(HashMap.class, String.class, Object.class);
        return mapper.readValue(json, type);
    }
}

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
import static org.mockito.Mockito.when;

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

    @Before
    public void init() {
        mapper = new ObjectMapper();
        when(applicationPathSettings.getApiRecordPath()).thenReturn(API_PATH);
        SerializationUtil.injectSettings(applicationPathSettings, searchSettings, solrSettings);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void briefMetadataToJSONTest() throws Exception {
        BriefObjectMetadataBean md = new BriefObjectMetadataBean();
        md.setId("uuid:test");
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
        BriefObjectMetadataBean md = new BriefObjectMetadataBean();
        md.setId("uuid:test");
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

    private Map<String, Object> getResultMap(String json) throws Exception {
        return mapper.readValue(json,
                new TypeReference<Map<String, Object>>(){});
    }
}

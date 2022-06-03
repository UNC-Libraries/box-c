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
package edu.unc.lib.boxc.web.services.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.search.api.SearchFieldKey;
import edu.unc.lib.boxc.web.services.rest.modify.AbstractAPIIT;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import edu.unc.lib.boxc.search.solr.test.TestCorpus;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author bbpennel
 */
@ContextHierarchy({
        @ContextConfiguration("/spring-test/cdr-client-container.xml"),
        @ContextConfiguration("/spring-test/solr-indexing-context.xml"),
        @ContextConfiguration("/facet-rest-it-servlet.xml")
})
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
public class FacetRestControllerIT extends AbstractAPIIT {
    // non-facet field selected
    // invalid field
    // with root id
    // without root id
    // with navigation params

    @Autowired
    private EmbeddedSolrServer embeddedSolrServer;
    private TestCorpus testCorpus;
    private static boolean corpusPopulated;

    @Before
    public void setup() throws Exception {
        if (!corpusPopulated) {
            testCorpus = new TestCorpus();
            embeddedSolrServer.add(testCorpus.populate());
            embeddedSolrServer.commit();
            corpusPopulated = true;
        }
    }

    @Test
    public void invalidFieldTest() throws Exception {
        var result = mvc.perform(get("/facet/notafield/listValues"))
                .andExpect(status().is4xxClientError())
                .andReturn();
        assertEquals("Unknown facet field specified", result.getResponse().getContentAsString());
    }

    @Test
    public void nonFacetFieldTest() throws Exception {
        var result = mvc.perform(get("/facet/title/listValues"))
                .andExpect(status().is4xxClientError())
                .andReturn();
        assertEquals("Invalid facet field specified: title", result.getResponse().getContentAsString());
    }

    @Test
    public void noParamsTest() throws Exception {
        var result = mvc.perform(get("/facet/fileType/listValues"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        var values = extractResponseFacetValues(result);
        assertEquals(4, values.size());

        assertValuePresent(values, 0, "text/plain", 2);
        assertValuePresent(values, 1, "application/pdf", 1);
        assertValuePresent(values, 2, "image/jpeg", 1);
        assertValuePresent(values, 3, "image/png", 1);
    }

    @Test
    public void withRootIdTest() throws Exception {
        var result = mvc.perform(get("/facet/fileType/listValues/" + testCorpus.coll1Pid.getId()))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        var values = extractResponseFacetValues(result);
        assertEquals(3, values.size());

        assertValuePresent(values, 0, "application/pdf", 1);
        assertValuePresent(values, 1, "image/jpeg", 1);
        assertValuePresent(values, 2, "text/plain", 1);
    }

    @Test
    public void withSortTest() throws Exception {
        var result = mvc.perform(get("/facet/fileType/listValues?facetSort=index"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        var values = extractResponseFacetValues(result);
        assertEquals(4, values.size());

        assertValuePresent(values, 0, "application/pdf", 1);
        assertValuePresent(values, 1, "image/jpeg", 1);
        assertValuePresent(values, 2, "image/png", 1);
        assertValuePresent(values, 3, "text/plain", 2);
    }

    @Test
    public void withInvalidSortTest() throws Exception {
        var result = mvc.perform(get("/facet/fileType/listValues?facetSort=what"))
                .andExpect(status().is4xxClientError())
                .andReturn();
        assertTrue("Incorrect response: " + result.getResponse().getContentAsString(),
                result.getResponse().getContentAsString().contains("Invalid facet sort type"));
    }

    @Test
    public void withPaginationTest() throws Exception {
        var result = mvc.perform(get("/facet/fileType/listValues?facetStart=1&facetRows=2"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        var values = extractResponseFacetValues(result);
        assertEquals(2, values.size());

        assertValuePresent(values, 0, "application/pdf", 1);
        assertValuePresent(values, 1, "image/jpeg", 1);
    }

    @Test
    public void withSearchParamsTest() throws Exception {
        var result = mvc.perform(get("/facet/fileType/listValues?format=Image"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        var values = extractResponseFacetValues(result);
        assertEquals(2, values.size());

        assertValuePresent(values, 0, "image/jpeg", 1);
        assertValuePresent(values, 1, "image/png", 1);
    }

    @Test
    public void exceedMaxRowsTest() throws Exception {
        var result = mvc.perform(get("/facet/fileType/listValues?facetRows=100000"))
                .andExpect(status().is4xxClientError())
                .andReturn();
        assertTrue("Incorrect response: " + result.getResponse().getContentAsString(),
                result.getResponse().getContentAsString().contains("Invalid facetRows value, max value is:"));
    }

    private List<JsonNode> extractResponseFacetValues(MvcResult result) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var respJson = mapper.readTree(result.getResponse().getContentAsString());
        assertEquals(SearchFieldKey.FILE_FORMAT_TYPE.name(), respJson.get("facetName").asText());
        return IteratorUtils.toList(respJson.get("values").elements());
    }

    private void assertValuePresent(List<JsonNode> values, int index, String type, int count) {
        var result = values.get(index);
        assertEquals(type, result.get("value").asText());
        assertEquals(count, result.get("count").asInt());
    }
}

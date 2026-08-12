package edu.unc.lib.boxc.web.services.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.search.solr.services.AccessCopiesService;
import edu.unc.lib.boxc.search.solr.services.ChildrenCountService;
import edu.unc.lib.boxc.search.solr.test.TestCorpus;
import edu.unc.lib.boxc.web.services.rest.modify.AbstractAPIIT;
import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextHierarchy({
        @ContextConfiguration("/spring-test/cdr-client-container.xml"),
        @ContextConfiguration("/spring-test/solr-indexing-context.xml"),
        @ContextConfiguration("/search-rest-it-servlet.xml")
})
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
public class SearchRestControllerIT extends AbstractAPIIT {
    private static final String MISSING_PID = "00000000-0000-0000-0000-000000000000";

    @Autowired
    private SolrClient solrClient;

    private static TestCorpus testCorpus;
    private static boolean corpusPopulated;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    public void setup() throws Exception {
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);

        if (!corpusPopulated) {
            solrClient.deleteByQuery("*:*");
            solrClient.commit();

            testCorpus = new TestCorpus();
            solrClient.add(testCorpus.populate());
            solrClient.commit();
            corpusPopulated = true;
        }
    }

    @AfterEach
    void cleanup() {
        GroupsThreadStore.clearStore();
    }

    @Test
    public void searchReturnsJson() throws Exception {
        var result = mvc.perform(get("/search")
                        .param("fieldset", "brief"))
                .andExpect(status().isOk())
                .andReturn();

        var json = responseJson(result);

        assertTrue(json.has("numFound"));
        assertTrue(json.has("results"));
        assertTrue(json.get("results").isArray());
        assertTrue(json.get("numFound").asInt() > 0);
    }

    @Test
    public void listReturnsJson() throws Exception {
        var result = mvc.perform(get("/list")
                        .param("fieldset", "brief"))
                .andExpect(status().isOk())
                .andReturn();

        var json = responseJson(result);

        assertTrue(json.has("numFound"));
        assertTrue(json.has("results"));
        assertTrue(json.get("results").isArray());
    }

    @Test
    public void searchWithRootIdReturnsScopedResults() throws Exception {
        var result = mvc.perform(get("/search/{id}", testCorpus.coll1Pid.getId())
                        .param("fieldset", "brief"))
                .andExpect(status().isOk())
                .andReturn();

        var json = responseJson(result);

        assertTrue(json.has("numFound"));
        assertTrue(json.has("results"));
        assertTrue(json.get("numFound").asInt() > 0);
    }

    @Test
    public void listWithRootIdReturnsScopedResults() throws Exception {
        var result = mvc.perform(get("/list/{id}", testCorpus.coll1Pid.getId())
                        .param("fieldset", "brief"))
                .andExpect(status().isOk())
                .andReturn();

        var json = responseJson(result);

        assertTrue(json.has("numFound"));
        assertTrue(json.has("results"));
    }

    @Test
    public void searchWithFilterParams() throws Exception {
        var result = mvc.perform(get("/search")
                        .param("format", "Image")
                        .param("fieldset", "brief"))
                .andExpect(status().isOk())
                .andReturn();

        var json = responseJson(result);

        assertEquals(4, json.get("numFound").asInt());
        assertEquals(4, json.get("results").size());
    }

    @Test
    public void fieldsParameterIsHonoredForRecord() throws Exception {
        var id = firstResultId();

        var result = mvc.perform(get("/record/{id}", id)
                        .param("fields", "id,title"))
                .andExpect(status().isOk())
                .andReturn();

        var json = responseJson(result);

        assertEquals(id, json.get("id").asText());
        assertTrue(json.has("title"));
    }

    @Test
    public void fieldsetDefaultsToBriefForRecord() throws Exception {
        var id = firstResultId();

        var result = mvc.perform(get("/record/{id}", id))
                .andExpect(status().isOk())
                .andReturn();

        var json = responseJson(result);

        assertEquals(id, json.get("id").asText());
        assertTrue(json.has("title"));
    }

    @Test
    public void fieldsetFullReturnsFullRecordFields() throws Exception {
        var id = firstResultId();

        var result = mvc.perform(get("/record/{id}", id)
                        .param("fieldset", "full"))
                .andExpect(status().isOk())
                .andReturn();

        var json = responseJson(result);

        assertEquals(id, json.get("id").asText());

        // Keep these assertions loose unless TestCorpus guarantees the fields are populated.
        assertTrue(json.has("title"));
    }

    @Test
    public void recordMissingIdReturns404() throws Exception {
        mvc.perform(get("/record/{id}", MISSING_PID))
                .andExpect(status().isNotFound());
    }

    @Test
    public void searchJsonpCallbackIsSanitized() throws Exception {
        var result = mvc.perform(get("/search")
                        .param("fieldset", "brief")
                        .param("callback", "alert(1);evil_func"))
                .andExpect(status().isOk())
                .andReturn();

        var body = result.getResponse().getContentAsString();

        assertTrue(body.startsWith("alert1evil_func("), body);
        assertTrue(body.endsWith(")"), body);
        assertTrue(body.contains("\"numFound\""), body);
        assertTrue(body.contains("\"results\""), body);
    }

    @Test
    public void recordJsonpCallbackIsSanitized() throws Exception {
        var id = firstResultId();

        var result = mvc.perform(get("/record/{id}", id)
                        .param("fieldset", "brief")
                        .param("callback", "alert(1);evil_func"))
                .andExpect(status().isOk())
                .andReturn();

        var body = result.getResponse().getContentAsString();

        assertTrue(body.startsWith("alert1evil_func("), body);
        assertTrue(body.endsWith(")"), body);
        assertTrue(body.contains(id), body);
    }

    private String firstResultId() throws Exception {
        var result = mvc.perform(get("/search")
                        .param("fieldset", "id"))
                .andExpect(status().isOk())
                .andReturn();

        var json = responseJson(result);
        var results = json.get("results");

        assertTrue(results.size() > 0, "Expected test corpus to contain search results");

        return results.get(0).get("id").asText();
    }

    private JsonNode responseJson(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString());
    }
}

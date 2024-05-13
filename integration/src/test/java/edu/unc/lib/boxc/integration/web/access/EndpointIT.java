package edu.unc.lib.boxc.integration.web.access;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.auth.api.models.AccessGroupSet;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.integration.factories.AdminUnitFactory;
import edu.unc.lib.boxc.integration.factories.CollectionFactory;
import edu.unc.lib.boxc.integration.factories.ContentRootObjectFactory;
import edu.unc.lib.boxc.integration.factories.FileFactory;
import edu.unc.lib.boxc.integration.factories.FolderFactory;
import edu.unc.lib.boxc.integration.factories.WorkFactory;
import edu.unc.lib.boxc.model.api.objects.AdminUnit;
import edu.unc.lib.boxc.model.api.objects.CollectionObject;
import edu.unc.lib.boxc.model.api.objects.WorkObject;
import edu.unc.lib.boxc.model.fcrepo.services.RepositoryInitializer;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.solr.client.solrj.SolrClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A parent class for SearchActionController Endpoint tests.
 * @author snluong
 */

@ContextHierarchy({
        @ContextConfiguration("/spring-test/cdr-client-container.xml"),
        @ContextConfiguration("/spring-test/acl-service-context.xml"),
        @ContextConfiguration("/spring-test/solr-standalone-context.xml"),
        @ContextConfiguration("/spring-test/solr-indexing-context.xml"),
        @ContextConfiguration("/spring-test/object-factory-context.xml")
})
public class EndpointIT {
    @Autowired
    protected AdminUnitFactory adminUnitFactory;
    @Autowired
    protected WorkFactory workFactory;
    @Autowired
    protected CollectionFactory collectionFactory;
    @Autowired
    protected FolderFactory folderFactory;
    @Autowired
    protected ContentRootObjectFactory contentRootObjectFactory;
    @Autowired
    protected String baseAddress;
    @Autowired
    protected RepositoryInitializer repoInitializer;
    @Autowired
    protected SolrClient solrClient;

    protected final static String USERNAME = "test_user";
    protected final static AccessGroupSet GROUPS = new AccessGroupSetImpl("adminGroup");
    protected final static String ACCESS_URL = "http://localhost:48080/access";

    protected CloseableHttpClient httpClient;
    protected CollectionObject collection;
    protected WorkObject work;
    protected AdminUnit adminUnit1;

    public void createDefaultObjects() throws Exception {
        adminUnit1 = adminUnitFactory.createAdminUnit(Map.of("title", "Admin Object1"));
        var adminUnit2 = adminUnitFactory.createAdminUnit(Map.of("title", "Admin Object2"));
        collection = collectionFactory.createCollection(adminUnit1,
                Map.of("title", "Collection Object", "readGroup", "everyone"));
        work = workFactory.createWork(collection,
                Map.of("title", "Work Object", "readGroup", "everyone"));
        var fileOptions = Map.of(
                "title", "File Object",
                WorkFactory.PRIMARY_OBJECT_KEY, "false",
                FileFactory.FILE_FORMAT_OPTION, FileFactory.AUDIO_FORMAT,
                "readGroup", "everyone");
        workFactory.createFileInWork(work, fileOptions);
        folderFactory.createFolder(collection,
                Map.of("title", "Folder Object", "readGroup", "everyone"));
    }

    public List<JsonNode> getNodeFromResponse(CloseableHttpResponse response, String fieldName) throws IOException {
        var respJson = getResponseAsJson(response);

        return IteratorUtils.toList(respJson.get(fieldName).elements());
    }

    public JsonNode getResponseAsJson(CloseableHttpResponse response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(response.getEntity().getContent());
    }

    public List<JsonNode> getMetadataFromResponse(CloseableHttpResponse response) throws IOException {
        return getNodeFromResponse(response, "metadata");
    }

    public List<JsonNode> getFacetsFromResponse(CloseableHttpResponse response) throws IOException {
        return getNodeFromResponse(response, "facetFields");
    }

    public void assertValuePresent(List<JsonNode> json, int index, String key, String value) {
        var result = json.get(index);
        assertEquals(value, result.get(key).asText());
    }

    public void assertStringValuePresent(List<JsonNode> json, int index, String key) {
        var result = json.get(index);
        assertNotNull(result.get(key).asText());
    }

    public void assertArrayValuePresent(List<JsonNode> json, int index, String key) {
        var result = json.get(index);
        assertFalse(result.get(key).isEmpty());
    }

    public void assertArrayValuePresent(List<JsonNode> json, int index, String key, String value) {
        var result = json.get(index);
        assertEquals(value, result.get(key).toString());
    }

    public void assertSuccessfulResponse(CloseableHttpResponse response) {
        assertEquals(200, response.getStatusLine().getStatusCode());
    }

    public void assertIdMatchesAny(List<JsonNode> json, String id) {
        assertTrue(json.stream().anyMatch(entry -> id.equals(entry.get("id").asText())));
    }

    public void assertIdMatchesNone(List<JsonNode> json, String id) {
        assertTrue(json.stream().noneMatch(entry -> id.equals(entry.get("id").asText())));
    }

    protected void assertResultCountEquals(String url, int expectedCount) throws IOException {
        assertResultCountEquals(new HttpGet(url), expectedCount);
    }

    protected void assertResultCountEquals(HttpGet getMethod, int expectedCount) throws IOException {
        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);
            assertSuccessfulResponse(resp);
            // two admin units, 1 collection (nested in the admin unit), 1 work (with nested file), and 1 folder
            assertEquals(expectedCount, metadata.size());
        }
    }
}

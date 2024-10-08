package edu.unc.lib.boxc.integration.web.access;

import edu.unc.lib.boxc.auth.fcrepo.services.GroupsThreadStore;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Map;

import static edu.unc.lib.boxc.auth.api.AccessPrincipalConstants.PUBLIC_PRINC;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author bbpennel, snluong
 */
@ExtendWith(SpringExtension.class)
public class CollectionsEndpointIT extends EndpointIT {
    protected HttpGet getMethod;

    @BeforeEach
    public void setup() throws Exception {
        TestHelper.setContentBase(baseAddress);
        GroupsThreadStore.storeUsername(USERNAME);
        GroupsThreadStore.storeGroups(GROUPS);
        // reset solr before every test
        solrClient.deleteByQuery("*:*");
        httpClient = HttpClients.createDefault();
        getMethod = new HttpGet( ACCESS_URL + "/collectionsJson");
        contentRootObjectFactory.initializeRepository();
    }

    @Test
    public void testCollectionsJsonOnlyReturnsAdminUnits() throws Exception {
        createDefaultObjects();

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);

            assertSuccessfulResponse(resp);
            assertValuePresent(metadata, 0, "type", "AdminUnit");
            assertValuePresent(metadata, 1, "type", "AdminUnit");
            assertEquals(2, metadata.size());
        }
    }

    @Test
    public void testCollectionsJsonReturnsSuccessWithNoAdminUnits() throws Exception {
        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);
            assertSuccessfulResponse(resp);
            assertEquals(0, metadata.size());
        }
    }

    @Test
    public void testCollectionsJsonReturnsCorrectTitle() throws Exception {
        adminUnitFactory.createAdminUnit(Map.of("title", "Object2"));

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);

            assertSuccessfulResponse(resp);
            assertValuePresent(metadata, 0, "title", "Object2");
            assertEquals(1, metadata.size());
        }
    }

    @Test
    public void testCollectionsJsonReturnsThumbnailUrl() throws Exception {
        var options = Map.of("title", "Object1", "addThumbnail", "true");
        adminUnitFactory.createAdminUnit(options);

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);

            assertSuccessfulResponse(resp);
            assertStringValuePresent(metadata, 0, "thumbnail_url");
            assertEquals(1, metadata.size());
        }
    }

    @Test
    public void testCollectionsJsonReturnsChildrenCount() throws Exception {
        var options = Map.of("title", "Object1", "addThumbnail", "true");
        var adminUnit = adminUnitFactory.createAdminUnit(options);
        collectionFactory.createCollection(adminUnit,
                Map.of("title", "Collection1", "readGroup", PUBLIC_PRINC));

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);
            var childCount = metadata.get(0).get("counts").get("child").asInt();

            assertSuccessfulResponse(resp);
            assertEquals(1, childCount);
            assertEquals(1, metadata.size());
        }
    }

    @Test
    public void testCollectionsJsonReturnsChildrenCountAccordingToPermission() throws Exception {
        var options = Map.of("title", "Object1", "addThumbnail", "true");
        var adminUnit = adminUnitFactory.createAdminUnit(options);
        // no readGroup permission
        collectionFactory.createCollection(adminUnit, Map.of("title", "Collection1"));

        try (var resp = httpClient.execute(getMethod)) {
            var metadata = getMetadataFromResponse(resp);
            var childCount = metadata.get(0).get("counts").get("child").asInt();

            assertSuccessfulResponse(resp);
            // childCount should be 0 because no one has permission to see the child Collection
            assertEquals(0, childCount);
            assertEquals(1, metadata.size());
        }
    }
}

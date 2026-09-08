package edu.unc.lib.boxc.model.fcrepo.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.fcrepo.FcrepoPaths;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import org.fcrepo.client.FcrepoClient;
import org.fcrepo.client.FcrepoOperationFailedException;
import org.fcrepo.client.FcrepoResponse;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Test utility for cleaning up the contents of a fedora repository
 * @author bbpennel
 */
public class TestRepositoryDeinitializer {
    // Maximum number of results returnable in a single page from the fcr:search endpoint
    private static final int MAX_RESULTS = 100;

    private TestRepositoryDeinitializer() {
    }

    /**
     * Deletes all the content in the test repository
     * @param fcrepoClient
     * @throws Exception
     */
    public static void cleanup(FcrepoClient fcrepoClient) throws Exception {
        // Load the full list up front, since we are deleting resources as we go and paging offsets
        // would otherwise skip records.
        List<String> fedoraIds = listResourceIdsForDeletion(fcrepoClient);
        for (String fedoraId : fedoraIds) {
            deleteResource(fcrepoClient, fedoraId);
        }
    }

    /**
     * Queries the fedora simple search endpoint for all resources, ordered by fedora_id in
     * descending order so that children are listed before their parents, allowing for a
     * depth first deletion.
     */
    private static List<String> listResourceIdsForDeletion(FcrepoClient fcrepoClient) throws Exception {
        var mapper = new ObjectMapper();
        var searchBaseUri = URIUtil.join(FcrepoPaths.getBaseUri(), "fcr:search");
        // The repository root itself is returned by the search, but it cannot be deleted
        var rootUri = FcrepoPaths.getBaseUri().replaceAll("/$", "");

        List<String> fedoraIds = new ArrayList<>();
        int offset = 0;
        while (true) {
            String queryUri = searchBaseUri
                    + "?condition=" + URLEncoder.encode("fedora_id=*", StandardCharsets.UTF_8)
                    + "&fields=fedora_id"
                    + "&order_by=fedora_id"
                    + "&order=desc"
                    + "&max_results=" + MAX_RESULTS
                    + "&offset=" + offset;

            JsonNode root;
            try (FcrepoResponse response = fcrepoClient.get(URI.create(queryUri))
                    .accept("application/json").perform()) {
                if (response.getStatusCode() != 200) {
                    throw new RuntimeException("Failed to query simple search endpoint, received status "
                            + response.getStatusCode());
                }
                root = mapper.readTree(response.getBody());
            }

            JsonNode results = root.get("items");
            if (results == null || results.isEmpty()) {
                break;
            }
            for (JsonNode result : results) {
                String fedoraId = result.get("fedora_id").asText();
                // Can't delete the fedora root or binary descriptions
                if (!fedoraId.equals(rootUri) && !fedoraId.contains("/fcr:metadata")) {
                    fedoraIds.add(fedoraId);
                }
            }

            if (results.size() < MAX_RESULTS) {
                break;
            }
            offset += MAX_RESULTS;
        }
        return fedoraIds;
    }

    private static void deleteResource(FcrepoClient fcrepoClient, String resourceUriString) throws Exception {
        URI resourceUri = URI.create(resourceUriString);

        try (var result = fcrepoClient.delete(resourceUri).perform()) {
            if (result.getStatusCode() != 204) {
                throw new RuntimeException("Failed to delete " + resourceUriString);
            }
        }
        String tombstoneString = URIUtil.join(resourceUriString, RepositoryPathConstants.FCR_TOMBSTONE);
        try (var result = fcrepoClient.delete(URI.create(tombstoneString)).perform()) {
            if (result.getStatusCode() != 204 && result.getStatusCode() != 404) {
                throw new RuntimeException("Failed to delete " + resourceUriString + " tombstone");
            }
        }
    }
}

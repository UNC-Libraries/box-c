package edu.unc.lib.boxc.model.fcrepo.test;

import edu.unc.lib.boxc.common.util.URIUtil;
import edu.unc.lib.boxc.fcrepo.FcrepoPaths;
import edu.unc.lib.boxc.model.api.ids.RepositoryPathConstants;
import org.fcrepo.client.FcrepoClient;

import java.net.URI;

/**
 * Test utility for cleaning up the contents of a fedora repository
 * @author bbpennel
 */
public class TestRepositoryDeinitializer {
    private TestRepositoryDeinitializer() {
    }

    /**
     * Deletes all the content in the test repository
     * @param fcrepoClient
     * @throws Exception
     */
    public static void cleanup(FcrepoClient fcrepoClient) throws Exception {
        String containerString = URIUtil.join(FcrepoPaths.getBaseUri(), RepositoryPathConstants.CONTENT_BASE);
        deleteContainer(fcrepoClient, containerString);
        String depositContainerString = URIUtil.join(FcrepoPaths.getBaseUri(), RepositoryPathConstants.DEPOSIT_RECORD_BASE);
        deleteContainer(fcrepoClient, depositContainerString);
    }

    private static void deleteContainer(FcrepoClient fcrepoClient, String containerString) throws Exception {
        try (var result = fcrepoClient.delete(URI.create(containerString)).perform()) {
            if (result.getStatusCode() != 204) {
                throw new RuntimeException("Failed to delete " + containerString);
            }
        }
        String tombstoneString = URIUtil.join(containerString, RepositoryPathConstants.FCR_TOMBSTONE);
        try (var result = fcrepoClient.delete(URI.create(tombstoneString)).perform()) {
            if (result.getStatusCode() != 204) {
                throw new RuntimeException("Failed to delete " + containerString + " tombstone");
            }
        }
    }
}

package edu.unc.lib.boxc.services.camel.longleaf;

import com.github.tomakehurst.wiremock.client.WireMock;
import edu.unc.lib.boxc.persist.impl.transfer.FileSystemTransferHelpers;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.slf4j.Logger;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author bbpennel
 */
public abstract class AbstractLongleafRouteTest extends CamelSpringTestSupport {
    private static final Logger log = getLogger(AbstractLongleafRouteTest.class);

    protected String outputPath;
    protected List<String> output;

    /**
     * Waits up to timeout ms for WireMock to have received a POST request whose body field
     * contains the base path of each provided content URI.
     */
    protected void assertPostRequestedForPaths(long timeout, String apiPath, URI... contentUris) throws Exception {
        long start = System.currentTimeMillis();
        do {
            try {
                for (URI contentUri : contentUris) {
                    Path contentPath = contentUri.getScheme() == null
                            ? Paths.get(contentUri.toString()) : Paths.get(contentUri);
                    String basePath = FileSystemTransferHelpers.getBaseBinaryPath(contentPath);
                    WireMock.verify(postRequestedFor(urlPathEqualTo(apiPath))
                            .withRequestBody(matchingJsonPath("$.body", WireMock.containing(basePath))));
                }
                return;
            } catch (AssertionError e) {
                if ((System.currentTimeMillis() - start) > timeout) {
                    throw e;
                }
                Thread.sleep(25);
            }
        } while (true);
    }

    protected void assertSubmittedPaths(String... contentUris) {
        for (String contentUri : contentUris) {
            URI uri = URI.create(contentUri);
            Path contentPath;
            if (uri.getScheme() == null) {
                contentPath = Paths.get(contentUri);
            } else {
                contentPath = Paths.get(uri);
            }
            String basePath = FileSystemTransferHelpers.getBaseBinaryPath(contentPath);
            assertTrue(output.stream().anyMatch(line -> line.contains(basePath)),
                    "Expected content uri to be submitted: " + contentPath);
        }
    }

    protected void assertNoSubmittedPaths() throws Exception {
        output = LongleafTestHelpers.readOutput(outputPath);
        assertEquals(0, output.size(),
                "Expected no calls to longleaf, but received output:\n" + String.join("\n", output));
    }
}

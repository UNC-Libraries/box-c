package edu.unc.lib.boxc.services.camel.longleaf;

import edu.unc.lib.boxc.persist.impl.transfer.FileSystemTransferHelpers;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.slf4j.Logger;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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
     * Assert that all of the provided content uris are present in the longleaf output
     * @param timeout time in milliseconds allowed for the condition to become true,
     *      to accommodate asynchronous unpredictable batch cutoffs
     * @param contentUris list of expected content uris
     * @throws Exception
     */
    protected void assertSubmittedPaths(long timeout, String... contentUris) throws Exception {
        long start = System.currentTimeMillis();
        do {
            try {
                output = LongleafTestHelpers.readOutput(outputPath);
                assertSubmittedPaths(contentUris);
                return;
            } catch (AssertionError e) {
                if ((System.currentTimeMillis() - start) > timeout) {
                    throw e;
                }
                Thread.sleep(25);
                log.debug("DeregisterPaths not yet satisfied, retrying");
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

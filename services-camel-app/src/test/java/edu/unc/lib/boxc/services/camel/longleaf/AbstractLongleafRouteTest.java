package edu.unc.lib.boxc.services.camel.longleaf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.camel.EndpointInject;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.slf4j.Logger;

import edu.unc.lib.boxc.persist.impl.transfer.FileSystemTransferHelpers;

/**
 * @author bbpennel
 */
public abstract class AbstractLongleafRouteTest extends CamelTestSupport {
    private static final Logger log = getLogger(AbstractLongleafRouteTest.class);

    protected String outputPath;
    protected List<String> output;
    @TempDir
    public Path tmpFolder;
    protected String longleafScript;

    @EndpointInject(uri = "mock:direct:longleaf.dlq")
    protected MockEndpoint mockDlq;

    protected RegisterToLongleafProcessor registerToLongleafProcessor;
    protected DeregisterLongleafProcessor deregisterLongleafProcessor;

    private LongleafAggregationStrategy aggregationStrategy;
    private GetUrisProcessor getUrisProcessor;

    protected LongleafRouter getLongleafRouter() {
        aggregationStrategy = new LongleafAggregationStrategy();
        getUrisProcessor = new GetUrisProcessor();
        var router = new LongleafRouter();
        router.setRegisterProcessor(registerToLongleafProcessor);
        router.setDeregisterProcessor(deregisterLongleafProcessor);
        router.setLongleafAggregationStrategy(aggregationStrategy);
        router.setGetUrisProcessor(getUrisProcessor);
        router.setLongleafRegisterConsumer("direct:register.longleaf.consumer");
        router.setLongleafRegisterDestination("direct:register.longleaf.consumer");
        router.setLongleafDeregisterConsumer("direct:deregister.longleaf.consumer");
        router.setLongleafDeregisterDestination("direct:deregister.longleaf.consumer");
        router.setLongleafDeadLetterQueueDestination("mock:direct:longleaf.dlq");
        router.setLongleafFilterDeregister("direct:filter.longleaf.deregister");
        router.setLongleafRegisterBatchConsumer("direct:register.longleaf.batch.consumer");
        router.setLongleafDeregisterBatchDestination("direct:deregister.longleaf.batch.consumer");
        router.setBatchSize(5);
        router.setBatchTimeout(100L);
        return router;
    }

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
            assertTrue("Expected content uri to be submitted: " + contentPath,
                    output.stream().anyMatch(line -> line.contains(basePath)));
        }
    }

    protected void assertNoSubmittedPaths() throws Exception {
        output = LongleafTestHelpers.readOutput(outputPath);
        assertEquals("Expected no calls to longleaf, but received output:\n" + String.join("\n", output),
                0, output.size());
    }
}

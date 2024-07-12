package edu.unc.lib.boxc.services.camel.binaryCleanup;

import com.google.common.collect.ImmutableMap;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.DatastreamPids;
import edu.unc.lib.boxc.model.fcrepo.test.TestHelper;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * @author bbpennel
 */
@ExtendWith(MockitoExtension.class)
public class BinaryCleanupRouterTest extends CamelTestSupport {
    @Produce("direct:binaryCleanup")
    private ProducerTemplate template;

    @Mock
    private BinaryCleanupProcessor binaryCleanupProcessor;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        var router = new BinaryCleanupRouter();
        router.setBinaryCleanupProcessor(binaryCleanupProcessor);
        router.setBinaryCleanupStream("direct:binaryCleanup");
        return router;
    }

    @Test
    public void routeTest() throws Exception {
        PID pid = TestHelper.makePid();
        PID dsPid = DatastreamPids.getMdDescriptivePid(pid);

        NotifyBuilder notify = new NotifyBuilder(context)
                .whenCompleted(1)
                .create();

        template.sendBody(ImmutableMap.of(dsPid.getRepositoryPath(), "file:///path/to/something.txt"));

        assertTrue(notify.matches(5L, TimeUnit.SECONDS), "Route not satisfied");
        verify(binaryCleanupProcessor).process(any());
    }
}

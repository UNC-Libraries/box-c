package edu.unc.lib.boxc.services.camel.accessSurrogates;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequest;
import edu.unc.lib.boxc.operations.jms.accessSurrogates.AccessSurrogateRequestSerializationHelper;
import edu.unc.lib.boxc.services.camel.ProcessorTestHelper;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AccessSurrogateRouterTest extends CamelTestSupport {
    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    @Produce(uri = "direct:start")
    protected ProducerTemplate template;
    @Mock
    private AccessSurrogateRequestProcessor processor;

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        var router = new AccessSurrogateRouter();
        router.setAccessSurrogateRequestProcessor(processor);
        router.setAccessSurrogatesStreamCamel("direct:accessSurrogates.stream");
        return router;
    }

    @Test
    public void requestSentTest() throws Exception {
        createContext("DcrAccessSurrogates");
        var pid = ProcessorTestHelper.makePid();

        var request = new AccessSurrogateRequest();
        request.setAgent(agent);
        request.setPidString(pid.toString());
        var body = AccessSurrogateRequestSerializationHelper.toJson(request);
        template.sendBody(body);

        verify(processor).process(any());
    }

    private void createContext(String routeName) throws Exception {
        AdviceWith.adviceWith(context, routeName, a -> {
            a.replaceFromWith("direct:start");
        });
    }
}

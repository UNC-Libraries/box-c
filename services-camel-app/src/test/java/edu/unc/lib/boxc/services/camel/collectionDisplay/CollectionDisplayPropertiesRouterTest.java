package edu.unc.lib.boxc.services.camel.collectionDisplay;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.jms.collectionDisplay.CollectionDisplayPropertiesRequest;
import edu.unc.lib.boxc.operations.jms.collectionDisplay.CollectionDisplayPropertiesSerializationHelper;
import edu.unc.lib.boxc.services.camel.TestHelper;
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
public class CollectionDisplayPropertiesRouterTest extends CamelTestSupport {
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    @Produce("direct:start")
    protected ProducerTemplate template;

    @Mock
    private CollectionDisplayPropertiesRequestProcessor processor;

    @Override
    protected RouteBuilder createRouteBuilder() {
        var router = new CollectionDisplayPropertiesRouter();
        router.setCollectionDisplayPropertiesRequestProcessor(processor);
        String endpointUri = "direct:start";
        router.setCollectionDisplayPropertiesRequestStream(endpointUri);
        return router;
    }

    private void createContext(String routeName) throws Exception {
        AdviceWith.adviceWith(context, routeName, a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("*");
        });
        context.start();
    }

    @Test
    public void requestSentTest() throws Exception {
        createContext("DcrCollectionDisplayProperties");
        var pid = TestHelper.makePid();

        var request = new CollectionDisplayPropertiesRequest();
        request.setAgent(agent);
        request.setId(pid.getId());
        request.setSortType("default,normal");
        request.setDisplayType("gallery-view");
        request.setWorksOnly(true);
        var body = CollectionDisplayPropertiesSerializationHelper.toJson(request);
        template.sendBody(body);

        verify(processor).process(any());
    }
}

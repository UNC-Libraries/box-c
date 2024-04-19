package edu.unc.lib.boxc.services.camel.order;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.operations.jms.order.MultiParentOrderRequest;
import edu.unc.lib.boxc.operations.jms.order.OrderOperationType;
import edu.unc.lib.boxc.operations.jms.order.OrderRequestSerializationHelper;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * @author bbpennel
 */
@ExtendWith(MockitoExtension.class)
public class OrderMembersRouterTest extends CamelTestSupport {
    private static final String PARENT1_UUID = "f277bb38-272c-471c-a28a-9887a1328a1f";
    private static final String CHILD1_UUID = "83c2d7f8-2e6b-4f0b-ab7e-7397969c0682";
    private static final String EMAIL = "user1@example.com";
    private AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));

    @Produce(uri = "direct:start")
    private ProducerTemplate template;
    private String endpointUri = "direct:ordermembers";

    @Mock
    private OrderRequestProcessor processor;

    @Override
    protected RouteBuilder createRouteBuilder() {
        var router = new OrderMembersRouter();
        router.setOrderRequestProcessor(processor);
        router.setOrderMembersStream(endpointUri);
        return router;
    }

    @Test
    public void requestSentTest() throws Exception {
        createContext("DcrOrderMembers");

        var request = new MultiParentOrderRequest();
        request.setAgent(agent);
        request.setEmail(EMAIL);
        request.setOperation(OrderOperationType.SET);
        request.setParentToOrdered(Map.of(PARENT1_UUID, Arrays.asList(CHILD1_UUID)));
        var body = OrderRequestSerializationHelper.toJson(request);
        template.sendBody(body);

        verify(processor).process(any());
    }

    private void createContext(String routeName) throws Exception {
        AdviceWith.adviceWith(context, routeName, a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("*");
        });
        context.start();
    }
}

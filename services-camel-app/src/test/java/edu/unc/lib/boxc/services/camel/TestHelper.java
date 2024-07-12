package edu.unc.lib.boxc.services.camel;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.model.ModelCamelContext;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Basic methods used in several processor and router tests
 *
 * @author snluong
 */
public class TestHelper {
    public static PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }

    public static Exchange mockExchange(String body) {
        var exchange = mock(Exchange.class);
        var message = mock(Message.class);
        when(exchange.getIn()).thenReturn(message);
        when(message.getBody(String.class)).thenReturn(body);
        return exchange;
    }

    public static void createContext(ModelCamelContext context, String routeName) throws Exception {
        AdviceWith.adviceWith(context, routeName, a -> {
            a.replaceFromWith("direct:start");
            a.mockEndpointsAndSkip("*");
        });

        context.start();
    }
}

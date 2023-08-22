package edu.unc.lib.boxc.services.camel;

import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.apache.camel.Exchange;
import org.apache.camel.Message;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Basic methods used in several Processor tests
 *
 * @author snluong
 */
public class ProcessorTestHelper {
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
}

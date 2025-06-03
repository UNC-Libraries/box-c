package edu.unc.lib.boxc.operations.jms.aspace;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.jms.core.JmsTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

public class BulkRefIdRequestSenderTest {
    @Mock
    private JmsTemplate jmsTemplate;
    private AutoCloseable closeable;
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));
    private BulkRefIdRequestSender sender = new BulkRefIdRequestSender();

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        sender.setJmsTemplate(jmsTemplate);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void sendToQueueTest() throws IOException {
        Map<String, String> map = new HashMap<>();
        map.put(UUID.randomUUID().toString(), "ref ID 1");
        var request = new BulkRefIdRequest();
        request.setAgent(agent);
        request.setRefIdMap(map);
        request.setEmail("user@email.com");

        sender.sendToQueue(request);
        verify(jmsTemplate).send(any());
    }
}

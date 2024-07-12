package edu.unc.lib.boxc.operations.jms.streaming;

import edu.unc.lib.boxc.auth.api.models.AgentPrincipals;
import edu.unc.lib.boxc.auth.fcrepo.models.AccessGroupSetImpl;
import edu.unc.lib.boxc.auth.fcrepo.models.AgentPrincipalsImpl;
import edu.unc.lib.boxc.model.api.StreamingConstants;
import edu.unc.lib.boxc.model.api.ids.PID;
import edu.unc.lib.boxc.model.fcrepo.ids.PIDs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.jms.core.JmsTemplate;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;

public class StreamingPropertiesRequestSenderTest {
    @Mock
    private JmsTemplate jmsTemplate;
    private StreamingPropertiesRequestSender streamingPropertiesRequestSender = new StreamingPropertiesRequestSender();
    private AutoCloseable closeable;
    private final AgentPrincipals agent = new AgentPrincipalsImpl("user", new AccessGroupSetImpl("agroup"));

    @BeforeEach
    public void setup() {
        closeable = openMocks(this);
        streamingPropertiesRequestSender.setJmsTemplate(jmsTemplate);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    public void sendToQueueTest() throws IOException {
        var filePidString = makePid().toString();
        var request = new StreamingPropertiesRequest();
        request.setAgent(agent);
        request.setId(filePidString);
        request.setUrl(StreamingConstants.STREAMREAPER_PREFIX_URL + "?params=more");
        request.setAction("add");
        request.setType("video");

        streamingPropertiesRequestSender.sendToQueue(request);
        verify(jmsTemplate).send(any());
    }

    public static PID makePid() {
        return PIDs.get(UUID.randomUUID().toString());
    }
}
